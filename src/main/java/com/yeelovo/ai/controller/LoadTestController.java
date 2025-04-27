package com.yeelovo.ai.controller;

import com.yeelovo.ai.model.openai.ChatRequest;
import com.yeelovo.ai.service.OpenAiCompatibleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 负载测试控制器
 * 用于测试系统在高并发下的性能表现
 */
@RestController
@RequestMapping("/api/load-test")
@RequiredArgsConstructor
@Slf4j
public class LoadTestController {

    private final OpenAiCompatibleService openAiCompatibleService;
    
    // 使用注入的线程池，而不是默认的ForkJoinPool
    @Qualifier("streamTaskExecutor") 
    private final Executor streamTaskExecutor;
    
    // 专用于处理测试任务的线程池
    @Qualifier("blockingTaskExecutor")
    private final Executor blockingTaskExecutor;
    
    // 用于记录测试会话
    private final Map<String, List<SseEmitter>> testSessions = new ConcurrentHashMap<>();
    
    // 计数器
    private final AtomicInteger successCounter = new AtomicInteger(0);
    private final AtomicInteger failureCounter = new AtomicInteger(0);
    
    /**
     * 开始负载测试
     * @param concurrentUsers 并发用户数
     * @param prompt 提示词
     * @return 测试会话ID
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, String>> startLoadTest(
            @RequestParam(defaultValue = "10") int concurrentUsers,
            @RequestParam(defaultValue = "你好，请介绍一下自己") String prompt) {
        
        // 安全检查：限制最大并发用户数
        if (concurrentUsers > 100) {
            concurrentUsers = 100;
        }
        
        // 生成测试会话ID
        String sessionId = "test-" + System.currentTimeMillis();
        
        // 重置计数器
        successCounter.set(0);
        failureCounter.set(0);
        
        // 创建存储该会话的SSE发射器列表
        List<SseEmitter> emitters = Collections.synchronizedList(new ArrayList<>());
        testSessions.put(sessionId, emitters);
        
        log.info("开始负载测试，会话ID: {}, 并发用户数: {}", sessionId, concurrentUsers);
        
        // 使用注入的线程池执行测试，而不是默认的ForkJoinPool
        final int finalConcurrentUsers = concurrentUsers;
        blockingTaskExecutor.execute(() -> {
            try {
                // 创建请求对象
                ChatRequest request = createChatRequest(prompt);
                
                // 启动多个并发任务
                List<CompletableFuture<Void>> futures = new ArrayList<>();
                
                for (int i = 0; i < finalConcurrentUsers; i++) {
                    CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
                        try {
                            // 调用流式接口
                            SseEmitter emitter = openAiCompatibleService.chatStream(request);
                            emitters.add(emitter);
                            
                            // 记录成功数
                            successCounter.incrementAndGet();
                            return emitter;
                        } catch (Exception e) {
                            // 记录失败数
                            failureCounter.incrementAndGet();
                            log.error("负载测试任务失败: {}", e.getMessage());
                            throw new RuntimeException(e);
                        }
                    }, streamTaskExecutor).thenApply(result -> null);
                    
                    futures.add(future);
                }
                
                // 等待所有任务完成
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                
                log.info("负载测试完成，会话ID: {}, 成功: {}, 失败: {}", 
                        sessionId, successCounter.get(), failureCounter.get());
                
            } catch (Exception e) {
                log.error("负载测试执行失败", e);
            }
        });
        
        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "message", "负载测试已启动，并发用户数: " + concurrentUsers
        ));
    }
    
    /**
     * 获取测试状态
     */
    @GetMapping("/status/{sessionId}")
    public ResponseEntity<Map<String, Object>> getTestStatus(@PathVariable String sessionId) {
        // 检查会话是否存在
        if (!testSessions.containsKey(sessionId)) {
            return ResponseEntity.notFound().build();
        }
        
        List<SseEmitter> emitters = testSessions.get(sessionId);
        
        // 统计结果
        int total = emitters.size();
        int success = successCounter.get();
        int failure = failureCounter.get();
        int inProgress = total - (success + failure);
        
        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "total", total,
                "success", success,
                "failure", failure,
                "inProgress", inProgress
        ));
    }
    
    /**
     * 创建一个聊天请求
     */
    private ChatRequest createChatRequest(String prompt) {
        List<ChatRequest.Message> messages = new ArrayList<>();
        messages.add(new ChatRequest.Message("user", prompt));
        
        return ChatRequest.builder()
                .model("gpt-4o")
                .messages(messages)
                .stream(true)
                .build();
    }
} 
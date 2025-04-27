package com.yeelovo.ai.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
public class ApiTestController {

    @Qualifier("openaiChatClient")
    private final ChatClient openaiChatClient;

    @GetMapping("/blocking")
    public String testBlocking(@RequestParam String prompt) {
        return openaiChatClient.prompt().user(prompt).call().content();
    }

    @GetMapping("/streaming")
    public SseEmitter testStreaming(@RequestParam String prompt) {
        // 设置不超时的SseEmitter
        SseEmitter emitter = new SseEmitter(0L);
        
        // 线程安全的列表，用于收集所有内容
        CopyOnWriteArrayList<String> contentBuffer = new CopyOnWriteArrayList<>();
        
        ExecutorService executor = Executors.newSingleThreadExecutor();

        // 设置回调处理
        emitter.onCompletion(() -> {
            log.info("客户端断开连接");
            executor.shutdown();
        });
        
        emitter.onTimeout(() -> {
            log.info("连接超时");
            executor.shutdown();
        });
        
        emitter.onError(error -> {
            log.error("SSE错误: {}", error.getMessage());
            executor.shutdown();
        });

        executor.execute(() -> {
            try {
                Flux<String> contentFlux = openaiChatClient.prompt().user(prompt).stream().content();

                contentFlux.subscribe(
                    // 处理每个数据项
                    content -> {
                        try {
                            // 添加到缓冲区
                            contentBuffer.add(content);
                            // 发送到客户端
                            emitter.send(content);
                            log.info("发送流式内容: {}", content);
                        } catch (IOException e) {
                            log.error("发送流式响应失败", e);
                        }
                    },
                    // 处理错误
                    error -> {
                        log.error("流处理错误", error);
                        try {
                            // 尝试发送已缓冲的内容
                            if (!contentBuffer.isEmpty()) {
                                String combinedContent = String.join("", contentBuffer);
                                emitter.send("错误，但已接收部分内容: " + combinedContent);
                            }
                            emitter.completeWithError(error);
                        } catch (IOException e) {
                            log.error("发送错误响应失败", e);
                        }
                        executor.shutdown();
                    },
                    // 处理完成
                    () -> {
                        try {
                            // 发送最终的结束标记
                            emitter.send("[完成]");
                            log.info("流式响应完成");
                            emitter.complete();
                        } catch (IOException e) {
                            log.error("发送完成响应失败", e);
                        }
                        executor.shutdown();
                    }
                );

            } catch (Exception e) {
                log.error("流式调用异常", e);
                emitter.completeWithError(e);
                executor.shutdown();
            }
        });

        return emitter;
    }
} 
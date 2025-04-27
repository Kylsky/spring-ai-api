package com.yeelovo.ai.service.impl;

import com.yeelovo.ai.model.openai.ChatRequest;
import com.yeelovo.ai.model.openai.ChatResponse;
import com.yeelovo.ai.model.openai.ChatStreamResponse;
import com.yeelovo.ai.service.OpenAiCompatibleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAiCompatibleServiceImpl implements OpenAiCompatibleService {
    @Qualifier("openAiChatClient")
    private final ChatClient openaiChatClient;

    // 注入配置的线程池，而不是在服务中创建
    @Qualifier("streamTaskExecutor")
    private final Executor streamTaskExecutor;

    @Qualifier("blockingTaskExecutor")
    private final Executor blockingTaskExecutor;

    // 当前活跃连接计数
    private final AtomicInteger activeConnections = new AtomicInteger(0);

    // 最大并发连接数
    private static final int MAX_CONCURRENT_CONNECTIONS = 100;

    @Override
    public ChatResponse chat(ChatRequest request) {
        try {
            List<Message> messages = createMessages(request);

            // 阻塞式调用
            String content = openaiChatClient
                                     .prompt()
                                     .messages(messages)
                                     //.user(request.getQuery())
                                     .options(OpenAiChatOptions.builder().model(request.getModel()).build())
                                     .call()
                                     .content();

            return createChatResponse(content, request.getModel());
        } catch (Exception e) {
            log.error("阻塞调用异常", e);
            throw new RuntimeException("调用AI服务失败", e);
        }
    }

    @Override
    public SseEmitter chatStream(ChatRequest request) {
        // 高并发限流控制
        if (activeConnections.get() >= MAX_CONCURRENT_CONNECTIONS) {
            throw new RuntimeException("服务器当前负载过高，请稍后再试");
        }

        // 增加活跃连接计数
        activeConnections.incrementAndGet();

        // 设置不超时的SseEmitter
        SseEmitter emitter = new SseEmitter(0L);

        // 使用线程安全的列表收集所有内容，防止数据丢失
        CopyOnWriteArrayList<String> contentBuffer = new CopyOnWriteArrayList<>();

        // 使用注入的线程池处理请求，而不是自己创建
        streamTaskExecutor.execute(() -> {
            try {
                List<Message> messages = createMessages(request);

                // 流式调用
                // 使用Reactor的调度器管理线程
                Flux<String> contentFlux = openaiChatClient.prompt()
                                                   .messages(messages)
                                                   //.user(request.getQuery())
                                                   .options(OpenAiChatOptions.builder().model(request.getModel()).build())
                                                   .stream()
                                                   .content()
                                                   .publishOn(Schedulers.boundedElastic()); // 使用有边界的弹性线程池

                // 设置客户端断开连接时的回调
                emitter.onCompletion(() -> {
                    log.info("客户端断开连接");
                    decrementConnection();
                });

                emitter.onTimeout(() -> {
                    log.info("连接超时");
                    decrementConnection();
                });

                emitter.onError(error -> {
                    log.error("SSE错误", error);
                    decrementConnection();
                });

                // 订阅Flux流
                contentFlux.subscribe(
                        // 处理每个数据项
                        content -> {
                            try {
                                // 将内容添加到缓冲区
                                contentBuffer.add(content);

                                // 创建流式响应并发送
                                ChatStreamResponse streamResponse = createStreamResponse(content, request.getModel(), false);
                                emitter.send(streamResponse);
                            } catch (IOException e) {
                                log.error("发送流式响应失败", e);
                            }
                        },
                        // 处理错误
                        error -> {
                            log.error("流处理错误", error);
                            try {
                                // 即使发生错误，也尝试发送已缓冲的内容
                                if (!contentBuffer.isEmpty()) {
                                    String combinedContent = String.join("", contentBuffer);
                                    ChatStreamResponse finalResponse = createStreamResponse(combinedContent, request.getModel(), true);
                                    emitter.send(finalResponse);
                                }
                                emitter.completeWithError(error);
                            } catch (IOException e) {
                                log.error("发送错误响应失败", e);
                            }
                            decrementConnection();
                        },
                        // 处理完成
                        () -> {
                            try {
                                // 发送一个最终的标记完成的响应
                                ChatStreamResponse finalResponse = createStreamResponse("", request.getModel(), true);
                                emitter.send(finalResponse);
                                log.info("流式响应完成");
                                emitter.complete();
                            } catch (IOException e) {
                                log.error("发送完成响应失败", e);
                            }
                            decrementConnection();
                        }
                );

            } catch (Exception e) {
                log.error("流式调用异常", e);
                emitter.completeWithError(e);
                decrementConnection();
            }
        });

        return emitter;
    }

    // 减少活跃连接计数
    private void decrementConnection() {
        activeConnections.decrementAndGet();
    }

    private List<Message> createMessages(ChatRequest request) {
        List<Message> messages = new ArrayList<>();

        for (ChatRequest.Message msg : request.getMessages()) {
            switch (msg.getRole()) {
                case "system" -> messages.add(new SystemMessage(msg.getContent()));
                case "user" -> messages.add(new UserMessage(msg.getContent()));
                case "assistant" -> messages.add(new AssistantMessage(msg.getContent()));
                default -> log.warn("未知角色类型: {}", msg.getRole());
            }
        }

        return messages;
    }



    private ChatResponse createChatResponse(String content, String model) {
        ChatResponse.Message message = ChatResponse.Message.builder()
                                               .role("assistant")
                                               .content(content)
                                               .build();

        ChatResponse.Choice choice = ChatResponse.Choice.builder()
                                             .index(0)
                                             .message(message)
                                             .finish_reason("stop")
                                             .build();

        List<ChatResponse.Choice> choices = new ArrayList<>();
        choices.add(choice);

        ChatResponse.Usage usage = ChatResponse.Usage.builder()
                                           .prompt_tokens(0)
                                           .completion_tokens(0)
                                           .total_tokens(0)
                                           .build();

        return ChatResponse.builder()
                       .id("chatcmpl-" + UUID.randomUUID().toString().replace("-", ""))
                       .object("chat.completion")
                       .created(System.currentTimeMillis() / 1000)
                       .model(model)
                       .choices(choices)
                       .usage(usage)
                       .build();
    }

    private ChatStreamResponse createStreamResponse(String content, String model, boolean isDone) {
        ChatStreamResponse.Delta delta = ChatStreamResponse.Delta.builder()
                                                 .content(content)
                                                 .build();

        ChatStreamResponse.Choice choice = ChatStreamResponse.Choice.builder()
                                                   .index(0)
                                                   .delta(delta)
                                                   .finish_reason(isDone ? "stop" : null)
                                                   .build();

        List<ChatStreamResponse.Choice> choices = new ArrayList<>();
        choices.add(choice);

        return ChatStreamResponse.builder()
                       .id("chatcmpl-" + UUID.randomUUID().toString().replace("-", ""))
                       .object("chat.completion.chunk")
                       .created(System.currentTimeMillis() / 1000)
                       .model(model)
                       .choices(choices)
                       .build();
    }
} 
package com.yeelovo.ai.controller;

import com.yeelovo.ai.model.openai.ChatRequest;
import com.yeelovo.ai.model.openai.ChatResponse;
import com.yeelovo.ai.service.OpenAiCompatibleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class OpenAiCompatibleController {

    private final OpenAiCompatibleService openAiCompatibleService;

    /**
     * 聊天补全API - 阻塞式或流式（根据请求中的stream参数）
     */
    @PostMapping("/chat/completions")
    public Object chatCompletions(@RequestBody ChatRequest request) {
        if (Boolean.TRUE.equals(request.getStream())) {
            return chatCompletionsStream(request);
        } else {
            return chatCompletionsBlocking(request);
        }
    }

    /**
     * 阻塞式调用
     */
    private ChatResponse chatCompletionsBlocking(ChatRequest request) {
        return openAiCompatibleService.chat(request);
    }

    /**
     * 流式调用
     */
    @PostMapping(value = "/chat/completions/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatCompletionsStream(@RequestBody ChatRequest request) {
        // 强制设置为流式
        request.setStream(true);
        return openAiCompatibleService.chatStream(request);
    }
} 
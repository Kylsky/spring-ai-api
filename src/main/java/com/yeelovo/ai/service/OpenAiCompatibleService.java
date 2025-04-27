package com.yeelovo.ai.service;

import com.yeelovo.ai.model.openai.ChatRequest;
import com.yeelovo.ai.model.openai.ChatResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface OpenAiCompatibleService {
    /**
     * 阻塞式调用
     * @param request 聊天请求
     * @return 聊天响应
     */
    ChatResponse chat(ChatRequest request);
    
    /**
     * 流式调用
     * @param request 聊天请求
     * @return SSE流发射器
     */
    SseEmitter chatStream(ChatRequest request);
} 
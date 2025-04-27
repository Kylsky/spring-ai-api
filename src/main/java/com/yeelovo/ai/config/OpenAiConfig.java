package com.yeelovo.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class OpenAiConfig {
    @Autowired
    private OpenAiChatModel openAiChatModel;

    @Bean
    public ChatClient openAiChatClient() {
        ChatClient.Builder builder = ChatClient.builder(openAiChatModel);

        return builder.build();
    }
}

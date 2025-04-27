package com.yeelovo.ai.config;

import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OpenAiConfig {
    @Autowired
    private OpenAiChatModel openAiChatModel;
    @Autowired
    private ToolCallbackProvider toolCallbackProvider;

    @Bean
    public ChatClient openAiChatClient() {
        ChatClient.Builder builder = ChatClient.builder(openAiChatModel);
        builder.defaultTools(toolCallbackProvider.getToolCallbacks());
        builder.defaultAdvisors(new MessageChatMemoryAdvisor(new InMemoryChatMemory()));
        return builder.build();
    }

}

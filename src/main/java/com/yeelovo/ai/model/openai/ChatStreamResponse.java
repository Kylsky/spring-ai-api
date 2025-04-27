package com.yeelovo.ai.model.openai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatStreamResponse {
    private String id;
    private String object;
    private long created;
    private String model;
    private List<Choice> choices;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Choice {
        private Integer index;
        private Delta delta;
        private String finish_reason;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Delta {
        private String role;
        private String content;
    }
} 
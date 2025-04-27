package com.yeelovo.ai.model.openai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private String model;
    private List<Message> messages;
    private Double temperature;
    private Integer max_tokens;
    private Boolean stream;
    private Integer n;
    private Map<String, Object> options;
    
    public Boolean getStream() {
        return stream;
    }
    
    public void setStream(Boolean stream) {
        this.stream = stream;
    }
    
    public String getModel() {
        if (model == null) {
            return "gpt-4o";
        }
        return model;
    }
    
    public List<Message> getMessages() {
        return messages;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String role;
        private String content;
    }
} 
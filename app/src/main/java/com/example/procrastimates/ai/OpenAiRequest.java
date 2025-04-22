package com.example.procrastimates.ai;

import java.util.List;

public class OpenAiRequest {
    private String model;
    private List<Message> messages;
    private int max_tokens;

    public OpenAiRequest(String model, List<Message> messages, int max_tokens) {
        this.model = model;
        this.messages = messages;
        this.max_tokens = max_tokens;
    }

    public static class Message {
        private String role;
        private String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public String getContent() {
            return content;
        }
    }

    public String getModel() {
        return model;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public int getMax_tokens() {
        return max_tokens;
    }
}



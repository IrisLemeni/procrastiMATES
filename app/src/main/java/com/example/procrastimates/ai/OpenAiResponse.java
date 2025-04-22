package com.example.procrastimates.ai;

import java.util.List;

public class OpenAiResponse {
    public List<Choice> choices;

    public static class Choice {
        public Message message;
    }

    public static class Message {
        public String role;
        public String content;

        public String getRole() {
            return role;
        }

        public String getContent() {
            return content;
        }
    }
}


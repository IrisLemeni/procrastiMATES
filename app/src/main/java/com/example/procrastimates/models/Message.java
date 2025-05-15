package com.example.procrastimates.models;

import com.example.procrastimates.MessageType;
import com.google.firebase.Timestamp;

import java.util.UUID;

public class Message {
    private String messageId;
    private String circleId;
    private String senderId;
    private String text;
    private MessageType type;
    private String taskId; // Optional, for task-related messages
    private Timestamp timestamp;

    // Constructor for task-related messages
    public Message(String circleId, String senderId, MessageType type, String taskId) {
        this.messageId = UUID.randomUUID().toString();
        this.circleId = circleId;
        this.senderId = senderId;
        this.type = type;
        this.taskId = taskId;
        this.timestamp = Timestamp.now();
    }

    public Message() {
    }

    // Getters and setters

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getCircleId() {
        return circleId;
    }

    public void setCircleId(String circleId) {
        this.circleId = circleId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
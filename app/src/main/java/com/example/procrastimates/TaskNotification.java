package com.example.procrastimates;

import com.google.firebase.Timestamp;

public class TaskNotification {
    private String id;
    private String userId;
    private String message;
    private Timestamp createdAt;
    private NotificationType type;
    private boolean read;
    private String sourceUserId;
    private String taskId;

    public TaskNotification() {
        // Required empty constructor for Firestore
    }

    public TaskNotification(String id, String userId, String message, Timestamp createdAt,
                            NotificationType type, boolean read, String sourceUserId, String taskId) {
        this.id = id;
        this.userId = userId;
        this.message = message;
        this.createdAt = createdAt;
        this.type = type;
        this.read = read;
        this.sourceUserId = sourceUserId;
        this.taskId = taskId;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public NotificationType getType() {
        return type;
    }

    // For Firestore compatibility
    public String getTypeString() {
        return type != null ? type.name() : null;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    // For Firestore compatibility
    public void setTypeString(String typeStr) {
        if (typeStr != null) {
            try {
                this.type = NotificationType.valueOf(typeStr);
            } catch (IllegalArgumentException e) {
                this.type = NotificationType.TASK_COMPLETED; // Default value
            }
        }
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public String getSourceUserId() {
        return sourceUserId;
    }

    public void setSourceUserId(String sourceUserId) {
        this.sourceUserId = sourceUserId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
}
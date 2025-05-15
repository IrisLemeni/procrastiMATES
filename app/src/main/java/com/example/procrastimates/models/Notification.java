package com.example.procrastimates.models;

import com.example.procrastimates.NotificationType;
import com.google.firebase.Timestamp;

import java.util.UUID;

public class Notification {
    private String notificationId;
    private String userId; // Recipient
    private String title;
    private String body;
    private String circleId;
    private String taskId;
    private NotificationType type;
    private boolean isRead;
    private Timestamp createdAt;

    // Constructor
    public Notification(String userId, String title, String body, String circleId, String taskId, NotificationType type) {
        this.notificationId = UUID.randomUUID().toString();
        this.userId = userId;
        this.title = title;
        this.body = body;
        this.circleId = circleId;
        this.taskId = taskId;
        this.type = type;
        this.isRead = false;
        this.createdAt = Timestamp.now();
    }

    public Notification() {

    }

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getCircleId() {
        return circleId;
    }

    public void setCircleId(String circleId) {
        this.circleId = circleId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }
}
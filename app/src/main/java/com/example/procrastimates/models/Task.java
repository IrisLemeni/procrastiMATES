package com.example.procrastimates.models;

import com.example.procrastimates.Priority;
import com.google.firebase.Timestamp;


public class Task {
    private String taskId;
    private String title;
    private boolean isCompleted;
    private Timestamp dueDate;
    private String userId;
    private Priority priority;
    private String circleId;
    private Timestamp createdAt;
    private Timestamp completedAt;

    public Task() {}

    public Task(String taskId, String title,  boolean isCompleted, Timestamp dueDate, String userId, Priority priority, String circleId, Timestamp createdAt, Timestamp completedAt) {
        this.taskId = taskId;
        this.title = title;
        this.isCompleted = isCompleted;
        this.dueDate = dueDate;
        this.userId = userId;
        this.priority = priority;
        this.circleId = circleId;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public Timestamp getDueDate() {
        return dueDate;
    }

    public void setDueDate(Timestamp dueDate) {
        this.dueDate = dueDate;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public String getCircleId() {
        return circleId;
    }

    public void setCircleId(String circleId) {
        this.circleId = circleId;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Timestamp completedAt) {
        this.completedAt = completedAt;
    }
}

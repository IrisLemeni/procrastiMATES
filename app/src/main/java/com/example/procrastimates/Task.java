package com.example.procrastimates;

import com.google.firebase.Timestamp;

import java.util.Date;

public class Task {
    private String taskId;
    private String title;
    private boolean isCompleted;
    private Timestamp dueDate;
    private String userId;
    private Priority priority;

    public Task() {}

    public Task(String taskId, String title,  boolean isCompleted, Timestamp dueDate, String userId, Priority priority) {
        this.taskId = taskId;
        this.title = title;
        this.isCompleted = isCompleted;
        this.dueDate = dueDate;
        this.userId = userId;
        this.priority = priority;
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

}

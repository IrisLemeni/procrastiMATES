package com.example.procrastimates;

public class Task {
    private String taskId;
    private String title;
    private boolean isCompleted;
    private long dueDate;
    private String userId;

    public Task() {}

    public Task(String taskId, String title,  boolean isCompleted, long dueDate, String userId) {
        this.taskId = taskId;
        this.title = title;
        this.isCompleted = isCompleted;
        this.dueDate = dueDate;
        this.userId = userId;
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

    public long getDueDate() {
        return dueDate;
    }

    public void setDueDate(long dueDate) {
        this.dueDate = dueDate;
    }

}

package com.example.procrastimates;

import java.util.Date;

public class Task {
    private String taskId;
    private String title;
    private boolean isCompleted;
    private Date dueDate;
    private String userId;
    private Priority priority;

    public Task() {}

    public Task(String taskId, String title,  boolean isCompleted, Date dueDate, String userId, Priority priority) {
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

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

}

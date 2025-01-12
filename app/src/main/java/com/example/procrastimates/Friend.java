package com.example.procrastimates;

public class Friend {
    private String name;
    private int taskProgress;

    public Friend(String name, int taskProgress) {
        this.name = name;
        this.taskProgress = taskProgress;
    }

    public String getName() {
        return name;
    }

    public int getTaskProgress() {
        return taskProgress;
    }
}


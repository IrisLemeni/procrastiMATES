package com.example.procrastimates.models;

public class Achievement {
    private String id;
    private String title;
    private String description;
    private String iconUrl;
    private int targetCount;
    private boolean unlocked;

    public Achievement(String id, String title, String description, String iconUrl, int targetCount) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.iconUrl = iconUrl;
        this.targetCount = targetCount;
        this.unlocked = false;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public int getTargetCount() {
        return targetCount;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
    }
}
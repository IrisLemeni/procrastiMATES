package com.example.procrastimates;

public class Achievement {
    private String id;
    private String title;
    private String description;
    private String iconResource;
    private int threshold;
    private int xpReward;

    public Achievement(String id, String title, String description,
                       String iconResource, int threshold, int xpReward) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.iconResource = iconResource;
        this.threshold = threshold;
        this.xpReward = xpReward;
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

    public String getIconResource() {
        return iconResource;
    }

    public int getThreshold() {
        return threshold;
    }

    public int getXpReward() {
        return xpReward;
    }
}
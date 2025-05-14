package com.example.procrastimates;
public class Friend {
    private String friendId;
    private String name;
    private int completedTasks;
    private int totalTasks;
    private String profileImageUrl;

    public Friend(String friendId, String name, int completedTasks, int totalTasks) {
        this.name = name;
        this.completedTasks = completedTasks;
        this.totalTasks = totalTasks;
    }

    public String getFriendId() {
        return friendId;
    }

    public void setFriendId(String friendId) {
        this.friendId = friendId;
    }

    public void setCompletedTasks(int completedTasks) {
        this.completedTasks = completedTasks;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTotalTasks(int totalTasks) {
        this.totalTasks = totalTasks;
    }

    public String getName() {
        return name;
    }

    public int getCompletedTasks() {
        return completedTasks;
    }

    public int getTotalTasks() {
        return totalTasks;
    }

    public int getProgress() {
        return totalTasks == 0 ? 0 : (completedTasks * 100) / totalTasks;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
}

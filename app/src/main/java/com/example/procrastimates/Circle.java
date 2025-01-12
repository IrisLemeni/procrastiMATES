package com.example.procrastimates;

import java.util.ArrayList;
import java.util.List;

public class Circle {
    private String userId;
    private List<String> members;

    public Circle() {
    }

    public Circle(String userId) {
        this.userId = userId;
        this.members = new ArrayList<>();
        this.members.add(userId); // Add the user as the first member
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }

    public void addMember(String userId) {
        if (!members.contains(userId)) {
            members.add(userId);
        }
    }
}


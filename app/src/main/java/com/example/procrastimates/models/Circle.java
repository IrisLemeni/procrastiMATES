package com.example.procrastimates.models;

import java.util.ArrayList;
import java.util.List;

public class Circle {
    private String circleId;
    private String userId;  // User that created the circle
    private List<String> members;

    // Constructor pentru a crea un cerc cu un userId
    public Circle(String circleId, String userId) {
        this.circleId = circleId;
        this.userId = userId;
        this.members = new ArrayList<>();
        this.members.add(userId); // Add the user as the first member
    }

    // Constructor fără circleId pentru a crea un obiect nou
    public Circle() {
        this.members = new ArrayList<>();
    }

    // Getters și Setters
    public String getCircleId() {
        return circleId;
    }

    public void setCircleId(String circleId) {
        this.circleId = circleId;
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
}

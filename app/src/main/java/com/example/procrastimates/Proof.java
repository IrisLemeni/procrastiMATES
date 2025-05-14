package com.example.procrastimates;

import com.google.firebase.Timestamp;

import java.util.UUID;

public class Proof {
    private String proofId;
    private String taskId;
    private String submittedByUserId; // User who submitted proof
    private String imageUrl; // Firebase Storage URL
    private Timestamp createdAt;

    // Constructor
    public Proof(String taskId, String submittedByUserId, String imageUrl) {
        this.proofId = UUID.randomUUID().toString();
        this.taskId = taskId;
        this.submittedByUserId = submittedByUserId;
        this.imageUrl = imageUrl;
        this.createdAt = Timestamp.now();
    }

    public Proof() {

    }

    // Getters and setters

    public String getProofId() {
        return proofId;
    }

    public void setProofId(String proofId) {
        this.proofId = proofId;
    }

    public String getSubmittedByUserId() {
        return submittedByUserId;
    }

    public void setSubmittedByUserId(String submittedByUserId) {
        this.submittedByUserId = submittedByUserId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
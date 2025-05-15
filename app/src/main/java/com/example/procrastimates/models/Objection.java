package com.example.procrastimates.models;

import com.example.procrastimates.ObjectionStatus;
import com.google.firebase.Timestamp;

public class Objection {
    private String objectionId;
    private String taskId;
    private String taskTitle;
    private String targetUserId;  // User who completed the task being challenged
    private String objectorUserId;  // User who raised the objection
    private Timestamp createdAt;
    private ObjectionStatus status; // PENDING, VERIFIED, REJECTED
    private String proofImageUrl; // URL to the image uploaded as proof
    private String circleId;

    public Objection() {
        // Required empty constructor for Firestore
    }

    public Objection(String objectionId, String taskId, String taskTitle, String targetUserId,
                     String objectorUserId, Timestamp createdAt, String circleId) {
        this.objectionId = objectionId;
        this.taskId = taskId;
        this.taskTitle = taskTitle;
        this.targetUserId = targetUserId;
        this.objectorUserId = objectorUserId;
        this.createdAt = createdAt;
        this.status = ObjectionStatus.PENDING;
        this.circleId = circleId;
    }

    // Getters and Setters
    public String getObjectionId() {
        return objectionId;
    }

    public void setObjectionId(String objectionId) {
        this.objectionId = objectionId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(String targetUserId) {
        this.targetUserId = targetUserId;
    }

    public String getChallengerUserId() {
        return objectorUserId;
    }

    public void setChallengerUserId(String challengerUserId) {
        this.objectorUserId = challengerUserId;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public ObjectionStatus getStatus() {
        return status;
    }

    public void setStatus(ObjectionStatus status) {
        this.status = status;
    }

    public String getProofImageUrl() {
        return proofImageUrl;
    }

    public void setProofImageUrl(String proofImageUrl) {
        this.proofImageUrl = proofImageUrl;
    }

    public String getTaskTitle() {
        return taskTitle;
    }

    public void setTaskTitle(String taskTitle) {
        this.taskTitle = taskTitle;
    }

    public String getObjectorUserId() {
        return objectorUserId;
    }

    public void setObjectorUserId(String objectorUserId) {
        this.objectorUserId = objectorUserId;
    }

    public String getCircleId() {
        return circleId;
    }

    public void setCircleId(String circleId) {
        this.circleId = circleId;
    }
}



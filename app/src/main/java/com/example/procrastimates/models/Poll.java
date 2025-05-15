package com.example.procrastimates.models;

import com.example.procrastimates.PollStatus;
import com.google.firebase.Timestamp;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Poll {
    private String pollId;
    private String taskId;
    private String circleId;
    private Map<String, Boolean> votes; // Key: userId, Value: true=accept, false=reject
    private Timestamp endTime; // 12 hours after creation
    private PollStatus status;

    // Constructor
    public Poll(String taskId, String circleId) {
        this.pollId = UUID.randomUUID().toString();
        this.taskId = taskId;
        this.circleId = circleId;
        this.votes = new HashMap<>();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY, 12); // Adaugă 12 ore
        this.endTime = new Timestamp(calendar.getTime());

        this.status = PollStatus.ACTIVE;
    }

    public Poll() {

    }

    // Methods
    public void addVote(String userId, boolean isAccepted) {
        if (status == PollStatus.ACTIVE) {
            votes.put(userId, isAccepted);
        }
    }

    // Getters and setters

    public String getPollId() {
        return pollId;
    }

    public void setPollId(String pollId) {
        this.pollId = pollId;
    }

    public Map<String, Boolean> getVotes() {
        return votes;
    }

    public void setVotes(Map<String, Boolean> votes) {
        this.votes = votes;
    }

    public String getCircleId() {
        return circleId;
    }

    public void setCircleId(String circleId) {
        this.circleId = circleId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public Timestamp getEndTime() {
        return endTime;
    }

    public void setEndTime(Timestamp endTime) {
        this.endTime = endTime;
    }

    public PollStatus getStatus() {
        return status;
    }

    public void setStatus(PollStatus status) {
        this.status = status;
    }
}

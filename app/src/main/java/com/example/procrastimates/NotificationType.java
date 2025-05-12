package com.example.procrastimates;

public enum NotificationType {
    TASK_COMPLETED,        // Someone completed a task
    OBJECTION_RAISED,      // Someone objected to your task completion
    PROOF_REQUESTED,       // You need to provide proof for a task
    PROOF_PROVIDED,        // Someone provided proof for a task
    OBJECTION_LIMIT_REACHED // You've used your daily objection
}

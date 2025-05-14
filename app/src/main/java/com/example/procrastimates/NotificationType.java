package com.example.procrastimates;

public enum NotificationType {
    TASK_COMPLETED,        // When a user completes a task
    TASK_REJECTED,
    OBJECTION_RAISED,      // When a user objects to a completed task
    PROOF_SUBMITTED,       // When a user submits proof for an objected task
    POLL_STARTED,          // When a poll is started for an objection
    POLL_ENDED,            // When a poll ends
    CIRCLE_INVITATION,     // When a user is invited to a circle
    CIRCLE_JOIN,           // When a user joins a circle
    SYSTEM_MESSAGE         // General system notifications
}
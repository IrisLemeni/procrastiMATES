package com.example.procrastimates.fragments;

import com.example.procrastimates.Circle;
import com.example.procrastimates.Notification;
import com.example.procrastimates.NotificationSender;
import com.example.procrastimates.ObjectionStatus;
import com.example.procrastimates.Poll;
import com.example.procrastimates.PollStatus;
import com.example.procrastimates.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class PollProcessor {
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Find all active polls
    public void findActivePolls(Consumer<List<Poll>> callback) {
        db.collection("polls")
                .whereEqualTo("status", PollStatus.ACTIVE)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Poll> polls = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        Poll poll = document.toObject(Poll.class);
                        if (poll != null) {
                            polls.add(poll);
                        }
                    }
                    callback.accept(polls);
                });
    }

    // Get circle members for a given circle
    public void getCircleMembers(String circleId, Consumer<List<String>> callback) {
        db.collection("circles").document(circleId)
                .get()
                .addOnSuccessListener(circleDoc -> {
                    List<String> members = new ArrayList<>();
                    if (circleDoc.exists()) {
                        Circle circle = circleDoc.toObject(Circle.class);
                        if (circle != null && circle.getMembers() != null) {
                            members.addAll(circle.getMembers());
                        }
                    }
                    callback.accept(members);
                });
    }

    // Close a poll and process its result
    public void closePoll(Poll poll) {
        db.collection("polls").document(poll.getPollId())
                .update("status", PollStatus.CLOSED)
                .addOnSuccessListener(aVoid -> {
                    processPollResult(poll);
                });
    }

    // Process the result of a closed poll
    private void processPollResult(Poll poll) {
        // Calculate the result
        int acceptVotes = 0;
        int rejectVotes = 0;

        if (poll.getVotes() != null) {
            for (Boolean vote : poll.getVotes().values()) {
                if (vote) {
                    acceptVotes++;
                } else {
                    rejectVotes++;
                }
            }
        }

        final boolean isAccepted = acceptVotes > rejectVotes;

        // Update the task based on the result
        db.collection("tasks").document(poll.getTaskId())
                .get()
                .addOnSuccessListener(taskDoc -> {
                    if (taskDoc.exists()) {
                        Task task = taskDoc.toObject(Task.class);
                        if (task != null) {
                            // Update task status
                            if (!isAccepted) {
                                db.collection("tasks").document(poll.getTaskId())
                                        .update("completed", false, "completedAt", null)
                                        .addOnSuccessListener(aVoid -> {
                                            // Send notification to the user who completed the task
                                            sendTaskRejectedNotification(task);
                                        });
                            } else {
                                // Resolve the objection as accepted
                                db.collection("objections")
                                        .whereEqualTo("taskId", task.getTaskId())
                                        .get()
                                        .addOnSuccessListener(objectionSnapshots -> {
                                            if (!objectionSnapshots.isEmpty()) {
                                                String objectionId = objectionSnapshots.getDocuments().get(0).getId();
                                                db.collection("objections").document(objectionId)
                                                        .update("status", ObjectionStatus.RESOLVED);
                                            }
                                        });
                            }
                        }
                    }
                });
    }

    // Send notification when a task is rejected
    private void sendTaskRejectedNotification(Task task) {
        Notification notification = new Notification();
        notification.setNotificationId(UUID.randomUUID().toString());
        notification.setUserId(task.getUserId());
        notification.setTitle("Task respins");
        notification.setBody("Dovada pentru task-ul \"" + task.getTitle() + "\" a fost respinsă de grup.");
        notification.setCircleId(task.getCircleId());
        notification.setTaskId(task.getTaskId());
        notification.setRead(false);
        notification.setCreatedAt(new Timestamp(new Date()));

        db.collection("notifications").document(notification.getNotificationId())
                .set(notification)
                .addOnSuccessListener(aVoid -> {
                    // Send push notification
                    NotificationSender.sendPushNotification(
                            task.getUserId(),
                            "Task respins",
                            "Dovada pentru task-ul \"" + task.getTitle() + "\" a fost respinsă de grup."
                    );
                });
    }
}

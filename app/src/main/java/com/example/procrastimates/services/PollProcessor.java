package com.example.procrastimates.services;

import com.example.procrastimates.enums.ObjectionStatus;
import com.example.procrastimates.enums.PollStatus;
import com.example.procrastimates.enums.NotificationType;
import com.example.procrastimates.models.Circle;
import com.example.procrastimates.models.Notification;
import com.example.procrastimates.models.Poll;
import com.example.procrastimates.models.Task;
import com.example.procrastimates.services.NotificationSender;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PollProcessor {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final long TIMEOUT = 30;

    // --- synchronous methods for Worker ---

    public List<Poll> findActivePollsSync()
            throws ExecutionException, InterruptedException, TimeoutException {
        return Tasks.await(
                db.collection("polls")
                        .whereEqualTo("status", PollStatus.ACTIVE)
                        .get(),
                TIMEOUT, TimeUnit.SECONDS
        ).toObjects(Poll.class);
    }

    public List<String> getCircleMembersSync(String circleId)
            throws ExecutionException, InterruptedException, TimeoutException {
        DocumentSnapshot doc = Tasks.await(
                db.collection("circles").document(circleId)
                        .get(),
                TIMEOUT, TimeUnit.SECONDS
        );
        List<String> members = new ArrayList<>();
        if (doc.exists()) {
            Circle circle = doc.toObject(Circle.class);
            if (circle != null && circle.getMembers() != null) {
                members.addAll(circle.getMembers());
            }
        }
        return members;
    }

    public void closePollSync(Poll poll)
            throws ExecutionException, InterruptedException, TimeoutException {
        // Mark poll closed
        Tasks.await(
                db.collection("polls").document(poll.getPollId())
                        .update("status", PollStatus.CLOSED),
                TIMEOUT, TimeUnit.SECONDS
        );
        // Process result
        processPollResultSync(poll);
    }

    private void processPollResultSync(Poll poll)
            throws ExecutionException, InterruptedException, TimeoutException {
        int accept = 0, reject = 0;
        if (poll.getVotes() != null) {
            for (Boolean v : poll.getVotes().values()) {
                if (v) accept++; else reject++;
            }
        }
        boolean accepted = accept > reject;

        // Fetch task
        DocumentSnapshot taskDoc = Tasks.await(
                db.collection("tasks").document(poll.getTaskId())
                        .get(),
                TIMEOUT, TimeUnit.SECONDS
        );
        if (!taskDoc.exists()) return;

        Task task = taskDoc.toObject(Task.class);
        if (task == null) return;

        if (!accepted) {
            // Revert completion
            Tasks.await(
                    db.collection("tasks").document(task.getTaskId())
                            .update("completed", false, "completedAt", null),
                    TIMEOUT, TimeUnit.SECONDS
            );
            sendRejectedNotificationSync(task);
        } else {
            // Resolve objection
            List<DocumentSnapshot> objs = Tasks.await(
                    db.collection("objections")
                            .whereEqualTo("taskId", task.getTaskId())
                            .get(),
                    TIMEOUT, TimeUnit.SECONDS
            ).getDocuments();
            if (!objs.isEmpty()) {
                String objId = objs.get(0).getId();
                Tasks.await(
                        db.collection("objections").document(objId)
                                .update("status", ObjectionStatus.RESOLVED),
                        TIMEOUT, TimeUnit.SECONDS
                );
            }
        }
    }

    private void sendRejectedNotificationSync(Task task)
            throws ExecutionException, InterruptedException, TimeoutException {
        Notification notif = new Notification();
        notif.setNotificationId(UUID.randomUUID().toString());
        notif.setUserId(task.getUserId());
        notif.setTitle("Task respins");
        notif.setBody("Dovada pentru task-ul \"" + task.getTitle() + "\" a fost respinsă de grup.");
        notif.setCircleId(task.getCircleId());
        notif.setTaskId(task.getTaskId());
        notif.setType(NotificationType.TASK_REJECTED);
        notif.setRead(false);
        notif.setCreatedAt(new Timestamp(new Date()));

        Tasks.await(
                db.collection("notifications").document(notif.getNotificationId())
                        .set(notif),
                TIMEOUT, TimeUnit.SECONDS
        );
        NotificationSender.sendPushNotification(
                task.getUserId(),
                "Task respins",
                notif.getBody(),
                task.getTaskId(),
                task.getCircleId(),
                NotificationType.TASK_REJECTED
        );
    }
}
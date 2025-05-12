package com.example.procrastimates;

import android.net.Uri;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class ObjectionRepository {
    private static ObjectionRepository instance;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private ObjectionRepository() {
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    public static synchronized ObjectionRepository getInstance() {
        if (instance == null) {
            instance = new ObjectionRepository();
        }
        return instance;
    }

    // Check if a user has already raised an objection today
    public void canUserRaiseObjection(String userId, OnObjectionActionListener listener) {
        // Get start of today
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Timestamp startOfDay = new Timestamp(calendar.getTime());

        // Get end of today
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        Timestamp endOfDay = new Timestamp(calendar.getTime());

        db.collection("objections")
                .whereEqualTo("objectorUserId", userId)
                .whereGreaterThanOrEqualTo("createdAt", startOfDay)
                .whereLessThanOrEqualTo("createdAt", endOfDay)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean canRaiseObjection = queryDocumentSnapshots.isEmpty();
                    listener.onSuccess(canRaiseObjection);
                })
                .addOnFailureListener(listener::onFailure);
    }

    // Get recently completed tasks from a circle (tasks completed in the last 5 minutes)
    public void getRecentlyCompletedTasks(String circleId, String currentUserId, OnObjectionActionListener listener) {
        // Get timestamp for 5 minutes ago
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, -5);
        Timestamp fiveMinutesAgo = new Timestamp(calendar.getTime());

        db.collection("tasks")
                .whereEqualTo("circleId", circleId)
                .whereEqualTo("isCompleted", true)
                .whereGreaterThanOrEqualTo("completedAt", fiveMinutesAgo)
                .whereNotEqualTo("userId", currentUserId) // Don't show user's own tasks
                .orderBy("completedAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<TaskWithUser> recentTasks = new ArrayList<>();

                    if (queryDocumentSnapshots.isEmpty()) {
                        listener.onSuccess(recentTasks);
                        return;
                    }

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String taskId = doc.getId();
                        String title = doc.getString("title");
                        String userId = doc.getString("userId");
                        Timestamp completedAt = doc.getTimestamp("completedAt");

                        // Get user details
                        db.collection("users").document(userId).get()
                                .addOnSuccessListener(userDoc -> {
                                    String username = userDoc.getString("username");

                                    TaskWithUser taskWithUser = new TaskWithUser(
                                            taskId, title, userId, username, completedAt
                                    );

                                    recentTasks.add(taskWithUser);

                                    if (recentTasks.size() == queryDocumentSnapshots.size()) {
                                        listener.onSuccess(recentTasks);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    recentTasks.add(new TaskWithUser(
                                            taskId, title, userId, "Unknown User", completedAt
                                    ));

                                    if (recentTasks.size() == queryDocumentSnapshots.size()) {
                                        listener.onSuccess(recentTasks);
                                    }
                                });
                    }
                })
                .addOnFailureListener(listener::onFailure);
    }

    // Raise an objection for a completed task
    public void raiseObjection(String taskId, String taskTitle, String targetUserId,
                               String objectorUserId, String circleId, OnObjectionActionListener listener) {
        DocumentReference newObjRef = db.collection("objections").document();
        String objectionId = newObjRef.getId();

        Objection objection = new Objection(
                objectionId, taskId, taskTitle, targetUserId,
                objectorUserId, new Timestamp(new Date()), circleId
        );

        newObjRef.set(objection)
                .addOnSuccessListener(aVoid -> listener.onSuccess(objection))
                .addOnFailureListener(listener::onFailure);
    }

    // Upload proof image for an objection
    public void uploadProofImage(Uri imageUri, String objectionId, OnObjectionActionListener listener) {
        String imageName = "proof_" + UUID.randomUUID().toString();
        StorageReference imageRef = storage.getReference().child("proof_images").child(imageName);

        UploadTask uploadTask = imageRef.putFile(imageUri);
        uploadTask.continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            return imageRef.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Uri downloadUri = task.getResult();
                updateObjectionWithProof(objectionId, downloadUri.toString(), listener);
            } else {
                listener.onFailure(task.getException());
            }
        });
    }

    // Update objection with proof image URL
    private void updateObjectionWithProof(String objectionId, String imageUrl, OnObjectionActionListener listener) {
        db.collection("objections").document(objectionId)
                .update(
                        "proofImageUrl", imageUrl,
                        "status", ObjectionStatus.RESOLVED.name()
                )
                .addOnSuccessListener(aVoid -> listener.onSuccess(true))
                .addOnFailureListener(listener::onFailure);
    }

    // Get objections for a user (either raised against them or by them)
    public void getObjectionsForUser(String userId, boolean isTarget, OnObjectionActionListener listener) {
        String field = isTarget ? "targetUserId" : "objectorUserId";

        db.collection("objections")
                .whereEqualTo(field, userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Objection> objections = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Objection objection = doc.toObject(Objection.class);
                        objections.add(objection);
                    }
                    listener.onSuccess(objections);
                })
                .addOnFailureListener(listener::onFailure);
    }

    // Get objections for a circle
    public void getObjectionsForCircle(String circleId, OnObjectionActionListener listener) {
        db.collection("objections")
                .whereEqualTo("circleId", circleId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Objection> objections = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Objection objection = doc.toObject(Objection.class);
                        objections.add(objection);
                    }
                    listener.onSuccess(objections);
                })
                .addOnFailureListener(listener::onFailure);
    }

    // Helper class for tasks with user info
    public static class TaskWithUser {
        private String taskId;
        private String title;
        private String userId;
        private String username;
        private Timestamp completedAt;

        public TaskWithUser(String taskId, String title, String userId, String username, Timestamp completedAt) {
            this.taskId = taskId;
            this.title = title;
            this.userId = userId;
            this.username = username;
            this.completedAt = completedAt;
        }

        public String getTaskId() {
            return taskId;
        }

        public String getTitle() {
            return title;
        }

        public String getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        public Timestamp getCompletedAt() {
            return completedAt;
        }

        // Get time remaining for objection (in seconds)
        public int getTimeRemainingSeconds() {
            long completedTimeMs = completedAt.toDate().getTime();
            long currentTimeMs = System.currentTimeMillis();
            long timeSinceCompletionMs = currentTimeMs - completedTimeMs;
            long timeRemainingMs = (5 * 60 * 1000) - timeSinceCompletionMs;

            return (int) (timeRemainingMs / 1000);
        }
    }

    public interface OnObjectionActionListener {
        void onSuccess(Object result);
        void onFailure(Exception e);
    }
}
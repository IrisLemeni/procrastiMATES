package com.example.procrastimates.repositories;

import android.util.Log;

import com.example.procrastimates.services.NotificationSender;
import com.example.procrastimates.enums.NotificationType;
import com.example.procrastimates.models.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TaskRepository {
    private static TaskRepository instance;
    private FirebaseFirestore db;

    public TaskRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized TaskRepository getInstance() {
        if (instance == null) {
            instance = new TaskRepository();
        }
        return instance;
    }

    public void addTask(Task task, String userId, OnTaskActionListener listener) {
        if (userId == null || userId.isEmpty()) {
            listener.onFailure(new IllegalArgumentException("User ID is missing"));
            return;
        }

        CollectionReference tasksRef = db.collection("tasks");

        DocumentReference newTaskRef = tasksRef.document();
        task.setTaskId(newTaskRef.getId());
        task.setUserId(userId);
        task.setCreatedAt(new Timestamp(new Date()));

        newTaskRef.set(task)
                .addOnSuccessListener(aVoid -> {
                    listener.onSuccess(task);
                })
                .addOnFailureListener(e -> {
                    listener.onFailure(e);
                });
    }

    public void updateTask(String taskId, Task task, OnTaskActionListener listener) {
        if (taskId == null || task == null) {
            listener.onFailure(new IllegalArgumentException("Invalid task or taskId"));
            return;
        }

        DocumentReference taskRef = db.collection("tasks").document(taskId);

        taskRef.set(task)
                .addOnSuccessListener(aVoid -> listener.onSuccess(null)) // Niciun rezultat, doar succes
                .addOnFailureListener(e -> listener.onFailure(e));
    }

    public void completeTask(String taskId, String userId, String circleId, OnTaskActionListener listener) {
        DocumentReference taskRef = db.collection("tasks").document(taskId);
        taskRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    Task task = documentSnapshot.toObject(Task.class);
                    if (task != null) {
                        task.setCompleted(true);
                        task.setCompletedAt(new Timestamp(new Date()));
                        taskRef.set(task)
                                .addOnSuccessListener(aVoid -> {
                                    // Notify circle members about the completed task
                                    notifyCircleMembers(userId, circleId, task);
                                    listener.onSuccess(task);
                                })
                                .addOnFailureListener(e -> listener.onFailure(e));
                    } else {
                        listener.onFailure(new Exception("Task not found"));
                    }
                })
                .addOnFailureListener(e -> listener.onFailure(e));
    }

    private void notifyCircleMembers(String userId, String circleId, Task task) {
        // This method would typically send Firebase Cloud Messages to circle members
        // For simplicity, I'm just adding notifications to their Firestore collection

        db.collection("circles").document(circleId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> members = (List<String>) documentSnapshot.get("members");
                        if (members != null) {
                            for (String memberId : members) {
                                if (!memberId.equals(userId)) {
                                    // Create notification for this member
                                    String notificationId = db.collection("notifications").document().getId();

                                    db.collection("users").document(userId).get()
                                            .addOnSuccessListener(userDoc -> {
                                                String username = userDoc.getString("username");
                                                String message = username + " completed task: " + task.getTitle();

                                                NotificationSender.sendPushNotification(
                                                        memberId,
                                                        "Task Completed",
                                                        message,
                                                        task.getTaskId(),
                                                        task.getCircleId(),
                                                        NotificationType.TASK_COMPLETED
                                                );
                                            });
                                }
                            }
                        }
                    }
                });
    }

    public void deleteTask(String taskId, OnTaskActionListener listener) {
        db.collection("tasks")
                .document(taskId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    listener.onSuccess("Task deleted successfully");
                })
                .addOnFailureListener(e -> listener.onFailure(e));
    }

    public void getUserTasks(String userId, OnTaskActionListener listener) {
        db.collection("tasks")
                .whereEqualTo("userId", userId)  // Filtrează după userId
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        List<Task> tasks = new ArrayList<>();
                        for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            Task task = documentSnapshot.toObject(Task.class);
                            tasks.add(task);
                        }
                        listener.onSuccess(tasks);
                    } else {
                        listener.onSuccess(new ArrayList<>()); // Nu sunt task-uri
                        System.out.println("No tasks found for user: " + userId); // Log informativ
                    }
                })
                .addOnFailureListener(e -> listener.onFailure(e));
    }

    public void getUserTasksForToday(String userId, Timestamp startOfDay, Timestamp endOfDay, OnTaskActionListener listener) {
        db.collection("tasks")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("dueDate", startOfDay)
                .whereLessThanOrEqualTo("dueDate", endOfDay)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Task> tasks = new ArrayList<>();
                    for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        Task task = documentSnapshot.toObject(Task.class);
                        tasks.add(task);
                    }
                    listener.onSuccess(tasks);
                })
                .addOnFailureListener(listener::onFailure);
    }

    public Task getTaskById(String taskId) {
        try {
            DocumentSnapshot doc = db.collection("tasks").document(taskId).get().getResult();
            return doc.toObject(Task.class);
        } catch (Exception e) {
            Log.e("TaskService", "Error getting task", e);
            return null;
        }
    }



    public interface OnTaskActionListener {
        void onSuccess(Object result);
        void onFailure(Exception e);
    }
}
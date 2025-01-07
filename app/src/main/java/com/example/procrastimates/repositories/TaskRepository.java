package com.example.procrastimates.repositories;

import android.util.Log;

import com.example.procrastimates.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
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

        // Creați documentul și obțineți ID-ul generat automat
        DocumentReference newTaskRef = tasksRef.document(); // Aceasta va genera un ID automat
        task.setTaskId(newTaskRef.getId()); // Setează taskId-ul cu ID-ul generat de Firestore
        task.setUserId(userId); // Asociază userId-ul la task

        // Adăugați task-ul în Firestore
        newTaskRef.set(task)
                .addOnSuccessListener(aVoid -> {
                    listener.onSuccess(task);  // Răspunde cu task-ul adăugat
                })
                .addOnFailureListener(e -> {
                    listener.onFailure(e);  // Răspunde cu eroarea, dacă există
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

    public interface OnTaskActionListener {
        void onSuccess(Object result);
        void onFailure(Exception e);
    }
}
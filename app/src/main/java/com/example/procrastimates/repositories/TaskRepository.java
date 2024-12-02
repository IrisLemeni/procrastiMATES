package com.example.procrastimates.repositories;

import com.example.procrastimates.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class TaskRepository {
    private FirebaseFirestore db;

    public TaskRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public void addTask(Task task, String userId, OnTaskActionListener listener) {
        task.setUserId(userId);

        db.collection("tasks")
                .add(task)
                .addOnSuccessListener(documentReference -> {
                    task.setTaskId(documentReference.getId());
                    listener.onSuccess(task);
                })
                .addOnFailureListener(e -> listener.onFailure(e));
    }

    public void updateTask(Task task, OnTaskActionListener listener) {
        db.collection("tasks")
                .document(task.getTaskId())
                .set(task)
                .addOnSuccessListener(aVoid -> listener.onSuccess(task))
                .addOnFailureListener(e -> listener.onFailure(e));
    }

    public void deleteTask(String taskId, OnTaskActionListener listener) {
        db.collection("tasks")
                .document(taskId)
                .delete()
                .addOnSuccessListener(aVoid -> listener.onSuccess(null))
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
                    }
                })
                .addOnFailureListener(e -> listener.onFailure(e));
    }

    public interface OnTaskActionListener {
        void onSuccess(Object result);
        void onFailure(Exception e);
    }
}


package com.example.procrastimates.service;

import com.example.procrastimates.Task;
import com.example.procrastimates.repositories.TaskRepository;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TaskService {
    private TaskRepository taskRepository;

    public TaskService() {
        taskRepository = new TaskRepository();
    }

    // Adaugă un task
    public void addTask(Task task, String userId, TaskRepository.OnTaskActionListener listener) {
        taskRepository.addTask(task, userId, listener);
    }

    // Modifică un task
    public void updateTask(String taskId, Task task, TaskRepository.OnTaskActionListener listener) {
        taskRepository.updateTask(taskId, task, listener);
    }

    public void completeTask(String taskId, String userId, String circleId, TaskRepository.OnTaskActionListener listener){
        taskRepository.completeTask(taskId, userId, circleId, listener);
    }

    // Șterge un task
    public void deleteTask(String taskId, TaskRepository.OnTaskActionListener listener) {
        taskRepository.deleteTask(taskId, listener);
    }

    // Obține task-urile utilizatorului
    public void getUserTasks(String userId, TaskRepository.OnTaskActionListener listener) {
        taskRepository.getUserTasks(userId, listener);
    }

    // Add to TaskService.java
    public void getTasksForDateRange(String userId, Timestamp startDate, Timestamp endDate,
                                     TaskRepository.OnTaskActionListener listener) {
        FirebaseFirestore.getInstance().collection("tasks")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("dueDate", startDate)
                .whereLessThanOrEqualTo("dueDate", endDate)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Task> tasks = new ArrayList<>();
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Task task = document.toObject(Task.class);
                            tasks.add(task);
                        }
                    }
                    listener.onSuccess(tasks);
                })
                .addOnFailureListener(e -> listener.onFailure(e));
    }
}

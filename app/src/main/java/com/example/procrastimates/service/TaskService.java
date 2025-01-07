package com.example.procrastimates.service;

import com.example.procrastimates.Task;
import com.example.procrastimates.repositories.TaskRepository;

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


    // Șterge un task
    public void deleteTask(String taskId, TaskRepository.OnTaskActionListener listener) {
        taskRepository.deleteTask(taskId, listener);
    }

    // Obține task-urile utilizatorului
    public void getUserTasks(String userId, TaskRepository.OnTaskActionListener listener) {
        taskRepository.getUserTasks(userId, listener);
    }
}

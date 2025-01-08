package com.example.procrastimates;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.procrastimates.Task;
import com.example.procrastimates.repositories.TaskRepository;
import com.example.procrastimates.service.TaskService;
import com.example.procrastimates.Priority;

import java.util.ArrayList;
import java.util.List;

public class TaskViewModel extends AndroidViewModel {

    private MutableLiveData<List<Task>> tasksLiveData;
    private TaskService taskService;
    private List<Task> originalTasks = new ArrayList<>();

    public TaskViewModel(Application application) {
        super(application);
        taskService = new TaskService();
        tasksLiveData = new MutableLiveData<>();
    }

    public LiveData<List<Task>> getTasksLiveData() {
        return tasksLiveData;
    }

    public void loadTasks(String userId) {
        taskService.getUserTasks(userId, new TaskRepository.OnTaskActionListener() {
            @Override
            public void onSuccess(Object result) {
                List<Task> tasks = (List<Task>) result;

                if (tasks != null) {
                    originalTasks = tasks;
                    tasksLiveData.setValue(tasks);
                }
            }

            @Override
            public void onFailure(Exception e) {
                tasksLiveData.setValue(null);
            }
        });
    }

    public void filterTasksByPriority(Priority priority) {
        List<Task> filteredTasks = new ArrayList<>();
        for (Task task : originalTasks) {
            if (task.getPriority() == priority) {
                filteredTasks.add(task);
            }
        }
        tasksLiveData.setValue(filteredTasks); // Setează task-urile filtrate
    }

    public void resetFilters() {
        tasksLiveData.setValue(originalTasks);
    }

    public void addTask(Task task, String userId) {
        taskService.addTask(task, userId, new TaskRepository.OnTaskActionListener() {
            @Override
            public void onSuccess(Object result) {
                loadTasks(userId);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getApplication(), "Failed to add task.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Actualizează un task
    public void updateTask(String taskId, Task task) {
        if(taskId == null || task == null){
            return;
        }
        taskService.updateTask(taskId, task, new TaskRepository.OnTaskActionListener() {
            @Override
            public void onSuccess(Object result) {
                // Notifică UI-ul că task-ul a fost actualizat cu succes
                Log.d("TaskViewModel", "Task updated successfully");
                loadTasks(task.getUserId());
            }

            @Override
            public void onFailure(Exception e) {
                // Notifică UI-ul că a avut loc o eroare
                Log.e("TaskViewModel", "Failed to update task: " + e.getMessage());
            }
        });
    }

    // Șterge un task
    public void deleteTask(Task task) {
        taskService.deleteTask(task.getTaskId(), new TaskRepository.OnTaskActionListener() {
            @Override
            public void onSuccess(Object result) {
                // Task șters cu succes
                loadTasks(task.getUserId());
            }

            @Override
            public void onFailure(Exception e) {
                // Eroare la ștergere
                Toast.makeText(getApplication(), "Failed to delete task.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

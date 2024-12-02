// TaskViewModel.java
package com.example.procrastimates;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.procrastimates.Task;
import com.example.procrastimates.repositories.TaskRepository;
import com.example.procrastimates.service.TaskService;

import java.util.List;

public class TaskViewModel extends AndroidViewModel {

    private MutableLiveData<List<Task>> tasksLiveData;
    private TaskService taskService;

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
                tasksLiveData.setValue((List<Task>) result);
            }

            @Override
            public void onFailure(Exception e) {
                tasksLiveData.setValue(null);
            }
        });
    }

    public void addTask(Task task, String userId) {
        taskService.addTask(task, userId, new TaskRepository.OnTaskActionListener() {
            @Override
            public void onSuccess(Object result) {
                loadTasks(userId);
            }

            @Override
            public void onFailure(Exception e) {
               //de facut ceva
            }
        });
    }
}

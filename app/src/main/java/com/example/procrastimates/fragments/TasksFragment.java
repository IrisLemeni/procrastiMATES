package com.example.procrastimates.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.procrastimates.AddTaskBottomSheet;
import com.example.procrastimates.EditTaskBottomSheet;
import com.example.procrastimates.R;
import com.example.procrastimates.RecyclerItemTouchHelper;
import com.example.procrastimates.Task;
import com.example.procrastimates.TaskAdapter;
import com.example.procrastimates.TaskViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

public class TasksFragment extends Fragment {

    private RecyclerView tasksRecyclerView;
    private TaskAdapter taskAdapter;
    private TaskViewModel taskViewModel;
    private FloatingActionButton fabAddTask;

    public TasksFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tasks, container, false);

        // Obține referința la ViewModel
        taskViewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);

        fabAddTask = view.findViewById(R.id.fabAddTask);
        tasksRecyclerView = view.findViewById(R.id.tasksRecyclerView);
        tasksRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        taskAdapter = new TaskAdapter(new ArrayList<>());
        tasksRecyclerView.setAdapter(taskAdapter);

        // lista de task-uri
        taskViewModel.getTasksLiveData().observe(getViewLifecycleOwner(), tasks -> {
            if (tasks != null) {
                taskAdapter.setTasks(tasks);
            } else {
                Toast.makeText(getContext(), "Failed to load tasks.", Toast.LENGTH_SHORT).show();
            }
        });

        // Încarcă task-urile utilizatorului
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        taskViewModel.loadTasks(userId);

        fabAddTask.setOnClickListener(v -> showAddTaskBottomSheet());

        taskAdapter.setOnEditTaskListener(this::showEditTaskBottomSheet);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new RecyclerItemTouchHelper(taskAdapter));
        itemTouchHelper.attachToRecyclerView(tasksRecyclerView);

        //setupSwipetoDelete();


        return view;
    }

    private void showEditTaskBottomSheet(Task task) {
        if (task.getTaskId() == null) {
            Toast.makeText(getContext(), "Task ID is missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        EditTaskBottomSheet bottomSheet = new EditTaskBottomSheet();
        bottomSheet.setTask(task);
        bottomSheet.setOnTaskUpdatedListener(updatedTask -> {
            if (updatedTask != null) {
                taskViewModel.updateTask(updatedTask.getTaskId(), updatedTask);
                Toast.makeText(getContext(), "Task updated successfully.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Failed to update task.", Toast.LENGTH_SHORT).show();
            }
        });
        bottomSheet.show(getParentFragmentManager(), "EditTaskBottomSheet");
    }

    private void setupSwipetoDelete() {
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Task task = taskAdapter.getTaskAt(position);
                if (task != null) {
                    taskViewModel.deleteTask(task);
                    taskAdapter.removeTask(position);
                    Toast.makeText(getContext(), "Task deleted.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Error deleting task.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        itemTouchHelper.attachToRecyclerView(tasksRecyclerView);
    }

    private void showAddTaskBottomSheet() {
        AddTaskBottomSheet addTaskBottomSheet = new AddTaskBottomSheet();
        addTaskBottomSheet.setOnTaskAddedListener(newTask -> {
            if (newTask != null) {
                Toast.makeText(getContext(), "Task added successfully.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Failed to add task.", Toast.LENGTH_SHORT).show();
            }
        });
        addTaskBottomSheet.show(getParentFragmentManager(), "AddTaskBottomSheet");
    }
}

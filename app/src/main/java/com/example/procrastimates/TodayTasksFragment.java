package com.example.procrastimates;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TodayTasksFragment extends Fragment {

    private RecyclerView tasksRecyclerView;
    private TaskAdapter taskAdapter;
    private TaskViewModel taskViewModel;
    private FloatingActionButton fabAddTask, fabSortTasks;

    public TodayTasksFragment(){
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_today_tasks, container, false);

        taskViewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);

        fabAddTask = view.findViewById(R.id.fabAddTask);
        fabSortTasks = view.findViewById(R.id.fabSortTasks);
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
        taskViewModel.loadTodayTasks(userId);

        fabAddTask.setOnClickListener(v -> showAddTaskBottomSheet());
        fabSortTasks.setOnClickListener(this::showSortMenu);

        taskAdapter.setOnEditTaskListener(this::showEditTaskBottomSheet);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new RecyclerItemTouchHelper(taskAdapter));
        itemTouchHelper.attachToRecyclerView(tasksRecyclerView);

        // Inside TodayTasksFragment
        taskAdapter.setOnTaskCheckedChangeListener((task, isChecked) -> {
            if (isChecked) {
                taskViewModel.completeTask(task);

                Snackbar.make(tasksRecyclerView, "Task marked as completed", Snackbar.LENGTH_LONG)
                        .setAction("Undo", v -> taskViewModel.undoCompleteTask())
                        .show();
            }
        });


        return view;
    }

    private void showSortMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(getContext(), v);
        popupMenu.getMenuInflater().inflate(R.menu.sort_menu, popupMenu.getMenu());

        // Mapăm ID-urile din meniu la acțiunile corespunzătoare
        Map<Integer, Runnable> filterActions = new HashMap<>();
        filterActions.put(R.id.sort_high, () -> taskViewModel.filterTasksByPriority(Priority.HIGH));
        filterActions.put(R.id.sort_medium, () -> taskViewModel.filterTasksByPriority(Priority.MEDIUM));
        filterActions.put(R.id.sort_low, () -> taskViewModel.filterTasksByPriority(Priority.LOW));
        filterActions.put(R.id.sort_reset, taskViewModel::resetFilters);

        // Setăm listener-ul pentru meniul popup
        popupMenu.setOnMenuItemClickListener(item -> {
            Runnable action = filterActions.get(item.getItemId());
            if (action != null) {
                action.run();
                Toast.makeText(getContext(), "Filter applied.", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });

        popupMenu.show();
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
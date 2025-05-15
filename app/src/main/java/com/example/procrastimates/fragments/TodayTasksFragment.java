package com.example.procrastimates.fragments;

import android.content.Intent;
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

import com.example.procrastimates.AddTaskBottomSheet;
import com.example.procrastimates.EditTaskBottomSheet;
import com.example.procrastimates.Priority;
import com.example.procrastimates.R;
import com.example.procrastimates.RecyclerItemTouchHelper;
import com.example.procrastimates.models.Task;
import com.example.procrastimates.adapters.TaskAdapter;
import com.example.procrastimates.TaskViewModel;
import com.example.procrastimates.activities.AskAiActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TodayTasksFragment extends Fragment {

    private RecyclerView tasksRecyclerView, completedTasksRecyclerView;
    private TaskAdapter taskAdapter, completedTaskAdapter;
    private TaskViewModel taskViewModel;
    private FloatingActionButton fabAddTask, fabSortTasks, askAiButton;

    public TodayTasksFragment(){
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_today_tasks, container, false);

        taskViewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);

        fabAddTask = view.findViewById(R.id.fabAddTask);
        fabSortTasks = view.findViewById(R.id.fabSortTasks);
        askAiButton = view.findViewById(R.id.askAiButton);

        tasksRecyclerView = view.findViewById(R.id.tasksRecyclerView);
        tasksRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        taskAdapter = new TaskAdapter(new ArrayList<>());
        tasksRecyclerView.setAdapter(taskAdapter);

        completedTasksRecyclerView = view.findViewById(R.id.completedTasksRecyclerView);
        completedTasksRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        completedTaskAdapter = new TaskAdapter(new ArrayList<>());
        completedTasksRecyclerView.setAdapter(completedTaskAdapter);

        // lista de task-uri
        taskViewModel.getTasksLiveData().observe(getViewLifecycleOwner(), tasks -> {
            if (tasks != null) {
                taskAdapter.setTasks(tasks);
            } else {
                Toast.makeText(getContext(), "Failed to load tasks.", Toast.LENGTH_SHORT).show();
            }
        });

        taskViewModel.getCompletedTasksLiveData().observe(getViewLifecycleOwner(), completedTasks -> {
            if (completedTasks != null) {
                completedTaskAdapter.setTasks(completedTasks);
            }
        });

        // Încarcă task-urile utilizatorului
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        taskViewModel.loadTodayTasks(userId);

        fabAddTask.setOnClickListener(v -> showAddTaskBottomSheet());
        fabSortTasks.setOnClickListener(this::showSortMenu);
        askAiButton.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AskAiActivity.class);
            startActivity(intent);
        });

        taskAdapter.setOnEditTaskListener(this::showEditTaskBottomSheet);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new RecyclerItemTouchHelper(taskAdapter));
        itemTouchHelper.attachToRecyclerView(tasksRecyclerView);

        // Inside TodayTasksFragment
        taskAdapter.setOnTaskCheckedChangeListener((task, isChecked) -> {
            if (isChecked) {
                getCircleIdAndCompleteTask(task);
            }
        });



        return view;
    }
    private void getCircleIdAndCompleteTask(Task task) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance()
                .collection("circles")
                .whereArrayContains("members", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String circleId = queryDocumentSnapshots.getDocuments().get(0).getId();

                        taskViewModel.completeTask(task, circleId);

                        Snackbar.make(tasksRecyclerView, "Task marked as completed", Snackbar.LENGTH_LONG)
                                .setAction("Undo", v -> taskViewModel.undoCompleteTask())
                                .show();
                    } else {
                        Toast.makeText(getContext(), "Nu ești într-un cerc.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Eroare la căutarea cercului: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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
                // Acum apelăm direct Repository pentru a salva task-ul
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                taskViewModel.addTask(newTask, userId);

                Toast.makeText(getContext(), "Task added successfully.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Failed to add task.", Toast.LENGTH_SHORT).show();
            }
        });
        addTaskBottomSheet.show(getParentFragmentManager(), "AddTaskBottomSheet");
    }

}
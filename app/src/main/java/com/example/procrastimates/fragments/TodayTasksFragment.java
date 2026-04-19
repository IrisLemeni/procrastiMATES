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
import android.widget.TextView;
import android.widget.LinearLayout;

import com.example.procrastimates.components.AddTaskBottomSheet;
import com.example.procrastimates.components.EditTaskBottomSheet;
import com.example.procrastimates.enums.Priority;
import com.example.procrastimates.R;
import com.example.procrastimates.utils.RecyclerItemTouchHelper;
import com.example.procrastimates.models.Task;
import com.example.procrastimates.adapters.TaskAdapter;
import com.example.procrastimates.utils.TaskViewModel;
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

    // Add references to empty state layouts and count TextViews
    private LinearLayout emptyActiveTasksLayout, emptyCompletedTasksLayout;
    private TextView activeTasksCount, completedTasksCount;

    public TodayTasksFragment(){
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_today_tasks, container, false);

        taskViewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);

        // Initialize views
        initializeViews(view);
        setupRecyclerViews();
        setupObservers();
        setupClickListeners();

        // Load user tasks
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        taskViewModel.loadTodayTasks(userId);

        return view;
    }

    private void initializeViews(View view) {
        fabAddTask = view.findViewById(R.id.fabAddTask);
        fabSortTasks = view.findViewById(R.id.fabSortTasks);
        askAiButton = view.findViewById(R.id.askAiButton);

        tasksRecyclerView = view.findViewById(R.id.tasksRecyclerView);
        completedTasksRecyclerView = view.findViewById(R.id.completedTasksRecyclerView);

        emptyActiveTasksLayout = view.findViewById(R.id.emptyActiveTasksLayout);
        emptyCompletedTasksLayout = view.findViewById(R.id.emptyCompletedTasksLayout);

        activeTasksCount = view.findViewById(R.id.activeTasksCount);
        completedTasksCount = view.findViewById(R.id.completedTasksCount);
    }

    private void setupRecyclerViews() {
        // Setup pentru task-uri active
        tasksRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        taskAdapter = new TaskAdapter(new ArrayList<>(), false); // false = active tasks
        tasksRecyclerView.setAdapter(taskAdapter);

        // Setup pentru task-uri completate
        completedTasksRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        completedTaskAdapter = new TaskAdapter(new ArrayList<>(), true); // true = completed tasks
        completedTasksRecyclerView.setAdapter(completedTaskAdapter);

        // Setup touch helper pentru swipe actions doar pe task-urile active
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new RecyclerItemTouchHelper(taskAdapter));
        itemTouchHelper.attachToRecyclerView(tasksRecyclerView);
    }

    private void setupObservers() {
        // Observe active tasks
        taskViewModel.getTasksLiveData().observe(getViewLifecycleOwner(), tasks -> {
            if (tasks != null) {
                taskAdapter.setTasks(tasks);
                updateActiveTasksVisibility(tasks.size());
                updateActiveTasksCount(tasks.size());
            } else {
                Toast.makeText(getContext(), "Failed to load tasks.", Toast.LENGTH_SHORT).show();
                updateActiveTasksVisibility(0);
                updateActiveTasksCount(0);
            }
        });

        // Observe completed tasks
        taskViewModel.getCompletedTasksLiveData().observe(getViewLifecycleOwner(), completedTasks -> {
            if (completedTasks != null) {
                completedTaskAdapter.setTasks(completedTasks);
                updateCompletedTasksVisibility(completedTasks.size());
                updateCompletedTasksCount(completedTasks.size());
            } else {
                updateCompletedTasksVisibility(0);
                updateCompletedTasksCount(0);
            }
        });
    }

    private void setupClickListeners() {
        fabAddTask.setOnClickListener(v -> showAddTaskBottomSheet());
        fabSortTasks.setOnClickListener(this::showSortMenu);
        askAiButton.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AskAiActivity.class);
            startActivity(intent);
        });

        // Set listeners pentru task-uri active
        taskAdapter.setOnEditTaskListener(this::showEditTaskBottomSheet);
        taskAdapter.setOnTaskCheckedChangeListener((task, isChecked) -> {
            if (isChecked) {
                getCircleIdAndCompleteTask(task);
            }
        });

        // Set listeners pentru task-uri completate
        // Nu setăm OnEditTaskListener pentru că nu avem buton de edit
        completedTaskAdapter.setOnTaskCheckedChangeListener((task, isChecked) -> {
            if (!isChecked) {
                // Handle unchecking completed tasks - move back to active
                uncompleteTask(task);
            }
        });
    }

    private void uncompleteTask(Task task) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance()
                .collection("circles")
                .whereArrayContains("members", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String circleId = queryDocumentSnapshots.getDocuments().get(0).getId();

                        // Update task status
                        task.setCompleted(false);
                        taskViewModel.updateTask(task.getTaskId(), task);

                        Snackbar.make(completedTasksRecyclerView, "Task moved back to active", Snackbar.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Nu ești într-un cerc.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Eroare: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateActiveTasksVisibility(int taskCount) {
        if (taskCount == 0) {
            tasksRecyclerView.setVisibility(View.GONE);
            emptyActiveTasksLayout.setVisibility(View.VISIBLE);
        } else {
            tasksRecyclerView.setVisibility(View.VISIBLE);
            emptyActiveTasksLayout.setVisibility(View.GONE);
        }
    }

    private void updateCompletedTasksVisibility(int taskCount) {
        if (taskCount == 0) {
            completedTasksRecyclerView.setVisibility(View.GONE);
            emptyCompletedTasksLayout.setVisibility(View.VISIBLE);
        } else {
            completedTasksRecyclerView.setVisibility(View.VISIBLE);
            emptyCompletedTasksLayout.setVisibility(View.GONE);
        }
    }

    private void updateActiveTasksCount(int count) {
        if (activeTasksCount != null) {
            activeTasksCount.setText(String.valueOf(count));
        }
    }

    private void updateCompletedTasksCount(int count) {
        if (completedTasksCount != null) {
            completedTasksCount.setText(String.valueOf(count));
        }
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

        Map<Integer, Runnable> filterActions = new HashMap<>();
        filterActions.put(R.id.sort_high, () -> taskViewModel.filterTasksByPriority(Priority.HIGH));
        filterActions.put(R.id.sort_medium, () -> taskViewModel.filterTasksByPriority(Priority.MEDIUM));
        filterActions.put(R.id.sort_low, () -> taskViewModel.filterTasksByPriority(Priority.LOW));
        filterActions.put(R.id.sort_reset, taskViewModel::resetFilters);

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
package com.example.procrastimates.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.procrastimates.R;
import com.example.procrastimates.models.Task;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private static final String TAG = "TaskAdapter";
    private List<Task> taskList;
    private OnEditTaskListener onEditTaskListener;
    private OnTaskCheckedChangeListener onTaskCheckedChangeListener;
    private boolean isCompletedTasksAdapter; // Flag pentru a distingue între active și completate

    public TaskAdapter(List<Task> taskList) {
        this.taskList = taskList != null ? taskList : new ArrayList<>();
        this.isCompletedTasksAdapter = false; // Default pentru task-uri active
    }

    // Constructor pentru a specifica tipul de adapter
    public TaskAdapter(List<Task> taskList, boolean isCompletedTasksAdapter) {
        this.taskList = taskList != null ? taskList : new ArrayList<>();
        this.isCompletedTasksAdapter = isCompletedTasksAdapter;
    }

    public interface OnEditTaskListener {
        void onEditTask(Task task);
    }

    public interface OnTaskCheckedChangeListener {
        void onTaskChecked(Task task, boolean isChecked);
    }

    public void setOnEditTaskListener(OnEditTaskListener listener) {
        this.onEditTaskListener = listener;
    }

    public void setOnTaskCheckedChangeListener(OnTaskCheckedChangeListener listener) {
        this.onTaskCheckedChangeListener = listener;
    }

    @Override
    public TaskViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Folosește layout-uri diferite în funcție de tipul de task-uri
        int layoutId = isCompletedTasksAdapter ? R.layout.completed_task_layout : R.layout.task_layout;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new TaskViewHolder(view, isCompletedTasksAdapter);
    }

    @Override
    public void onBindViewHolder(TaskViewHolder holder, int position) {
        if (position < 0 || position >= taskList.size()) {
            Log.e(TAG, "Invalid position: " + position + ", taskList size: " + taskList.size());
            return;
        }

        Task task = taskList.get(position);
        if (task == null) {
            Log.e(TAG, "Task at position " + position + " is null");
            return;
        }

        // Clear previous listeners to avoid conflicts
        holder.checkBox.setOnCheckedChangeListener(null);
        if (holder.editTask != null) {
            holder.editTask.setOnClickListener(null);
        }

        // Bind data
        holder.titleTextView.setText(task.getTitle() != null ? task.getTitle() : "Untitled Task");
        holder.checkBox.setChecked(task.isCompleted());

        // Set visibility
        holder.titleTextView.setVisibility(View.VISIBLE);
        holder.checkBox.setVisibility(View.VISIBLE);

        // Set listeners
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (onTaskCheckedChangeListener != null) {
                onTaskCheckedChangeListener.onTaskChecked(task, isChecked);
            }
        });

        // Set edit listener only for active tasks (completed tasks don't have edit button)
        if (!isCompletedTasksAdapter && holder.editTask != null) {
            holder.editTask.setOnClickListener(v -> {
                if (onEditTaskListener != null) {
                    onEditTaskListener.onEditTask(task);
                }
            });
        }

        Log.d(TAG, "Bound task: " + task.getTitle() + " at position " + position +
                " (completed adapter: " + isCompletedTasksAdapter + ")");
    }

    @Override
    public int getItemCount() {
        int count = taskList != null ? taskList.size() : 0;
        Log.d(TAG, "getItemCount: " + count + " (completed adapter: " + isCompletedTasksAdapter + ")");
        return count;
    }

    public void removeTask(int position) {
        if (position >= 0 && position < taskList.size()) {
            taskList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, taskList.size());
            Log.d(TAG, "Task removed at position: " + position);
        }
    }

    public Task getTaskAt(int position) {
        if (position >= 0 && position < taskList.size()) {
            return taskList.get(position);
        }
        return null;
    }

    public void setTasks(List<Task> newTaskList) {
        this.taskList = newTaskList != null ? newTaskList : new ArrayList<>();
        notifyDataSetChanged();
        Log.d(TAG, "Tasks updated. New count: " + this.taskList.size() +
                " (completed adapter: " + isCompletedTasksAdapter + ")");
    }

    public void addTask(Task task) {
        if (task != null) {
            taskList.add(task);
            notifyItemInserted(taskList.size() - 1);
            Log.d(TAG, "Task added: " + task.getTitle());
        }
    }

    public void updateTask(int position, Task updatedTask) {
        if (position >= 0 && position < taskList.size() && updatedTask != null) {
            taskList.set(position, updatedTask);
            notifyItemChanged(position);
            Log.d(TAG, "Task updated at position: " + position);
        }
    }

    public boolean isCompletedTasksAdapter() {
        return isCompletedTasksAdapter;
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        CheckBox checkBox;
        MaterialButton editTask; // Poate fi null pentru completed tasks

        public TaskViewHolder(View itemView, boolean isCompletedTask) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.taskTitle);
            checkBox = itemView.findViewById(R.id.todoCheckBox);

            // Edit button există doar în layout-ul pentru task-uri active
            if (!isCompletedTask) {
                editTask = itemView.findViewById(R.id.editTask);
                if (editTask == null) {
                    Log.e("TaskViewHolder", "editTask not found in active task layout - check R.id.editTask");
                }
            } else {
                editTask = null; // Explicitly set to null for completed tasks
                Log.d("TaskViewHolder", "Edit button not initialized for completed task (as expected)");
            }

            // Check if required views are properly found
            if (titleTextView == null) {
                Log.e("TaskViewHolder", "titleTextView not found - check R.id.taskTitle");
            }
            if (checkBox == null) {
                Log.e("TaskViewHolder", "checkBox not found - check R.id.todoCheckBox");
            }

            Log.d("TaskViewHolder", "TaskViewHolder created for " +
                    (isCompletedTask ? "completed" : "active") + " task");
        }
    }
}
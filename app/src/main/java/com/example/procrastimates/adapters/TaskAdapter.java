package com.example.procrastimates.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.procrastimates.Priority;
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
    private OnTaskDeleteListener onTaskDeleteListener;
    private boolean isCompletedTasksAdapter;

    public TaskAdapter(List<Task> taskList) {
        this.taskList = taskList != null ? taskList : new ArrayList<>();
        this.isCompletedTasksAdapter = false;
    }

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

    public interface OnTaskDeleteListener {
        void onTaskDelete(Task task);
    }

    public void setOnEditTaskListener(OnEditTaskListener listener) {
        this.onEditTaskListener = listener;
    }

    public void setOnTaskCheckedChangeListener(OnTaskCheckedChangeListener listener) {
        this.onTaskCheckedChangeListener = listener;
    }

    public void setOnTaskDeleteListener(OnTaskDeleteListener listener) {
        this.onTaskDeleteListener = listener;
    }

    @Override
    public TaskViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int layoutId = isCompletedTasksAdapter ? R.layout.completed_task_layout : R.layout.task_layout;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new TaskViewHolder(view, isCompletedTasksAdapter);
    }

    @Override
    public void onBindViewHolder(TaskViewHolder holder, int position) {
        if (position < 0 || position >= taskList.size()) return;

        Task task = taskList.get(position);
        if (task == null) return;

        holder.checkBox.setOnCheckedChangeListener(null);
        if (holder.editButton != null) holder.editButton.setOnClickListener(null);
        if (holder.deleteButton != null) holder.deleteButton.setOnClickListener(null);

        holder.titleTextView.setText(task.getTitle() != null ? task.getTitle() : "Untitled Task");
        holder.checkBox.setChecked(task.isCompleted());

        if (!isCompletedTasksAdapter) {
            holder.setPriorityIndicator(task.getPriority());
            holder.setPriorityChip(task.getPriority());
        }

        holder.titleTextView.setVisibility(View.VISIBLE);
        holder.checkBox.setVisibility(View.VISIBLE);

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (onTaskCheckedChangeListener != null) {
                onTaskCheckedChangeListener.onTaskChecked(task, isChecked);
            }
        });

        if (!isCompletedTasksAdapter && holder.editButton != null) {
            holder.editButton.setOnClickListener(v -> {
                if (onEditTaskListener != null) {
                    onEditTaskListener.onEditTask(task);
                }
            });
        }

        if (!isCompletedTasksAdapter && holder.deleteButton != null) {
            holder.deleteButton.setOnClickListener(v -> {
                if (onTaskDeleteListener != null) {
                    onTaskDeleteListener.onTaskDelete(task);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return taskList != null ? taskList.size() : 0;
    }

    public void removeTask(int position) {
        if (position >= 0 && position < taskList.size()) {
            taskList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, taskList.size());
        }
    }

    public Task getTaskAt(int position) {
        return (position >= 0 && position < taskList.size()) ? taskList.get(position) : null;
    }

    public void setTasks(List<Task> newTaskList) {
        this.taskList = newTaskList != null ? newTaskList : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void addTask(Task task) {
        if (task != null) {
            taskList.add(task);
            notifyItemInserted(taskList.size() - 1);
        }
    }

    public void updateTask(int position, Task updatedTask) {
        if (position >= 0 && position < taskList.size() && updatedTask != null) {
            taskList.set(position, updatedTask);
            notifyItemChanged(position);
        }
    }

    public boolean isCompletedTasksAdapter() {
        return isCompletedTasksAdapter;
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        CheckBox checkBox;
        MaterialButton editButton;
        MaterialButton deleteButton;
        View priorityIndicator;
        TextView priorityChip;

        public TaskViewHolder(View itemView, boolean isCompletedTask) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.taskTitle);
            checkBox = itemView.findViewById(R.id.todoCheckBox);

            if (!isCompletedTask) {
                editButton = itemView.findViewById(R.id.editTask);
                deleteButton = itemView.findViewById(R.id.delete);
                priorityIndicator = itemView.findViewById(R.id.priorityIndicator);
                priorityChip = itemView.findViewById(R.id.taskPriority);
            }
        }

        private void setPriorityIndicator(Priority priority) {
            if (priorityIndicator == null) return;
            int color;
            switch (priority) {
                case HIGH:
                    color = ContextCompat.getColor(itemView.getContext(), R.color.priority_high);
                    break;
                case MEDIUM:
                    color = ContextCompat.getColor(itemView.getContext(), R.color.priority_medium);
                    break;
                case LOW:
                    color = ContextCompat.getColor(itemView.getContext(), R.color.priority_low);
                    break;
                default:
                    color = ContextCompat.getColor(itemView.getContext(), R.color.text_tertiary);
                    break;
            }
            priorityIndicator.setBackgroundTintList(ColorStateList.valueOf(color));
        }

        private void setPriorityChip(Priority priority) {
            if (priorityChip == null) return;
            String priorityText;
            int textColor;
            int backgroundColor;

            switch (priority) {
                case HIGH:
                    priorityText = "High";
                    textColor = ContextCompat.getColor(itemView.getContext(), R.color.priority_high);
                    backgroundColor = ContextCompat.getColor(itemView.getContext(), R.color.priority_high);
                    break;
                case MEDIUM:
                    priorityText = "Medium";
                    textColor = ContextCompat.getColor(itemView.getContext(), R.color.priority_medium);
                    backgroundColor = ContextCompat.getColor(itemView.getContext(), R.color.priority_medium);
                    break;
                case LOW:
                    priorityText = "Low";
                    textColor = ContextCompat.getColor(itemView.getContext(), R.color.priority_low);
                    backgroundColor = ContextCompat.getColor(itemView.getContext(), R.color.priority_low);
                    break;
                default:
                    priorityText = "Normal";
                    textColor = ContextCompat.getColor(itemView.getContext(), R.color.text_secondary);
                    backgroundColor = ContextCompat.getColor(itemView.getContext(), R.color.surface_variant);
                    break;
            }

            priorityChip.setText(priorityText);
            priorityChip.setTextColor(textColor);
            int alpha = 0x20;
            int chipBackgroundColor = (backgroundColor & 0x00FFFFFF) | (alpha << 24);
            priorityChip.setBackgroundTintList(ColorStateList.valueOf(chipBackgroundColor));
        }
    }
}

package com.example.procrastimates.adapters;

import android.content.res.ColorStateList;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.procrastimates.enums.Priority;
import com.example.procrastimates.R;
import com.example.procrastimates.models.Task;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class CalendarTaskAdapter extends RecyclerView.Adapter<CalendarTaskAdapter.CalendarTaskViewHolder> {

    private List<Task> taskList;
    private OnTaskActionListener onTaskActionListener;

    public interface OnTaskActionListener {
        void onTaskEdit(Task task);
        void onTaskDelete(Task task);
        void onTaskComplete(Task task, boolean isCompleted);
    }

    public CalendarTaskAdapter(List<Task> taskList) {
        this.taskList = taskList;
    }

    public void setOnTaskActionListener(OnTaskActionListener listener) {
        this.onTaskActionListener = listener;
    }

    public void updateTasks(List<Task> newTasks) {
        this.taskList.clear();
        this.taskList.addAll(newTasks);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CalendarTaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_task, parent, false);
        return new CalendarTaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarTaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.bind(task);

        // Long click for editing
        holder.itemView.setOnLongClickListener(v -> {
            if (onTaskActionListener != null) {
                onTaskActionListener.onTaskEdit(task);
            }
            return true;
        });

        // Edit button click
        holder.editButton.setOnClickListener(v -> {
            if (onTaskActionListener != null) {
                onTaskActionListener.onTaskEdit(task);
            }
        });

        // Delete button click
        holder.deleteButton.setOnClickListener(v -> {
            if (onTaskActionListener != null) {
                onTaskActionListener.onTaskDelete(task);
            }
        });
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    static class CalendarTaskViewHolder extends RecyclerView.ViewHolder {
        TextView taskTitle;
        TextView taskPriority;
        MaterialButton editButton;
        MaterialButton deleteButton;
        View priorityIndicator;
        LinearLayout taskMetaLayout;

        public CalendarTaskViewHolder(@NonNull View itemView) {
            super(itemView);
            taskTitle = itemView.findViewById(R.id.taskTitle);
            taskPriority = itemView.findViewById(R.id.taskPriority);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.delete);
            priorityIndicator = itemView.findViewById(R.id.priorityIndicator);
            taskMetaLayout = itemView.findViewById(R.id.taskMetaLayout);
        }

        void bind(Task task) {
            // Set task title
            taskTitle.setText(task.getTitle());

            // Apply strikethrough effect if completed
            if (task.isCompleted()) {
                taskTitle.setPaintFlags(taskTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                taskTitle.setAlpha(0.6f);
            } else {
                taskTitle.setPaintFlags(taskTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                taskTitle.setAlpha(1.0f);
            }

            // Set priority indicator and chip
            setPriorityIndicator(task.getPriority());
            setPriorityChip(task.getPriority());

            // Show priority meta layout
            taskMetaLayout.setVisibility(View.VISIBLE);
        }

        private void setPriorityIndicator(Priority priority) {
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

            taskPriority.setText(priorityText);
            taskPriority.setTextColor(textColor);

            // Create a lighter background color for the chip
            int alpha = 0x20; // 12.5% opacity
            int chipBackgroundColor = (backgroundColor & 0x00FFFFFF) | (alpha << 24);
            taskPriority.setBackgroundTintList(ColorStateList.valueOf(chipBackgroundColor));
        }
    }
}
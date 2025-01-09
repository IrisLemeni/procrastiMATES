package com.example.procrastimates;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.procrastimates.repositories.TaskRepository;

import java.util.List;

public class CalendarTaskAdapter extends RecyclerView.Adapter<CalendarTaskAdapter.CalendarTaskViewHolder> {

    private List<Task> taskList;
    private OnTaskActionListener onTaskActionListener;

    public interface OnTaskActionListener {
        void onTaskEdit(Task task);
        void onTaskDelete(Task task);
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

        // Listener pentru apăsare lungă (editare)
        holder.itemView.setOnLongClickListener(v -> {
            if (onTaskActionListener != null) {
                onTaskActionListener.onTaskEdit(task);
            }
            return true; // Evenimentul este consumat
        });

        // Listener pentru ștergere
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
        CheckBox checkBox;
        ImageButton deleteButton;

        public CalendarTaskViewHolder(@NonNull View itemView) {
            super(itemView);
            taskTitle = itemView.findViewById(R.id.taskTitle);
            checkBox = itemView.findViewById(R.id.todoCheckBox);
            deleteButton = itemView.findViewById(R.id.delete);
        }
        void bind(Task task) {
            taskTitle.setText(task.getTitle());
        }
    }
}

package com.example.procrastimates;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.procrastimates.R;
import com.example.procrastimates.Task;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> taskList;
    private OnEditTaskListener onEditTaskListener;

    public TaskAdapter(List<Task> taskList) {
        this.taskList = taskList;
    }

    @Override
    public TaskViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.task_layout, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.titleTextView.setText(task.getTitle());
        holder.checkBox.setChecked(task.isCompleted());

        holder.editTask.setOnClickListener(v -> {
            if (onEditTaskListener != null) {
                onEditTaskListener.onEditTask(task);
            }
        });

    }

    public void setOnEditTaskListener(OnEditTaskListener listener) {
        this.onEditTaskListener = listener;
    }

    public interface OnEditTaskListener {
        void onEditTask(Task task);
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        CheckBox checkBox;
        ImageButton editTask;

        public TaskViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.taskTitle);
            checkBox = itemView.findViewById(R.id.todoCheckBox);
            editTask = itemView.findViewById(R.id.editTask);
        }
    }

    public void setTasks(List<Task> taskList) {
        this.taskList = taskList;
        notifyDataSetChanged();
    }
}

package com.example.procrastimates;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.procrastimates.R;
import com.example.procrastimates.ObjectionRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RecentTasksAdapter extends RecyclerView.Adapter<RecentTasksAdapter.TaskViewHolder> {
    private List<ObjectionRepository.TaskWithUser> tasks;
    private OnObjectionClickListener listener;
    private Context context;

    public RecentTasksAdapter(Context context, OnObjectionClickListener listener) {
        this.context = context;
        this.tasks = new ArrayList<>();
        this.listener = listener;
    }

    public void setTasks(List<ObjectionRepository.TaskWithUser> tasks) {
        this.tasks = tasks;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        ObjectionRepository.TaskWithUser task = tasks.get(position);

        holder.tvUsername.setText(task.getUsername());
        holder.tvTaskTitle.setText(task.getTitle());

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        holder.tvCompletedTime.setText("Completed at: " + sdf.format(task.getCompletedAt().toDate()));

        int timeRemaining = task.getTimeRemainingSeconds();
        int progress = (int) ((timeRemaining / (5.0 * 60)) * 100);

        holder.progressBar.setProgress(progress);
        holder.tvTimeRemaining.setText(formatTime(timeRemaining));

        if (timeRemaining <= 0) {
            holder.btnRaiseObjection.setEnabled(false);
            holder.btnRaiseObjection.setText("Time expired");
        } else {
            holder.btnRaiseObjection.setEnabled(true);
            holder.btnRaiseObjection.setText("Raise Objection");
            holder.btnRaiseObjection.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRaiseObjectionClick(task);
                }
            });
        }
    }

    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, remainingSeconds);
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    public interface OnObjectionClickListener {
        void onRaiseObjectionClick(ObjectionRepository.TaskWithUser task);
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername, tvTaskTitle, tvCompletedTime, tvTimeRemaining;
        Button btnRaiseObjection;
        ProgressBar progressBar;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvTaskTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvCompletedTime = itemView.findViewById(R.id.tvCompletedTime);
            tvTimeRemaining = itemView.findViewById(R.id.tvTimeRemaining);
            btnRaiseObjection = itemView.findViewById(R.id.btnRaiseObjection);
            progressBar = itemView.findViewById(R.id.progressBar);
        }
    }
}
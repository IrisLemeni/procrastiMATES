package com.example.procrastimates.fragments;

import android.content.Context;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.procrastimates.models.Message;
import com.example.procrastimates.models.Poll;
import com.example.procrastimates.PollStatus;
import com.example.procrastimates.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

class PollViewHolder extends RecyclerView.ViewHolder {
    TextView pollText, pollStatus, timestamp;
    ProgressBar acceptProgress, rejectProgress;
    TextView acceptCount, rejectCount, timeRemaining;
    private final Context context;
    private final String currentUserId;
    private final FirebaseFirestore db;

    PollViewHolder(View itemView, Context context, String currentUserId, FirebaseFirestore db) {
        super(itemView);
        this.context = context;
        this.currentUserId = currentUserId;
        this.db = db;
        pollText = itemView.findViewById(R.id.poll_text);
        pollStatus = itemView.findViewById(R.id.poll_status);
        timestamp = itemView.findViewById(R.id.timestamp);
        acceptProgress = itemView.findViewById(R.id.accept_progress);
        rejectProgress = itemView.findViewById(R.id.reject_progress);
        acceptCount = itemView.findViewById(R.id.accept_count);
        rejectCount = itemView.findViewById(R.id.reject_count);
        timeRemaining = itemView.findViewById(R.id.time_remaining);
    }

    void bind(Message message) {
        pollText.setText(message.getText());

        if (message.getTaskId() != null) {
            // Load the poll
            db.collection("polls")
                    .whereEqualTo("taskId", message.getTaskId())
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            Poll poll = queryDocumentSnapshots.getDocuments().get(0).toObject(Poll.class);
                            if (poll != null) {
                                updatePollUI(poll);
                            }
                        }
                    });
        }

        // Format timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm, dd MMM", Locale.getDefault());
        timestamp.setText(sdf.format(message.getTimestamp().toDate()));
    }

    private void updatePollUI(Poll poll) {
        // Calculate voting statistics
        int acceptVotes = 0;
        int rejectVotes = 0;

        if (poll.getVotes() != null) {
            for (Boolean vote : poll.getVotes().values()) {
                if (vote) {
                    acceptVotes++;
                } else {
                    rejectVotes++;
                }
            }
        }

        int totalVotes = acceptVotes + rejectVotes;
        if (totalVotes > 0) {
            int acceptPercentage = (acceptVotes * 100) / totalVotes;
            int rejectPercentage = (rejectVotes * 100) / totalVotes;

            acceptProgress.setProgress(acceptPercentage);
            rejectProgress.setProgress(rejectPercentage);

            acceptCount.setText(acceptVotes + " (" + acceptPercentage + "%)");
            rejectCount.setText(rejectVotes + " (" + rejectPercentage + "%)");
        } else {
            acceptProgress.setProgress(0);
            rejectProgress.setProgress(0);
            acceptCount.setText("0 (0%)");
            rejectCount.setText("0 (0%)");
        }

        // Display poll status
        if (poll.getStatus() == PollStatus.CLOSED) {
            pollStatus.setText("Poll închis");
            timeRemaining.setVisibility(View.GONE);
        } else {
            pollStatus.setText("Poll activ");

            // Calculate remaining time
            long remainingTime = poll.getEndTime().toDate().getTime() - System.currentTimeMillis();
            if (remainingTime > 0) {
                long hours = TimeUnit.MILLISECONDS.toHours(remainingTime);
                long minutes = TimeUnit.MILLISECONDS.toMinutes(remainingTime) % 60;
                timeRemaining.setText("Timp rămas: " + hours + "h " + minutes + "m");
                timeRemaining.setVisibility(View.VISIBLE);
            } else {
                timeRemaining.setText("Poll-ul se închide în curând");
            }
        }
    }
}
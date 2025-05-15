package com.example.procrastimates.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import android.widget.Button;
import android.widget.TextView;

import com.example.procrastimates.models.Message;
import com.example.procrastimates.R;
import com.example.procrastimates.models.Task;
import com.example.procrastimates.activities.ProofSubmissionActivity;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Locale;

class ObjectionViewHolder extends RecyclerView.ViewHolder {
    TextView objectionText, objectorName, timestamp;
    Button submitProofButton;
    private final Context context;
    private final String currentUserId;
    private final FirebaseFirestore db;

    ObjectionViewHolder(View itemView, Context context, String currentUserId, FirebaseFirestore db) {
        super(itemView);
        this.context = context;
        this.currentUserId = currentUserId;
        this.db = db;
        objectionText = itemView.findViewById(R.id.objection_text);
        objectorName = itemView.findViewById(R.id.objector_name);
        timestamp = itemView.findViewById(R.id.timestamp);
        submitProofButton = itemView.findViewById(R.id.submit_proof_button);
    }

    void bind(Message message) {
        objectionText.setText(message.getText());

        // Verifică dacă utilizatorul actual este cel care a completat task-ul
        if (message.getTaskId() != null) {
            db.collection("tasks").document(message.getTaskId())
                    .get()
                    .addOnSuccessListener(taskDoc -> {
                        if (taskDoc.exists()) {
                            Task task = taskDoc.toObject(Task.class);
                            if (task != null && task.getUserId().equals(currentUserId)) {
                                submitProofButton.setVisibility(View.VISIBLE);
                                submitProofButton.setOnClickListener(v -> openProofSubmission(message.getTaskId()));
                            } else {
                                submitProofButton.setVisibility(View.GONE);
                            }
                        }
                    });
        }

        // Încarcă numele expeditorului (cel care a făcut obiecția)
        db.collection("users").document(message.getSenderId())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String name = document.getString("username");
                        objectorName.setText(name != null ? name : "Unknown");
                    }
                });

        // Formatează timestamp-ul
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm, dd MMM", Locale.getDefault());
        timestamp.setText(sdf.format(message.getTimestamp().toDate()));
    }

    private void openProofSubmission(String taskId) {
        Intent intent = new Intent(context, ProofSubmissionActivity.class);
        intent.putExtra("taskId", taskId);
        context.startActivity(intent);
    }
}

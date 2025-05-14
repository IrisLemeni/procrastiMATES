package com.example.procrastimates.fragments;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.procrastimates.Message;
import com.example.procrastimates.MessageType;
import com.example.procrastimates.Poll;
import com.example.procrastimates.PollStatus;
import com.example.procrastimates.Proof;
import com.example.procrastimates.R;
import com.example.procrastimates.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
class ProofViewHolder extends RecyclerView.ViewHolder {
    TextView proofText, submittedBy, timestamp;
    ImageView proofImage;
    LinearLayout pollContainer;
    Button acceptButton, rejectButton;
    private final Context context;
    private final String currentUserId;
    private final FirebaseFirestore db;

    ProofViewHolder(View itemView, Context context, String currentUserId, FirebaseFirestore db) {
        super(itemView);
        this.context = context;
        this.currentUserId = currentUserId;
        this.db = db;
        proofText = itemView.findViewById(R.id.proof_text);
        submittedBy = itemView.findViewById(R.id.submitted_by);
        timestamp = itemView.findViewById(R.id.timestamp);
        proofImage = itemView.findViewById(R.id.proof_image);
        pollContainer = itemView.findViewById(R.id.poll_container);
        acceptButton = itemView.findViewById(R.id.accept_button);
        rejectButton = itemView.findViewById(R.id.reject_button);
    }

    void bind(Message message) {
        proofText.setText("A furnizat dovadă pentru task");


        if (message.getTaskId() != null) {
            // Încarcă dovada
            db.collection("proofs")
                    .whereEqualTo("taskId", message.getTaskId())
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            Proof proof = queryDocumentSnapshots.getDocuments().get(0).toObject(Proof.class);
                            if (proof != null && proof.getImageUrl() != null) {
                                // Încarcă imaginea dovezii
                                Glide.with(context)
                                        .load(proof.getImageUrl())
                                        .into(proofImage);
                            }
                        }
                    });

            // Verifică dacă există deja un poll pentru acest task
            db.collection("polls")
                    .whereEqualTo("taskId", message.getTaskId())
                    .get()
                    .addOnSuccessListener(pollSnapshots -> {
                        if (pollSnapshots.isEmpty()) {
                            // Nu există poll, afișează butoanele de vot
                            pollContainer.setVisibility(View.VISIBLE);
                            setupVoteButtons(message.getTaskId());
                        } else {
                            // Există poll, verifică dacă utilizatorul a votat deja
                            Poll poll = pollSnapshots.getDocuments().get(0).toObject(Poll.class);
                            if (poll != null) {
                                if (poll.getVotes() != null && poll.getVotes().containsKey(currentUserId)) {
                                    // Utilizatorul a votat deja
                                    pollContainer.setVisibility(View.GONE);
                                } else if (poll.getStatus() == PollStatus.CLOSED) {
                                    // Poll-ul s-a închis
                                    pollContainer.setVisibility(View.GONE);
                                } else {
                                    // Utilizatorul poate vota
                                    pollContainer.setVisibility(View.VISIBLE);
                                    setupVoteButtons(message.getTaskId());
                                }
                            }
                        }
                    });
        }

        // Încarcă numele expeditorului
        db.collection("users").document(message.getSenderId())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String name = document.getString("username");
                        submittedBy.setText(name != null ? name : "Unknown");
                    }
                });

        // Formatează timestamp-ul
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm, dd MMM", Locale.getDefault());
        timestamp.setText(sdf.format(message.getTimestamp().toDate()));
    }

    private void setupVoteButtons(String taskId) {
        acceptButton.setOnClickListener(v -> submitVote(taskId, true));
        rejectButton.setOnClickListener(v -> submitVote(taskId, false));
    }

    private void submitVote(String taskId, boolean isAccepted) {
        // Verifică dacă există deja un poll
        db.collection("polls")
                .whereEqualTo("taskId", taskId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Poll poll;
                    String pollId;

                    if (queryDocumentSnapshots.isEmpty()) {
                        // Creează un nou poll
                        poll = new Poll();
                        pollId = UUID.randomUUID().toString();
                        poll.setPollId(pollId);
                        poll.setTaskId(taskId);

                        // Obține circleId din task
                        db.collection("tasks").document(taskId)
                                .get()
                                .addOnSuccessListener(taskDoc -> {
                                    if (taskDoc.exists()) {
                                        Task task = taskDoc.toObject(Task.class);
                                        if (task != null) {
                                            poll.setCircleId(task.getCircleId());

                                            // Calculează timpul de încheiere (12 ore de acum)
                                            Calendar calendar = Calendar.getInstance();
                                            calendar.add(Calendar.HOUR_OF_DAY, 12);
                                            poll.setEndTime(new Timestamp(calendar.getTime()));

                                            // Inițializează voturile
                                            Map<String, Boolean> votes = new HashMap<>();
                                            votes.put(currentUserId, isAccepted);
                                            poll.setVotes(votes);
                                            poll.setStatus(PollStatus.ACTIVE);

                                            // Salvează poll-ul
                                            db.collection("polls").document(pollId)
                                                    .set(poll)
                                                    .addOnSuccessListener(aVoid -> {
                                                        // Creează un mesaj pentru poll
                                                        createPollMessage(task);
                                                        Toast.makeText(context, "Vot înregistrat!", Toast.LENGTH_SHORT).show();
                                                        pollContainer.setVisibility(View.GONE);
                                                    });
                                        }
                                    }
                                });
                    } else {
                        // Actualizează poll-ul existent
                        DocumentSnapshot pollDoc = queryDocumentSnapshots.getDocuments().get(0);
                        poll = pollDoc.toObject(Poll.class);
                        pollId = pollDoc.getId();

                        if (poll != null) {
                            Map<String, Boolean> votes = poll.getVotes();
                            if (votes == null) {
                                votes = new HashMap<>();
                            }
                            votes.put(currentUserId, isAccepted);
                            poll.setVotes(votes);

                            // Salvează actualizarea
                            db.collection("polls").document(pollId)
                                    .update("votes", votes)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(context, "Vot înregistrat!", Toast.LENGTH_SHORT).show();
                                        pollContainer.setVisibility(View.GONE);
                                    });
                        }
                    }
                });
    }

    private void createPollMessage(Task task) {
        Message pollMessage = new Message();
        pollMessage.setMessageId(UUID.randomUUID().toString());
        pollMessage.setCircleId(task.getCircleId());
        pollMessage.setSenderId(currentUserId);
        pollMessage.setText("A început un vot pentru dovada task-ului: " + task.getTitle());
        pollMessage.setType(MessageType.POLL_CREATED);
        pollMessage.setTaskId(task.getTaskId());
        pollMessage.setTimestamp(new Timestamp(new Date()));

        db.collection("messages").document(pollMessage.getMessageId())
                .set(pollMessage);
    }
}

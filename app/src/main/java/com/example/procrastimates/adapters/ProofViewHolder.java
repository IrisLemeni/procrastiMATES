package com.example.procrastimates.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.procrastimates.activities.FullScreenImageActivity;
import com.example.procrastimates.models.Message;
import com.example.procrastimates.enums.MessageType;
import com.example.procrastimates.models.Poll;
import com.example.procrastimates.enums.PollStatus;
import com.example.procrastimates.models.Proof;
import com.example.procrastimates.R;
import com.example.procrastimates.models.Task;
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
    private String currentImageUrl; // Store current image URL for full screen view

    private static final String FIELD_TASK_ID = "taskId";
    private static final String COLLECTION_POLLS = "polls";

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

        // Setup image click listener for full screen view
        proofImage.setOnClickListener(v -> openImageInFullScreen());
    }

    void bind(Message message) {
        proofText.setText("Provided proof for task completion");

        if (message.getTaskId() != null) {
            loadProof(message.getTaskId());
            checkPollStatus(message.getTaskId());
        }

        loadSenderName(message.getSenderId());

        // Format timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm, dd MMM", Locale.getDefault());
        timestamp.setText(sdf.format(message.getTimestamp().toDate()));
    }

    private void loadProof(String taskId) {
        db.collection("proofs")
                .whereEqualTo(FIELD_TASK_ID, taskId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Proof proof = queryDocumentSnapshots.getDocuments().get(0).toObject(Proof.class);
                        if (proof != null && proof.getImageUrl() != null) {
                            currentImageUrl = proof.getImageUrl();
                            Glide.with(context)
                                    .load(proof.getImageUrl())
                                    .into(proofImage);
                        }
                    }
                });
    }

    private void checkPollStatus(String taskId) {
        db.collection(COLLECTION_POLLS)
                .whereEqualTo(FIELD_TASK_ID, taskId)
                .get()
                .addOnSuccessListener(pollSnapshots -> {
                    if (pollSnapshots.isEmpty()) {
                        pollContainer.setVisibility(View.VISIBLE);
                        setupVoteButtons(taskId);
                    } else {
                        handleExistingPoll(pollSnapshots.getDocuments().get(0).toObject(Poll.class), taskId);
                    }
                });
    }

    private void handleExistingPoll(Poll poll, String taskId) {
        if (poll == null) return;

        if (hasUserVoted(poll) || poll.getStatus() == PollStatus.CLOSED) {
            pollContainer.setVisibility(View.GONE);
        } else {
            pollContainer.setVisibility(View.VISIBLE);
            setupVoteButtons(taskId);
        }
    }

    private boolean hasUserVoted(Poll poll) {
        return poll.getVotes() != null && poll.getVotes().containsKey(currentUserId);
    }

    private void loadSenderName(String senderId) {
        db.collection("users").document(senderId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String name = document.getString("username");
                        submittedBy.setText(name != null ? name : "Unknown User");
                    }
                });
    }

    private void openImageInFullScreen() {
        if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
            // Create intent to open image in full screen
            // You'll need to create a FullScreenImageActivity
            Intent intent = new Intent(context, FullScreenImageActivity.class);
            intent.putExtra("image_url", currentImageUrl);
            context.startActivity(intent);
        } else {
            Toast.makeText(context, "Image not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupVoteButtons(String taskId) {
        acceptButton.setOnClickListener(v -> submitVote(taskId, true));
        rejectButton.setOnClickListener(v -> submitVote(taskId, false));
    }

    private void submitVote(String taskId, boolean isAccepted) {
        db.collection(COLLECTION_POLLS)
                .whereEqualTo(FIELD_TASK_ID, taskId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        createNewPoll(taskId, isAccepted);
                    } else {
                        updateExistingPoll(queryDocumentSnapshots.getDocuments().get(0), isAccepted);
                    }
                });
    }

    private void createNewPoll(String taskId, boolean isAccepted) {
        Poll poll = new Poll();
        String pollId = UUID.randomUUID().toString();
        poll.setPollId(pollId);
        poll.setTaskId(taskId);

        db.collection("tasks").document(taskId)
                .get()
                .addOnSuccessListener(taskDoc -> {
                    if (taskDoc.exists()) {
                        Task task = taskDoc.toObject(Task.class);
                        if (task != null) {
                            setupPollDetails(poll, task, isAccepted);
                            savePoll(poll, pollId, task);
                        }
                    }
                });
    }

    private void setupPollDetails(Poll poll, Task task, boolean isAccepted) {
        poll.setCircleId(task.getCircleId());

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY, 12);
        poll.setEndTime(new Timestamp(calendar.getTime()));

        Map<String, Boolean> votes = new HashMap<>();
        votes.put(currentUserId, isAccepted);
        poll.setVotes(votes);
        poll.setStatus(PollStatus.ACTIVE);
    }

    private void savePoll(Poll poll, String pollId, Task task) {
        db.collection(COLLECTION_POLLS).document(pollId)
                .set(poll)
                .addOnSuccessListener(aVoid -> {
                    createPollMessage(task);
                    showVoteRecordedMessage();
                });
    }

    private void updateExistingPoll(DocumentSnapshot pollDoc, boolean isAccepted) {
        Poll poll = pollDoc.toObject(Poll.class);
        String pollId = pollDoc.getId();

        if (poll != null) {
            Map<String, Boolean> votes = poll.getVotes();
            if (votes == null) {
                votes = new HashMap<>();
            }
            votes.put(currentUserId, isAccepted);
            poll.setVotes(votes);

            db.collection(COLLECTION_POLLS).document(pollId)
                    .update("votes", votes)
                    .addOnSuccessListener(aVoid -> showVoteRecordedMessage());
        }
    }

    private void showVoteRecordedMessage() {
        Toast.makeText(context, "Vote recorded!", Toast.LENGTH_SHORT).show();
        pollContainer.setVisibility(View.GONE);
    }

    private void createPollMessage(Task task) {
        Message pollMessage = new Message();
        pollMessage.setMessageId(UUID.randomUUID().toString());
        pollMessage.setCircleId(task.getCircleId());
        pollMessage.setSenderId(currentUserId);
        pollMessage.setText("A poll has started for task proof: " + task.getTitle());
        pollMessage.setType(MessageType.POLL_CREATED);
        pollMessage.setTaskId(task.getTaskId());
        pollMessage.setTimestamp(new Timestamp(new Date()));

        db.collection("messages").document(pollMessage.getMessageId())
                .set(pollMessage);
    }
}

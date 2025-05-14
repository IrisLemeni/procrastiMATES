package com.example.procrastimates.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.procrastimates.Message;
import com.example.procrastimates.MessageType;
import com.example.procrastimates.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class CircleChatActivity extends AppCompatActivity {
    private RecyclerView chatRecyclerView;
    private MessageAdapter messageAdapter;
    private EditText messageInput;
    private Button sendButton;
    private String circleId;
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_circle_chat);

        circleId = getIntent().getStringExtra("circleId");
        if (circleId == null || circleId.isEmpty()) {
            Log.e("CircleChatActivity", "No circleId provided!");
            Toast.makeText(this, "Error: No circle ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d("CircleChatActivity", "Activity started with circleId: " + circleId);
        // Restul codului...
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        initializeViews();
        setupRecyclerView();
        loadMessages();
        setupSendButton();
    }

    private void initializeViews() {
        chatRecyclerView = findViewById(R.id.chat_recycler_view);
        messageInput = findViewById(R.id.message_input);
        sendButton = findViewById(R.id.send_button);
    }

    private void setupRecyclerView() {
        messageAdapter = new MessageAdapter(this, currentUserId);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(messageAdapter);
    }

    private void loadMessages() {
        Log.d("CircleChatActivity", "Loading messages for circleId: " + circleId);

        db.collection("messages")
                .whereEqualTo("circleId", circleId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("CircleChatActivity", "Error loading messages", error);
                        return;
                    }

                    if (value != null) {
                        List<Message> messages = new ArrayList<>();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Message message = doc.toObject(Message.class);
                            if (message != null) {
                                Log.d("CircleChatActivity", "Message loaded: " + message.getMessageId()
                                        + ", Type: " + message.getType()
                                        + ", CircleId: " + message.getCircleId());
                                messages.add(message);
                            }
                        }

                        Log.d("CircleChatActivity", "Total messages loaded: " + messages.size());
                        messageAdapter.setMessages(messages);

                        if (!messages.isEmpty()) {
                            chatRecyclerView.scrollToPosition(messages.size() - 1);
                        }
                    }
                });
    }

    private void setupSendButton() {
        sendButton.setOnClickListener(v -> {
            String text = messageInput.getText().toString().trim();
            if (!text.isEmpty()) {
                sendMessage(text, MessageType.GENERAL_MESSAGE, null);
                messageInput.setText("");
            }
        });
    }

    public void sendMessage(String text, MessageType type, String taskId) {
        Message message = new Message();
        message.setMessageId(UUID.randomUUID().toString());
        message.setCircleId(circleId);
        message.setSenderId(currentUserId);
        message.setText(text);
        message.setType(type);
        message.setTaskId(taskId);
        message.setTimestamp(new Timestamp(new Date()));

        db.collection("messages").document(message.getMessageId())
                .set(message)
                .addOnSuccessListener(aVoid -> Log.d("CircleChatActivity", "Message sent successfully"))
                .addOnFailureListener(e -> Log.e("CircleChatActivity", "Error sending message", e));
    }
}

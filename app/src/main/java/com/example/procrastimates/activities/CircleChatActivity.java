package com.example.procrastimates.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.procrastimates.models.Message;
import com.example.procrastimates.enums.MessageType;
import com.example.procrastimates.R;
import com.example.procrastimates.adapters.MessageAdapter;
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
    private CardView sendButton;
    private ImageButton backButton;
    private TextView chatTitle;
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
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        initializeViews();
        setupRecyclerView();
        loadMessages();
        setupInputHandlers();
        loadCircleInfo();
    }

    private void initializeViews() {
        chatRecyclerView = findViewById(R.id.chat_recycler_view);
        messageInput = findViewById(R.id.message_input);
        sendButton = findViewById(R.id.send_button);
        backButton = findViewById(R.id.back_button);
        chatTitle = findViewById(R.id.chat_title);
    }

    private void setupRecyclerView() {
        messageAdapter = new MessageAdapter(this, currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(messageAdapter);

        // Scroll automat la ultimul mesaj când se adaugă unul nou
        messageAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int totalItems = messageAdapter.getItemCount();
                if (totalItems > 0) {
                    chatRecyclerView.smoothScrollToPosition(totalItems - 1);
                }
            }
        });
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

    private void setupInputHandlers() {
        // Buton Back
        backButton.setOnClickListener(v -> finish());

        // Buton Send
        sendButton.setOnClickListener(v -> sendMessageIfNotEmpty());

        // Send pe Enter în EditText
        messageInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                sendMessageIfNotEmpty();
                return true;
            }
            return false;
        });

    }

    private void loadCircleInfo() {
        // Încarcă numele cercului pentru a-l afișa în toolbar
        db.collection("circles").document(circleId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String circleName = document.getString("name");
                        if (circleName != null && !circleName.isEmpty()) {
                            chatTitle.setText(circleName);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("CircleChatActivity", "Error loading circle info", e);
                });
    }

    private void sendMessageIfNotEmpty() {
        String text = messageInput.getText().toString().trim();
        if (!text.isEmpty()) {
            sendMessage(text, MessageType.GENERAL_MESSAGE, null);
            messageInput.setText("");
        }
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
                .addOnFailureListener(e -> {
                    Log.e("CircleChatActivity", "Error sending message", e);
                    Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
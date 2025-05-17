package com.example.procrastimates.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.procrastimates.R;
import com.example.procrastimates.ai.AiServiceClient;
import com.example.procrastimates.adapters.ConversationAdapter;
import com.example.procrastimates.models.ConversationMessage;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import io.noties.markwon.Markwon;
import java.util.ArrayList;

public class AskAiActivity extends AppCompatActivity {
    private EditText questionInput;
    private Button sendButton;
    private TextView responseText;
    private ProgressBar progressBar;
    private RecyclerView historyRecyclerView;
    private Button viewHistoryButton;

    private AiServiceClient aiServiceClient;
    private Markwon markwon;
    private ArrayList<ConversationMessage> conversationHistory = new ArrayList<>();
    private ConversationAdapter conversationAdapter;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ask_ai);

        // Initialize Firebase if not already initialized
        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();

        // Initialize the Cloud Function client
        aiServiceClient = new AiServiceClient(this);

        // UI references
        questionInput = findViewById(R.id.questionInput);
        sendButton = findViewById(R.id.sendButton);
        responseText = findViewById(R.id.responseText);
        progressBar = findViewById(R.id.progressBar);
        historyRecyclerView = findViewById(R.id.historyRecyclerView);
        viewHistoryButton = findViewById(R.id.viewHistoryButton);

        markwon = Markwon.create(this);

        // Set up RecyclerView for conversation history
        setupHistoryRecyclerView();

        sendButton.setOnClickListener(v -> {
            String question = questionInput.getText().toString();

            if (question != null && !question.trim().isEmpty()) {
                askQuestion(question.trim());
                questionInput.setText(""); // Clear input field after sending
            } else {
                Toast.makeText(this, "Scrie o întrebare!", Toast.LENGTH_SHORT).show();
            }
        });

        viewHistoryButton.setOnClickListener(v -> {
            if (historyRecyclerView.getVisibility() == View.VISIBLE) {
                historyRecyclerView.setVisibility(View.GONE);
                viewHistoryButton.setText("Arată Istoricul");
            } else {
                loadConversationHistory();
                historyRecyclerView.setVisibility(View.VISIBLE);
                viewHistoryButton.setText("Ascunde Istoricul");
            }
        });

        // Load conversation history when activity starts
        loadConversationHistory();
    }

    private void setupHistoryRecyclerView() {
        conversationAdapter = new ConversationAdapter(this, conversationHistory, markwon);
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        historyRecyclerView.setAdapter(conversationAdapter);
    }

    private void loadConversationHistory() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String userId = user.getUid();
        db.collection("conversations")
                .document(userId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(20) // Limit to most recent 20 conversations
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    conversationHistory.clear();
                    for (int i = 0; i < queryDocumentSnapshots.size(); i++) {
                        String question = queryDocumentSnapshots.getDocuments().get(i).getString("question");
                        String answer = queryDocumentSnapshots.getDocuments().get(i).getString("answer");
                        conversationHistory.add(new ConversationMessage(question, answer));
                    }
                    conversationAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Eroare la încărcarea istoricului: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private boolean isRequestInProgress = false;
    private void askQuestion(String question) {
        // Add input validation with clearer error messages
        if (question == null || question.trim().isEmpty()) {
            Toast.makeText(this, "Întrebarea nu poate fi goală", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isRequestInProgress) return;

        // Show loading state
        isRequestInProgress = true;
        progressBar.setVisibility(View.VISIBLE);
        sendButton.setEnabled(false);
        responseText.setText("Se generează răspunsul...");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            aiServiceClient.askAi(question, userId, new AiServiceClient.AiCallback() {
                @Override
                public void onSuccess(String answer) {
                    isRequestInProgress = false;
                    progressBar.setVisibility(View.GONE);
                    sendButton.setEnabled(true);
                    markwon.setMarkdown(responseText, answer);

                    // Add the new message to the conversation history UI
                    conversationHistory.add(0, new ConversationMessage(question, answer));
                    conversationAdapter.notifyItemInserted(0);

                }

                @Override
                public void onError(String errorMessage) {
                    isRequestInProgress = false;
                    progressBar.setVisibility(View.GONE);
                    sendButton.setEnabled(true);
                    responseText.setText("Eroare: " + errorMessage);
                    Toast.makeText(AskAiActivity.this, "Nu am putut primi răspunsul.",
                            Toast.LENGTH_SHORT).show();
                }
            });

        } else {
            isRequestInProgress = false;
            Toast.makeText(this, "Trebuie să fii autentificat.", Toast.LENGTH_SHORT).show();
        }
    }
}
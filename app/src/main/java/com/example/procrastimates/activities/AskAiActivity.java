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
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import io.noties.markwon.Markwon;
import java.util.ArrayList;

public class AskAiActivity extends AppCompatActivity {
    private EditText questionInput;
    private Button sendButton;
    private TextView responseText;
    private ProgressBar progressBar;
    private RecyclerView historyRecyclerView;
    private Button viewHistoryButton;
    private LinearLayout emptyStateText;
    private View inputContainer;
    private View responseContainer;
    private View historyContainer;
    private AiServiceClient aiServiceClient;
    private Markwon markwon;
    private MaterialToolbar toolbar;
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
        emptyStateText = findViewById(R.id.emptyStateText);
        inputContainer = findViewById(R.id.inputContainer);
        responseContainer = findViewById(R.id.responseContainer);
        toolbar = findViewById(R.id.toolbar);

        // Find the history container (the MaterialCardView that contains the RecyclerView)
        historyContainer = (View) historyRecyclerView.getParent().getParent();

        markwon = Markwon.create(this);

        // Set up RecyclerView for conversation history
        setupHistoryRecyclerView();

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("AI Assistant");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        sendButton.setOnClickListener(v -> {
            String question = questionInput.getText().toString();

            if (question != null && !question.trim().isEmpty()) {
                askQuestion(question.trim());
                questionInput.setText("");
            } else {
                Toast.makeText(this, "Please enter a question!", Toast.LENGTH_SHORT).show();
            }
        });

        viewHistoryButton.setOnClickListener(v -> {
            if (historyContainer.getVisibility() == View.VISIBLE) {
                historyContainer.setVisibility(View.GONE);
                historyRecyclerView.setVisibility(View.GONE);
                viewHistoryButton.setText("Show History");
                viewHistoryButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_expand_more, 0);
            } else {
                loadConversationHistory();
                historyContainer.setVisibility(View.VISIBLE);
                historyRecyclerView.setVisibility(View.VISIBLE);
                viewHistoryButton.setText("Hide History");
                viewHistoryButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_expand_less, 0);
            }
        });

        loadConversationHistory();
        showEmptyState();
    }

    private void setupHistoryRecyclerView() {
        conversationAdapter = new ConversationAdapter(this, conversationHistory, markwon);
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        historyRecyclerView.setAdapter(conversationAdapter);
    }

    private void loadConversationHistory() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please login to view history", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();
        db.collection("conversations")
                .document(userId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    conversationHistory.clear();

                    System.out.println("Found " + queryDocumentSnapshots.size() + " conversation documents");

                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        String question = document.getString("question");
                        String answer = document.getString("answer");

                        System.out.println("Question: " + question + ", Answer: " + answer);

                        if (question != null && answer != null) {
                            conversationHistory.add(new ConversationMessage(question, answer));
                        }
                    }

                    conversationAdapter.notifyDataSetChanged();

                    if (conversationHistory.isEmpty()) {
                        viewHistoryButton.setVisibility(View.GONE);
                        historyContainer.setVisibility(View.GONE);
                        System.out.println("No conversation history found");
                    } else {
                        viewHistoryButton.setVisibility(View.VISIBLE);
                        System.out.println("Loaded " + conversationHistory.size() + " conversations");
                    }
                })
                .addOnFailureListener(e -> {
                    System.err.println("Error loading history: " + e.getMessage());
                    Toast.makeText(this, "Error loading history: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private boolean isRequestInProgress = false;

    private void askQuestion(String question) {
        if (question == null || question.trim().isEmpty()) {
            Toast.makeText(this, "Question cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isRequestInProgress) return;

        hideEmptyState();
        showResponseContainer();

        isRequestInProgress = true;
        progressBar.setVisibility(View.VISIBLE);
        sendButton.setEnabled(false);
        sendButton.setAlpha(0.5f);
        responseText.setText("Generating response...");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            aiServiceClient.askAi(question, userId, new AiServiceClient.AiCallback() {
                @Override
                public void onSuccess(String answer) {
                    isRequestInProgress = false;
                    progressBar.setVisibility(View.GONE);
                    sendButton.setEnabled(true);
                    sendButton.setAlpha(1.0f);
                    markwon.setMarkdown(responseText, answer);

                    conversationHistory.add(0, new ConversationMessage(question, answer));
                    conversationAdapter.notifyItemInserted(0);

                    if (viewHistoryButton.getVisibility() == View.GONE) {
                        viewHistoryButton.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    isRequestInProgress = false;
                    progressBar.setVisibility(View.GONE);
                    sendButton.setEnabled(true);
                    sendButton.setAlpha(1.0f);
                    responseText.setText("Error: " + errorMessage);
                    Toast.makeText(AskAiActivity.this, "Could not get response. Please try again.",
                            Toast.LENGTH_SHORT).show();
                }
            });

        } else {
            isRequestInProgress = false;
            progressBar.setVisibility(View.GONE);
            sendButton.setEnabled(true);
            sendButton.setAlpha(1.0f);
            Toast.makeText(this, "You must be authenticated.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showEmptyState() {
        emptyStateText.setVisibility(View.VISIBLE);
        responseContainer.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        emptyStateText.setVisibility(View.GONE);
    }

    private void showResponseContainer() {
        responseContainer.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

}
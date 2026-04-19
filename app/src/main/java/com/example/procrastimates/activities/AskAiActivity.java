package com.example.procrastimates.activities;

import android.os.Bundle;
import android.util.Log;
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

import io.noties.markwon.Markwon;
import java.util.ArrayList;

public class AskAiActivity extends AppCompatActivity {

    private static final String TAG = "AskAiActivity";

    // --- Fields kept as members because they are accessed across multiple methods ---
    private Button sendButton;
    private TextView responseText;
    private ProgressBar progressBar;
    private RecyclerView historyRecyclerView;
    private Button viewHistoryButton;
    private LinearLayout emptyStateText;
    private View responseContainer;
    private View historyContainer;
    private AiServiceClient aiServiceClient;
    private Markwon markwon;
    private ArrayList<ConversationMessage> conversationHistory = new ArrayList<>();
    private ConversationAdapter conversationAdapter;
    private FirebaseFirestore db;
    private boolean isRequestInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ask_ai);
        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();

        // Initialize the Cloud Function client
        aiServiceClient = new AiServiceClient(this);

        // Local variable — only used here to wire up the toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);

        // Local variable — only used inside the send click listener
        EditText questionInput = findViewById(R.id.questionInput);

        sendButton = findViewById(R.id.sendButton);
        responseText = findViewById(R.id.responseText);
        progressBar = findViewById(R.id.progressBar);
        historyRecyclerView = findViewById(R.id.historyRecyclerView);
        viewHistoryButton = findViewById(R.id.viewHistoryButton);
        emptyStateText = findViewById(R.id.emptyStateText);
        responseContainer = findViewById(R.id.responseContainer);

        historyContainer = (View) historyRecyclerView.getParent().getParent();

        markwon = Markwon.create(this);
        setupHistoryRecyclerView();

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.ai_assistant_title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        sendButton.setOnClickListener(v -> {
            String question = questionInput.getText().toString();
            if (!question.trim().isEmpty()) {
                askQuestion(question.trim());
                questionInput.setText("");
            } else {
                Toast.makeText(this, R.string.enter_question_prompt, Toast.LENGTH_SHORT).show();
            }
        });

        viewHistoryButton.setOnClickListener(v -> {
            if (historyContainer.getVisibility() == View.VISIBLE) {
                historyContainer.setVisibility(View.GONE);
                historyRecyclerView.setVisibility(View.GONE);
                viewHistoryButton.setText(R.string.show_history);
                viewHistoryButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_expand_more, 0);
            } else {
                loadConversationHistory();
                historyContainer.setVisibility(View.VISIBLE);
                historyRecyclerView.setVisibility(View.VISIBLE);
                viewHistoryButton.setText(R.string.hide_history);
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
            Toast.makeText(this, R.string.login_to_view_history, Toast.LENGTH_SHORT).show();
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

                    Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " conversation documents");

                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        String question = document.getString("question");
                        String answer = document.getString("answer");

                        Log.d(TAG, "Question: " + question + ", Answer: " + answer);

                        if (question != null && answer != null) {
                            conversationHistory.add(new ConversationMessage(question, answer));
                        }
                    }

                    // Use a targeted range notification instead of notifyDataSetChanged
                    conversationAdapter.notifyItemRangeInserted(0, conversationHistory.size());

                    if (conversationHistory.isEmpty()) {
                        viewHistoryButton.setVisibility(View.GONE);
                        historyContainer.setVisibility(View.GONE);
                        Log.d(TAG, "No conversation history found");
                    } else {
                        viewHistoryButton.setVisibility(View.VISIBLE);
                        Log.d(TAG, "Loaded " + conversationHistory.size() + " conversations");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading history: " + e.getMessage(), e);
                    Toast.makeText(this, R.string.error_loading_history, Toast.LENGTH_SHORT).show();
                });
    }

    private void askQuestion(String question) {
        if (question == null || question.trim().isEmpty()) {
            Toast.makeText(this, R.string.question_cannot_be_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        if (isRequestInProgress) return;

        hideEmptyState();
        showResponseContainer();

        isRequestInProgress = true;
        progressBar.setVisibility(View.VISIBLE);
        sendButton.setEnabled(false);
        sendButton.setAlpha(0.5f);
        responseText.setText(R.string.generating_response);

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
                    Log.e(TAG, "AI error: " + errorMessage);
                    responseText.setText(getString(R.string.error_prefix, errorMessage));
                    Toast.makeText(AskAiActivity.this, R.string.could_not_get_response, Toast.LENGTH_SHORT).show();
                }
            });

        } else {
            isRequestInProgress = false;
            progressBar.setVisibility(View.GONE);
            sendButton.setEnabled(true);
            sendButton.setAlpha(1.0f);
            Toast.makeText(this, R.string.must_be_authenticated, Toast.LENGTH_SHORT).show();
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
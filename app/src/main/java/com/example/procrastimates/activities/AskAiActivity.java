package com.example.procrastimates.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.example.procrastimates.R;
import com.example.procrastimates.ai.CloudFunctionClient;
import com.google.firebase.FirebaseApp;

public class AskAiActivity extends AppCompatActivity {
    private EditText questionInput;
    private Button sendButton;
    private TextView responseText;
    private ProgressBar progressBar;
    private CloudFunctionClient cloudFunctionClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ask_ai);

        // Initialize Firebase if not already initialized
        FirebaseApp.initializeApp(this);

        // Initialize the Cloud Function client
        cloudFunctionClient = new CloudFunctionClient();

        // UI references
        questionInput = findViewById(R.id.questionInput);
        sendButton = findViewById(R.id.sendButton);
        responseText = findViewById(R.id.responseText);
        progressBar = findViewById(R.id.progressBar); // Make sure to add this to your layout

        sendButton.setOnClickListener(v -> {
            String question = questionInput.getText().toString().trim();
            if (!question.isEmpty()) {
                askQuestion(question);
            } else {
                Toast.makeText(this, "Scrie o întrebare!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void askQuestion(String question) {
        // Add input validation
        if (question == null || question.trim().isEmpty()) {
            Toast.makeText(this, "Question cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        sendButton.setEnabled(false);
        responseText.setText("Se generează răspunsul...");

        // Add debug log
        System.out.println("Asking question: " + question);

        cloudFunctionClient.askAi(question.trim()) // Trim whitespace
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    sendButton.setEnabled(true);

                    if (task.isSuccessful()) {
                        String answer = task.getResult();
                        responseText.setText(answer != null ? answer : "Empty response");
                    } else {
                        Exception e = task.getException();
                        String errorMsg = "Error: " + (e != null ? e.getMessage() : "Unknown error");
                        responseText.setText(errorMsg);
                        System.err.println("Error: " + errorMsg);
                    }
                });
    }
}
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
        progressBar = findViewById(R.id.progressBar);

        sendButton.setOnClickListener(v -> {
            String question = questionInput.getText().toString();

            // Extra debug to check the raw input
            System.out.println("Raw input from EditText: [" + question + "]");

            if (question != null && !question.isEmpty()) {
                question = question.trim(); // Trim whitespace
                System.out.println("Trimmed question: [" + question + "]");

                if (!question.isEmpty()) {
                    askQuestion(question);
                } else {
                    Toast.makeText(this, "Scrie o întrebare!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Scrie o întrebare!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void askQuestion(String question) {
        // Add input validation with clearer error messages
        if (question == null) {
            Toast.makeText(this, "Question is null", Toast.LENGTH_SHORT).show();
            return;
        }

        if (question.trim().isEmpty()) {
            Toast.makeText(this, "Question cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state
        progressBar.setVisibility(View.VISIBLE);
        sendButton.setEnabled(false);
        responseText.setText("Se generează răspunsul...");

        cloudFunctionClient.askAi(question)
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
                        System.err.println("Error calling cloud function: " + errorMsg);

                        // Show a more user-friendly message
                        Toast.makeText(this, "Could not get an answer. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
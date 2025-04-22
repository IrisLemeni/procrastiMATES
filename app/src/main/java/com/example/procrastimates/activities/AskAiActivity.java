package com.example.procrastimates.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.example.procrastimates.R;
import com.example.procrastimates.ai.OpenAiClient;
import com.example.procrastimates.ai.OpenAiRequest;
import com.example.procrastimates.ai.OpenAiResponse;

import java.util.Collections;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AskAiActivity extends AppCompatActivity {

    private EditText questionInput;
    private Button sendButton;
    private TextView responseText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ask_ai);

        questionInput = findViewById(R.id.questionInput);
        sendButton = findViewById(R.id.sendButton);
        responseText = findViewById(R.id.responseText);

        sendButton.setOnClickListener(v -> {
            String question = questionInput.getText().toString().trim();
            if (!question.isEmpty()) {
                askOpenAi(question);
            } else {
                Toast.makeText(this, "Scrie o întrebare!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void askOpenAi(String question) {
        responseText.setText("Se generează răspunsul...");

        // Crează obiectul Message cu întrebarea utilizatorului
        OpenAiRequest.Message message = new OpenAiRequest.Message("user", question);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            OpenAiRequest request = new OpenAiRequest(
                    "gpt-3.5-turbo",
                    Collections.singletonList(message), // Folosește variabila 'message' definită mai sus
                    300
            );
            OpenAiClient.getService().askAi(request)
                    .enqueue(new Callback<OpenAiResponse>() {
                        @Override
                        public void onResponse(Call<OpenAiResponse> call, Response<OpenAiResponse> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                String answer = response.body().choices.get(0).message.content;
                                responseText.setText(answer);
                            } else {
                                responseText.setText("Eroare de răspuns: " + response.code());
                            }
                        }

                        @Override
                        public void onFailure(Call<OpenAiResponse> call, Throwable t) {
                            responseText.setText("Eroare de rețea: " + t.getMessage());
                        }
                    });
        }, 2000); // Delay de 2 secunde (2000 ms)
    }
}

package com.example.procrastimates.ai;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class AiServiceClient {

    private final RequestQueue requestQueue;
    private final String BASE_URL = "https://ai-server-qask.onrender.com/ask";
    private final FirebaseFirestore db;

    public interface AiCallback {
        void onSuccess(String answer);
        void onError(String errorMessage);
    }

    public AiServiceClient(Context context) {
        requestQueue = Volley.newRequestQueue(context.getApplicationContext());
        db = FirebaseFirestore.getInstance();
    }

    public void askAi(String question, String userId, AiCallback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("question", question);
            body.put("userId", userId);
        } catch (JSONException e) {
            callback.onError("Eroare la formarea JSON-ului");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                BASE_URL,
                body,
                response -> {
                    try {
                        String answer = response.getString("answer");
                        saveConversationToFirestore(userId, question, answer);
                        callback.onSuccess(answer);
                    } catch (JSONException e) {
                        callback.onError("Răspuns invalid de la server");
                    }
                },
                error -> {
                    error.printStackTrace();
                    callback.onError("Eroare la server: " + error.toString());
                }
        );

        requestQueue.add(request);
    }
    private void saveConversationToFirestore(String userId, String question, String answer) {
        Map<String, Object> conversationData = new HashMap<>();
        conversationData.put("question", question);
        conversationData.put("answer", answer);
        conversationData.put("timestamp", FieldValue.serverTimestamp());

        db.collection("conversations")
                .document(userId)
                .collection("messages")
                .add(conversationData)
                .addOnSuccessListener(documentReference -> {
                    System.out.println("Conversation saved with ID: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    System.err.println("Error saving conversation: " + e.getMessage());
                });
    }
}
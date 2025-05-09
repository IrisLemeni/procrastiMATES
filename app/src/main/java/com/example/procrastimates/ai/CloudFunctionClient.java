package com.example.procrastimates.ai;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

public class CloudFunctionClient {

    private final RequestQueue requestQueue;
    private final String BASE_URL = "https://ai-server-qask.onrender.com/ask";

    public interface AiCallback {
        void onSuccess(String answer);
        void onError(String errorMessage);
    }

    public CloudFunctionClient(Context context) {
        requestQueue = Volley.newRequestQueue(context.getApplicationContext());
    }

    public void askAi(String question, AiCallback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("question", question);
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
}
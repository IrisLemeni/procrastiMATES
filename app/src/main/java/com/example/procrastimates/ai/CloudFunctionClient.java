package com.example.procrastimates.ai;

import com.google.android.gms.tasks.Task;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.HashMap;
import java.util.Map;

public class CloudFunctionClient {
    private final FirebaseFunctions functions;

    public CloudFunctionClient() {
        functions = FirebaseFunctions.getInstance();
    }

    public Task<String> askAi(String question) {
        // Create the arguments properly
        Map<String, Object> data = new HashMap<>();
        data.put("question", question);

        // Add debug log
        System.out.println("Sending question: " + question);

        return functions
                .getHttpsCallable("askAi")
                .call(data)
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        Exception e = task.getException();
                        if (e != null) {
                            // Add more detailed error logging
                            System.err.println("Cloud function error: " + e.getMessage());
                            throw e;
                        }
                        throw new Exception("Unknown error");
                    }

                    HttpsCallableResult result = task.getResult();
                    if (result == null || result.getData() == null) {
                        throw new Exception("Empty response from server");
                    }

                    // Handle response more safely
                    Object responseData = result.getData();
                    if (responseData instanceof Map) {
                        Map<String, Object> response = (Map<String, Object>) responseData;
                        Object answer = response.get("answer");
                        if (answer instanceof String) {
                            return (String) answer;
                        }
                    }
                    throw new Exception("Invalid response format");
                });
    }
}
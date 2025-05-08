package com.example.procrastimates.ai;

import com.google.android.gms.tasks.Task;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableReference;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.HashMap;
import java.util.Map;

public class CloudFunctionClient {
    private final FirebaseFunctions functions;

    public CloudFunctionClient() {
        // Get the default instance
        functions = FirebaseFunctions.getInstance();
    }

    public Task<String> askAi(String question) {
        if (question == null) {
            System.err.println("Warning: Question is null");
            question = "";  // Convert null to empty string to avoid NPE
        }

        final String finalQuestion = question; // Need final for lambda
        System.out.println("Creating data object with question: [" + finalQuestion + "]");

        // Try using direct string approach
        // Some Cloud Functions might expect the raw data instead of a Map
        return functions
                .getHttpsCallable("askAi")
                .call(finalQuestion)  // Try sending the question directly as a string
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        Exception e = task.getException();
                        System.err.println("Cloud function call failed with direct string approach: " +
                                (e != null ? e.getMessage() : "Unknown error"));

                        // Fallback to Map approach will happen in askAiWithFallback
                        throw e;
                    }

                    return processCloudFunctionResult(task.getResult());
                })
                .continueWithTask(task -> {
                    if (task.isSuccessful()) {
                        return task;
                    }

                    // If direct string approach failed, try with a Map
                    System.out.println("Direct string approach failed, trying with Map...");
                    return askAiWithMap(finalQuestion);
                });
    }

    private Task<String> askAiWithMap(String question) {
        // Create the arguments as a Map
        Map<String, Object> data = new HashMap<>();
        data.put("question", question);
        System.out.println("Sending with Map approach: " + data);

        return functions
                .getHttpsCallable("askAi")
                .call(data)
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        Exception e = task.getException();
                        System.err.println("Cloud function call failed with Map approach: " +
                                (e != null ? e.getMessage() : "Unknown error"));
                        throw e;
                    }

                    return processCloudFunctionResult(task.getResult());
                });
    }

    private String processCloudFunctionResult(HttpsCallableResult result) {
        if (result == null) {
            throw new RuntimeException("Null result from cloud function");
        }

        Object responseData = result.getData();
        System.out.println("Raw response from cloud function: " + responseData);

        if (responseData == null) {
            throw new RuntimeException("Empty response data from server");
        }

        // Handle case where result is directly the answer string
        if (responseData instanceof String) {
            return (String) responseData;
        }

        // Handle case where result is a Map containing the answer
        if (responseData instanceof Map) {
            Map<String, Object> response = (Map<String, Object>) responseData;

            // Try to get answer from the Map
            Object answer = response.get("answer");
            if (answer instanceof String) {
                return (String) answer;
            } else {
                // If no answer field, check if there's only one value in the Map
                if (response.size() == 1) {
                    Object singleValue = response.values().iterator().next();
                    if (singleValue instanceof String) {
                        return (String) singleValue;
                    }
                }

                System.err.println("Answer is not a string: " +
                        (answer != null ? answer.getClass().getName() : "null"));
            }
        } else {
            System.err.println("Response is not a Map or String: " +
                    responseData.getClass().getName());
        }

        throw new RuntimeException("Invalid response format");
    }
}
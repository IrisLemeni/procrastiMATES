package com.example.procrastimates.ai;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class OpenAiClient {
    private static final String BASE_URL = "https://api.openai.com/";
    private static final // În OpenAiClient
    String apiKey = FirebaseRemoteConfig.getInstance().getString("openai_api_key");

    public static OpenAiService getService() {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request newRequest = chain.request().newBuilder()
                            .addHeader("Authorization", apiKey)
                            .build();
                    return chain.proceed(newRequest);
                })
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(OpenAiService.class);
    }
}


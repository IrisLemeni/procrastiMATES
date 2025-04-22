package com.example.procrastimates.ai;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface OpenAiService {
    @POST("v1/chat/completions")
    Call<OpenAiResponse> askAi(@Body OpenAiRequest request);
}

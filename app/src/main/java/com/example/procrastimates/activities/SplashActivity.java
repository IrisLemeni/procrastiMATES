package com.example.procrastimates.activities;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.procrastimates.R;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setEnterTransition(null);
        getWindow().setExitTransition(null);

        getWindow().setBackgroundDrawableResource(R.drawable.login_background);

        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.app_logo);

        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(
                    SplashActivity.this,
                    logo,
                    "appLogo"
            );
            startActivity(intent, options.toBundle());
            finish();
        }, 2000); // Așteaptă 2 secunde
    }
}

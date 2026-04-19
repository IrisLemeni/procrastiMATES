package com.example.procrastimates.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.procrastimates.R;

public class FullScreenImageActivity extends AppCompatActivity {

    private ImageView fullScreenImageView;
    private ProgressBar progressBar;
    private String imageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_image);

        // Hide system UI for immersive experience
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        initViews();
        loadImage();
        setupClickListeners();
    }

    private void initViews() {
        fullScreenImageView = findViewById(R.id.fullscreen_image);
        progressBar = findViewById(R.id.progress_bar);

        // Get image URL from intent
        imageUrl = getIntent().getStringExtra("image_url");

        if (imageUrl == null || imageUrl.isEmpty()) {
            Toast.makeText(this, "Image not available", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadImage() {
        progressBar.setVisibility(View.VISIBLE);

        Glide.with(this)
                .load(imageUrl)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(fullScreenImageView);

        // Hide progress bar after a short delay (Glide doesn't have a direct callback for this setup)
        fullScreenImageView.postDelayed(() -> progressBar.setVisibility(View.GONE), 1000);
    }

    private void setupClickListeners() {
        // Close activity when image is clicked
        fullScreenImageView.setOnClickListener(v -> finish());

        // Also close when clicking outside the image
        findViewById(R.id.fullscreen_container).setOnClickListener(v -> finish());

        // Close activity when close button is clicked
        findViewById(R.id.close_button).setOnClickListener(v -> finish());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
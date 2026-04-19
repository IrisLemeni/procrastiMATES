package com.example.procrastimates.activities;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

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

        // Hide system UI for immersive experience (only on API 30+ to avoid deprecated APIs)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.systemBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }
        // On older Android versions, system bars remain visible (no deprecated API usage)

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
    }
}
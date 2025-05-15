package com.example.procrastimates;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.procrastimates.activities.AchievementsActivity;
import com.example.procrastimates.models.Achievement;
import com.squareup.picasso.Picasso;

public class AchievementDialogHelper {

    public static void showAchievementUnlockedDialog(Activity activity, Achievement achievement) {
        // Create custom dialog
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.dialog_achievement_unlocked);

        // Set dialog window properties
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            // Set explicit width to prevent squeezing
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(window.getAttributes());
            layoutParams.width = (int)(activity.getResources().getDisplayMetrics().widthPixels * 0.85);
            window.setAttributes(layoutParams);
        }

        // Set dialog UI elements
        TextView titleTextView = dialog.findViewById(R.id.textAchievementTitle);
        TextView descriptionTextView = dialog.findViewById(R.id.textAchievementDescription);
        ImageView iconImageView = dialog.findViewById(R.id.imageAchievement);

        titleTextView.setText(achievement.getTitle());
        descriptionTextView.setText(achievement.getDescription());

        // Load achievement icon
        if (achievement.getIconUrl().startsWith("icons/")) {
            // For local drawables, we need to get the resource ID
            String iconName = achievement.getIconUrl().replace("icons/", "");
            iconName = iconName.replace(".png", "");
            int resourceId = activity.getResources().getIdentifier(
                    iconName, "drawable", activity.getPackageName());

            if (resourceId != 0) {
                iconImageView.setImageResource(resourceId);
            } else {
                // Fallback to a default icon
                iconImageView.setImageResource(R.drawable.ic_achievement_default);
            }
        } else {
            // For remote URLs, load with Picasso
            Picasso.get()
                    .load(achievement.getIconUrl())
                    .placeholder(R.drawable.ic_achievement_default)
                    .error(R.drawable.ic_achievement_default)
                    .into(iconImageView);
        }

        // Set button actions
        Button btnGoToAchievements = dialog.findViewById(R.id.btnGoToAchievements);
        Button btnOk = dialog.findViewById(R.id.btnOk);

        btnGoToAchievements.setOnClickListener(v -> {
            dialog.dismiss();
            // Navigate to Achievements activity
            Intent intent = new Intent(activity, AchievementsActivity.class);
            activity.startActivity(intent);
        });

        btnOk.setOnClickListener(v -> dialog.dismiss());

        // Show the dialog
        dialog.show();
    }
}
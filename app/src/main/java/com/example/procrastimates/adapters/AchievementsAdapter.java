package com.example.procrastimates.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.procrastimates.R;
import com.example.procrastimates.models.Achievement;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.List;


public class AchievementsAdapter extends RecyclerView.Adapter<AchievementsAdapter.AchievementViewHolder> {

    private static final String TAG = "AchievementsAdapter";
    private Context context;
    private List<Achievement> achievements;

    public AchievementsAdapter(Context context, List<Achievement> achievements) {
        this.context = context;
        this.achievements = achievements;
    }

    @NonNull
    @Override
    public AchievementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_achievement, parent, false);
        return new AchievementViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AchievementViewHolder holder, int position) {
        Achievement achievement = achievements.get(position);

        holder.textTitle.setText(achievement.getTitle());
        holder.textDescription.setText(achievement.getDescription());

        if (achievement.isUnlocked()) {
            loadAchievementIcon(holder.imageIcon, achievement);
            holder.imageLock.setVisibility(View.GONE);
            holder.itemView.setAlpha(1.0f);
            holder.textTitle.setTextColor(context.getResources().getColor(R.color.achievement_unlocked_text));
        } else {
            loadAchievementIcon(holder.imageIcon, achievement);
            holder.imageLock.setVisibility(View.VISIBLE);
            holder.itemView.setAlpha(0.5f);
            holder.textTitle.setTextColor(context.getResources().getColor(R.color.achievement_locked_text));
        }
    }

    private void loadAchievementIcon(ImageView imageView, Achievement achievement) {
        String iconUrl = achievement.getIconUrl();
        String achievementId = achievement.getId();

        Log.d(TAG, "Loading icon for achievement: " + achievementId + ", iconUrl: " + iconUrl);

        int resourceId = getLocalIconResourceId(achievementId);
        if (resourceId != R.drawable.placeholder_achievement) {
            Log.d(TAG, "Loading local resource for " + achievementId);
            imageView.setImageResource(resourceId);
            return;
        }

        if (iconUrl != null && !iconUrl.isEmpty()) {
            if (iconUrl.startsWith("icons/")) {
                String firebaseStorageUrl = "https://firebasestorage.googleapis.com/v0/b/procrastinate-cc91b.appspot.com/o/" +
                        iconUrl.replace("/", "%2F") + "?alt=media";

                Log.d(TAG, "Loading from Firebase: " + firebaseStorageUrl);

                Picasso.get()
                        .load(firebaseStorageUrl)
                        .placeholder(R.drawable.placeholder_achievement)
                        .error(R.drawable.placeholder_achievement)
                        .into(imageView, new Callback() {
                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "Successfully loaded image for " + achievementId);
                            }

                            @Override
                            public void onError(Exception e) {
                                Log.e(TAG, "Failed to load image for " + achievementId, e);
                                imageView.setImageResource(resourceId);
                            }
                        });
            } else {
                Log.d(TAG, "Attempting to load direct URL: " + iconUrl);
                Picasso.get()
                        .load(iconUrl)
                        .placeholder(R.drawable.placeholder_achievement)
                        .error(R.drawable.placeholder_achievement)
                        .into(imageView);
            }
        } else {
            Log.d(TAG, "No icon URL for " + achievementId + ", using placeholder");
            imageView.setImageResource(R.drawable.placeholder_achievement);
        }
    }

    private int getLocalIconResourceId(String achievementId) {
        try {
            int resourceId = context.getResources().getIdentifier(
                    achievementId.replace("_", ""),
                    "drawable",
                    context.getPackageName()
            );

            if (resourceId != 0) {
                Log.d(TAG, "Found dynamic resource ID: " + resourceId);
                return resourceId;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error finding dynamic resource:", e);
        }

        switch (achievementId) {
            case "session_starter":
                return R.drawable.launcher;
            case "focus_apprentice":
                return R.drawable.apprentice;
            case "focus_master":
                return R.drawable.master;
            case "focus_wizard":
                return R.drawable.wizard;
            case "laser_focus":
                return R.drawable.laser;
            case "undistracted":
                return R.drawable.focus;
            case "concentration_guru":
                return R.drawable.guru;
            case "daily_streak":
                return R.drawable.fire;
            case "weekly_focus":
                return R.drawable.timetable;
            case "focus_warrior":
                return R.drawable.swordsman;
            case "marathon_focus":
                return R.drawable.winner;
            case "endurance_master":
                return R.drawable.persistence;
            default:
                Log.d(TAG, "No match found for " + achievementId + ", using placeholder");
                return R.drawable.placeholder_achievement;
        }
    }

    @Override
    public int getItemCount() {
        return achievements.size();
    }

    public static class AchievementViewHolder extends RecyclerView.ViewHolder {
        ImageView imageIcon;
        ImageView imageLock;
        TextView textTitle;
        TextView textDescription;

        public AchievementViewHolder(@NonNull View itemView) {
            super(itemView);
            imageIcon = itemView.findViewById(R.id.imageAchievementIcon);
            imageLock = itemView.findViewById(R.id.imageAchievementLock);
            textTitle = itemView.findViewById(R.id.textAchievementTitle);
            textDescription = itemView.findViewById(R.id.textAchievementDescription);
        }
    }
}
package com.example.procrastimates;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.procrastimates.R;
import com.example.procrastimates.Achievement;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Adapter pentru afișarea achievement-urilor în RecyclerView
 */
public class AchievementsAdapter extends RecyclerView.Adapter<AchievementsAdapter.AchievementViewHolder> {

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

        // Încarcă iconița achievement-ului
        if (achievement.getIconUrl() != null && !achievement.getIconUrl().isEmpty()) {
            Picasso.get()
                    .load(achievement.getIconUrl())
                    .placeholder(R.drawable.placeholder_achievement)
                    .error(R.drawable.placeholder_achievement)
                    .into(holder.imageIcon);
        } else {
            holder.imageIcon.setImageResource(R.drawable.placeholder_achievement);
        }

        // Setează stilul pentru achievement-uri deblocate/blocate
        if (achievement.isUnlocked()) {
            holder.itemView.setAlpha(1.0f);
            holder.textTitle.setTextColor(context.getResources().getColor(R.color.achievement_unlocked_text));
            holder.imageLock.setVisibility(View.GONE);
        } else {
            holder.itemView.setAlpha(0.5f);
            holder.textTitle.setTextColor(context.getResources().getColor(R.color.achievement_locked_text));
            holder.imageLock.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return achievements.size();
    }

    /**
     * ViewHolder pentru elementele de achievement
     */
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
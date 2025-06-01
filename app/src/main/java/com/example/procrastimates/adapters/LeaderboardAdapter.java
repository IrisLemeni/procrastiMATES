package com.example.procrastimates.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.procrastimates.R;
import com.example.procrastimates.models.Friend;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_PODIUM = 0;
    private static final int VIEW_TYPE_NORMAL = 1;

    private List<Friend> friendsList;
    private boolean isPodiumView;

    public LeaderboardAdapter(List<Friend> friendsList, boolean isPodiumView) {
        this.friendsList = friendsList;
        this.isPodiumView = isPodiumView;
    }

    public LeaderboardAdapter(List<Friend> friendsList) {
        this.friendsList = friendsList;
        this.isPodiumView = false;
    }

    public void setFriends(List<Friend> friendsList) {
        this.friendsList = friendsList;
        notifyDataSetChanged();
    }

    public void setPodiumView(boolean isPodiumView) {
        this.isPodiumView = isPodiumView;
        notifyDataSetChanged(); // Add this to refresh the view when switching
    }

    @Override
    public int getItemViewType(int position) {
        return isPodiumView ? VIEW_TYPE_PODIUM : VIEW_TYPE_NORMAL;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_PODIUM) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_podium_friend, parent, false);
            return new PodiumViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend_leaderboard, parent, false);
            return new NormalViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Friend friend = friendsList.get(position);

        if (holder instanceof PodiumViewHolder) {
            PodiumViewHolder podiumHolder = (PodiumViewHolder) holder;
            podiumHolder.friendName.setText(friend.getName());

            // Show both completed and total tasks for better synchronization
            String taskText = friend.getCompletedTasks() + "/" + friend.getTotalTasks();
            podiumHolder.completedTasks.setText(taskText);

            // Load profile image if available
            if (friend.getProfileImageUrl() != null && !friend.getProfileImageUrl().isEmpty()) {
                Glide.with(podiumHolder.itemView.getContext())
                        .load(friend.getProfileImageUrl())
                        .circleCrop()
                        .placeholder(R.drawable.default_user_image)
                        .into(podiumHolder.friendAvatar);
            } else {
                podiumHolder.friendAvatar.setImageResource(R.drawable.default_user_image);
            }

        } else if (holder instanceof NormalViewHolder) {
            NormalViewHolder normalHolder = (NormalViewHolder) holder;
            normalHolder.name.setText((position + 4) + ". " + friend.getName());
            normalHolder.completedTasks.setText("Completed Tasks: " + friend.getCompletedTasks());
            normalHolder.totalTasks.setText("Total Tasks: " + friend.getTotalTasks());
        }
    }

    @Override
    public int getItemCount() {
        return friendsList.size();
    }

    public static class PodiumViewHolder extends RecyclerView.ViewHolder {
        TextView friendName, completedTasks;
        ImageView friendAvatar, medalIcon;
        MaterialCardView medalCard;

        public PodiumViewHolder(View itemView) {
            super(itemView);
            friendName = itemView.findViewById(R.id.friendName);
            completedTasks = itemView.findViewById(R.id.completedTasks);
            friendAvatar = itemView.findViewById(R.id.friendAvatar);
        }
    }

    public static class NormalViewHolder extends RecyclerView.ViewHolder {
        TextView name, completedTasks, totalTasks;

        public NormalViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.friendName);
            completedTasks = itemView.findViewById(R.id.completedTasks);
            totalTasks = itemView.findViewById(R.id.totalTasks);
        }
    }
}
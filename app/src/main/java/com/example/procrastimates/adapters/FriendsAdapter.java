package com.example.procrastimates.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.procrastimates.R;
import com.example.procrastimates.models.Friend;

import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.FriendViewHolder> {
    private List<Friend> friendsList;

    public FriendsAdapter(List<Friend> friendsList) {
        this.friendsList = friendsList;
    }

    public void setFriends(List<Friend> friendsList) {
        this.friendsList = friendsList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend, parent, false);
        return new FriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        Friend friend = friendsList.get(position);

        // Set friend name
        holder.friendName.setText(friend.getName());

        // Set progress text
        holder.progressText.setText(friend.getCompletedTasks() + "/" + friend.getTotalTasks() + " tasks completed");

        // Calculate and set progress percentage
        int progressPercentage = 0;
        if (friend.getTotalTasks() > 0) {
            progressPercentage = (int) ((float) friend.getCompletedTasks() / friend.getTotalTasks() * 100);
        }

        // Set progress bar (0-100)
        holder.friendTaskProgress.setMax(100);
        holder.friendTaskProgress.setProgress(progressPercentage);

        // Set progress percentage text
        holder.progressPercentage.setText(progressPercentage + "%");

        // Set ranking badge (position + 1 since position starts from 0)
        holder.rankBadge.setText(String.valueOf(position + 1));

        // Load profile image using Glide
        if (friend.getProfileImageUrl() != null && !friend.getProfileImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(friend.getProfileImageUrl())
                    .circleCrop()
                    .placeholder(R.drawable.default_user_image)
                    .error(R.drawable.default_user_image)
                    .into(holder.friendAvatar);
        } else {
            // Set default image if no profile image URL
            holder.friendAvatar.setImageResource(R.drawable.default_user_image);
        }
    }

    @Override
    public int getItemCount() {
        return friendsList.size();
    }

    public static class FriendViewHolder extends RecyclerView.ViewHolder {
        TextView friendName, progressText, progressPercentage, rankBadge;
        ProgressBar friendTaskProgress;
        ImageView friendAvatar;

        public FriendViewHolder(View itemView) {
            super(itemView);
            friendName = itemView.findViewById(R.id.friendName);
            friendTaskProgress = itemView.findViewById(R.id.friendTaskProgress);
            progressText = itemView.findViewById(R.id.progressText);
            progressPercentage = itemView.findViewById(R.id.progressPercentage);
            rankBadge = itemView.findViewById(R.id.rankBadge);
            friendAvatar = itemView.findViewById(R.id.friendAvatar);
        }
    }
}
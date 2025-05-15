package com.example.procrastimates.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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
        holder.friendName.setText(friend.getName());
        holder.friendTaskProgress.setProgress(friend.getProgress());
        holder.progressText.setText(friend.getCompletedTasks() + "/" + friend.getTotalTasks() + " tasks completed");
    }

    @Override
    public int getItemCount() {
        return friendsList.size();
    }

    public static class FriendViewHolder extends RecyclerView.ViewHolder {
        TextView friendName, progressText;
        ProgressBar friendTaskProgress;

        public FriendViewHolder(View itemView) {
            super(itemView);
            friendName = itemView.findViewById(R.id.friendName);
            friendTaskProgress = itemView.findViewById(R.id.friendTaskProgress);
            progressText = itemView.findViewById(R.id.progressText);
        }
    }
}
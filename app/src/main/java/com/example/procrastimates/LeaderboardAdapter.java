package com.example.procrastimates;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.checkerframework.checker.units.qual.C;

import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.LeaderboardViewHolder> {
    private List<Friend> friendsList;

    public LeaderboardAdapter(List<Friend> friendsList) {
        this.friendsList = friendsList;
    }

    public void setFriends(List<Friend> friendsList) {
        this.friendsList = friendsList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LeaderboardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend_leaderboard, parent, false);
        return new LeaderboardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LeaderboardViewHolder holder, int position) {
        Friend friend = friendsList.get(position);
        holder.name.setText(friend.getName());
        holder.completedTasks.setText("Completed Tasks: " + friend.getCompletedTasks());
        holder.totalTasks.setText("Total Tasks: " + friend.getTotalTasks());

        // Dacă este în top 3, aplică un stil special (ex: coroană, aur, etc.)
        if (position == 0) {
            holder.itemView.setBackgroundColor(Color.YELLOW); // Exemplu: fundal galben pentru primul loc
        } else if (position == 1) {
            holder.itemView.setBackgroundColor(Color.MAGENTA);

        }

    }

    @Override
    public int getItemCount() {
        return friendsList.size();
    }

    public static class LeaderboardViewHolder extends RecyclerView.ViewHolder {
        TextView name, completedTasks, totalTasks;

        public LeaderboardViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.friendName);
            completedTasks = itemView.findViewById(R.id.completedTasks);
            totalTasks = itemView.findViewById(R.id.totalTasks);
        }
    }
}


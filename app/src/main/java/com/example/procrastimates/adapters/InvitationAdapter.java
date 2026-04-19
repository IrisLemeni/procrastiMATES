package com.example.procrastimates.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.procrastimates.R;
import com.example.procrastimates.activities.InvitationsActivity;
import com.example.procrastimates.models.Invitation;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class InvitationAdapter extends RecyclerView.Adapter<InvitationAdapter.ViewHolder> {

    private List<Invitation> invitations;
    private FirebaseFirestore db;
    private InvitationsActivity invitationsActivity;

    public InvitationAdapter(List<Invitation> invitations, InvitationsActivity invitationsActivity) {
        this.invitations = invitations;
        this.db = FirebaseFirestore.getInstance();
        this.invitationsActivity = invitationsActivity;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_invitation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Invitation invitation = invitations.get(position);

        // Set sender name and invitation message
        String senderName = invitation.getSenderName();
        if (senderName == null || senderName.trim().isEmpty()) {
            senderName = "Unknown User";
        }
        String invitationMessage = senderName + " wants to add you to their circle";
        holder.senderNameText.setText(invitationMessage);

        // Set avatar initials
        String initials = getInitials(senderName);
        holder.avatarText.setText(initials);

        // Format and display date using Timestamp
        String formattedDate = formatInvitationDate(invitation.getTimestamp());
        holder.dateTextView.setText(formattedDate);

        // Set button click listeners
        holder.acceptButton.setOnClickListener(v -> {
            invitationsActivity.acceptInvitation(invitation);
        });

        holder.declineButton.setOnClickListener(v -> {
            invitationsActivity.declineInvitation(invitation);
        });
    }

    @Override
    public int getItemCount() {
        return invitations.size();
    }

    /**
     * Get initials from a full name
     */
    private String getInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "?";
        }

        String[] names = fullName.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();

        for (int i = 0; i < Math.min(names.length, 2); i++) {
            if (!names[i].isEmpty()) {
                initials.append(names[i].charAt(0));
            }
        }

        return initials.toString().toUpperCase(Locale.ROOT);
    }

    /**
     * Format invitation date to show relative time using Firebase Timestamp
     */
    private String formatInvitationDate(Timestamp timestamp) {
        if (timestamp == null) {
            return "Just now";
        }

        Date date = timestamp.toDate();
        long now = System.currentTimeMillis();
        long invitationTime = date.getTime();
        long diff = now - invitationTime;

        // If in the future or just happened
        if (diff < 0 || diff < TimeUnit.MINUTES.toMillis(1)) {
            return "Just now";
        }

        // Try different time units
        String result = formatTimeUnit(diff, TimeUnit.HOURS.toMillis(1), "minute", "minutes");
        if (result != null) return result;

        result = formatTimeUnit(diff, TimeUnit.DAYS.toMillis(1), "hour", "hours");
        if (result != null) return result;

        result = formatDays(diff);
        if (result != null) return result;

        result = formatTimeUnit(diff, TimeUnit.DAYS.toMillis(30), "week", "weeks");
        if (result != null) return result;

        // More than a month ago - show actual date
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(date);
    }

    private String formatTimeUnit(long diff, long unitMillis, String singular, String plural) {
        if (diff < unitMillis) {
            long units;
            if (unitMillis == TimeUnit.HOURS.toMillis(1)) {
                units = TimeUnit.MILLISECONDS.toMinutes(diff);
            } else if (unitMillis == TimeUnit.DAYS.toMillis(1)) {
                units = TimeUnit.MILLISECONDS.toHours(diff);
            } else if (unitMillis == TimeUnit.DAYS.toMillis(30)) {
                units = TimeUnit.MILLISECONDS.toDays(diff) / 7;
            } else {
                return null;
            }
            return units + (units == 1 ? " " + singular + " ago" : " " + plural + " ago");
        }
        return null;
    }

    private String formatDays(long diff) {
        if (diff < TimeUnit.DAYS.toMillis(7)) {
            long days = TimeUnit.MILLISECONDS.toDays(diff);
            if (days == 1) {
                return "Yesterday";
            } else {
                return days + " days ago";
            }
        }
        return null;
    }

    public void updateInvitations(List<Invitation> newInvitations) {
        this.invitations = newInvitations;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView avatarText;
        public TextView senderNameText;
        public TextView dateTextView;
        public MaterialButton acceptButton;
        public MaterialButton declineButton;

        public ViewHolder(View itemView) {
            super(itemView);
            avatarText = itemView.findViewById(R.id.avatarText);
            senderNameText = itemView.findViewById(R.id.senderNameText);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            declineButton = itemView.findViewById(R.id.declineButton);
        }
    }
}
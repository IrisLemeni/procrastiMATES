package com.example.procrastimates;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.procrastimates.activities.NotificationsActivity;
import com.example.procrastimates.models.Invitation;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class InvitationAdapter extends RecyclerView.Adapter<InvitationAdapter.ViewHolder> {

    private List<Invitation> invitations;
    private FirebaseFirestore db;
    private NotificationsActivity notificationsActivity;

    public InvitationAdapter(List<Invitation> invitations,  NotificationsActivity notificationsActivity) {
        this.invitations = invitations;
        this.db = FirebaseFirestore.getInstance();
        this.notificationsActivity = notificationsActivity;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_invitation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Invitation invitation = invitations.get(position);

        String message = invitation.getSenderName() + " wants you to join their circle";
        holder.statusTextView.setText(message);

        holder.acceptButton.setOnClickListener(v -> {
            // Folosește direct NotificationsActivity din constructor
            notificationsActivity.acceptInvitation(invitation);
        });

        holder.declineButton.setOnClickListener(v -> {
            // Folosește direct NotificationsActivity din constructor
            notificationsActivity.declineInvitation(invitation);
        });
    }

    @Override
    public int getItemCount() {
        return invitations.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView statusTextView;
        public Button acceptButton;
        public Button declineButton;

        public ViewHolder(View itemView) {
            super(itemView);
            statusTextView = itemView.findViewById(R.id.statusTextView);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            declineButton = itemView.findViewById(R.id.declineButton);
        }
    }
}

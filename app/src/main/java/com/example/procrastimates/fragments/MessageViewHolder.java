package com.example.procrastimates.fragments;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.procrastimates.Message;
import com.example.procrastimates.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Locale;

class MessageViewHolder extends RecyclerView.ViewHolder {
    TextView messageText, senderName, timestamp;
    private final Context context;
    private final String currentUserId;
    private final FirebaseFirestore db;


    MessageViewHolder(View itemView, Context context, String currentUserId, FirebaseFirestore db) {
        super(itemView);
        this.context = context;
        this.currentUserId = currentUserId;
        this.db = db;
        messageText = itemView.findViewById(R.id.message_text);
        senderName = itemView.findViewById(R.id.sender_name);
        timestamp = itemView.findViewById(R.id.timestamp);

    }

    void bind(Message message) {
        messageText.setText(message.getText());

        // Încarcă numele expeditorului
        db.collection("users").document(message.getSenderId())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String name = document.getString("username");
                        senderName.setText(name != null ? name : "Unknown");
                    }
                });

        // Formatează timestamp-ul
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm, dd MMM", Locale.getDefault());
        timestamp.setText(sdf.format(message.getTimestamp().toDate()));
    }
}

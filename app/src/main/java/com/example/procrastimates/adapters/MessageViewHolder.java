package com.example.procrastimates.adapters;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.procrastimates.models.Message;
import com.example.procrastimates.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Locale;

class MessageViewHolder extends RecyclerView.ViewHolder {
    // Containerele pentru mesajele mele vs. ale celorlalți
    LinearLayout myMessageContainer, otherMessageContainer;

    // Elementele pentru mesajele mele
    TextView myMessageText, myTimestamp;

    // Elementele pentru mesajele celorlalți
    TextView otherMessageText, otherTimestamp, senderName, avatarInitial;

    private final Context context;
    private final String currentUserId;
    private final FirebaseFirestore db;

    MessageViewHolder(View itemView, Context context, String currentUserId, FirebaseFirestore db) {
        super(itemView);
        this.context = context;
        this.currentUserId = currentUserId;
        this.db = db;

        // Inițializez containerele
        myMessageContainer = itemView.findViewById(R.id.my_message_container);
        otherMessageContainer = itemView.findViewById(R.id.other_message_container);

        // Elementele pentru mesajele mele
        myMessageText = itemView.findViewById(R.id.my_message_text);
        myTimestamp = itemView.findViewById(R.id.my_timestamp);

        // Elementele pentru mesajele celorlalți
        otherMessageText = itemView.findViewById(R.id.other_message_text);
        otherTimestamp = itemView.findViewById(R.id.other_timestamp);
        senderName = itemView.findViewById(R.id.sender_name);
        avatarInitial = itemView.findViewById(R.id.avatar_initial);
    }

    void bind(Message message) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String timeText = sdf.format(message.getTimestamp().toDate());

        // Verific dacă mesajul este trimis de utilizatorul curent
        boolean isMyMessage = message.getSenderId().equals(currentUserId);

        if (isMyMessage) {
            // Afișez mesajul meu (partea dreaptă)
            myMessageContainer.setVisibility(View.VISIBLE);
            otherMessageContainer.setVisibility(View.GONE);

            myMessageText.setText(message.getText());
            myTimestamp.setText(timeText);

        } else {
            // Afișez mesajul altcuiva (partea stângă)
            myMessageContainer.setVisibility(View.GONE);
            otherMessageContainer.setVisibility(View.VISIBLE);

            otherMessageText.setText(message.getText());
            otherTimestamp.setText(timeText);

            // Încarcă numele expeditorului
            db.collection("users").document(message.getSenderId())
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            String name = document.getString("username");
                            if (name != null && !name.isEmpty()) {
                                senderName.setText(name);
                                // Setez prima literă pentru avatar
                                avatarInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
                            } else {
                                senderName.setText("Unknown");
                                avatarInitial.setText("?");
                            }
                        } else {
                            senderName.setText("Unknown");
                            avatarInitial.setText("?");
                        }
                    })
                    .addOnFailureListener(e -> {
                        senderName.setText("Unknown");
                        avatarInitial.setText("?");
                    });
        }
    }
}
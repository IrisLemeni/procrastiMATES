package com.example.procrastimates.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.procrastimates.R;
import com.example.procrastimates.models.ConversationMessage;

import java.util.ArrayList;

import io.noties.markwon.Markwon;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {
    private final ArrayList<ConversationMessage> conversationMessages;
    private final Context context;
    private final Markwon markwon;

    public ConversationAdapter(Context context, ArrayList<ConversationMessage> messages, Markwon markwon) {
        this.context = context;
        this.conversationMessages = messages;
        this.markwon = markwon;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ConversationMessage message = conversationMessages.get(position);
        holder.questionTextView.setText(message.getQuestion());
        markwon.setMarkdown(holder.answerTextView, message.getAnswer());
    }

    @Override
    public int getItemCount() {
        return conversationMessages.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView questionTextView;
        public TextView answerTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            questionTextView = itemView.findViewById(R.id.questionTextView);
            answerTextView = itemView.findViewById(R.id.answerTextView);
        }
    }
}
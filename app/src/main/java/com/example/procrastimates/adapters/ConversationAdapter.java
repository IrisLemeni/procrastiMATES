package com.example.procrastimates.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.procrastimates.R;
import com.example.procrastimates.models.ConversationMessage;
import io.noties.markwon.Markwon;
import java.util.List;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder> {

    private Context context;
    private List<ConversationMessage> conversations;
    private Markwon markwon;

    public ConversationAdapter(Context context, List<ConversationMessage> conversations, Markwon markwon) {
        this.context = context;
        this.conversations = conversations;
        this.markwon = markwon;
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_conversation, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        ConversationMessage conversation = conversations.get(position);

        holder.questionText.setText(conversation.getQuestion());
        markwon.setMarkdown(holder.answerText, conversation.getAnswer());

        // Add a subtle animation
        holder.itemView.setAlpha(0f);
        holder.itemView.animate()
                .alpha(1f)
                .setDuration(300)
                .setStartDelay(position * 50L)
                .start();
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    public static class ConversationViewHolder extends RecyclerView.ViewHolder {
        TextView questionText;
        TextView answerText;
        ImageView userIcon;
        ImageView aiIcon;

        public ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
            questionText = itemView.findViewById(R.id.questionText);
            answerText = itemView.findViewById(R.id.answerText);
            userIcon = itemView.findViewById(R.id.userIcon);
            aiIcon = itemView.findViewById(R.id.aiIcon);
        }
    }
}
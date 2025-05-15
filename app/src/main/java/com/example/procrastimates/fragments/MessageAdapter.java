package com.example.procrastimates.fragments;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.example.procrastimates.models.Message;
import com.example.procrastimates.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<Message> messages = new ArrayList<>();
    private Context context;
    private String currentUserId;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private static final int VIEW_TYPE_MESSAGE = 0;
    private static final int VIEW_TYPE_TASK_COMPLETED = 1;
    private static final int VIEW_TYPE_OBJECTION = 2;
    private static final int VIEW_TYPE_PROOF = 3;
    private static final int VIEW_TYPE_POLL = 4;

    public MessageAdapter(Context context, String currentUserId) {
        this.context = context;
        this.currentUserId = currentUserId;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        switch (message.getType()) {
            case TASK_COMPLETED:
                return VIEW_TYPE_TASK_COMPLETED;
            case OBJECTION_RAISED:
                return VIEW_TYPE_OBJECTION;
            case PROOF_SUBMITTED:
                return VIEW_TYPE_PROOF;
            case POLL_CREATED:
                return VIEW_TYPE_POLL;
            default:
                return VIEW_TYPE_MESSAGE;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch (viewType) {
            case VIEW_TYPE_TASK_COMPLETED:
                return new TaskCompletedViewHolder(
                        inflater.inflate(R.layout.item_circle_chat_completed_task, parent, false),
                        context,
                        currentUserId,
                        db
                );
            case VIEW_TYPE_OBJECTION:
                return new ObjectionViewHolder(
                        inflater.inflate(R.layout.item_circle_chat_objection, parent, false),
                        context,
                        currentUserId,
                        db
                );
            case VIEW_TYPE_PROOF:
                return new ProofViewHolder(
                        inflater.inflate(R.layout.item_circle_chat_proof, parent, false),
                        context,
                        currentUserId,
                        db
                );
            case VIEW_TYPE_POLL:
                return new PollViewHolder(
                        inflater.inflate(R.layout.item_circle_chat_poll, parent, false),
                        context,
                        currentUserId,
                        db
                );
            default:
                return new MessageViewHolder(
                        inflater.inflate(R.layout.item_circle_chat_message, parent, false),
                        context,
                        currentUserId,
                        db
                );
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);

        switch (holder.getItemViewType()) {
            case VIEW_TYPE_MESSAGE:
                ((MessageViewHolder) holder).bind(message);
                break;
            case VIEW_TYPE_TASK_COMPLETED:
                ((TaskCompletedViewHolder) holder).bind(message);
                break;
            case VIEW_TYPE_OBJECTION:
                ((ObjectionViewHolder) holder).bind(message);
                break;
            case VIEW_TYPE_PROOF:
                ((ProofViewHolder) holder).bind(message);
                break;
            case VIEW_TYPE_POLL:
                ((PollViewHolder) holder).bind(message);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }
}
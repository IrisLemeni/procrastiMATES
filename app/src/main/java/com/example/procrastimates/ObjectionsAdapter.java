package com.example.procrastimates;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.procrastimates.Objection;
import com.example.procrastimates.R;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ObjectionsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_OBJECTION = 0;
    private static final int VIEW_TYPE_PROOF = 1;

    private List<Objection> objections;
    private OnObjectionInteractionListener listener;
    private Context context;
    private Map<String, String> usernames;
    private String currentUserId;

    public ObjectionsAdapter(Context context, OnObjectionInteractionListener listener) {
        this.context = context;
        this.objections = new ArrayList<>();
        this.listener = listener;
        this.usernames = new HashMap<>();
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    public void setObjections(List<Objection> objections) {
        this.objections = objections;
        notifyDataSetChanged();
    }

    public void setUsername(String userId, String username) {
        usernames.put(userId, username);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_OBJECTION) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_objection, parent, false);
            return new ObjectionViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_proof, parent, false);
            return new ProofViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Objection objection = objections.get(position);

        if (holder.getItemViewType() == VIEW_TYPE_OBJECTION) {
            ObjectionViewHolder objectionHolder = (ObjectionViewHolder) holder;
            String objectorName = usernames.getOrDefault(objection.getObjectorUserId(), "Unknown User");
            String targetName = usernames.getOrDefault(objection.getTargetUserId(), "Unknown User");

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());

            if (objection.getTargetUserId().equals(currentUserId)) {
                // This objection is against the current user
                objectionHolder.tvObjectionMessage.setText(objectorName + " raised an objection about your task: " + objection.getTaskTitle());

                if (objection.getStatus() == ObjectionStatus.PENDING) {
                    objectionHolder.btnAction.setVisibility(View.VISIBLE);
                    objectionHolder.btnAction.setText("Upload Proof");
                    objectionHolder.btnAction.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onUploadProofClick(objection);
                        }
                    });
                } else {
                    objectionHolder.btnAction.setVisibility(View.GONE);
                }
            } else {
                // Current user raised this objection or it's between other users
                objectionHolder.tvObjectionMessage.setText("You raised an objection about " + targetName + "'s task: " + objection.getTaskTitle());
                objectionHolder.btnAction.setVisibility(View.GONE);
            }

            objectionHolder.tvTime.setText(sdf.format(objection.getCreatedAt().toDate()));

            switch (objection.getStatus()) {
                case PENDING:
                    objectionHolder.tvStatus.setText("Pending proof");
                    objectionHolder.tvStatus.setTextColor(context.getResources().getColor(R.color.objection_pending));
                    break;
                case RESOLVED:
                    objectionHolder.tvStatus.setText("Proof provided");
                    objectionHolder.tvStatus.setTextColor(context.getResources().getColor(R.color.objection_resolved));
                    break;
                case EXPIRED:
                    objectionHolder.tvStatus.setText("Expired");
                    objectionHolder.tvStatus.setTextColor(context.getResources().getColor(R.color.objection_expired));
                    break;
            }
        } else {
            ProofViewHolder proofHolder = (ProofViewHolder) holder;

            if (objection.getProofImageUrl() != null && !objection.getProofImageUrl().isEmpty()) {
                proofHolder.ivProof.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(objection.getProofImageUrl())
                        .into(proofHolder.ivProof);

                String targetName = usernames.getOrDefault(objection.getTargetUserId(), "Unknown User");
                proofHolder.tvProofTitle.setText(targetName + "'s proof for task: " + objection.getTaskTitle());
            } else {
                proofHolder.ivProof.setVisibility(View.GONE);
                proofHolder.tvProofTitle.setText("No proof provided yet");
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        Objection objection = objections.get(position);
        return objection.getStatus() == ObjectionStatus.RESOLVED ? VIEW_TYPE_PROOF : VIEW_TYPE_OBJECTION;
    }

    @Override
    public int getItemCount() {
        return objections.size();
    }

    public interface OnObjectionInteractionListener {
        void onUploadProofClick(Objection objection);
    }

    static class ObjectionViewHolder extends RecyclerView.ViewHolder {
        TextView tvObjectionMessage, tvTime, tvStatus;
        Button btnAction;

        public ObjectionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvObjectionMessage = itemView.findViewById(R.id.tvObjectionMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnAction = itemView.findViewById(R.id.btnAction);
        }
    }

    static class ProofViewHolder extends RecyclerView.ViewHolder {
        TextView tvProofTitle;
        ImageView ivProof;

        public ProofViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProofTitle = itemView.findViewById(R.id.tvProofTitle);
            ivProof = itemView.findViewById(R.id.ivProof);
        }
    }
}
package com.example.procrastimates.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.procrastimates.models.Circle;
import com.example.procrastimates.models.Invitation;
import com.example.procrastimates.InvitationAdapter;
import com.example.procrastimates.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class InvitationsActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private RecyclerView recyclerView;
    private List<Invitation> invitationList;
    private InvitationAdapter adapter;
    private MaterialToolbar toolbar;
    private LinearLayout emptyStateLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        // Initialize views
        recyclerView = findViewById(R.id.recyclerViewNotifications);
        toolbar = findViewById(R.id.toolbar);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);

        // Set up toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Handle back navigation
        toolbar.setNavigationOnClickListener(v -> finish());

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        invitationList = new ArrayList<>();
        adapter = new InvitationAdapter(invitationList, this);
        recyclerView.setAdapter(adapter);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        loadInvitations();
    }

    private void loadInvitations() {
        String currentUserId = auth.getCurrentUser().getUid();

        db.collection("invitations")
                .whereEqualTo("to", currentUserId)
                .whereEqualTo("status", "PENDING")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot value = task.getResult();
                        if (value != null && !value.isEmpty()) {
                            invitationList.clear();
                            int totalInvitations = value.getDocuments().size();
                            int processedInvitations = 0;

                            for (DocumentSnapshot doc : value.getDocuments()) {
                                Invitation invitation = doc.toObject(Invitation.class);
                                if (invitation != null) {
                                    invitation.setInvitationId(doc.getId());

                                    // Get sender username
                                    String senderId = invitation.getFrom();
                                    db.collection("users").document(senderId).get()
                                            .addOnSuccessListener(userDoc -> {
                                                String senderUsername = userDoc.getString("username");
                                                if (senderUsername == null || senderUsername.trim().isEmpty()) {
                                                    senderUsername = "Unknown User";
                                                }
                                                invitation.setSenderName(senderUsername);

                                                // Add to list and notify adapter
                                                invitationList.add(invitation);
                                                adapter.notifyDataSetChanged();

                                                // Update UI visibility
                                                updateEmptyState();
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e("InvitationsActivity", "Failed to get sender username", e);
                                                invitation.setSenderName("Unknown User");
                                                invitationList.add(invitation);
                                                adapter.notifyDataSetChanged();
                                                updateEmptyState();
                                            });
                                }
                            }
                        } else {
                            Log.d("InvitationsActivity", "No invitations found.");
                            updateEmptyState();
                        }
                    } else {
                        Log.e("InvitationsActivity", "Error loading invitations", task.getException());
                        updateEmptyState();
                    }
                });
    }

    private void updateEmptyState() {
        if (invitationList.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    public void acceptInvitation(Invitation invitation) {
        String currentUserId = auth.getCurrentUser().getUid();

        // Check if user is already in a circle
        db.collection("circles")
                .whereArrayContains("members", currentUserId)
                .get()
                .addOnSuccessListener(userCircleQuerySnapshot -> {
                    if (!userCircleQuerySnapshot.isEmpty()) {
                        // User is already in a circle
                        Toast.makeText(InvitationsActivity.this, "You are already in a circle.", Toast.LENGTH_SHORT).show();
                        deleteInvitation(invitation);
                    } else {
                        // Find the circle belonging to the invitation sender
                        db.collection("circles")
                                .whereEqualTo("userId", invitation.getFrom())
                                .get()
                                .addOnSuccessListener(queryDocumentSnapshots -> {
                                    if (!queryDocumentSnapshots.isEmpty()) {
                                        DocumentSnapshot circleDoc = queryDocumentSnapshots.getDocuments().get(0);
                                        Circle circle = circleDoc.toObject(Circle.class);

                                        if (circle != null && !circle.getMembers().contains(currentUserId)) {
                                            addUserToCircle(circle, circleDoc.getId(), currentUserId, invitation);
                                        }
                                    } else {
                                        // Create new circle if none exists
                                        createCircle(invitation, currentUserId);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(InvitationsActivity.this, "Failed to get circle", Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(InvitationsActivity.this, "Error checking your circles", Toast.LENGTH_SHORT).show();
                });
    }

    private void addUserToCircle(Circle circle, String circleId, String currentUserId, Invitation invitation) {
        if (!circle.getMembers().contains(currentUserId)) {
            circle.getMembers().add(currentUserId);

            // Ensure circle creator is in members list
            if (!circle.getMembers().contains(invitation.getFrom())) {
                circle.getMembers().add(invitation.getFrom());
            }

            // Update circle in database
            db.collection("circles")
                    .document(circleId)
                    .update("members", circle.getMembers())
                    .addOnSuccessListener(aVoid -> {
                        deleteInvitation(invitation);
                        Toast.makeText(InvitationsActivity.this, "Successfully joined circle!", Toast.LENGTH_SHORT).show();
                        refreshActivity();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(InvitationsActivity.this, "Failed to update circle", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void createCircle(Invitation invitation, String currentUserId) {
        Circle newCircle = new Circle();
        newCircle.setUserId(invitation.getFrom());
        List<String> members = new ArrayList<>();
        members.add(invitation.getFrom());
        members.add(currentUserId);
        newCircle.setMembers(members);

        db.collection("circles")
                .add(newCircle)
                .addOnSuccessListener(documentReference -> {
                    newCircle.setCircleId(documentReference.getId());

                    db.collection("circles")
                            .document(documentReference.getId())
                            .update("circleId", newCircle.getCircleId())
                            .addOnSuccessListener(aVoid -> {
                                deleteInvitation(invitation);
                                Toast.makeText(InvitationsActivity.this, "Successfully created and joined circle!", Toast.LENGTH_SHORT).show();
                                refreshActivity();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(InvitationsActivity.this, "Failed to create circle", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(InvitationsActivity.this, "Failed to create circle", Toast.LENGTH_SHORT).show();
                });
    }

    public void declineInvitation(Invitation invitation) {
        deleteInvitation(invitation);
        Toast.makeText(this, "Invitation declined", Toast.LENGTH_SHORT).show();
    }

    private void deleteInvitation(Invitation invitation) {
        db.collection("invitations")
                .document(invitation.getInvitationId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Remove from local list and update UI
                    invitationList.remove(invitation);
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(InvitationsActivity.this, "Failed to delete invitation", Toast.LENGTH_SHORT).show();
                });
    }

    private void refreshActivity() {
        // Simply reload the invitations instead of restarting the activity
        loadInvitations();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
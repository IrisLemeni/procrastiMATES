package com.example.procrastimates.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.procrastimates.models.Circle;
import com.example.procrastimates.models.Invitation;
import com.example.procrastimates.InvitationAdapter;
import com.example.procrastimates.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private RecyclerView recyclerView;
    private List<Invitation> invitationList;
    private InvitationAdapter adapter;
    private ImageButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        recyclerView = findViewById(R.id.recyclerViewNotifications);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        backButton = findViewById(R.id.backButtonInv);

        backButton.setOnClickListener(v -> {
            finish();
        });

        invitationList = new ArrayList<>();
        // Transmite 'this' pentru a da activitatea ca parametru
        adapter = new InvitationAdapter(invitationList, this);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        loadInvitations();
    }

    private void loadInvitations() {
        String currentUserId = auth.getCurrentUser().getUid();

        db.collection("invitations")
                .whereEqualTo("to", currentUserId) // Verificăm dacă utilizatorul este destinatarul
                .whereEqualTo("status", "PENDING")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot value = task.getResult();
                        if (value != null && !value.isEmpty()) {
                            invitationList.clear();
                            for (DocumentSnapshot doc : value.getDocuments()) {
                                Invitation invitation = doc.toObject(Invitation.class);
                                if (invitation != null) {
                                    // Obține username-ul utilizatorului care a trimis invitația
                                    String senderId = invitation.getFrom();
                                    db.collection("users").document(senderId).get()
                                            .addOnSuccessListener(userDoc -> {
                                                String senderUsername = userDoc.getString("username"); // Folosim "username" în loc de "name"
                                                invitation.setSenderName(senderUsername);
                                                invitation.setInvitationId(doc.getId());  // Setează ID-ul documentului invitației
                                                invitationList.add(invitation);
                                                adapter.notifyDataSetChanged();
                                            });
                                }
                            }
                        } else {
                            Log.d("NotificationsActivity", "No invitations found.");
                        }
                    } else {
                        Log.e("NotificationsActivity", "Error loading invitations", task.getException());
                    }
                });
    }
    // În NotificationsActivity.java, modifică metoda acceptInvitation:

    public void acceptInvitation(Invitation invitation) {
        String currentUserId = auth.getCurrentUser().getUid();

        // Verifică mai întâi dacă utilizatorul curent nu este deja într-un cerc
        db.collection("circles")
                .whereArrayContains("members", currentUserId)
                .get()
                .addOnSuccessListener(userCircleQuerySnapshot -> {
                    if (!userCircleQuerySnapshot.isEmpty()) {
                        // Utilizatorul este deja într-un cerc
                        Toast.makeText(NotificationsActivity.this, "You are already in a circle.", Toast.LENGTH_SHORT).show();
                        // Șterge invitația deoarece nu mai este relevantă
                        deleteInvitation(invitation);
                    } else {
                        // Continuă cu adăugarea utilizatorului în cercul invitatorului
                        // Căutăm cercul care aparține utilizatorului care a trimis invitația
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
                                        // Dacă cercul nu există, creăm unul nou
                                        createCircle(invitation, currentUserId);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(NotificationsActivity.this, "Failed to get circle", Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(NotificationsActivity.this, "Error checking your circles", Toast.LENGTH_SHORT).show();
                });
    }
    private void addUserToCircle(Circle circle, String circleId, String currentUserId, Invitation invitation) {
        // Dacă utilizatorul curent nu este deja în cerc, îl adăugăm
        if (!circle.getMembers().contains(currentUserId)) {
            circle.getMembers().add(currentUserId);

            // Verificăm dacă creatorul cercului este în lista de membri, altfel îl adăugăm
            if (!circle.getMembers().contains(invitation.getFrom())) {
                circle.getMembers().add(invitation.getFrom());
            }

            // Actualizăm cercul în baza de date
            db.collection("circles")
                    .document(circleId)
                    .update("members", circle.getMembers())
                    .addOnSuccessListener(aVoid -> {
                        // După ce utilizatorul a fost adăugat în cerc, ștergem invitația
                        deleteInvitation(invitation);
                        Intent intent = new Intent(NotificationsActivity.this, NotificationsActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); // Dacă activitatea este deja în stack, o va aduce în față
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(NotificationsActivity.this, "Failed to update circle", Toast.LENGTH_SHORT).show();
                    });
        }
    }


    private void createCircle(Invitation invitation, String currentUserId) {
        // Creăm un cerc nou
        Circle newCircle = new Circle();
        newCircle.setUserId(invitation.getFrom()); // ID-ul creatorului cercului
        List<String> members = new ArrayList<>();

        // Adăugăm utilizatorul creator al cercului în lista de membri
        members.add(invitation.getFrom());  // ID-ul creatorului cercului
        members.add(currentUserId);         // Adăugăm utilizatorul curent ca membru
        newCircle.setMembers(members);

        // Adăugăm cercul în baza de date
        db.collection("circles")
                .add(newCircle)
                .addOnSuccessListener(documentReference -> {
                    // După ce cercul a fost creat, adăugăm circleId
                    newCircle.setCircleId(documentReference.getId());

                    // Actualizăm documentul cu circleId
                    db.collection("circles")
                            .document(documentReference.getId())
                            .update("circleId", newCircle.getCircleId())
                            .addOnSuccessListener(aVoid -> {
                                // După ce cercul a fost creat, ștergem invitația
                                deleteInvitation(invitation);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(NotificationsActivity.this, "Failed to create circle", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(NotificationsActivity.this, "Failed to create circle", Toast.LENGTH_SHORT).show();
                });
    }


    public void declineInvitation(Invitation invitation) {
        // Șterge invitația
        deleteInvitation(invitation);
        Intent intent = new Intent(NotificationsActivity.this, NotificationsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); // Dacă activitatea este deja în stack, o va aduce în față
        startActivity(intent);
        finish();
    }

    private void deleteInvitation(Invitation invitation) {
        db.collection("invitations")
                .document(invitation.getInvitationId()) // Folosim ID-ul invitației pentru a o șterge
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // După ce invitația a fost ștearsă, actualizează lista de invitații
                    loadInvitations();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(NotificationsActivity.this, "Failed to delete invitation", Toast.LENGTH_SHORT).show();
                });
    }
}

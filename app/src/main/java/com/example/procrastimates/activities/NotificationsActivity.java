package com.example.procrastimates.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.procrastimates.Circle;
import com.example.procrastimates.Invitation;
import com.example.procrastimates.InvitationAdapter;
import com.example.procrastimates.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private RecyclerView recyclerView;
    private List<Invitation> invitationList;
    private InvitationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        recyclerView = findViewById(R.id.recyclerViewNotifications);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

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
                                                invitation.setId(doc.getId());  // Setează ID-ul documentului invitației
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
    public void acceptInvitation(Invitation invitation) {
        String currentUserId = auth.getCurrentUser().getUid();

        // Adaugă utilizatorul în cercul sender-ului
        db.collection("circles")
                .whereEqualTo("userId", invitation.getFrom())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Dacă cercul există deja, adaugă utilizatorul în lista de membri
                        DocumentSnapshot circleDoc = queryDocumentSnapshots.getDocuments().get(0);
                        Circle circle = circleDoc.toObject(Circle.class);

                        if (circle != null && !circle.getMembers().contains(currentUserId)) {
                            circle.getMembers().add(currentUserId);
                            // Actualizează cercul în baza de date
                            db.collection("circles")
                                    .document(circleDoc.getId())
                                    .update("members", circle.getMembers())
                                    .addOnSuccessListener(aVoid -> {
                                        // După ce utilizatorul a fost adăugat în cerc, șterge invitația
                                        deleteInvitation(invitation);
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(NotificationsActivity.this, "Failed to update circle", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    } else {
                        // Dacă cercul nu există, creează unul nou cu utilizatorul adăugat
                        Circle newCircle = new Circle();
                        newCircle.setUserId(invitation.getFrom());
                        List<String> members = new ArrayList<>();
                        members.add(currentUserId);
                        newCircle.setMembers(members);

                        db.collection("circles")
                                .add(newCircle)
                                .addOnSuccessListener(documentReference -> {
                                    // După ce cercul a fost creat, șterge invitația
                                    deleteInvitation(invitation);
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(NotificationsActivity.this, "Failed to create circle", Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(NotificationsActivity.this, "Error getting circle", Toast.LENGTH_SHORT).show();
                });
    }

    public void declineInvitation(Invitation invitation) {
        // Șterge invitația
        deleteInvitation(invitation);
    }

    private void deleteInvitation(Invitation invitation) {
        db.collection("invitations")
                .document(invitation.getId()) // Folosim ID-ul invitației pentru a o șterge
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

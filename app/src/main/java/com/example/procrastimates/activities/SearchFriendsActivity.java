package com.example.procrastimates.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.procrastimates.Invitation;
import com.example.procrastimates.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SearchFriendsActivity extends AppCompatActivity {

    private EditText emailEditText;
    private Button sendInvitationButton;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_friends);

        emailEditText = findViewById(R.id.emailEditText);
        sendInvitationButton = findViewById(R.id.sendInvitationButton);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        sendInvitationButton.setOnClickListener(v -> {
            String friendEmail = emailEditText.getText().toString().trim();

            if (friendEmail.isEmpty()) {
                Toast.makeText(this, "Please enter an email address.", Toast.LENGTH_SHORT).show();
                return;
            }

            sendInvitation(friendEmail);
        });
    }

    private void sendInvitation(String friendEmail) {
        String currentUserId = auth.getCurrentUser().getUid();

        // Verifică dacă utilizatorul există
        getUserByEmail(friendEmail, currentUserId);
    }

    private void getUserByEmail(String friendEmail, String currentUserId) {
        db.collection("users")
                .whereEqualTo("email", friendEmail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String friendId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        checkExistingInvitation(currentUserId, friendId);
                    } else {
                        Toast.makeText(this, "User not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void checkExistingInvitation(String currentUserId, String friendId) {
        db.collection("invitations")
                .whereEqualTo("from", currentUserId)
                .whereEqualTo("to", friendId)
                .whereEqualTo("status", "PENDING")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        Toast.makeText(this, "An invitation to this person has already been sent", Toast.LENGTH_SHORT).show();
                    } else {
                        sendNewInvitation(currentUserId, friendId);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void sendNewInvitation(String currentUserId, String friendId) {
        Map<String, Object> invitation = new HashMap<>();
        invitation.put("from", currentUserId);
        invitation.put("to", friendId);
        invitation.put("status", "PENDING");
        invitation.put("timestamp", FieldValue.serverTimestamp());

        db.collection("invitations")
                .add(invitation)
                .addOnSuccessListener(documentReference -> {
                    saveInvitation(documentReference, currentUserId, friendId);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to send invitation.", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveInvitation(DocumentReference documentReference, String currentUserId, String friendId) {
        documentReference.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Obține valoarea timestamp din document
                        Object timestampValue = documentSnapshot.get("timestamp");
                        Invitation newInvitation = new Invitation();
                        newInvitation.setId(documentReference.getId());
                        newInvitation.setFrom(currentUserId);
                        newInvitation.setTo(friendId);
                        newInvitation.setStatus("PENDING");

                        if (timestampValue instanceof Timestamp) {
                            newInvitation.setTimestamp((Timestamp) timestampValue);
                        } else {
                            Log.e("sendInvitation", "Timestamp is not a valid Timestamp: " + timestampValue);
                        }

                        Toast.makeText(this, "Invitation sent successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e("sendInvitation", "Document does not exist.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("sendInvitation", "Error getting document: " + e.getMessage());
                });
    }

}

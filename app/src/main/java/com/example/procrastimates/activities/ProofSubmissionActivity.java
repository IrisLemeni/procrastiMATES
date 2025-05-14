package com.example.procrastimates.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.procrastimates.Message;
import com.example.procrastimates.MessageType;
import com.example.procrastimates.Notification;
import com.example.procrastimates.NotificationSender;
import com.example.procrastimates.Objection;
import com.example.procrastimates.Proof;
import com.example.procrastimates.R;
import com.example.procrastimates.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Date;
import java.util.UUID;

public class ProofSubmissionActivity extends AppCompatActivity {
    private ImageView proofImageView;
    private Button uploadButton, submitButton;
    private String taskId;
    private Uri imageUri;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proof_submission);

        taskId = getIntent().getStringExtra("taskId");
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        initializeViews();
        setupButtons();
    }

    private void initializeViews() {
        proofImageView = findViewById(R.id.proof_image_view);
        uploadButton = findViewById(R.id.upload_button);
        submitButton = findViewById(R.id.submit_button);
        submitButton.setEnabled(false);
    }

    private void setupButtons() {
        uploadButton.setOnClickListener(v -> openImagePicker());
        submitButton.setOnClickListener(v -> uploadProof());
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Selectează imagine"), 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            proofImageView.setImageURI(imageUri);
            submitButton.setEnabled(true);
        }
    }

    private void uploadProof() {
        if (imageUri != null) {
            // Afișează un dialog de încărcare
            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Se încarcă dovada...");
            progressDialog.show();

            // Creează o referință unică pentru imagine
            String imageName = "proofs/" + taskId + "_" + System.currentTimeMillis() + ".jpg";
            StorageReference imageRef = storageRef.child(imageName);

            // Încarcă imaginea
            imageRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        // Obține URL-ul de descărcare
                        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            // Creează dovada în baza de date
                            Proof proof = new Proof();
                            proof.setProofId(UUID.randomUUID().toString());
                            proof.setTaskId(taskId);
                            proof.setSubmittedByUserId(currentUserId);
                            proof.setImageUrl(uri.toString());
                            proof.setCreatedAt(new Timestamp(new Date()));

                            db.collection("proofs").document(proof.getProofId())
                                    .set(proof)
                                    .addOnSuccessListener(aVoid -> {
                                        // Actualizează starea obiecției
                                        db.collection("objections")
                                                .whereEqualTo("taskId", taskId)
                                                .get()
                                                .addOnSuccessListener(queryDocumentSnapshots -> {
                                                    if (!queryDocumentSnapshots.isEmpty()) {
                                                        String objectionId = queryDocumentSnapshots.getDocuments().get(0).getId();
                                                        db.collection("objections").document(objectionId)
                                                                .update("proofImageUrl", uri.toString());
                                                    }

                                                    // Obține detaliile task-ului pentru a crea mesajul
                                                    db.collection("tasks").document(taskId)
                                                            .get()
                                                            .addOnSuccessListener(taskDoc -> {
                                                                Task task = taskDoc.toObject(Task.class);
                                                                if (task != null) {
                                                                    // Creează un mesaj de dovadă în chat
                                                                    sendProofMessage(task, uri.toString());

                                                                    // Notifică utilizatorul care a făcut obiecția
                                                                    notifyObjector(task, uri.toString());

                                                                    progressDialog.dismiss();
                                                                    Toast.makeText(ProofSubmissionActivity.this,
                                                                            "Dovadă încărcată cu succes!", Toast.LENGTH_SHORT).show();
                                                                    finish();
                                                                }
                                                            });
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        progressDialog.dismiss();
                                        Toast.makeText(ProofSubmissionActivity.this,
                                                "Eroare la salvarea dovezii: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        });
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(ProofSubmissionActivity.this,
                                "Eroare la încărcarea imaginii: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void sendProofMessage(Task task, String imageUrl) {
        Message proofMessage = new Message();
        proofMessage.setMessageId(UUID.randomUUID().toString());
        proofMessage.setCircleId(task.getCircleId());
        proofMessage.setSenderId(currentUserId);
        proofMessage.setText("A furnizat dovadă pentru task-ul: " + task.getTitle());
        proofMessage.setType(MessageType.PROOF_SUBMITTED);
        proofMessage.setTaskId(task.getTaskId());
        proofMessage.setTimestamp(new Timestamp(new Date()));

        db.collection("messages").document(proofMessage.getMessageId())
                .set(proofMessage);
    }

    private void notifyObjector(Task task, String imageUrl) {
        // Găsește cine a făcut obiecția
        db.collection("objections")
                .whereEqualTo("taskId", task.getTaskId())
                .get()
                .addOnSuccessListener(objectionSnapshots -> {
                    if (!objectionSnapshots.isEmpty()) {
                        Objection objection = objectionSnapshots.getDocuments().get(0).toObject(Objection.class);
                        if (objection != null) {
                            // Creează notificarea
                            Notification notification = new Notification();
                            notification.setNotificationId(UUID.randomUUID().toString());
                            notification.setUserId(objection.getObjectorUserId());
                            notification.setTitle("Dovadă primită");
                            notification.setBody("Dovadă furnizată pentru task-ul: " + task.getTitle());
                            notification.setCircleId(task.getCircleId());
                            notification.setTaskId(task.getTaskId());
                            notification.setRead(false);
                            notification.setCreatedAt(new Timestamp(new Date()));

                            // Salvează notificarea
                            db.collection("notifications").document(notification.getNotificationId())
                                    .set(notification)
                                    .addOnSuccessListener(aVoid -> {
                                        // Trimite notificarea push
                                        NotificationSender.sendPushNotification(
                                                objection.getObjectorUserId(),
                                                "Dovadă primită",
                                                "Dovadă furnizată pentru task-ul: " + task.getTitle()
                                        );
                                    });
                        }
                    }
                });
    }
}
package com.example.procrastimates.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.procrastimates.models.Message;
import com.example.procrastimates.enums.MessageType;
import com.example.procrastimates.models.Notification;
import com.example.procrastimates.services.NotificationSender;
import com.example.procrastimates.enums.NotificationType;
import com.example.procrastimates.models.Objection;
import com.example.procrastimates.models.Proof;
import com.example.procrastimates.R;
import com.example.procrastimates.models.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Date;
import java.util.UUID;

public class ProofSubmissionActivity extends AppCompatActivity {
    private static final String EXTRA_TASK_ID = "taskId";
    private static final String COLLECTION_PROOFS = "proofs";
    private static final String COLLECTION_OBJECTIONS = "objections";
    private static final String COLLECTION_TASKS = "tasks";
    private static final String COLLECTION_MESSAGES = "messages";
    private static final String COLLECTION_NOTIFICATIONS = "notifications";

    private ImageView proofImageView;
    private Button uploadButton, submitButton;
    private String taskId;
    private Uri imageUri;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private String currentUserId;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                    imageUri = result.getData().getData();
                    proofImageView.setImageURI(imageUri);
                    findViewById(R.id.empty_state_overlay).setVisibility(View.GONE);
                    submitButton.setEnabled(true);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proof_submission);

        taskId = getIntent().getStringExtra(EXTRA_TASK_ID);
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
        imagePickerLauncher.launch(Intent.createChooser(intent, "Select image"));
    }

    private void uploadProof() {
        if (imageUri != null) {
            Toast.makeText(this, "Uploading proof...", Toast.LENGTH_SHORT).show();

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

                            db.collection(COLLECTION_PROOFS).document(proof.getProofId())
                                    .set(proof)
                                    .addOnSuccessListener(aVoid -> {
                                        // Actualizează starea obiecției
                                        db.collection(COLLECTION_OBJECTIONS)
                                                .whereEqualTo("taskId", taskId)
                                                .get()
                                                .addOnSuccessListener(queryDocumentSnapshots -> {
                                                    if (!queryDocumentSnapshots.isEmpty()) {
                                                        String objectionId = queryDocumentSnapshots.getDocuments().get(0).getId();
                                                        db.collection(COLLECTION_OBJECTIONS).document(objectionId)
                                                                .update("proofImageUrl", uri.toString());
                                                    }

                                                    // Obține detaliile task-ului pentru a crea mesajul
                                                    db.collection(COLLECTION_TASKS).document(taskId)
                                                            .get()
                                                            .addOnSuccessListener(taskDoc -> {
                                                                Task task = taskDoc.toObject(Task.class);
                                                                if (task != null) {
                                                                    // Creează un mesaj de dovadă în chat
                                                                    sendProofMessage(task);

                                                                    // Notifică utilizatorul care a făcut obiecția
                                                                    notifyObjector(task);

                                                                    Toast.makeText(ProofSubmissionActivity.this,
                                                                            "Proof uploaded successfully!", Toast.LENGTH_SHORT).show();
                                                                    finish();
                                                                }
                                                            });
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(ProofSubmissionActivity.this,
                                                "Error saving proof: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(ProofSubmissionActivity.this,
                                "Error loading image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void sendProofMessage(Task task) {
        Message proofMessage = new Message();
        proofMessage.setMessageId(UUID.randomUUID().toString());
        proofMessage.setCircleId(task.getCircleId());
        proofMessage.setSenderId(currentUserId);
        proofMessage.setText("A furnizat dovadă pentru task-ul: " + task.getTitle());
        proofMessage.setType(MessageType.PROOF_SUBMITTED);
        proofMessage.setTaskId(task.getTaskId());
        proofMessage.setTimestamp(new Timestamp(new Date()));

        db.collection(COLLECTION_MESSAGES).document(proofMessage.getMessageId())
                .set(proofMessage);
    }

    private void notifyObjector(Task task) {
        // Găsește cine a făcut obiecția
        db.collection(COLLECTION_OBJECTIONS)
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
                            notification.setType(NotificationType.PROOF_SUBMITTED);
                            notification.setRead(false);
                            notification.setCreatedAt(new Timestamp(new Date()));

                            // Salvează notificarea
                            db.collection(COLLECTION_NOTIFICATIONS).document(notification.getNotificationId())
                                    .set(notification)
                                    .addOnSuccessListener(aVoid -> {
                                        // Trimite notificarea push
                                        NotificationSender.sendPushNotification(
                                                objection.getObjectorUserId(),
                                                "Dovadă primită",
                                                "Dovadă furnizată pentru task-ul: " + task.getTitle(),
                                                task.getTaskId(),
                                                task.getCircleId(),
                                                NotificationType.PROOF_SUBMITTED
                                        );
                                    });
                        }
                    }
                });
    }
}
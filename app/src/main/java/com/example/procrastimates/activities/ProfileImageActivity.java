package com.example.procrastimates.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.example.procrastimates.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class ProfileImageActivity extends AppCompatActivity {

    private ImageView profileImageView;
    private Button selectImageButton;
    private Button saveImageButton;
    private Button cancelButton;
    private Uri imageUri;
    private ImageButton backButton;

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_image);

        // Inițializare Firebase
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        } else {
            // Dacă utilizatorul nu este autentificat, redirectează-l la login
            Toast.makeText(this, "Trebuie să fii autentificat pentru a schimba imaginea de profil", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(ProfileImageActivity.this, LoginActivity.class));
            finish();
            return;
        }

        initializeViews();
        setupButtons();

        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();

                    // Afișează imaginea selectată în ImageView folosind Glide cu opțiuni îmbunătățite
                    RequestOptions requestOptions = new RequestOptions()
                            .transform(new CircleCrop())
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person);

                    Glide.with(this)
                            .load(imageUri)
                            .apply(requestOptions)
                            .into(profileImageView);

                    // Activează butonul de salvare
                    saveImageButton.setEnabled(true);
                }
            }
        );

        loadCurrentProfileImage();
    }

    private void initializeViews() {
        profileImageView = findViewById(R.id.profile_image_view);
        selectImageButton = findViewById(R.id.select_image_button);
        saveImageButton = findViewById(R.id.save_image_button);
        cancelButton = findViewById(R.id.cancel_button);
        backButton = findViewById(R.id.backButton);

        // Dezactivează butonul de salvare până când utilizatorul selectează o imagine
        saveImageButton.setEnabled(false);

        // Setează background transparent pentru ImageView
        profileImageView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
    }

    private void setupButtons() {
        selectImageButton.setOnClickListener(v -> openImagePicker());

        saveImageButton.setOnClickListener(v -> uploadProfileImage());

        cancelButton.setOnClickListener(v -> finish());

        backButton.setOnClickListener(v -> finish());
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        imagePickerLauncher.launch(Intent.createChooser(intent, "Select profile picture"));
    }

    private void loadCurrentProfileImage() {
        // Încarcă imaginea de profil curentă a utilizatorului (dacă există)
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        RequestOptions requestOptions = new RequestOptions()
                                .transform(new CircleCrop())
                                .placeholder(R.drawable.ic_person)
                                .error(R.drawable.ic_person);

                        Glide.with(ProfileImageActivity.this)
                                .load(profileImageUrl)
                                .apply(requestOptions)
                                .into(profileImageView);
                    } else {
                        // Dacă nu există imagine, afișează iconița de persoană
                        profileImageView.setImageResource(R.drawable.ic_person);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfileImageActivity.this,
                            "Could not load current profile picture",
                            Toast.LENGTH_SHORT).show();
                    // Afișează iconița de persoană în caz de eroare
                    profileImageView.setImageResource(R.drawable.ic_person);
                });
    }

    private void uploadProfileImage() {
        if (imageUri == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show();
            return;
        }

        // Afișează dialog de progres
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Loading profile picture...")
                .setView(new ProgressBar(this))
                .setCancelable(false)
                .create();
        dialog.show();

        // Creează o referință unică pentru imagine în storage
        StorageReference fileReference = storageReference.child("profile_images/" + currentUserId + ".jpg");

        // Încarcă fișierul
        fileReference.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Obține URL-ul de descărcare al imaginii
                    fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();

                        // Actualizează URL-ul imaginii în baza de date
                        updateProfileImageUrl(currentUserId, imageUrl, dialog);
                    });
                })
                .addOnFailureListener(e -> {
                    dialog.dismiss();
                    Toast.makeText(ProfileImageActivity.this,
                            "Upload failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void updateProfileImageUrl(String userId, String imageUrl, AlertDialog dialog) {
        db.collection("users").document(userId)
                .update("profileImageUrl", imageUrl)
                .addOnSuccessListener(aVoid -> {
                    dialog.dismiss();
                    Toast.makeText(ProfileImageActivity.this,
                            "Profile picture has been updated successfully!",
                            Toast.LENGTH_SHORT).show();
                    // Revine la ecranul anterior
                    finish();
                })
                .addOnFailureListener(e -> {
                    dialog.dismiss();
                    Toast.makeText(ProfileImageActivity.this,
                            "Error updating profile picture:" + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
}

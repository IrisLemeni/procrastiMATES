package com.example.procrastimates;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private TextView welcomeText, quoteText;
    private Button startPomodoroButton;
    private ImageView userImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        welcomeText = findViewById(R.id.welcomeText);
        userImage = findViewById(R.id.userImage);
        quoteText = findViewById(R.id.quoteText);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            loadUserData(currentUser.getUid());  // Folosește UID-ul pentru a cauta utilizatorul în Firestore
        }

        String[] quotes = {
                "The only way to do great work is to love what you do.",
                "Success is not final, failure is not fatal: It is the courage to continue that counts.",
                "Believe you can and you're halfway there.",
                "The future belongs to those who believe in the beauty of their dreams.",
                "It does not matter how slowly you go as long as you do not stop."
        };

        Random rand = new Random();
        String randomQuote = quotes[rand.nextInt(quotes.length)];
        quoteText.setText(randomQuote);

        startPomodoroButton = findViewById(R.id.startPomodoroButton);
        startPomodoroButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PomodoroActivity.class);
            startActivity(intent);
        });
    }

    private void loadUserData(String userId) {
        // Obtine instanta Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("username");
                        if (username != null) {
                            welcomeText.setText("Hello, " + username);
                        } else {
                            welcomeText.setText("Hello, User!");
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "User data not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Error loading user data", Toast.LENGTH_SHORT).show();
                });
    }


}

package com.example.procrastimates.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.procrastimates.R;
import com.example.procrastimates.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SignUpActivity extends AppCompatActivity {

    private EditText usernameInput, emailInput, passwordInput;
    private Button signUpButton;
    private TextView alreadyHaveAccountText;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();

        // Legare elemente din layout
        usernameInput = findViewById(R.id.signup_username_input);
        emailInput = findViewById(R.id.signup_email_input);
        passwordInput = findViewById(R.id.signup_password_input);
        signUpButton = findViewById(R.id.signup_button);
        alreadyHaveAccountText = findViewById(R.id.already_have_account_text);

        // Buton Sign Up
        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createUser();
            }
        });

        // Text pentru redirecționare la Login
        alreadyHaveAccountText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                finish();
            }
        });
    }

    private void createUser() {
        String email = emailInput.getText().toString();
        String password = passwordInput.getText().toString();

        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password required");
            return;
        }

        if (password.length() < 6) {
            passwordInput.setError("Password must be at least 6 characters");
            return;
        }

        // Creare cont nou
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        String userId = user.getUid();

                        saveUserData(userId, usernameInput.getText().toString(), email);

                        startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                        finish();
                    } else {
                        Toast.makeText(SignUpActivity.this, "Account creation failed. Please try again.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserData(String userId, String username, String email) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        User user = new User(userId, username, email);

        db.collection("users")
                .document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(SignUpActivity.this, "User data saved!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(SignUpActivity.this, "Error saving data. Please try again.",
                            Toast.LENGTH_SHORT).show();
                });
    }
}

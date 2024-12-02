package com.example.procrastimates.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import com.example.procrastimates.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Random;

public class HomeFragment extends Fragment {

    private TextView welcomeText;
    private ImageView userImage;
    private TextView quoteText;
    private View startPomodoroButton;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        welcomeText = view.findViewById(R.id.welcomeText);
        userImage = view.findViewById(R.id.userImage);
        quoteText = view.findViewById(R.id.quoteText);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            loadUserData(currentUser.getUid());
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

        return view;
    }

    private void loadUserData(String userId) {
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
                        Toast.makeText(getContext(), "User data not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error loading user data", Toast.LENGTH_SHORT).show();
                });
    }
}

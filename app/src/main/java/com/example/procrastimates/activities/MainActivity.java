package com.example.procrastimates.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;


import com.example.procrastimates.R;
import com.example.procrastimates.fragments.FriendsFragment;
import com.example.procrastimates.fragments.HomeFragment;
import com.example.procrastimates.fragments.PomodoroFragment;
import com.example.procrastimates.fragments.TasksFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity implements PomodoroFragment.FocusLockListener {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Load the default fragment
        loadFragment(new HomeFragment());

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    loadFragment(new HomeFragment());

                } else if (itemId == R.id.nav_tasks) {
                    loadFragment(new TasksFragment());

                } else if (itemId == R.id.nav_pomodoro) {
                    loadFragment(new PomodoroFragment());

                } else if (itemId == R.id.nav_friends) {
                    loadFragment(new FriendsFragment());
                }

                return true;
            }
        });
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String token = task.getResult();
                        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                        FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(userId)
                                .update("deviceToken", token)
                                .addOnSuccessListener(aVoid -> Log.d("FCM", "Token salvat în Firestore"))
                                .addOnFailureListener(e -> Log.e("FCM", "Eroare la salvare token: " + e.getMessage()));
                    } else {
                        Log.e("FCM", "Eroare la obținerea tokenului: " + task.getException());
                    }
                });

    }

    @Override
    public void setBottomNavEnabled(boolean enabled) {
        if (bottomNavigationView != null) {
            // Dezactivează navigarea făcând elementele neclicabile
            for (int i = 0; i < bottomNavigationView.getMenu().size(); i++) {
                bottomNavigationView.getMenu().getItem(i).setEnabled(enabled);
            }

            // Opțional: schimbă transparența pentru feedback vizual
            bottomNavigationView.setAlpha(enabled ? 1.0f : 0.5f);

            // Dacă vrei să blochezi și gesturi de swipe între fragmente (dacă folosești ViewPager):
            // viewPager.setUserInputEnabled(enabled);
        }
    }


    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frameLayout, fragment)
                .commit();
    }
}

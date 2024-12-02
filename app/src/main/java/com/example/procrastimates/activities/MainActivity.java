package com.example.procrastimates.activities;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;


import com.example.procrastimates.R;
import com.example.procrastimates.fragments.FriendsFragment;
import com.example.procrastimates.fragments.HomeFragment;
import com.example.procrastimates.fragments.PomodoroFragment;
import com.example.procrastimates.fragments.TasksFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

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
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frameLayout, fragment)
                .commit();
    }
}

package com.example.procrastimates.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.procrastimates.models.Achievement;
import com.example.procrastimates.AchievementDialogHelper;
import com.example.procrastimates.AchievementManager;
import com.example.procrastimates.adapters.AchievementsAdapter;
import com.example.procrastimates.R;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class AchievementsActivity extends AppCompatActivity implements AchievementManager.AchievementListener {

    private RecyclerView recyclerViewAchievements;
    private AchievementsAdapter achievementsAdapter;
    private List<Achievement> allAchievements = new ArrayList<>();
    private List<Achievement> unlockedAchievements = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_achievements);

        MaterialToolbar toolbar = findViewById(R.id.toolbarAchievements);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        recyclerViewAchievements = findViewById(R.id.recyclerViewAchievements);
        recyclerViewAchievements.setLayoutManager(new GridLayoutManager(this, 2));
    }

    @Override
    protected void onStart() {
        super.onStart();

        AchievementManager.getInstance().resetAllAchievementsLocked();

        allAchievements = AchievementManager.getInstance().getAllAchievements();
        achievementsAdapter = new AchievementsAdapter(this, allAchievements);
        recyclerViewAchievements.setAdapter(achievementsAdapter);

        loadUnlockedAchievements();

        AchievementManager.getInstance().addAchievementListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        AchievementManager.getInstance().removeAchievementListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AchievementManager.getInstance().removeAchievementListener(this);
    }

    private void loadUnlockedAchievements() {
        AchievementManager.getInstance().getUnlockedAchievements(new AchievementManager.AchievementsCallback() {
            @Override
            public void onAchievementsLoaded(List<Achievement> achievements) {
                unlockedAchievements = achievements;

                for (Achievement achievement : allAchievements) {
                    for (Achievement unlockedAchievement : unlockedAchievements) {
                        if (achievement.getId().equals(unlockedAchievement.getId())) {
                            achievement.setUnlocked(true);
                            break;
                        }
                    }
                }

                achievementsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onAchievementsLoadFailed(Exception exception) {
                Toast.makeText(AchievementsActivity.this,
                        "Failed to load achievements: " + exception.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onAchievementUnlocked(Achievement achievement) {
        runOnUiThread(() -> {
            for (Achievement a : allAchievements) {
                if (a.getId().equals(achievement.getId())) {
                    a.setUnlocked(true);
                    achievementsAdapter.notifyDataSetChanged();

                    // Show achievement dialog
                    AchievementDialogHelper.showAchievementUnlockedDialog(this, achievement);
                    break;
                }
            }
        });
    }
}
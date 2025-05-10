package com.example.procrastimates;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.procrastimates.Achievement;
import com.example.procrastimates.AchievementSystem;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GamificationManager {
    private static final String TAG = "GamificationManager";
    private static final String PREF_NAME = "procrastimates_gamification";

    private static FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Actualizează statisticile utilizatorului după o sesiune Pomodoro
    public static void updateUserStatsAfterSession(Context context, int focusScore, boolean isWorkSession,
                                                   OnGamificationUpdateListener listener) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Obține statisticile actuale
                        int totalSessions = documentSnapshot.getLong("totalSessions") != null ?
                                documentSnapshot.getLong("totalSessions").intValue() : 0;
                        int perfectSessions = documentSnapshot.getLong("perfectSessions") != null ?
                                documentSnapshot.getLong("perfectSessions").intValue() : 0;
                        int currentStreak = documentSnapshot.getLong("currentStreak") != null ?
                                documentSnapshot.getLong("currentStreak").intValue() : 0;
                        int experiencePoints = documentSnapshot.getLong("experiencePoints") != null ?
                                documentSnapshot.getLong("experiencePoints").intValue() : 0;
                        List<String> unlockedAchievements = (List<String>) documentSnapshot.get("unlockedAchievements");

                        if (unlockedAchievements == null) {
                            unlockedAchievements = new ArrayList<>();
                        }

                        // Actualizează contoarele
                        totalSessions += isWorkSession ? 1 : 0; // Incrementează doar pentru sesiunile de lucru

                        if (focusScore >= 100 && isWorkSession) {
                            perfectSessions += 1;
                        }

                        // Verifică dacă este o sesiune nocturnă sau matinală
                        Calendar calendar = Calendar.getInstance();
                        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);

                        int nightSessions = documentSnapshot.getLong("nightSessions") != null ?
                                documentSnapshot.getLong("nightSessions").intValue() : 0;
                        int morningSessions = documentSnapshot.getLong("morningSessions") != null ?
                                documentSnapshot.getLong("morningSessions").intValue() : 0;

                        if (hourOfDay >= 22 || hourOfDay < 5) {
                            nightSessions += isWorkSession ? 1 : 0;
                        }

                        if (hourOfDay >= 5 && hourOfDay < 8) {
                            morningSessions += isWorkSession ? 1 : 0;
                        }

                        // Calculate XP gained
                        int oldXP = experiencePoints;
                        int sessionXP = calculateSessionXP(focusScore, currentStreak);
                        // Create a final copy for use in the lambda
                        final int finalExperiencePoints = experiencePoints + sessionXP;

                        int completedTasks = documentSnapshot.getLong("completedTasks") != null ?
                                documentSnapshot.getLong("completedTasks").intValue() : 0;

                        int weekendTasks = documentSnapshot.getLong("weekendTasks") != null ?
                                documentSnapshot.getLong("weekendTasks").intValue() : 0;

                        // Verifică noile achievement-uri
                        List<Achievement> newAchievements = AchievementSystem.checkNewAchievements(
                                totalSessions, perfectSessions, currentStreak, completedTasks,
                                nightSessions, morningSessions, weekendTasks, unlockedAchievements);

                        // Adaugă XP pentru noile achievement-uri
                        int achievementXP = 0;
                        for (Achievement achievement : newAchievements) {
                            achievementXP += achievement.getXpReward();
                            unlockedAchievements.add(achievement.getId());
                        }

                        // Create final copies for use in lambda
                        final int finalAchievementXP = achievementXP;

                        // Calculate total XP
                        final int totalXP = finalExperiencePoints + finalAchievementXP;

                        // Verifică levelup
                        boolean leveledUp = AchievementSystem.hasLeveledUp(oldXP, totalXP);
                        int oldLevel = AchievementSystem.getLevelFromXP(oldXP);
                        int newLevel = AchievementSystem.getLevelFromXP(totalXP);

                        // Actualizează datele în Firestore
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("totalSessions", totalSessions);
                        updates.put("perfectSessions", perfectSessions);
                        updates.put("nightSessions", nightSessions);
                        updates.put("morningSessions", morningSessions);
                        updates.put("experiencePoints", totalXP);
                        updates.put("unlockedAchievements", unlockedAchievements);
                        updates.put("level", newLevel);

                        // Create final copies of variables needed in the lambda
                        final List<Achievement> finalNewAchievements = new ArrayList<>(newAchievements);
                        final int finalSessionXP = sessionXP;
                        final int finalTotalXP = totalXP;
                        final int finalNewLevel = newLevel;
                        final boolean finalLeveledUp = leveledUp;

                        db.collection("users").document(userId)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    // Notifică listener-ul despre rezultate
                                    if (listener != null) {
                                        GamificationResult result = new GamificationResult(
                                                finalSessionXP,
                                                finalAchievementXP,
                                                finalTotalXP,
                                                finalNewLevel,
                                                finalLeveledUp,
                                                finalNewAchievements
                                        );
                                        listener.onUpdateComplete(result);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error updating user stats", e);
                                    if (listener != null) {
                                        listener.onUpdateFailed(e.getMessage());
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting user document", e);
                    if (listener != null) {
                        listener.onUpdateFailed(e.getMessage());
                    }
                });
    }

    // Calculează XP câștigat pentru o sesiune
    private static int calculateSessionXP(int focusScore, int currentStreak) {
        // XP de bază bazat pe focus score (5-15 XP)
        int baseXP = (focusScore / 10) + 5;

        // Bonus de streak
        int streakBonus = 0;
        if (currentStreak % 5 == 0 && currentStreak > 0) {
            streakBonus = 10; // +10 XP la fiecare 5 zile streak
        }

        return baseXP + streakBonus;
    }

    // Interfață pentru callback-uri
    public interface OnGamificationUpdateListener {
        void onUpdateComplete(GamificationResult result);
        void onUpdateFailed(String errorMessage);
    }

    // Clasa pentru rezultatele gamificării
    public static class GamificationResult {
        private int sessionXP;
        private int achievementXP;
        private int totalXP;
        private int currentLevel;
        private boolean leveledUp;
        private List<Achievement> newAchievements;

        public GamificationResult(int sessionXP, int achievementXP, int totalXP,
                                  int currentLevel, boolean leveledUp,
                                  List<Achievement> newAchievements) {
            this.sessionXP = sessionXP;
            this.achievementXP = achievementXP;
            this.totalXP = totalXP;
            this.currentLevel = currentLevel;
            this.leveledUp = leveledUp;
            this.newAchievements = newAchievements;
        }

        public int getSessionXP() {
            return sessionXP;
        }

        public int getAchievementXP() {
            return achievementXP;
        }

        public int getTotalXP() {
            return totalXP;
        }

        public int getCurrentLevel() {
            return currentLevel;
        }

        public boolean isLeveledUp() {
            return leveledUp;
        }

        public List<Achievement> getNewAchievements() {
            return newAchievements;
        }

        public int getTotalEarnedXP() {
            return sessionXP + achievementXP;
        }
    }
}
package com.example.procrastimates.services;

import androidx.annotation.NonNull;

import com.example.procrastimates.models.Achievement;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class AchievementManager {

    private static final String COLLECTION_ACHIEVEMENTS = "user_achievements";
    private static AchievementManager instance;
    private final FirebaseFirestore db;
    private final String userId;
    private List<Achievement> allAchievements;
    private List<AchievementListener> listeners = new ArrayList<>();

    private boolean isLoading = false;

    private AchievementManager() {
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        initializeAchievements();
    }

    public static synchronized AchievementManager getInstance() {
        if (instance == null) {
            instance = new AchievementManager();
        }
        return instance;
    }

    private void initializeAchievements() {
        allAchievements = Arrays.asList(
                // Pomodoro session achievements
                new Achievement("session_starter", "Session Starter", "Complete your first Pomodoro session", "icons/launcher.png", 1),
                new Achievement("focus_apprentice", "Focus Apprentice", "Complete 5 Pomodoro sessions", "icons/apprentice.png", 5),
                new Achievement("focus_master", "Focus Master", "Complete 25 Pomodoro sessions", "icons/master.png", 25),
                new Achievement("focus_wizard", "Focus Wizard", "Complete 100 Pomodoro sessions", "icons/wizard.png", 100),

                // High focus score achievements
                new Achievement("laser_focus", "Laser Focus", "Maintain a focus score of 100 in one session", "icons/laser.png", 1),
                new Achievement("undistracted", "Undistracted", "Complete 5 sessions with focus score above 90", "icons/focus.png", 5),
                new Achievement("concentration_guru", "Concentration Guru", "Complete 15 sessions with focus score above 95", "icons/guru.png", 15),

                // Consecutive session achievements
                new Achievement("daily_streak", "Daily Streak", "Complete Pomodoro sessions 3 days in a row", "icons/fire.png", 3),
                new Achievement("weekly_focus", "Weekly Focus", "Complete Pomodoro sessions 7 consecutive days", "icons/timetable.png", 7),
                new Achievement("focus_warrior", "Focus Warrior", "Complete Pomodoro sessions 14 consecutive days", "icons/swordsman.png", 14),

                // Long session achievements
                new Achievement("marathon_focus", "Marathon Focus", "Complete one 50-minute Pomodoro session", "icons/winner.png", 1),
                new Achievement("endurance_master", "Endurance Master", "Complete 10 Pomodoro sessions of 50 minutes", "icons/persistence.png", 10)
        );
    }

    public void checkSessionAchievements(boolean isWorkSession, int sessionDuration, int focusScore) {
        if (!isWorkSession || userId == null) return;

        // Obține istoricul sesiunilor pentru verificări
        db.collection("pomodoro_sessions")
                .whereEqualTo("userId", userId)
                .whereEqualTo("type", "work")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            int totalSessions = task.getResult().size();
                            int highFocusSessions = 0;
                            int veryHighFocusSessions = 0;

                            // Numără sesiunile cu focus ridicat
                            for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                                Long score = doc.getLong("focusScore");
                                if (score != null && score >= 90) {
                                    highFocusSessions++;
                                }
                                if (score != null && score >= 95) {
                                    veryHighFocusSessions++;
                                }
                            }

                            // Verifică achievement-urile bazate pe numărul de sesiuni
                            checkAndUnlockAchievement("session_starter", totalSessions >= 1);
                            checkAndUnlockAchievement("focus_apprentice", totalSessions >= 5);
                            checkAndUnlockAchievement("focus_master", totalSessions >= 25);
                            checkAndUnlockAchievement("focus_wizard", totalSessions >= 100);

                            // Verifică achievement-urile bazate pe focus score
                            checkAndUnlockAchievement("laser_focus", focusScore >= 100);
                            checkAndUnlockAchievement("undistracted", highFocusSessions >= 5);
                            checkAndUnlockAchievement("concentration_guru", veryHighFocusSessions >= 15);

                            // Verifică achievement-urile bazate pe durata sesiunii
                            checkAndUnlockAchievement("marathon_focus", sessionDuration >= 50 * 60 * 1000);

                            // Verifică numărul de sesiuni de 50 de minute
                            checkLongSessionsAchievements();

                            // Verifică streaks zilnice
                            checkDailyStreakAchievements();
                        }
                    }
                });
    }

    private void checkLongSessionsAchievements() {
        if (userId == null) return;

        db.collection("pomodoro_sessions")
                .whereEqualTo("userId", userId)
                .whereEqualTo("type", "work")
                .whereGreaterThanOrEqualTo("duration", 50)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int longSessions = task.getResult().size();
                        checkAndUnlockAchievement("endurance_master", longSessions >= 10);
                    }
                });
    }

    private void checkDailyStreakAchievements() {
        if (userId == null) return;

        db.collection("daily_sessions")
                .document(userId)
                .collection("sessions_by_day")
                .orderBy("sessionCount", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<DocumentSnapshot> docs = task.getResult().getDocuments();
                        // Aici ar trebui să avem o logică mai complexă pentru a verifica zilele consecutive
                        // Aceasta este o simplificare pentru exemplu:
                        int consecutiveDays = Math.min(docs.size(), 14); // Maxim 14 pentru exemplu

                        checkAndUnlockAchievement("daily_streak", consecutiveDays >= 3);
                        checkAndUnlockAchievement("weekly_focus", consecutiveDays >= 7);
                        checkAndUnlockAchievement("focus_warrior", consecutiveDays >= 14);
                    }
                });
    }

    private void checkAndUnlockAchievement(String achievementId, boolean condition) {
        if (!condition || userId == null) return;

        db.collection(COLLECTION_ACHIEVEMENTS)
                .document(userId)
                .collection("achievements")
                .document(achievementId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (!document.exists()) {
                            // Achievement-ul nu a fost deblocat încă
                            unlockAchievement(achievementId);
                        }
                    }
                });
    }


    private void unlockAchievement(String achievementId) {
        Achievement achievement = getAchievementById(achievementId);
        if (achievement == null || userId == null) return;

        // Marchează achievement-ul ca deblocat în Firestore
        Map<String, Object> achievementData = new HashMap<>();
        achievementData.put("id", achievement.getId());
        achievementData.put("title", achievement.getTitle());
        achievementData.put("description", achievement.getDescription());
        achievementData.put("iconUrl", achievement.getIconUrl());
        achievementData.put("unlockDate", com.google.firebase.Timestamp.now());

        db.collection(COLLECTION_ACHIEVEMENTS)
                .document(userId)
                .collection("achievements")
                .document(achievementId)
                .set(achievementData)
                .addOnSuccessListener(aVoid -> {
                    // Setăm achievement-ul ca deblocat local
                    achievement.setUnlocked(true);

                    // Notifică ascultătorii despre noul achievement
                    notifyAchievementUnlocked(achievement);
                });
    }

    private void notifyAchievementUnlocked(Achievement achievement) {
        List<AchievementListener> listenersCopy = new ArrayList<>(listeners);
        for (AchievementListener listener : listenersCopy) {
            if (listener != null) {
                listener.onAchievementUnlocked(achievement);
            }
        }
    }


    private Achievement getAchievementById(String achievementId) {
        for (Achievement achievement : allAchievements) {
            if (achievement.getId().equals(achievementId)) {
                return achievement;
            }
        }
        return null;
    }

    public void getUnlockedAchievements(AchievementsCallback callback) {
        if (userId == null) {
            callback.onAchievementsLoadFailed(new Exception("User not logged in"));
            return;
        }

        isLoading = true;
        db.collection(COLLECTION_ACHIEVEMENTS)
                .document(userId)
                .collection("achievements")
                .get()
                .addOnCompleteListener(task -> {
                    isLoading = false;
                    if (task.isSuccessful()) {
                        List<Achievement> unlockedAchievements = new ArrayList<>();
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            String id = doc.getString("id");
                            String title = doc.getString("title");
                            String description = doc.getString("description");
                            String iconUrl = doc.getString("iconUrl");

                            Achievement achievement = new Achievement(id, title, description, iconUrl, 0);
                            achievement.setUnlocked(true);
                            unlockedAchievements.add(achievement);

                            // Update the unlocked status in our main list
                            updateAchievementUnlockedStatus(id, true);
                        }
                        callback.onAchievementsLoaded(unlockedAchievements);
                    } else {
                        callback.onAchievementsLoadFailed(task.getException());
                    }
                });
    }

    private void updateAchievementUnlockedStatus(String achievementId, boolean unlocked) {
        for (Achievement achievement : allAchievements) {
            if (achievement.getId().equals(achievementId)) {
                achievement.setUnlocked(unlocked);
                break;
            }
        }
    }

    public List<Achievement> getAllAchievements() {
        return new ArrayList<>(allAchievements); // Return a copy to prevent external modification
    }

    public void addAchievementListener(AchievementListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeAchievementListener(AchievementListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public void resetAllAchievementsLocked() {
        if (allAchievements == null) return;
        for (Achievement a : allAchievements) {
            a.setUnlocked(false);
        }
    }

    public interface AchievementsCallback {
        void onAchievementsLoaded(List<Achievement> achievements);
        void onAchievementsLoadFailed(Exception exception);
    }

    public interface AchievementListener {
        void onAchievementUnlocked(Achievement achievement);
    }
}
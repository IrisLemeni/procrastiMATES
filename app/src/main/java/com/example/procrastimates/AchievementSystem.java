package com.example.procrastimates;

import com.example.procrastimates.Achievement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AchievementSystem {

    // Definirea nivelurilor și experiența necesară
    public static final int[] XP_LEVELS = {
            0, 100, 250, 450, 700, 1000, 1350, 1750, 2200, 2700, 3250,
            3850, 4500, 5200, 6000, 6900, 7900, 9000, 10200, 11500, 13000
    };

    // Lista cu toate achievement-urile posibile
    public static final Map<String, Achievement> ACHIEVEMENTS = new HashMap<String, Achievement>() {{
        // Focus achievements
        put("focus_novice", new Achievement(
                "focus_novice",
                "Focus Novice",
                "Complete 5 Pomodoro sessions",
                "achievement_focus_novice",
                5,
                50
        ));
        put("focus_adept", new Achievement(
                "focus_adept",
                "Focus Adept",
                "Complete 25 Pomodoro sessions",
                "achievement_focus_adept",
                25,
                150
        ));
        put("focus_master", new Achievement(
                "focus_master",
                "Focus Master",
                "Complete 100 Pomodoro sessions",
                "achievement_focus_master",
                100,
                500
        ));

        // Perfect sessions (no interruptions)
        put("perfect_focus_1", new Achievement(
                "perfect_focus_1",
                "Perfect Focus I",
                "Complete 3 sessions with 100% focus score",
                "achievement_perfect_focus_1",
                3,
                100
        ));
        put("perfect_focus_2", new Achievement(
                "perfect_focus_2",
                "Perfect Focus II",
                "Complete 10 sessions with 100% focus score",
                "achievement_perfect_focus_2",
                10,
                250
        ));

        // Streak achievements
        put("streak_beginner", new Achievement(
                "streak_beginner",
                "Consistency Beginner",
                "Reach a 3-day streak",
                "achievement_streak_beginner",
                3,
                75
        ));
        put("streak_intermediate", new Achievement(
                "streak_intermediate",
                "Consistency Intermediate",
                "Reach a 7-day streak",
                "achievement_streak_intermediate",
                7,
                150
        ));
        put("streak_advanced", new Achievement(
                "streak_advanced",
                "Consistency Advanced",
                "Reach a 14-day streak",
                "achievement_streak_advanced",
                14,
                300
        ));
        put("streak_master", new Achievement(
                "streak_master",
                "Consistency Master",
                "Reach a 30-day streak",
                "achievement_streak_master",
                30,
                500
        ));

        // Task completion achievements
        put("task_beginner", new Achievement(
                "task_beginner",
                "Task Completer I",
                "Complete 10 tasks",
                "achievement_task_beginner",
                10,
                50
        ));
        put("task_intermediate", new Achievement(
                "task_intermediate",
                "Task Completer II",
                "Complete 50 tasks",
                "achievement_task_intermediate",
                50,
                150
        ));
        put("task_advanced", new Achievement(
                "task_advanced",
                "Task Completer III",
                "Complete 150 tasks",
                "achievement_task_advanced",
                150,
                300
        ));

        // Special achievements
        put("night_owl", new Achievement(
                "night_owl",
                "Night Owl",
                "Complete 5 Pomodoro sessions after 10PM",
                "achievement_night_owl",
                5,
                100
        ));
        put("early_bird", new Achievement(
                "early_bird",
                "Early Bird",
                "Complete 5 Pomodoro sessions before 8AM",
                "achievement_early_bird",
                5,
                100
        ));
        put("weekend_warrior", new Achievement(
                "weekend_warrior",
                "Weekend Warrior",
                "Complete 10 tasks during weekends",
                "achievement_weekend_warrior",
                10,
                150
        ));
    }};

    // Verifică dacă utilizatorul a obținut un achievement nou
    public static List<Achievement> checkNewAchievements(
            int totalSessions,
            int perfectSessions,
            int currentStreak,
            int completedTasks,
            int nightSessions,
            int morningSessions,
            int weekendTasks,
            List<String> unlockedAchievements) {

        List<Achievement> newAchievements = new ArrayList<>();

        // Verifică fiecare achievement posibil
        for (Achievement achievement : ACHIEVEMENTS.values()) {
            // Dacă achievement-ul nu este deja deblocat
            if (!unlockedAchievements.contains(achievement.getId())) {
                boolean unlocked = false;

                // Verifică condițiile în funcție de tipul achievement-ului
                switch (achievement.getId()) {
                    // Focus achievements
                    case "focus_novice":
                        unlocked = totalSessions >= 5;
                        break;
                    case "focus_adept":
                        unlocked = totalSessions >= 25;
                        break;
                    case "focus_master":
                        unlocked = totalSessions >= 100;
                        break;

                    // Perfect sessions
                    case "perfect_focus_1":
                        unlocked = perfectSessions >= 3;
                        break;
                    case "perfect_focus_2":
                        unlocked = perfectSessions >= 10;
                        break;

                    // Streak achievements
                    case "streak_beginner":
                        unlocked = currentStreak >= 3;
                        break;
                    case "streak_intermediate":
                        unlocked = currentStreak >= 7;
                        break;
                    case "streak_advanced":
                        unlocked = currentStreak >= 14;
                        break;
                    case "streak_master":
                        unlocked = currentStreak >= 30;
                        break;

                    // Task achievements
                    case "task_beginner":
                        unlocked = completedTasks >= 10;
                        break;
                    case "task_intermediate":
                        unlocked = completedTasks >= 50;
                        break;
                    case "task_advanced":
                        unlocked = completedTasks >= 150;
                        break;

                    // Special achievements
                    case "night_owl":
                        unlocked = nightSessions >= 5;
                        break;
                    case "early_bird":
                        unlocked = morningSessions >= 5;
                        break;
                    case "weekend_warrior":
                        unlocked = weekendTasks >= 10;
                        break;
                }

                if (unlocked) {
                    newAchievements.add(achievement);
                }
            }
        }

        return newAchievements;
    }

    // Determină nivelul bazat pe experiența totală
    public static int getLevelFromXP(int experiencePoints) {
        for (int i = XP_LEVELS.length - 1; i >= 0; i--) {
            if (experiencePoints >= XP_LEVELS[i]) {
                return i;
            }
        }
        return 0;
    }

    // Calculează progresul spre următorul nivel (0-100%)
    public static int getLevelProgress(int experiencePoints) {
        int currentLevel = getLevelFromXP(experiencePoints);

        // Dacă e nivelul maxim, returneză 100%
        if (currentLevel >= XP_LEVELS.length - 1) {
            return 100;
        }

        int currentLevelXP = XP_LEVELS[currentLevel];
        int nextLevelXP = XP_LEVELS[currentLevel + 1];
        int xpForNextLevel = nextLevelXP - currentLevelXP;
        int xpProgress = experiencePoints - currentLevelXP;

        return (xpProgress * 100) / xpForNextLevel;
    }

    // Verifică dacă utilizatorul a trecut la următorul nivel
    public static boolean hasLeveledUp(int oldXP, int newXP) {
        return getLevelFromXP(oldXP) < getLevelFromXP(newXP);
    }
}



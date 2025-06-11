package com.example.procrastimates.fragments;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.procrastimates.models.Achievement;
import com.example.procrastimates.components.AchievementDialogHelper;
import com.example.procrastimates.R;
import com.example.procrastimates.services.AchievementManager;
import com.example.procrastimates.services.FocusLockService;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PomodoroFragment extends Fragment implements AchievementManager.AchievementListener{

    private boolean isServiceBound = false;
    private FocusLockService boundService;
    private static final String KEY_IS_SESSION_RUNNING = "isSessionRunning";
    private static final String KEY_SESSION_DURATION = "sessionDuration";
    private static final String KEY_BREAK_DURATION = "breakDuration";
    private static final String KEY_REMAINING_TIME = "remainingTime";
    private static final String KEY_IS_WORK_SESSION = "isWorkSession";
    private static final String KEY_SELECTED_BACKGROUND = "selectedBackground";
    private static final String KEY_INTERRUPTION_COUNT = "interruptionCount";
    private static final String KEY_FOCUS_SCORE = "focusScore";
    private static final String KEY_TIME_OUTSIDE_APP = "timeOutsideApp";
    private static final String KEY_CURRENT_VIEW_STATE = "currentViewState";

    // UI Elements - Updated to match new layout
    private CardView selectDurationButton25, selectDurationButton50;
    private CardView selectBackgroundButton, startTimerButton;
    private int selectedBackground = -1;
    private TextView timerText, workingTime, breakTime, focusScoreText, interruptionCountText;
    private ImageView backgroundOption1, backgroundOption2, backgroundOption3;
    private ProgressBar focusProgressBar, circularProgress;
    private LinearLayout linearLayoutDuration, linearLayoutBackground;
    private RelativeLayout linearLayoutTimer;
    private CardView linearLayoutStats;

    // Session variables
    private boolean isSessionRunning = false;
    private int sessionDuration, breakDuration;
    private CountDownTimer countDownTimer;
    private int interruptionCount = 0;
    private long timeOutsideApp = 0;
    private int focusScore = 100;
    private BroadcastReceiver focusInterruptionReceiver;
    private long remainingTimeMillis = 0;
    private boolean isWorkSession = true;
    private String currentViewState = "duration";
    private int totalSessionTime = 0; // For progress calculation

    private AchievementManager achievementManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate layout for this fragment
        View view = inflater.inflate(R.layout.fragment_pomodoro, container, false);

        // Initialize AchievementManager
        achievementManager = AchievementManager.getInstance();
        achievementManager.addAchievementListener(this);

        initViews(view);
        setupListeners();
        setupBroadcastReceiver();

        // Restore state if available
        if (savedInstanceState != null) {
            restoreInstanceState(savedInstanceState);
        }

        return view;
    }

    public interface FocusLockListener {
        void setBottomNavEnabled(boolean enabled);
    }

    private FocusLockListener focusLockListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            focusLockListener = (FocusLockListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " trebuie să implementeze FocusLockListener");
        }
    }

    private void initViews(View view) {
        // Session type indicators
        workingTime = view.findViewById(R.id.workingTime);
        breakTime = view.findViewById(R.id.breakTime);

        // Layout containers
        linearLayoutDuration = view.findViewById(R.id.linearLayoutDuration);
        linearLayoutBackground = view.findViewById(R.id.linearLayoutBackground);
        linearLayoutTimer = view.findViewById(R.id.linearLayoutTimer);
        linearLayoutStats = view.findViewById(R.id.linearLayoutStats);

        // Stats elements
        focusScoreText = view.findViewById(R.id.focusScoreText);
        interruptionCountText = view.findViewById(R.id.interruptionCountText);
        focusProgressBar = view.findViewById(R.id.focusProgressBar);

        // Timer elements
        timerText = view.findViewById(R.id.timerText);
        circularProgress = view.findViewById(R.id.circularProgress);

        // Background selection
        backgroundOption1 = view.findViewById(R.id.backgroundOption1);
        backgroundOption2 = view.findViewById(R.id.backgroundOption2);
        backgroundOption3 = view.findViewById(R.id.backgroundOption3);
        selectBackgroundButton = view.findViewById(R.id.selectBackgroundButton);

        // Duration selection buttons (now CardViews)
        selectDurationButton25 = view.findViewById(R.id.selectDurationButton25);
        selectDurationButton50 = view.findViewById(R.id.selectDurationButton50);

        // Timer control button (now CardView)
        startTimerButton = view.findViewById(R.id.startTimerButton);
    }

    private void setupListeners() {
        // Background selection listeners
        backgroundOption1.setOnClickListener(v -> selectBackground(1));
        backgroundOption2.setOnClickListener(v -> selectBackground(2));
        backgroundOption3.setOnClickListener(v -> selectBackground(3));

        selectBackgroundButton.setOnClickListener(v -> {
            if (selectedBackground != -1) {
                linearLayoutBackground.setVisibility(View.GONE);
                linearLayoutTimer.setVisibility(View.VISIBLE);
                linearLayoutStats.setVisibility(View.VISIBLE);
                currentViewState = "timer";
            } else {
                Toast.makeText(getContext(), "Please select a background first.", Toast.LENGTH_SHORT).show();
            }
        });

        // Duration selection listeners
        selectDurationButton25.setOnClickListener(v -> {
            sessionDuration = 1 * 60 * 1000; // 25 minutes
            breakDuration = 5 * 60 * 1000; // 5 minutes
            totalSessionTime = sessionDuration; // Store total for progress calculation
            linearLayoutDuration.setVisibility(View.GONE);
            linearLayoutBackground.setVisibility(View.VISIBLE);
            currentViewState = "background";
        });

        selectDurationButton50.setOnClickListener(v -> {
            sessionDuration = 50 * 60 * 1000; // 50 minutes
            breakDuration = 10 * 60 * 1000; // 10 minutes
            totalSessionTime = sessionDuration; // Store total for progress calculation
            linearLayoutDuration.setVisibility(View.GONE);
            linearLayoutBackground.setVisibility(View.VISIBLE);
            currentViewState = "background";
        });

        // Timer control listener
        startTimerButton.setOnClickListener(v -> {
            if (isSessionRunning) {
                stopSession();
            } else {
                startSession();
            }
        });
    }

    private void setupBroadcastReceiver() {
        focusInterruptionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("PomodoroFragment", "Broadcast received: " + intent.getAction());

                if (FocusLockService.ACTION_INTERRUPTION.equals(intent.getAction())) {
                    String appName = intent.getStringExtra("app_name");
                    int interruptionCount = intent.getIntExtra("interruption_count", 0);
                    long timeElapsed = intent.getLongExtra("time_elapsed", 0);

                    Log.d("PomodoroFragment", "Processing interruption: app=" + appName +
                            ", count=" + interruptionCount + ", time=" + timeElapsed);

                    // FIX: Verifică că datele sunt valide înainte de procesare
                    if (appName != null && !appName.isEmpty() && interruptionCount > 0) {
                        // Procesează pe main thread
                        new Handler(Looper.getMainLooper()).post(() -> {
                            handleFocusInterruption(appName, interruptionCount, timeElapsed);
                        });
                    } else {
                        Log.w("PomodoroFragment", "Invalid interruption data received");
                    }
                }
            }
        };
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save all current state
        outState.putBoolean(KEY_IS_SESSION_RUNNING, isSessionRunning);
        outState.putInt(KEY_SESSION_DURATION, sessionDuration);
        outState.putInt(KEY_BREAK_DURATION, breakDuration);
        outState.putLong(KEY_REMAINING_TIME, remainingTimeMillis);
        outState.putBoolean(KEY_IS_WORK_SESSION, isWorkSession);
        outState.putInt(KEY_SELECTED_BACKGROUND, selectedBackground);
        outState.putInt(KEY_INTERRUPTION_COUNT, interruptionCount);
        outState.putInt(KEY_FOCUS_SCORE, focusScore);
        outState.putLong(KEY_TIME_OUTSIDE_APP, timeOutsideApp);
        outState.putString(KEY_CURRENT_VIEW_STATE, currentViewState);
    }

    private void restoreInstanceState(Bundle savedInstanceState) {
        isSessionRunning = savedInstanceState.getBoolean(KEY_IS_SESSION_RUNNING, false);
        sessionDuration = savedInstanceState.getInt(KEY_SESSION_DURATION, 25 * 60 * 1000);
        breakDuration = savedInstanceState.getInt(KEY_BREAK_DURATION, 5 * 60 * 1000);
        remainingTimeMillis = savedInstanceState.getLong(KEY_REMAINING_TIME, 0);
        isWorkSession = savedInstanceState.getBoolean(KEY_IS_WORK_SESSION, true);
        selectedBackground = savedInstanceState.getInt(KEY_SELECTED_BACKGROUND, -1);
        interruptionCount = savedInstanceState.getInt(KEY_INTERRUPTION_COUNT, 0);
        focusScore = savedInstanceState.getInt(KEY_FOCUS_SCORE, 100);
        timeOutsideApp = savedInstanceState.getLong(KEY_TIME_OUTSIDE_APP, 0);
        currentViewState = savedInstanceState.getString(KEY_CURRENT_VIEW_STATE, "duration");
        totalSessionTime = isWorkSession ? sessionDuration : breakDuration;

        // Restore UI state
        restoreUiState();

        // Restart timer if it was running
        if (isSessionRunning && remainingTimeMillis > 0) {
            restartTimer();
        }
    }

    private void restoreUiState() {
        // Restore the proper view visibility based on saved state
        switch (currentViewState) {
            case "background":
                linearLayoutDuration.setVisibility(View.GONE);
                linearLayoutBackground.setVisibility(View.VISIBLE);
                linearLayoutTimer.setVisibility(View.GONE);
                linearLayoutStats.setVisibility(View.GONE);
                break;
            case "timer":
                linearLayoutDuration.setVisibility(View.GONE);
                linearLayoutBackground.setVisibility(View.GONE);
                linearLayoutTimer.setVisibility(View.VISIBLE);
                linearLayoutStats.setVisibility(View.VISIBLE);

                // Update timer button text
                TextView buttonText = startTimerButton.findViewById(android.R.id.text1);
                if (buttonText == null) {
                    // If no specific TextView found, look for any TextView in the CardView
                    View child = startTimerButton.getChildAt(0);
                    if (child instanceof TextView) {
                        buttonText = (TextView) child;
                    }
                }
                if (buttonText != null) {
                    buttonText.setText(isSessionRunning ? "⏹ Stop Session" : "▶ Start Session");
                }

                // Show correct session type label
                workingTime.setVisibility(isWorkSession ? View.VISIBLE : View.GONE);
                breakTime.setVisibility(isWorkSession ? View.GONE : View.VISIBLE);

                // Update stats
                updateSessionStats();

                // Restore background
                if (selectedBackground != -1) {
                    selectBackground(selectedBackground);
                }
                break;
            case "duration":
            default:
                linearLayoutDuration.setVisibility(View.VISIBLE);
                linearLayoutBackground.setVisibility(View.GONE);
                linearLayoutTimer.setVisibility(View.GONE);
                linearLayoutStats.setVisibility(View.GONE);
                break;
        }
    }

    private void restartTimer() {
        // Cancel any existing timer
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        // Restart the focus lock service if needed
        if (isSessionRunning && isWorkSession) {
            Intent lockIntent = new Intent(requireContext(), FocusLockService.class);
            lockIntent.putExtra(FocusLockService.EXTRA_LOCK_ACTIVE, true);
            requireContext().startService(lockIntent);
        }

        // Start a new timer with the remaining time
        startTimer(remainingTimeMillis, isWorkSession);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Înregistrez receiver-ul pentru întreruperi
        LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(focusInterruptionReceiver,
                        new IntentFilter(FocusLockService.ACTION_INTERRUPTION));

        // Dacă sesiunea e pornită, atunci apelez bindService
        if (isSessionRunning) {
            Intent serviceIntent = new Intent(requireContext(), FocusLockService.class);
            requireContext().bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Dezînregistrăm receiver-ul
        try {
            LocalBroadcastManager.getInstance(requireContext())
                    .unregisterReceiver(focusInterruptionReceiver);
        } catch (Exception e) {
            Log.e("PomodoroFragment", "Error unregistering receiver: " + e.getMessage());
        }

        // Facem unbindService DOAR dacă serviciul chiar era bound
        if (isServiceBound) {
            requireContext().unbindService(serviceConnection);
            isServiceBound = false;
        }
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            FocusLockService.LocalBinder localBinder = (FocusLockService.LocalBinder) binder;
            boundService = localBinder.getService();
            isServiceBound = true;

            // Citește datele curente din serviciu
            interruptionCount = boundService.getTotalInterruptions();
            focusScore = boundService.getCurrentFocusScore();
            updateSessionStats();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;
            boundService = null;
        }
    };


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (focusLockListener != null && isSessionRunning) {
            focusLockListener.setBottomNavEnabled(true);
        }

        achievementManager.removeAchievementListener(this);
    }

    @Override
    public void onAchievementUnlocked(Achievement achievement) {
        if (isAdded() && getActivity() != null) {
            // Show the achievement unlocked dialog on the UI thread
            getActivity().runOnUiThread(() -> {
                // Play achievement sound
                playAchievementSound();

                // Show custom achievement dialog
                AchievementDialogHelper.showAchievementUnlockedDialog(getActivity(), achievement);
            });
        }
    }

    private void playAchievementSound() {
        MediaPlayer mediaPlayer = MediaPlayer.create(getContext(), R.raw.positive);
        mediaPlayer.setOnCompletionListener(MediaPlayer::release);
        mediaPlayer.start();
    }

    private void selectBackground(int option) {
        resetBackgroundSelection();
        selectedBackground = option;

        switch (option) {
            case 1:
                backgroundOption1.setForeground(requireContext().getDrawable(R.drawable.highlight_overlay));
                linearLayoutTimer.setBackgroundResource(R.drawable.photo1);
                break;
            case 2:
                backgroundOption2.setForeground(requireContext().getDrawable(R.drawable.highlight_overlay));
                linearLayoutTimer.setBackgroundResource(R.drawable.photo2);
                break;
            case 3:
                backgroundOption3.setForeground(requireContext().getDrawable(R.drawable.highlight_overlay));
                linearLayoutTimer.setBackgroundResource(R.drawable.photo3);
                break;
        }
    }

    private void resetBackgroundSelection() {
        backgroundOption1.setForeground(null);
        backgroundOption2.setForeground(null);
        backgroundOption3.setForeground(null);
    }

    private void startSession() {
        // Check for usage stats permission
        if (!FocusLockService.hasUsageStatsPermission(requireContext())) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Permission Required")
                    .setMessage("Focus lock requires usage access permission to work properly. Please enable it in the settings.")
                    .setPositiveButton("Go to Settings", (dialog, which) -> {
                        startActivity(FocusLockService.getUsageStatsSettingsIntent());
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }

        // Reset session metrics
        interruptionCount = 0;
        timeOutsideApp = 0;
        focusScore = 100;
        isWorkSession = true;
        totalSessionTime = sessionDuration;
        updateSessionStats();

        // Start the focus lock service
        try {
            Intent lockIntent = new Intent(requireContext(), FocusLockService.class);
            lockIntent.putExtra(FocusLockService.EXTRA_LOCK_ACTIVE, true);

            // Use startForegroundService for Android 8.0+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireContext().startForegroundService(lockIntent);
            } else {
                requireContext().startService(lockIntent);
            }

            Log.d("PomodoroFragment", "Focus lock service started successfully");
        } catch (Exception e) {
            Log.e("PomodoroFragment", "Error starting focus lock service: " + e.getMessage());
            Toast.makeText(requireContext(), "Focus lock may not work properly", Toast.LENGTH_SHORT).show();
        }

        if (focusLockListener != null) {
            focusLockListener.setBottomNavEnabled(false);
        }

        isSessionRunning = true;
        workingTime.setVisibility(View.VISIBLE);
        breakTime.setVisibility(View.GONE);

        // Update button text
        updateTimerButtonText("⏹ Stop Session");

        startTimer(sessionDuration, true);
    }

    private void stopSession() {
        if (focusLockListener != null) {
            focusLockListener.setBottomNavEnabled(true);
        }
        if (countDownTimer != null) countDownTimer.cancel();
        isSessionRunning = false;

        // Update button text and timer display
        updateTimerButtonText("▶ Start Session");
        timerText.setText("00:00");
        remainingTimeMillis = 0;

        // Reset circular progress
        if (circularProgress != null) {
            circularProgress.setProgress(0);
        }

        // Stop the focus lock service
        try {
            Intent lockIntent = new Intent(requireContext(), FocusLockService.class);
            lockIntent.putExtra(FocusLockService.EXTRA_LOCK_ACTIVE, false);
            requireContext().startService(lockIntent);

            // Also stop the service completely
            requireContext().stopService(new Intent(requireContext(), FocusLockService.class));
        } catch (Exception e) {
            Log.e("PomodoroFragment", "Error stopping focus lock service: " + e.getMessage());
        }

        // Reset to duration selection view
        linearLayoutTimer.setVisibility(View.GONE);
        linearLayoutBackground.setVisibility(View.GONE);
        linearLayoutStats.setVisibility(View.GONE);
        linearLayoutDuration.setVisibility(View.VISIBLE);
        currentViewState = "duration";
    }


    private void updateTimerButtonText(String text) {
        // Find the TextView inside the CardView and update its text
        View child = startTimerButton.getChildAt(0);
        if (child instanceof TextView) {
            ((TextView) child).setText(text);
        }
    }

    private void startTimer(long duration, boolean isWorkSession) {
        this.isWorkSession = isWorkSession;
        this.totalSessionTime = (int) duration;

        countDownTimer = new CountDownTimer(duration, 1000) {
            public void onTick(long millisUntilFinished) {
                remainingTimeMillis = millisUntilFinished;
                int minutes = (int) (millisUntilFinished / 1000) / 60;
                int seconds = (int) (millisUntilFinished / 1000) % 60;
                timerText.setText(String.format("%02d:%02d", minutes, seconds));

                // Update circular progress
                if (circularProgress != null && totalSessionTime > 0) {
                    int progress = (int) ((totalSessionTime - millisUntilFinished) * 100 / totalSessionTime);
                    circularProgress.setProgress(progress);
                }
            }

            public void onFinish() {
                remainingTimeMillis = 0;

                // Complete the circular progress
                if (circularProgress != null) {
                    circularProgress.setProgress(100);
                }

                if (isWorkSession) {
                    saveSessionToFirestore(true);

                    // Check achievements for completed session
                    achievementManager.checkSessionAchievements(true, sessionDuration, focusScore);

                    showCustomAlert("break");
                    breakTime.setVisibility(View.VISIBLE);
                    workingTime.setVisibility(View.GONE);
                    if (focusLockListener != null) {
                        focusLockListener.setBottomNavEnabled(true);
                    }

                    // Stop the focus lock during break
                    Intent lockIntent = new Intent(requireContext(), FocusLockService.class);
                    lockIntent.putExtra(FocusLockService.EXTRA_LOCK_ACTIVE, false);
                    requireContext().startService(lockIntent);

                    startTimer(breakDuration, false);
                } else {
                    saveSessionToFirestore(false);
                    showCustomAlert("work");
                    breakTime.setVisibility(View.GONE);
                    workingTime.setVisibility(View.VISIBLE);
                    if (focusLockListener != null) {
                        focusLockListener.setBottomNavEnabled(false);
                    }

                    // Start the focus lock for work session
                    Intent lockIntent = new Intent(requireContext(), FocusLockService.class);
                    lockIntent.putExtra(FocusLockService.EXTRA_LOCK_ACTIVE, true);
                    requireContext().startService(lockIntent);

                    // Reset focus metrics for new session
                    interruptionCount = 0;
                    timeOutsideApp = 0;
                    focusScore = 100;
                    updateSessionStats();

                    startTimer(sessionDuration, true);
                }
            }
        }.start();
    }

    private void saveSessionToFirestore(boolean isWorkSession) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Date currentDate = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dayString = sdf.format(currentDate);

        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("userId", userId);
        sessionData.put("timestamp", new Timestamp(currentDate));
        sessionData.put("type", isWorkSession ? "work" : "break");
        sessionData.put("duration", isWorkSession ? sessionDuration / 60000 : breakDuration / 60000); // Duration in minutes

        // Add focus metrics
        if (isWorkSession) {
            sessionData.put("focusScore", focusScore);
            sessionData.put("interruptionCount", interruptionCount);
            sessionData.put("timeOutsideApp", timeOutsideApp);
        }

        // Save session to "pomodoro_sessions" collection
        db.collection("pomodoro_sessions")
                .add(sessionData)
                .addOnSuccessListener(documentReference -> {
                    Log.d("PomodoroFragment", "Session saved successfully! ID: " + documentReference.getId());
                    updateDailySessionCounter(dayString, userId);
                })
                .addOnFailureListener(e -> Log.e("PomodoroFragment", "Error saving session", e));
    }

    // Update daily session counter
    private void updateDailySessionCounter(String dayString, String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Check if a document for this day already exists
        DocumentReference dailySessionRef = db.collection("daily_sessions")
                .document(userId)
                .collection("sessions_by_day")
                .document(dayString);

        dailySessionRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // If document exists, increment the session counter
                long currentCount = documentSnapshot.getLong("sessionCount") != null ? documentSnapshot.getLong("sessionCount") : 0;
                dailySessionRef.update("sessionCount", currentCount + 1);
            } else {
                // If document doesn't exist, create it with a session count of 1
                Map<String, Object> dailyData = new HashMap<>();
                dailyData.put("sessionCount", 1);
                dailySessionRef.set(dailyData);
            }
        });
    }

    private void showCustomAlert(String type) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View customView;

        if (type.equals("break")) {
            customView = getLayoutInflater().inflate(R.layout.custom_alert_breaktime, null);
            playSound(R.raw.positive);
        } else {
            customView = getLayoutInflater().inflate(R.layout.custom_alert_worktime, null);
            playSound(R.raw.positive);
        }

        builder.setView(customView);
        AlertDialog dialog = builder.create();

        TextView btnClose = customView.findViewById(R.id.btnClose);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void playSound(int soundResId) {
        MediaPlayer mediaPlayer = MediaPlayer.create(getContext(), soundResId);
        mediaPlayer.setOnCompletionListener(mp -> mp.release());
        mediaPlayer.start();
    }

    // Handle focus interruption from another app
    private void handleFocusInterruption(String appName, int interruptionCount, long timeElapsed) {
        Log.d("PomodoroFragment", "Handling focus interruption: " + appName + ", count: " + interruptionCount);

        // Update interruption count
        this.interruptionCount = interruptionCount;

        // Calculate focus score based on interruptions
        focusScore = Math.max(0, 100 - (interruptionCount * 10));

        // Update time outside app (convert to seconds for display)
        timeOutsideApp = timeElapsed / 1000;

        // FIX: Asigură-te că UI se actualizează pe main thread
        if (getActivity() != null && isAdded()) {
            getActivity().runOnUiThread(() -> {
                try {
                    updateSessionStats();

                    // Show brief feedback to user
                    Toast.makeText(requireContext(),
                            "Focus interrupted by " + getAppDisplayName(appName) +
                                    "! Score: " + focusScore + " (-" + (interruptionCount * 10) + " points)",
                            Toast.LENGTH_LONG).show(); // Schimbat la LONG pentru feedback mai clar

                    Log.d("PomodoroFragment", "UI updated - Score: " + focusScore + ", Interruptions: " + interruptionCount);
                } catch (Exception e) {
                    Log.e("PomodoroFragment", "Error updating UI: " + e.getMessage());
                }
            });
        }
    }


    private String getAppDisplayName(String packageName) {
        try {
            PackageManager pm = requireContext().getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            return pm.getApplicationLabel(appInfo).toString();
        } catch (Exception e) {
            // If we can't get the app name, return a simplified package name
            String[] parts = packageName.split("\\.");
            return parts.length > 0 ? parts[parts.length - 1] : packageName;
        }
    }

    // Update focus stats UI
    private void updateSessionStats() {
        try {
            // Verifică că suntem pe main thread
            if (Looper.myLooper() != Looper.getMainLooper()) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(this::updateSessionStats);
                }
                return;
            }

            if (!isAdded() || getContext() == null) {
                Log.w("PomodoroFragment", "Fragment not attached, skipping UI update");
                return;
            }

            // FIX: Verifică că toate view-urile există înainte de actualizare
            if (focusScoreText != null) {
                String scoreText = "Focus Score: " + focusScore;
                focusScoreText.setText(scoreText);
                Log.d("PomodoroFragment", "Updated focus score text: " + scoreText);
            } else {
                Log.w("PomodoroFragment", "focusScoreText is null");
            }

            if (interruptionCountText != null) {
                String interruptionText = "Interruptions: " + interruptionCount;
                interruptionCountText.setText(interruptionText);
                Log.d("PomodoroFragment", "Updated interruption text: " + interruptionText);
            } else {
                Log.w("PomodoroFragment", "interruptionCountText is null");
            }

            if (focusProgressBar != null) {
                focusProgressBar.setProgress(focusScore);
                Log.d("PomodoroFragment", "Updated progress bar: " + focusScore);
            } else {
                Log.w("PomodoroFragment", "focusProgressBar is null");
            }

            Log.d("PomodoroFragment", "Session stats updated successfully - Score: " + focusScore +
                    ", Interruptions: " + interruptionCount);

        } catch (Exception e) {
            Log.e("PomodoroFragment", "Error updating session stats: " + e.getMessage(), e);
        }
    }
}
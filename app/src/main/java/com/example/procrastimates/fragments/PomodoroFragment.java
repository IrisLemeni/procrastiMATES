package com.example.procrastimates.fragments;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.procrastimates.R;
import com.example.procrastimates.repositories.TaskRepository;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PomodoroFragment extends Fragment {

    private Button selectDurationButton25, selectDurationButton50;
    private Button selectBackgroundButton, sessionButton;
    private int selectedBackground = -1;
    private TextView timerText, workingTime, breakTime;
    private ImageView backgroundOption1, backgroundOption2, backgroundOption3;
    private LinearLayout linearLayoutDuration, linearLayoutBackground, linearLayoutTimer;
    private boolean isSessionRunning = false;
    private int sessionDuration, breakDuration;
    private CountDownTimer countDownTimer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate layout for this fragment
        View view = inflater.inflate(R.layout.fragment_pomodoro, container, false);

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        workingTime = view.findViewById(R.id.workingTime);
        breakTime = view.findViewById(R.id.breakTime);
        linearLayoutDuration = view.findViewById(R.id.linearLayoutDuration);
        linearLayoutBackground = view.findViewById(R.id.linearLayoutBackground);
        linearLayoutTimer = view.findViewById(R.id.linearLayoutTimer);

        backgroundOption1 = view.findViewById(R.id.backgroundOption1);
        backgroundOption2 = view.findViewById(R.id.backgroundOption2);
        backgroundOption3 = view.findViewById(R.id.backgroundOption3);
        selectBackgroundButton = view.findViewById(R.id.selectBackgroundButton);

        selectDurationButton25 = view.findViewById(R.id.selectDurationButton25);
        selectDurationButton50 = view.findViewById(R.id.selectDurationButton50);
        sessionButton = view.findViewById(R.id.startTimerButton);
        timerText = view.findViewById(R.id.timerText);

        backgroundOption1.setOnClickListener(v -> selectBackground(1));
        backgroundOption2.setOnClickListener(v -> selectBackground(2));
        backgroundOption3.setOnClickListener(v -> selectBackground(3));

        selectBackgroundButton.setOnClickListener(v -> {
            if (selectedBackground != -1) {
                linearLayoutBackground.setVisibility(View.GONE);
                linearLayoutTimer.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(getContext(), "Please select a background first.", Toast.LENGTH_SHORT).show();
            }
        });

        selectDurationButton25.setOnClickListener(v -> {
            sessionDuration = 24 * 60 * 1000; // 25 minutes
            breakDuration = 5 * 60 * 1000; // 5 minutes
            linearLayoutDuration.setVisibility(View.GONE);
            linearLayoutBackground.setVisibility(View.VISIBLE);
        });

        selectDurationButton50.setOnClickListener(v -> {
            sessionDuration = 50 * 60 * 1000; // 50 minutes
            breakDuration = 10 * 60 * 1000; // 10 minutes
            linearLayoutDuration.setVisibility(View.GONE);
            linearLayoutBackground.setVisibility(View.VISIBLE);
        });

        // Start or stop session
        sessionButton.setOnClickListener(v -> {
            if (isSessionRunning) {
                stopSession();
            } else {
                startSession();
            }
        });

        return view;
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
        isSessionRunning = true;
        workingTime.setVisibility(View.VISIBLE);
        breakTime.setVisibility(View.GONE);
        sessionButton.setText("Stop Session");
        startTimer(sessionDuration, true);
    }

    private void stopSession() {
        if (countDownTimer != null) countDownTimer.cancel();
        isSessionRunning = false;
        sessionButton.setText("Start Timer");
        timerText.setText("00:00");

        linearLayoutTimer.setVisibility(View.GONE);
        linearLayoutBackground.setVisibility(View.GONE);
        linearLayoutDuration.setVisibility(View.VISIBLE);
    }

    private void startTimer(long duration, boolean isWorkSession) {
        countDownTimer = new CountDownTimer(duration, 1000) {
            public void onTick(long millisUntilFinished) {
                int minutes = (int) (millisUntilFinished / 1000) / 60;
                int seconds = (int) (millisUntilFinished / 1000) % 60;
                timerText.setText(String.format("%02d:%02d", minutes, seconds));
            }

            public void onFinish() {
                if (isWorkSession) {
                    saveSessionToFirestore(true);
                    showCustomAlert("break");
                    breakTime.setVisibility(View.VISIBLE);
                    workingTime.setVisibility(View.GONE);
                    startTimer(breakDuration, false);
                } else {
                    saveSessionToFirestore(false);
                    showCustomAlert("work");
                    breakTime.setVisibility(View.GONE);
                    workingTime.setVisibility(View.VISIBLE);
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
        String dayString = sdf.format(currentDate); // Formatăm data ca "yyyy-MM-dd" pentru a salva o sesiune zilnică

        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("userId", userId);
        sessionData.put("timestamp", new Timestamp(currentDate));
        sessionData.put("type", isWorkSession ? "work" : "break");
        sessionData.put("duration", isWorkSession ? sessionDuration / 60000 : breakDuration / 60000); // Durata în minute

        // Salvăm sesiunea în colecția "pomodoro_sessions"
        db.collection("pomodoro_sessions")
                .add(sessionData)
                .addOnSuccessListener(documentReference -> {
                    Log.d("PomodoroFragment", "Session saved successfully! ID: " + documentReference.getId());

                    // După salvarea sesiunii, actualizăm contorul de sesiuni pe ziua respectivă
                    updateDailySessionCounter(dayString, userId);
                })
                .addOnFailureListener(e -> Log.e("PomodoroFragment", "Error saving session", e));
    }

    // Actualizăm contorul de sesiuni pentru ziua respectivă
    private void updateDailySessionCounter(String dayString, String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Verificăm dacă există deja un document pentru ziua respectivă
        DocumentReference dailySessionRef = db.collection("daily_sessions")
                .document(userId)
                .collection("sessions_by_day")
                .document(dayString);

        dailySessionRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Dacă documentul există, incrementăm contorul sesiunilor
                long currentCount = documentSnapshot.getLong("sessionCount") != null ? documentSnapshot.getLong("sessionCount") : 0;
                dailySessionRef.update("sessionCount", currentCount + 1);
            } else {
                // Dacă documentul nu există, îl creăm cu un contor de 1 sesiune
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

        Button btnClose = customView.findViewById(R.id.btnClose);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void playSound(int soundResId) {
        MediaPlayer mediaPlayer = MediaPlayer.create(getContext(), soundResId);
        mediaPlayer.setOnCompletionListener(mp -> mp.release());
        mediaPlayer.start();
    }

}

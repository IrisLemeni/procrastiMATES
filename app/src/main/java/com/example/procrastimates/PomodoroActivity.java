package com.example.procrastimates;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class PomodoroActivity extends AppCompatActivity {

    private Button selectDurationButton25, selectDurationButton50;
    private Button selectBackgroundButton, sessionButton;
    private int selectedBackground = -1;
    private TextView timerText, workingTime, breakTime;
    private ImageView backgroundOption1, backgroundOption2, backgroundOption3;
    private LinearLayout linearLayoutDuration, linearLayoutBackground, linearLayoutTimer;
    private boolean isSessionRunning = false;
    private int sessionDuration, breakDuration;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pomodoro);

        workingTime = findViewById(R.id.workingTime);
        breakTime = findViewById(R.id.breakTime);

        FrameLayout frameLayout = findViewById(R.id.frameLayout);
        linearLayoutDuration = findViewById(R.id.linearLayoutDuration);
        linearLayoutBackground = findViewById(R.id.linearLayoutBackground);
        linearLayoutTimer = findViewById(R.id.linearLayoutTimer);

        LinearLayout linearLayoutBackground = findViewById(R.id.linearLayoutBackground);
        LinearLayout linearLayoutTimer = findViewById(R.id.linearLayoutTimer);

        backgroundOption1 = findViewById(R.id.backgroundOption1);
        backgroundOption2 = findViewById(R.id.backgroundOption2);
        backgroundOption3 = findViewById(R.id.backgroundOption3);
        selectBackgroundButton = findViewById(R.id.selectBackgroundButton);

        backgroundOption1.setOnClickListener(v -> selectBackground(1));
        backgroundOption2.setOnClickListener(v -> selectBackground(2));
        backgroundOption3.setOnClickListener(v -> selectBackground(3));

        selectBackgroundButton.setOnClickListener(v -> {
            if (selectedBackground != -1) {
                // Proceed to the timer session if background confirmed
                linearLayoutBackground.setVisibility(View.GONE);
                linearLayoutTimer.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(this, "Please select a background first.", Toast.LENGTH_SHORT).show();
            }
        });

        timerText = findViewById(R.id.timerText);
        sessionButton = findViewById(R.id.startTimerButton);

        selectDurationButton25 = findViewById(R.id.selectDurationButton25);
        selectDurationButton50 = findViewById(R.id.selectDurationButton50);


        // Set duration based on button clicked
        selectDurationButton25.setOnClickListener(v -> {
            sessionDuration = 1 * 60 * 1000; // 25 minutes in milliseconds
            breakDuration = 5 * 60 * 1000; // 5 minutes in milliseconds
            linearLayoutDuration.setVisibility(View.GONE);
            linearLayoutBackground.setVisibility(View.VISIBLE);
        });

        selectDurationButton50.setOnClickListener(v -> {
            sessionDuration = 50 * 60 * 1000; // 50 minutes in milliseconds
            breakDuration = 10 * 60 * 1000; // 10 minutes in milliseconds
            linearLayoutDuration.setVisibility(View.GONE);
            linearLayoutBackground.setVisibility(View.VISIBLE);
        });

        selectBackgroundButton.setOnClickListener(v -> {
            linearLayoutBackground.setVisibility(View.GONE);
            linearLayoutTimer.setVisibility(View.VISIBLE);
        });

        // Start or Stop Session based on button state
        sessionButton.setOnClickListener(v -> {
            if (isSessionRunning) {
                stopSession();
            } else {
                startSession();
            }
        });
    }
    private void selectBackground(int option) {
        resetBackgroundSelection();
        selectedBackground = option;

        switch (option) {
            case 1:
                backgroundOption1.setForeground(getDrawable(R.drawable.highlight_overlay));
                linearLayoutTimer.setBackgroundResource(R.drawable.photo1);
                break;
            case 2:
                backgroundOption2.setForeground(getDrawable(R.drawable.highlight_overlay));
                linearLayoutTimer.setBackgroundResource(R.drawable.photo2);
                break;
            case 3:
                backgroundOption3.setForeground(getDrawable(R.drawable.highlight_overlay));
                linearLayoutTimer.setBackgroundResource(R.drawable.photo3);
                break;
        }
    }

    // Removes any highlight from previously selected backgrounds
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
        Intent intent = new Intent(PomodoroActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
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
                    showBreakTimePopup();
                    breakTime.setVisibility(View.VISIBLE);
                    workingTime.setVisibility(View.GONE);
                    startTimer(breakDuration, false); // Start break timer
                } else {
                    startTimer(sessionDuration, true); // Restart work session
                }
            }
        }.start();
    }

    private void showBreakTimePopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Break Time!")
                .setMessage("Enjoy your break before the next session.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }
}

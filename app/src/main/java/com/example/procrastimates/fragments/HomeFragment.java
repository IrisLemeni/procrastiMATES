package com.example.procrastimates.fragments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.procrastimates.FocusChartAdapter;
import com.example.procrastimates.R;
import com.example.procrastimates.Task;
import com.example.procrastimates.activities.LoginActivity;
import com.example.procrastimates.activities.ProfileImageActivity;
import com.example.procrastimates.repositories.TaskRepository;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class HomeFragment extends Fragment {

    private TextView welcomeText;
    private ImageView userImage;
    private TextView quoteText, progressText;
    private BarChart barChart;
    private ProgressBar dailyProgressBar;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ImageButton logoutButtton;

    private CircularProgressIndicator focusScoreIndicator;
    private TextView focusScoreText, totalSessionsText, interruptionsText, timeOutsideText, bestFocusTimeText;
    private LineChart focusLineChart;
    private List<FocusChartAdapter.PomodoroSession> todaySessions = new ArrayList<>();
    private FirebaseStorage storage;
    private StorageReference storageReference;


    public HomeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        dailyProgressBar = view.findViewById(R.id.dailyProgressBar);
        progressText = view.findViewById(R.id.progressText);
        db = FirebaseFirestore.getInstance();
        fetchTaskProgress();

        welcomeText = view.findViewById(R.id.welcomeText);
        userImage = view.findViewById(R.id.userImage);
        logoutButtton = view.findViewById(R.id.logoutButton);
        quoteText = view.findViewById(R.id.quoteText);
        barChart = view.findViewById(R.id.barChart);
        barChart.setDrawBarShadow(false);
        barChart.setDrawValueAboveBar(true);
        fetchPomodoroSessions();

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        userImage.setOnClickListener(v -> openProfileImageActivity());

        focusScoreIndicator = view.findViewById(R.id.focusScoreIndicator);
        focusScoreText = view.findViewById(R.id.focusScoreText);
        totalSessionsText = view.findViewById(R.id.totalSessionsText);
        interruptionsText = view.findViewById(R.id.interruptionsText);
        timeOutsideText = view.findViewById(R.id.timeOutsideText);
        bestFocusTimeText = view.findViewById(R.id.bestFocusTimeText);
        focusLineChart = view.findViewById(R.id.focusLineChart);
        fetchTodaysFocusMetrics();


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

        logoutButtton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });


        return view;
    }

    private void openProfileImageActivity() {
        Intent intent = new Intent(getActivity(), ProfileImageActivity.class);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Actualizează imaginea de profil în caz că a fost modificată
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            loadUserData(currentUser.getUid());
        }
    }

    private void fetchTaskProgress() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            String userId = currentUser.getUid();

            // Calculează începutul și sfârșitul zilei curente
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            Timestamp startOfDay = new Timestamp(calendar.getTime());

            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            calendar.set(Calendar.MILLISECOND, 999);
            Timestamp endOfDay = new Timestamp(calendar.getTime());

            // Folosește TaskRepository
            TaskRepository.getInstance().getUserTasksForToday(userId, startOfDay, endOfDay, new TaskRepository.OnTaskActionListener() {
                @Override
                public void onSuccess(Object result) {
                    List<Task> tasks = (List<Task>) result;
                    Log.d("fetchTaskProgress", "Fetched " + tasks.size() + " tasks");
                    if (tasks.isEmpty()) {
                        Log.d("fetchTaskProgress", "No tasks found for today");
                    }
                    int totalTasks = tasks.size();
                    int completedTasks = (int) tasks.stream().filter(Task::isCompleted).count();
                    updateProgressBar(completedTasks, totalTasks);
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e("fetchTaskProgress", "Error fetching tasks: " + e.getMessage(), e);
                    progressText.setText("Error loading tasks");
                }
            });

        } else {
            progressText.setText("User not logged in");
        }
    }

    private void updateProgressBar(int completedTasks, int totalTasks) {
        if (totalTasks > 0) {
            // Calculează progresul ca procentaj
            int progress = (int) ((completedTasks / (float) totalTasks) * 100);
            dailyProgressBar.setProgress(progress);
            progressText.setText(completedTasks + "/" + totalTasks + " tasks completed");
        } else {
            progressText.setText("No tasks for today");
            dailyProgressBar.setProgress(0); // Setează progresul la 0 când nu sunt task-uri
        }
    }

    private void fetchPomodoroSessions() {
        db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        String userId = mAuth.getCurrentUser().getUid();

        // Obține luna curentă sub format "yyyy-MM" (ex: "2023-10")
        SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        String currentMonth = monthFormat.format(new Date());

        // Log pentru debugging
        Log.d("HomeFragment", "Fetching daily sessions for month: " + currentMonth);

        // Interoghează daily_sessions pentru luna curentă
        db.collection("daily_sessions")
                .document(userId)
                .collection("sessions_by_day")
                .whereGreaterThanOrEqualTo(FieldPath.documentId(), currentMonth + "-01")  // ex: "2023-10-01"
                .whereLessThanOrEqualTo(FieldPath.documentId(), currentMonth + "-31")      // ex: "2023-10-31"
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Use the adapter to update the chart
                        FocusChartAdapter.setupMonthlySessionsBarChart(barChart, task.getResult());
                    } else {
                        Log.w("HomeFragment", "Error getting daily sessions.", task.getException());
                        FocusChartAdapter.setupMonthlySessionsBarChart(barChart, null);
                    }
                });
    }

    private void fetchTodaysFocusMetrics() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        // Calculate start and end of today
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date startOfDay = calendar.getTime();

        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        Date endOfDay = calendar.getTime();

        // Add debug logging
        Log.d("HomeFragment", "Fetching focus metrics from " + startOfDay + " to " + endOfDay);

        // Query today's Pomodoro sessions
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("pomodoro_sessions")
                .whereEqualTo("userId", userId)
                .whereEqualTo("type", "work") // Only work sessions have focus metrics
                .whereGreaterThanOrEqualTo("timestamp", new Timestamp(startOfDay))
                .whereLessThanOrEqualTo("timestamp", new Timestamp(endOfDay))
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d("HomeFragment", "Retrieved " + queryDocumentSnapshots.size() + " focus sessions");

                    todaySessions.clear();

                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d("HomeFragment", "No focus sessions found for today");
                        updateFocusUI();
                        return;
                    }

                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        // Extract session data
                        Timestamp timestamp = document.getTimestamp("timestamp");
                        Long focusScore = document.getLong("focusScore");
                        Long interruptionCount = document.getLong("interruptionCount");
                        Long timeOutsideApp = document.getLong("timeOutsideApp");
                        Long duration = document.getLong("duration");

                        // Debug log for each document
                        Log.d("HomeFragment", "Processing document: " + document.getId() +
                                " timestamp: " + timestamp +
                                " focusScore: " + focusScore);

                        if (timestamp != null && focusScore != null &&
                                interruptionCount != null && timeOutsideApp != null && duration != null) {

                            FocusChartAdapter.PomodoroSession session = new FocusChartAdapter.PomodoroSession(
                                    timestamp.toDate(),
                                    focusScore.intValue(),
                                    interruptionCount.intValue(),
                                    timeOutsideApp.intValue(),
                                    duration.intValue()
                            );

                            todaySessions.add(session);
                            Log.d("HomeFragment", "Added session with focus score: " + focusScore);
                        } else {
                            Log.w("HomeFragment", "Skipping document with missing data: " + document.getId());
                        }
                    }

                    Log.d("HomeFragment", "Total sessions to display: " + todaySessions.size());
                    updateFocusUI();
                })
                .addOnFailureListener(e -> {
                    Log.e("HomeFragment", "Error fetching focus metrics", e);
                    todaySessions.clear();
                    updateFocusUI();
                });
    }

    private void updateFocusUI() {
        // First log the state
        Log.d("HomeFragment", "Updating UI with " + todaySessions.size() + " sessions");

        // Use the adapter to calculate and update UI components
        int averageFocusScore = FocusChartAdapter.calculateAverageFocusScore(todaySessions);
        int totalInterruptions = FocusChartAdapter.calculateTotalInterruptions(todaySessions);
        int totalTimeOutside = FocusChartAdapter.calculateTotalTimeOutside(todaySessions);
        String bestFocusTime = FocusChartAdapter.determineBestFocusTime(todaySessions);

        Log.d("HomeFragment", "Calculated metrics - average: " + averageFocusScore +
                ", interruptions: " + totalInterruptions);

        // Update focus score indicator
        focusScoreIndicator.setProgress(averageFocusScore);
        focusScoreText.setText(averageFocusScore + "%");

        // Update session stats
        int totalSessions = todaySessions.size();
        totalSessionsText.setText(totalSessions + " sessions completed");
        interruptionsText.setText(totalInterruptions + " interruptions");
        timeOutsideText.setText(totalTimeOutside + " min outside app");
        bestFocusTimeText.setText("Best focus time: " + bestFocusTime);

        // Use the adapter to update the focus line chart
        FocusChartAdapter.setupFocusLineChart(focusLineChart, todaySessions);

        // Force a redraw of the chart
        focusLineChart.invalidate();
    }


    private void loadUserData(String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Setează textul de bun venit
                        String username = documentSnapshot.getString("username");
                        if (username != null) {
                            welcomeText.setText("Bun venit, " + username + "!");
                        }

                        // Încarcă imaginea de profil
                        String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            Glide.with(HomeFragment.this)
                                    .load(profileImageUrl)
                                    .circleCrop()
                                    .into(userImage);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error loading user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}

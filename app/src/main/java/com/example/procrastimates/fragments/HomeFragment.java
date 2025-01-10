package com.example.procrastimates.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import com.example.procrastimates.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

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
    private TextView quoteText;
    private BarChart barChart;


    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        welcomeText = view.findViewById(R.id.welcomeText);
        userImage = view.findViewById(R.id.userImage);
        quoteText = view.findViewById(R.id.quoteText);
        barChart = view.findViewById(R.id.barChart);
        barChart.setDrawBarShadow(false);
        barChart.setDrawValueAboveBar(true);
        fetchPomodoroSessions();


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

        return view;
    }

    private int getDaysInCurrentMonth() {
        Calendar calendar = Calendar.getInstance();
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    private void fetchPomodoroSessions() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        String userId = mAuth.getCurrentUser().getUid();

        int daysInMonth = getDaysInCurrentMonth(); // Numărul exact de zile din luna curentă
        List<String> daysOfMonth = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        for (int i = 1; i <= daysInMonth; i++) {
            calendar.set(Calendar.DAY_OF_MONTH, i);
            daysOfMonth.add(sdf.format(calendar.getTime()));
        }
        // Obține sesiunile Pomodoro pentru utilizatorul curent
        db.collection("pomodoro_sessions")
                .whereEqualTo("userId", userId)
                .whereEqualTo("type", "work")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot result = task.getResult();
                        if (result != null) {
                            updateBarChart(result);
                        }
                    } else {
                        Log.w("HomeFragment", "Error getting documents.", task.getException());
                    }
                });
    }

    private void updateBarChart(QuerySnapshot sessions) {
        ArrayList<BarEntry> entries = new ArrayList<>();

        // Calendar pentru a completa toate zilele
        Calendar calendar = Calendar.getInstance();
        int maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Inițializează zilele cu 0 sesiuni
        for (int i = 1; i <= maxDays; i++) {
            entries.add(new BarEntry(i, 0)); // Fiecare zi începe cu 0 sesiuni
        }

        // Parcurge sesiunile din Firestore
        for (QueryDocumentSnapshot document : sessions) {
            Date timestamp = document.getDate("timestamp");
            if (timestamp != null) {
                calendar.setTime(timestamp);
                int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

                // Crește numărul de sesiuni pentru ziua respectivă
                BarEntry entry = entries.get(dayOfMonth - 1);
                entry.setY(entry.getY() + 1);
            }
        }

        // Creează setul de date
        BarDataSet dataSet = new BarDataSet(entries, "Pomodoro Sessions");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.BLACK);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.9f);

        // Setează datele în grafic
        barChart.setData(data);

        // Configurare descriere
        String monthYear = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(new Date());
        barChart.getDescription().setEnabled(true);
        barChart.getDescription().setText("Pomodoro Sessions - " + monthYear);
        barChart.getDescription().setTextSize(14f);
        barChart.getDescription().setTextColor(Color.BLACK);
        barChart.getDescription().setPosition(500f, 20f); // Ajustează poziția descrierii

        // Centrare pe ziua curentă
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
        barChart.moveViewToX(currentDay - 1);

        // Activare scroll orizontal
        barChart.setDragEnabled(true);
        barChart.setVisibleXRangeMaximum(7);

        configureBarChart();
        barChart.invalidate();
    }

    private void configureBarChart() {
        barChart.setDrawGridBackground(false);
        barChart.setDragEnabled(true);
        barChart.setScaleXEnabled(true);
        barChart.setScaleYEnabled(false);
        barChart.setPinchZoom(false);
        barChart.setVisibleXRangeMaximum(7);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(7);
        xAxis.setTextSize(14f); // Dimensiunea textului axei X
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter((value, axis) -> {
            int day = (int) value;
            return (day > 0 && day <= 31) ? String.valueOf(day) : "";
        });

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setGranularity(1f);
        leftAxis.setTextSize(14f); // Dimensiunea textului axei Y
        leftAxis.setDrawGridLines(true);

        barChart.getAxisRight().setEnabled(false);
        barChart.setFitBars(true);
        barChart.animateY(1000);

        // Mută vizualizarea pe ziua curentă
        Calendar calendar = Calendar.getInstance();
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
        barChart.moveViewToX(currentDay - 4); // Centrare pe ziua curentă
    }




    private void loadUserData(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("username");
                        if (username != null) {
                            welcomeText.setText("Hello, " + username);
                        } else {
                            welcomeText.setText("Hello, User!");
                        }
                    } else {
                        Toast.makeText(getContext(), "User data not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error loading user data", Toast.LENGTH_SHORT).show();
                });
    }
}

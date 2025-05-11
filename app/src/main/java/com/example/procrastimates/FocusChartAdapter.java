package com.example.procrastimates;

import android.graphics.Color;
import android.util.Pair;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FocusChartAdapter {

    public static class PomodoroSession {
        private Date timestamp;
        private int focusScore;
        private int interruptionCount;
        private int timeOutsideApp;
        private int duration; // in minutes

        public PomodoroSession(Date timestamp, int focusScore, int interruptionCount, int timeOutsideApp, int duration) {
            this.timestamp = timestamp;
            this.focusScore = focusScore;
            this.interruptionCount = interruptionCount;
            this.timeOutsideApp = timeOutsideApp;
            this.duration = duration;
        }

        public Date getTimestamp() {
            return timestamp;
        }

        public int getFocusScore() {
            return focusScore;
        }

        public int getInterruptionCount() {
            return interruptionCount;
        }

        public int getTimeOutsideApp() {
            return timeOutsideApp;
        }

        public int getDuration() {
            return duration;
        }
    }

    public static void setupFocusLineChart(LineChart lineChart, List<PomodoroSession> sessions) {
        // Configure chart appearance first
        configureFocusLineChart(lineChart);

        if (sessions == null || sessions.isEmpty()) {
            // Set empty chart
            LineData lineData = new LineData();
            lineChart.setData(lineData);
            lineChart.invalidate();
            return;
        }

        ArrayList<Entry> entries = new ArrayList<>();

        // Convert sessions to chart entries
        for (PomodoroSession session : sessions) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(session.getTimestamp());

            // X-axis: hour of day with decimal for minutes (e.g., 14.5 for 2:30 PM)
            float hourOfDay = calendar.get(Calendar.HOUR_OF_DAY) +
                    calendar.get(Calendar.MINUTE) / 60.0f;

            entries.add(new Entry(hourOfDay, session.getFocusScore()));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Focus Score");
        dataSet.setColor(Color.parseColor("#4CAF50"));
        dataSet.setCircleColor(Color.parseColor("#4CAF50"));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setValueTextSize(10f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        // Add gradient under the line
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#804CAF50")); // Semi-transparent green

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        // Add animation
        lineChart.animateX(1000);
        lineChart.invalidate(); // Refresh chart
    }

    private static void configureFocusLineChart(LineChart lineChart) {
        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.setDrawGridBackground(false);

        // Configure X-axis (hours)
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f); // 1 hour interval
        xAxis.setLabelCount(12);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter((value, axis) -> {
            // Format hour values (0-23) to AM/PM format
            int hour = (int) value;
            if (hour < 0 || hour > 23) return "";

            if (hour == 0) return "12 AM";
            if (hour < 12) return hour + " AM";
            if (hour == 12) return "12 PM";
            return (hour - 12) + " PM";
        });

        // Configure Y-axis (focus score percentage)
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(100f);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGranularity(20f);
        leftAxis.setValueFormatter((value, axis) -> (int) value + "%");

        lineChart.getAxisRight().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
    }

    public static void setupMonthlySessionsBarChart(BarChart barChart, com.google.firebase.firestore.QuerySnapshot dailySessions) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        int maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // Inițializează toate zilele cu 0 sesiuni
        for (int i = 1; i <= maxDays; i++) {
            entries.add(new BarEntry(i, 0));
        }

        // Dacă există înregistrări daily_sessions, actualizează valorile
        if (dailySessions != null && !dailySessions.isEmpty()) {
            for (com.google.firebase.firestore.QueryDocumentSnapshot document : dailySessions) {
                try {
                    // Extrage ziua din ID-ul documentului (format "yyyy-MM-dd")
                    String documentId = document.getId();
                    Date date = dayFormat.parse(documentId);
                    calendar.setTime(date);

                    int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
                    long sessionCount = document.getLong("sessionCount");

                    // Actualizează intrarea pentru ziua respectivă
                    if (dayOfMonth >= 1 && dayOfMonth <= maxDays) {
                        entries.get(dayOfMonth - 1).setY(sessionCount);
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing date from document ID");
                }
            }
        }

        // Configure bar chart appearance and data
        configureMonthlyBarChart(barChart, entries);
    }

    private static void configureMonthlyBarChart(BarChart barChart, ArrayList<BarEntry> entries) {
        // Crează setul de date
        BarDataSet dataSet = new BarDataSet(entries, "Pomodoro Sessions");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.BLACK);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.9f);
        barChart.setData(barData);

        // Configurare grafic
        String monthYear = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(new Date());

        Description description = new Description();
        description.setText(monthYear);
        description.setTextSize(20f);
        description.setTextColor(Color.BLACK);
        description.setPosition(250f, 50f);

        barChart.setDescription(description);


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
        xAxis.setTextSize(14f);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter((value, axis) -> {
            int day = (int) value;
            return (day > 0 && day <= 31) ? String.valueOf(day) : "";
        });

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setGranularity(1f);
        leftAxis.setTextSize(14f);
        leftAxis.setDrawGridLines(true);

        barChart.getAxisRight().setEnabled(false);
        barChart.setFitBars(true);

        // Centrare pe ziua curentă
        int currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        barChart.moveViewToX(currentDay - 4);

        barChart.animateY(1000);
        barChart.invalidate();
    }

    public static int calculateAverageFocusScore(List<PomodoroSession> sessions) {
        if (sessions == null || sessions.isEmpty()) return 0;

        int totalFocusScore = 0;
        int totalDuration = 0;

        // Weight focus score by session duration
        for (PomodoroSession session : sessions) {
            totalFocusScore += session.getFocusScore() * session.getDuration();
            totalDuration += session.getDuration();
        }

        return totalDuration > 0 ? totalFocusScore / totalDuration : 0;
    }

    public static int calculateTotalInterruptions(List<PomodoroSession> sessions) {
        if (sessions == null || sessions.isEmpty()) return 0;

        int total = 0;
        for (PomodoroSession session : sessions) {
            total += session.getInterruptionCount();
        }
        return total;
    }

    public static int calculateTotalTimeOutside(List<PomodoroSession> sessions) {
        if (sessions == null || sessions.isEmpty()) return 0;

        int total = 0;
        for (PomodoroSession session : sessions) {
            total += session.getTimeOutsideApp();
        }
        return total / 10000;
    }

    public static String determineBestFocusTime(List<PomodoroSession> sessions) {
        if (sessions == null || sessions.isEmpty()) return "No data available";

        // Group sessions by hour of day
        Map<Integer, List<PomodoroSession>> sessionsByHour = new HashMap<>();

        for (PomodoroSession session : sessions) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(session.getTimestamp());
            int hour = calendar.get(Calendar.HOUR_OF_DAY);

            if (!sessionsByHour.containsKey(hour)) {
                sessionsByHour.put(hour, new ArrayList<>());
            }

            sessionsByHour.get(hour).add(session);
        }

        // Find hour with highest average focus
        int bestHour = -1;
        float bestAverage = -1;

        for (Map.Entry<Integer, List<PomodoroSession>> entry : sessionsByHour.entrySet()) {
            int hour = entry.getKey();
            List<PomodoroSession> hourSessions = entry.getValue();

            int totalScore = 0;
            int totalDuration = 0;

            for (PomodoroSession session : hourSessions) {
                totalScore += session.getFocusScore() * session.getDuration();
                totalDuration += session.getDuration();
            }

            float averageScore = totalDuration > 0 ? (float) totalScore / totalDuration : 0;

            if (averageScore > bestAverage) {
                bestAverage = averageScore;
                bestHour = hour;
            }
        }

        if (bestHour == -1) return "No data available";

        // Format hour range (e.g., "10:00 AM - 11:00 AM")
        SimpleDateFormat hourFormat = new SimpleDateFormat("h:00 a", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, bestHour);
        calendar.set(Calendar.MINUTE, 0);

        String startTime = hourFormat.format(calendar.getTime());

        calendar.add(Calendar.HOUR_OF_DAY, 1);
        String endTime = hourFormat.format(calendar.getTime());

        return startTime + " - " + endTime;
    }

    public static Map<String, Integer> findProductivityPatterns(List<PomodoroSession> sessions) {
        Map<String, Integer> patterns = new HashMap<>();

        if (sessions == null || sessions.isEmpty()) {
            return patterns;
        }

        // Group into morning, afternoon, evening
        List<PomodoroSession> morning = new ArrayList<>();   // 5:00 - 11:59
        List<PomodoroSession> afternoon = new ArrayList<>(); // 12:00 - 16:59
        List<PomodoroSession> evening = new ArrayList<>();   // 17:00 - 23:59

        for (PomodoroSession session : sessions) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(session.getTimestamp());
            int hour = cal.get(Calendar.HOUR_OF_DAY);

            if (hour >= 5 && hour < 12) {
                morning.add(session);
            } else if (hour >= 12 && hour < 17) {
                afternoon.add(session);
            } else if (hour >= 17 && hour < 24) {
                evening.add(session);
            }
        }

        // Calculate average focus score for each period
        if (!morning.isEmpty()) {
            patterns.put("Morning", calculateAverageFocusScore(morning));
        }

        if (!afternoon.isEmpty()) {
            patterns.put("Afternoon", calculateAverageFocusScore(afternoon));
        }

        if (!evening.isEmpty()) {
            patterns.put("Evening", calculateAverageFocusScore(evening));
        }

        return patterns;
    }
}
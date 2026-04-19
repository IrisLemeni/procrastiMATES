package com.example.procrastimates.adapters;

import android.graphics.Color;

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
import java.util.Collections;
import java.util.Comparator;
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
        private int duration;

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
        // Configure chart appearance
        configureFocusLineChart(lineChart);

        ArrayList<Entry> entries = new ArrayList<>();

        if (sessions != null && !sessions.isEmpty()) {
            // Create entries for each session
            for (PomodoroSession session : sessions) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(session.getTimestamp());
                float hourOfDay = cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60f;
                entries.add(new Entry(hourOfDay, session.getFocusScore()));
            }
            // Sort by X value
            Collections.sort(entries, Comparator.comparing(Entry::getX));
            // Add base point at start if needed
            if (entries.get(0).getX() > 0f) {
                entries.add(0, new Entry(0f, 0f));
            }
            // Add base point at end if needed
            if (entries.get(entries.size() - 1).getX() < 24f) {
                entries.add(new Entry(24f, 0f));
            }
        } else {
            // No sessions: flat line at 0
            entries.add(new Entry(0f, 0f));
            entries.add(new Entry(24f, 0f));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Focus Score");
        dataSet.setColor(Color.parseColor("#4CAF50"));
        dataSet.setCircleColor(Color.parseColor("#4CAF50"));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setValueTextSize(10f);
        dataSet.setDrawValues(false);
        // Straight lines between points
        dataSet.setMode(LineDataSet.Mode.LINEAR);
        // Optional fill
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#804CAF50"));

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.animateX(1000);
        lineChart.invalidate();
    }

    private static void configureFocusLineChart(LineChart lineChart) {
        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.setDrawGridBackground(false);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(4f);
        xAxis.setLabelCount(6, true);
        xAxis.setDrawGridLines(false);
        xAxis.setAxisMinimum(0f);
        xAxis.setAxisMaximum(24f);
        xAxis.setAvoidFirstLastClipping(true);
        xAxis.setValueFormatter((value, axis) -> {
            int hour = (int) value;
            if (hour < 0 || hour > 23) return "";
            if (hour == 0) return "12 AM";
            if (hour < 12) return hour + " AM";
            if (hour == 12) return "12 PM";
            return (hour - 12) + " PM";
        });

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

        // Initialize all days with zero sessions
        for (int i = 1; i <= maxDays; i++) {
            entries.add(new BarEntry(i, 0));
        }

        if (dailySessions != null && !dailySessions.isEmpty()) {
            for (com.google.firebase.firestore.QueryDocumentSnapshot doc : dailySessions) {
                try {
                    String id = doc.getId();
                    Date date = dayFormat.parse(id);
                    calendar.setTime(date);
                    int day = calendar.get(Calendar.DAY_OF_MONTH);
                    long count = doc.getLong("sessionCount");
                    if (day >= 1 && day <= maxDays) {
                        entries.get(day - 1).setY(count);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        configureMonthlyBarChart(barChart, entries);
    }

    private static void configureMonthlyBarChart(BarChart barChart, ArrayList<BarEntry> entries) {
        BarDataSet dataSet = new BarDataSet(entries, "Pomodoro Sessions");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(12f);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.9f);
        barChart.setData(barData);

        String monthYear = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(new Date());
        Description desc = new Description();
        desc.setText(monthYear);
        desc.setTextSize(20f);
        desc.setPosition(250f, 50f);
        barChart.setDescription(desc);

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
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter((value, axis) -> {
            int day = (int) value;
            return (day > 0 && day <= entries.size()) ? String.valueOf(day) : "";
        });

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setGranularity(1f);
        leftAxis.setDrawGridLines(true);

        barChart.getAxisRight().setEnabled(false);
        barChart.setFitBars(true);

        int currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        barChart.moveViewToX(currentDay - 4);
        barChart.animateY(1000);
        barChart.invalidate();
    }

    public static int calculateAverageFocusScore(List<PomodoroSession> sessions) {
        if (sessions == null || sessions.isEmpty()) return 0;
        int totalScore = 0, totalDur = 0;
        for (PomodoroSession s : sessions) {
            totalScore += s.getFocusScore() * s.getDuration();
            totalDur += s.getDuration();
        }
        return totalDur > 0 ? totalScore / totalDur : 0;
    }

    public static int calculateTotalInterruptions(List<PomodoroSession> sessions) {
        if (sessions == null || sessions.isEmpty()) return 0;
        int total = 0;
        for (PomodoroSession s : sessions) total += s.getInterruptionCount();
        return total;
    }

    public static int calculateTotalTimeOutside(List<PomodoroSession> sessions) {
        if (sessions == null || sessions.isEmpty()) return 0;
        int total = 0;
        for (PomodoroSession s : sessions) total += s.getTimeOutsideApp();
        return total / 10000; // ajustat dacă e nevoie
    }

    public static String determineBestFocusTime(List<PomodoroSession> sessions) {
        if (sessions == null || sessions.isEmpty()) return "No data available";
        Map<Integer, List<PomodoroSession>> byHour = new HashMap<>();
        for (PomodoroSession s : sessions) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(s.getTimestamp());
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            byHour.computeIfAbsent(hour, k -> new ArrayList<>()).add(s);
        }
        int bestHour = -1; float bestAvg = -1;
        for (Map.Entry<Integer, List<PomodoroSession>> e : byHour.entrySet()) {
            int hour = e.getKey(); int sum=0, dur=0;
            for (PomodoroSession s : e.getValue()) {
                sum += s.getFocusScore() * s.getDuration();
                dur += s.getDuration();
            }
            float avg = dur>0?(float)sum/dur:0;
            if (avg>bestAvg) { bestAvg=avg; bestHour=hour; }
        }
        if (bestHour<0) return "No data available";
        SimpleDateFormat fmt = new SimpleDateFormat("h:00 a", Locale.getDefault());
        Calendar cal = Calendar.getInstance(); cal.set(Calendar.HOUR_OF_DAY, bestHour); cal.set(Calendar.MINUTE, 0);
        String start = fmt.format(cal.getTime()); cal.add(Calendar.HOUR_OF_DAY, 1);
        String end = fmt.format(cal.getTime());
        return start + " - " + end;
    }

    public static Map<String, Integer> findProductivityPatterns(List<PomodoroSession> sessions) {
        Map<String, Integer> patterns = new HashMap<>();
        if (sessions == null || sessions.isEmpty()) return patterns;
        List<PomodoroSession> morning = new ArrayList<>();
        List<PomodoroSession> afternoon = new ArrayList<>();
        List<PomodoroSession> evening = new ArrayList<>();
        for (PomodoroSession s : sessions) {
            Calendar cal = Calendar.getInstance(); cal.setTime(s.getTimestamp());
            int h = cal.get(Calendar.HOUR_OF_DAY);
            if (h>=5 && h<12) morning.add(s);
            else if (h>=12 && h<17) afternoon.add(s);
            else if (h>=17 && h<24) evening.add(s);
        }
        if (!morning.isEmpty()) patterns.put("Morning", calculateAverageFocusScore(morning));
        if (!afternoon.isEmpty()) patterns.put("Afternoon", calculateAverageFocusScore(afternoon));
        if (!evening.isEmpty()) patterns.put("Evening", calculateAverageFocusScore(evening));
        return patterns;
    }
}

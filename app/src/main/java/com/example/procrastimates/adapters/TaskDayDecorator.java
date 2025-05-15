package com.example.procrastimates.adapters;
import android.text.style.ForegroundColorSpan;

import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewFacade;


import java.util.HashSet;

public class TaskDayDecorator implements DayViewDecorator {

    private final int color;
    private final HashSet<CalendarDay> dates;

    public TaskDayDecorator(int color, HashSet<CalendarDay> dates) {
        this.color = color;
        this.dates = dates;
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return dates.contains(day); // Decorează doar zilele care au task-uri
    }

    @Override
    public void decorate(DayViewFacade view) {
        view.addSpan(new ForegroundColorSpan(color)); // Schimbă culoarea textului
    }
}


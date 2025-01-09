package com.example.procrastimates;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.procrastimates.fragments.TasksFragment;

public class ViewPagerAdapter extends FragmentStateAdapter {

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new TasksFragment(); // Today’s Tasks tab
            case 1:
                return new CalendarFragment(); // Calendar tab
            default:
                return new TasksFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2; // Avem două taburi: Today's Tasks și Calendar
    }
}


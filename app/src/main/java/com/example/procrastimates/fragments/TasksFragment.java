package com.example.procrastimates.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.procrastimates.AddTaskBottomSheet;
import com.example.procrastimates.CalendarFragment;
import com.example.procrastimates.EditTaskBottomSheet;
import com.example.procrastimates.Priority;
import com.example.procrastimates.R;
import com.example.procrastimates.RecyclerItemTouchHelper;
import com.example.procrastimates.Task;
import com.example.procrastimates.TaskAdapter;
import com.example.procrastimates.TaskViewModel;
import com.example.procrastimates.TodayTasksFragment;
import com.example.procrastimates.ViewPagerAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TasksFragment extends Fragment {

    private RecyclerView tasksRecyclerView;
    private TaskAdapter taskAdapter;
    private TaskViewModel taskViewModel;
    private FloatingActionButton fabAddTask, fabSortTasks;

    public TasksFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tasks, container, false);

        TabLayout tabLayout = view.findViewById(R.id.tabLayout);
        ViewPager2 viewPager = view.findViewById(R.id.viewPager);

        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentStateAdapter adapter = new FragmentStateAdapter(fragmentManager, getLifecycle()) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                if (position == 0) {
                    return new TodayTasksFragment(); // Tab-ul "Today's Tasks"
                } else {
                    return new CalendarFragment();  // Tab-ul "Calendar"
                }
            }

            @Override
            public int getItemCount() {
                return 2; // Două tab-uri
            }
        };

        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("Today's Tasks");
            } else {
                tab.setText("Calendar");
            }
        }).attach();

        return view;
    }

}

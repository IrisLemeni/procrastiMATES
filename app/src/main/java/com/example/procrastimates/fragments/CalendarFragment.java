package com.example.procrastimates.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.procrastimates.components.AddTaskBottomSheet;
import com.example.procrastimates.adapters.CalendarTaskAdapter;
import com.example.procrastimates.components.EditTaskBottomSheet;
import com.example.procrastimates.R;
import com.example.procrastimates.models.Task;
import com.example.procrastimates.adapters.TaskDayDecorator;
import com.example.procrastimates.utils.TaskViewModel;
import com.example.procrastimates.repositories.TaskRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.CalendarMode;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class CalendarFragment extends Fragment {

    private MaterialCalendarView calendarView;
    private TaskViewModel taskViewModel;
    private MaterialButton toggleViewButton;
    private TextView calendarTitle;
    private TextView tasksTitle;
    private TextView taskCount;
    private LinearLayout emptyStateLayout;
    private RecyclerView tasksRecyclerView;
    private CalendarTaskAdapter adapter;
    private boolean isMonthView = false;

    String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

    public CalendarFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        initializeViews(view);
        setupCalendar();
        setupRecyclerView();
        setupObservers();
        setupListeners(view);

        CalendarDay today = CalendarDay.today();
        calendarView.setSelectedDate(today);
        getTasksForDay(today);

        return view;
    }

    private void initializeViews(View view) {
        calendarView = view.findViewById(R.id.calendarView);
        toggleViewButton = view.findViewById(R.id.toggleViewButton);
        calendarTitle = view.findViewById(R.id.calendarTitle);
        tasksTitle = view.findViewById(R.id.tasksTitle);
        taskCount = view.findViewById(R.id.taskCount);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        tasksRecyclerView = view.findViewById(R.id.tasksRecyclerView);
    }

    private void setupCalendar() {
        calendarView.setDateTextAppearance(R.style.CalendarDateText);
        calendarView.setWeekDayTextAppearance(R.style.CalendarWeekDayText);
        calendarView.setHeaderTextAppearance(R.style.CalendarHeaderText);

        // Start with week view
        calendarView.state().edit()
                .setCalendarDisplayMode(CalendarMode.WEEKS)
                .commit();

        taskViewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);
    }

    private void setupRecyclerView() {
        adapter = new CalendarTaskAdapter(new ArrayList<>());
        tasksRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        tasksRecyclerView.setAdapter(adapter);
    }

    private void setupObservers() {
        taskViewModel.getTasksLiveData().observe(getViewLifecycleOwner(), tasks -> {
            if (tasks != null) {
                CalendarDay selectedDay = calendarView.getSelectedDate();
                if (selectedDay != null) {
                    List<Task> tasksForSelectedDay = filterTasksForDay(tasks, selectedDay);
                    showTasksForDay(tasksForSelectedDay);
                    updateTaskCount(tasksForSelectedDay.size());
                    updateTasksTitle(selectedDay);
                }
            }
        });
    }

    private void setupListeners(View view) {
        // Toggle view button
        toggleViewButton.setOnClickListener(v -> toggleCalendarView());

        // Calendar date selection
        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            if (selected) {
                getTasksForDay(date);
            }
        });

        // Add task button
        ExtendedFloatingActionButton addTaskButton = view.findViewById(R.id.addTaskButton);

        addTaskButton.setOnClickListener(v -> {
            CalendarDay selectedDay = calendarView.getSelectedDate();
            if (selectedDay != null) {
                showAddTaskBottomSheet(selectedDay);
            } else {
                Toast.makeText(getContext(), "Select a day first!", Toast.LENGTH_SHORT).show();
            }
        });

        // Task actions
        adapter.setOnTaskActionListener(new CalendarTaskAdapter.OnTaskActionListener() {
            @Override
            public void onTaskEdit(Task task) {
                showEditTaskBottomSheet(task);
            }

            @Override
            public void onTaskDelete(Task task) {
                deleteTask(task);
            }

            @Override
            public void onTaskComplete(Task task, boolean isCompleted) {
                // No action needed for calendar view
            }

        });
    }

    private void toggleCalendarView() {
        isMonthView = !isMonthView;
        CalendarDay selectedDay = calendarView.getSelectedDate();

        if (isMonthView) {
            calendarView.state().edit()
                    .setCalendarDisplayMode(CalendarMode.MONTHS)
                    .commit();
            toggleViewButton.setIconResource(R.drawable.ic_calendar_view_week);

            // Load tasks for the entire month
            if (selectedDay != null) {
                getTasksForMonth(selectedDay);
            }
        } else {
            calendarView.state().edit()
                    .setCalendarDisplayMode(CalendarMode.WEEKS)
                    .commit();
            toggleViewButton.setIconResource(R.drawable.ic_calendar_view_month);

            // Load tasks for the week
            if (selectedDay != null) {
                getTasksForWeek(selectedDay);
            }
        }

        // Update tasks for the selected day
        if (selectedDay != null) {
            new android.os.Handler().postDelayed(() -> {
                getTasksForDay(selectedDay);
            }, 100);
        }
    }

    private void updateTaskCount(int count) {
        taskCount.setText(String.valueOf(count));
        taskCount.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
    }

    private void updateTasksTitle(CalendarDay selectedDay) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMM d", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        calendar.set(selectedDay.getYear(), selectedDay.getMonth() - 1, selectedDay.getDay());

        CalendarDay today = CalendarDay.today();
        if (selectedDay.equals(today)) {
            tasksTitle.setText("Tasks for Today");
        } else {
            tasksTitle.setText("Tasks for " + dateFormat.format(calendar.getTime()));
        }
    }

    private List<Task> filterTasksForDay(List<Task> allTasks, CalendarDay day) {
        long startOfDayMillis = getStartOfDayInMillis(day);
        long endOfDayMillis = getEndOfDayInMillis(day);

        List<Task> filteredTasks = new ArrayList<>();
        for (Task task : allTasks) {
            if (task.getDueDate() != null) {
                long taskTimeMillis = task.getDueDate().getSeconds() * 1000;
                if (taskTimeMillis >= startOfDayMillis && taskTimeMillis <= endOfDayMillis) {
                    filteredTasks.add(task);
                }
            }
        }
        return filteredTasks;
    }

    private void getTasksForWeek(CalendarDay selectedDay) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(selectedDay.getYear(), selectedDay.getMonth() - 1, selectedDay.getDay());
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        Date startOfWeek = calendar.getTime();

        calendar.add(Calendar.DAY_OF_YEAR, 6);
        Date endOfWeek = calendar.getTime();

        taskViewModel.loadTasksForDateRange(currentUserId, startOfWeek, endOfWeek);
        loadAllTasksForDecorators();
    }

    private void getTasksForMonth(CalendarDay selectedDay) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(selectedDay.getYear(), selectedDay.getMonth() - 1, 1);
        Date startOfMonth = calendar.getTime();

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date endOfMonth = calendar.getTime();

        taskViewModel.loadTasksForDateRange(currentUserId, startOfMonth, endOfMonth);
        loadAllTasksForDecorators();
    }

    private void getTasksForDay(CalendarDay selectedDay) {
        Date startOfDay = new Date(getStartOfDayInMillis(selectedDay));
        Date endOfDay = new Date(getEndOfDayInMillis(selectedDay));

        taskViewModel.loadTasksForDateRange(currentUserId, startOfDay, endOfDay);
        loadAllTasksForDecorators();
    }

    private void loadAllTasksForDecorators() {
        taskViewModel.loadAllTasks(currentUserId, new TaskRepository.OnTaskActionListener() {
            @Override
            public void onSuccess(Object result) {
                List<Task> allTasks = (List<Task>) result;
                updateDayDecorators(allTasks, currentUserId);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("CalendarFragment", "Error getting all tasks for decorators", e);
            }
        });
    }

    private void showTasksForDay(List<Task> tasksForDay) {
        if (tasksForDay.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            tasksRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            tasksRecyclerView.setVisibility(View.VISIBLE);
            adapter.updateTasks(tasksForDay);
        }
    }

    private long getStartOfDayInMillis(CalendarDay day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(day.getYear(), day.getMonth() - 1, day.getDay(), 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private long getEndOfDayInMillis(CalendarDay day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(day.getYear(), day.getMonth() - 1, day.getDay(), 23, 59, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }

    private void showEditTaskBottomSheet(Task task) {
        EditTaskBottomSheet bottomSheet = new EditTaskBottomSheet();
        bottomSheet.setTask(task);
        bottomSheet.setOnTaskUpdatedListener(updatedTask -> {
            if (updatedTask != null) {
                taskViewModel.updateTask(updatedTask.getTaskId(), updatedTask);
                Toast.makeText(getContext(), "Task updated", Toast.LENGTH_SHORT).show();

                CalendarDay selectedDay = calendarView.getSelectedDate();
                if (selectedDay != null) {
                    getTasksForDay(selectedDay);
                }
            } else {
                Toast.makeText(getContext(), "Failed to update task", Toast.LENGTH_SHORT).show();
            }
        });
        bottomSheet.show(getChildFragmentManager(), "EditTaskBottomSheet");
    }

    private void showAddTaskBottomSheet(CalendarDay selectedDay) {
        AddTaskBottomSheet addTaskBottomSheet = new AddTaskBottomSheet();

        addTaskBottomSheet.setOnTaskAddedListener(newTask -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(selectedDay.getYear(), selectedDay.getMonth() - 1, selectedDay.getDay());
            newTask.setDueDate(new Timestamp(calendar.getTime()));

            taskViewModel.addTask(newTask, currentUserId);
            Toast.makeText(getContext(), "Task added", Toast.LENGTH_SHORT).show();
            getTasksForDay(selectedDay);
        });

        addTaskBottomSheet.show(getChildFragmentManager(), "AddTaskBottomSheet");
    }

    private void deleteTask(Task task) {
        taskViewModel.deleteTask(task);
        Toast.makeText(getContext(), "Task deleted", Toast.LENGTH_SHORT).show();
        getTasksForDay(calendarView.getSelectedDate());
    }

    private void updateDayDecorators(List<Task> tasks, String currentUserId) {
        HashSet<CalendarDay> daysWithTasks = new HashSet<>();
        HashSet<CalendarDay> daysWithCompletedTasks = new HashSet<>();

        for (Task task : tasks) {
            if (task.getUserId().equals(currentUserId) && task.getDueDate() != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(task.getDueDate().getSeconds() * 1000);
                CalendarDay day = CalendarDay.from(
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH) + 1,
                        calendar.get(Calendar.DAY_OF_MONTH)
                );

                daysWithTasks.add(day);

                if (task.isCompleted()) {
                    daysWithCompletedTasks.add(day);
                }
            }
        }

        // Clear old decorators and apply new ones
        calendarView.removeDecorators();

        // Add decorators for days with tasks
        if (!daysWithTasks.isEmpty()) {
            calendarView.addDecorator(new TaskDayDecorator(
                    ContextCompat.getColor(requireContext(), R.color.primary_color),
                    daysWithTasks
            ));
        }

        // Add decorators for days with completed tasks
        if (!daysWithCompletedTasks.isEmpty()) {
            calendarView.addDecorator(new TaskDayDecorator(
                    ContextCompat.getColor(requireContext(), R.color.success_color),
                    daysWithCompletedTasks
            ));
        }
    }
}
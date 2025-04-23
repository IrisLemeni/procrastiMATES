package com.example.procrastimates.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.procrastimates.AddTaskBottomSheet;
import com.example.procrastimates.CalendarTaskAdapter;
import com.example.procrastimates.EditTaskBottomSheet;
import com.example.procrastimates.R;
import com.example.procrastimates.Task;
import com.example.procrastimates.TaskDayDecorator;
import com.example.procrastimates.TaskViewModel;
import com.example.procrastimates.repositories.TaskRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

public class CalendarFragment extends Fragment {

    private MaterialCalendarView calendarView;
    private TaskViewModel taskViewModel;
    String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();


    public CalendarFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        calendarView = view.findViewById(R.id.calendarView);
        calendarView.setDateTextAppearance(R.style.CalendarDateText);

        CalendarDay today = CalendarDay.today();
        calendarView.setSelectedDate(today);
        taskViewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);

        getTasksForWeek(today);

        taskViewModel.getTasksLiveData().observe(getViewLifecycleOwner(), tasks -> {
            if (tasks != null) {
                // Filter tasks for the selected day
                CalendarDay selectedDay = calendarView.getSelectedDate();
                if (selectedDay != null) {
                    List<Task> tasksForSelectedDay = filterTasksForDay(tasks, selectedDay);
                    showTasksForDay(tasksForSelectedDay);
                }
            }
        });

        // Ascultă modificările de dată
        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            if (selected) {
                getTasksForDay(date);
            }
        });

        getTasksForDay(today);

        RecyclerView recyclerView = view.findViewById(R.id.tasksRecyclerView);
        if (recyclerView == null) {
            Log.e("CalendarFragment", "RecyclerView is not found!");
        } else {
            CalendarTaskAdapter adapter = new CalendarTaskAdapter(new ArrayList<>());
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerView.setAdapter(adapter);
        }

        view.findViewById(R.id.addTaskButton).setOnClickListener(v -> {
            CalendarDay selectedDay = calendarView.getSelectedDate();
            if (selectedDay != null) {
                showAddTaskBottomSheet(selectedDay);
            } else {
                Toast.makeText(getContext(), "Select a day first!", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
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

    // Refactored getTasksForWeek in CalendarFragment
    private void getTasksForWeek(CalendarDay selectedDay) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(selectedDay.getYear(), selectedDay.getMonth() - 1, selectedDay.getDay());
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        Date startOfWeek = calendar.getTime();

        calendar.add(Calendar.DAY_OF_YEAR, 6);
        Date endOfWeek = calendar.getTime();

        // Use TaskViewModel to get tasks
        taskViewModel.loadTasksForDateRange(currentUserId, startOfWeek, endOfWeek);

        // The observer on taskViewModel.getTasksLiveData() will handle displaying the tasks
    }

    // Refactored getTasksForDay in CalendarFragment
    private void getTasksForDay(CalendarDay selectedDay) {
        Date startOfDay = new Date(getStartOfDayInMillis(selectedDay));
        Date endOfDay = new Date(getEndOfDayInMillis(selectedDay));

        // Use TaskViewModel to get tasks for selected day
        taskViewModel.loadTasksForDateRange(currentUserId, startOfDay, endOfDay);

        // For updating decorators, we need all tasks
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

    // Refactored showTasksForDay in CalendarFragment
    private void showTasksForDay(List<Task> tasksForDay) {
        RecyclerView recyclerView = getView().findViewById(R.id.tasksRecyclerView);
        if (recyclerView != null) {
            CalendarTaskAdapter adapter = (CalendarTaskAdapter) recyclerView.getAdapter();
            if (adapter == null) {
                adapter = new CalendarTaskAdapter(tasksForDay);
                recyclerView.setAdapter(adapter);
            } else {
                adapter.updateTasks(tasksForDay);
            }

            // Listener pentru acțiuni pe task-uri
            adapter.setOnTaskActionListener(new CalendarTaskAdapter.OnTaskActionListener() {
                @Override
                public void onTaskEdit(Task task) {
                    showEditTaskBottomSheet(task);
                }

                @Override
                public void onTaskDelete(Task task) {
                    deleteTask(task);
                }
            });
        }
    }

    private long getStartOfDayInMillis(CalendarDay day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(day.getYear(), day.getMonth() - 1, day.getDay(), 0, 0, 0); // Lunile încep de la 0 în Calendar
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private long getEndOfDayInMillis(CalendarDay day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(day.getYear(), day.getMonth() - 1, day.getDay(), 23, 59, 59); // Lunile încep de la 0 în Calendar
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }

    private void showEditTaskBottomSheet(Task task) {
        EditTaskBottomSheet bottomSheet = new EditTaskBottomSheet();
        bottomSheet.setTask(task);
        bottomSheet.setOnTaskUpdatedListener(updatedTask -> {
            if (updatedTask != null) {
                // Use the TaskViewModel to handle the update
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
            // Setează data
            Calendar calendar = Calendar.getInstance();
            calendar.set(selectedDay.getYear(), selectedDay.getMonth() - 1, selectedDay.getDay());
            newTask.setDueDate(new Timestamp(calendar.getTime()));

            // Acum apelăm direct Repository pentru a salva task-ul
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

        for (Task task : tasks) {
            // Filtrare task-uri pentru utilizatorul curent
            if (task.getUserId().equals(currentUserId) && task.getDueDate() != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(task.getDueDate().getSeconds() * 1000);
                CalendarDay day = CalendarDay.from(
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH) + 1,
                        calendar.get(Calendar.DAY_OF_MONTH)
                );
                daysWithTasks.add(day);
            }
        }

        // Curăță decoratorii vechi și aplică unii noi
        calendarView.removeDecorators();
        if (!daysWithTasks.isEmpty()) {
            calendarView.addDecorator(new TaskDayDecorator(Color.MAGENTA, daysWithTasks));
        }
    }

}

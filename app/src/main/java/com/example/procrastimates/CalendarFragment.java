package com.example.procrastimates;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;
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

        getTasksForWeek(today);

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

        return view;
    }

    private void getTasksForWeek(CalendarDay selectedDay) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(selectedDay.getYear(), selectedDay.getMonth() - 1, selectedDay.getDay());
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        long startOfWeekMillis = calendar.getTimeInMillis();

        calendar.add(Calendar.DAY_OF_YEAR, 6);
        long endOfWeekMillis = calendar.getTimeInMillis();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("tasks")
                .whereGreaterThanOrEqualTo("dueDate", new Timestamp(new Date(startOfWeekMillis)))
                .whereLessThanOrEqualTo("dueDate", new Timestamp(new Date(endOfWeekMillis)))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Task> tasksForWeek = new ArrayList<>();
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Task task = document.toObject(Task.class);
                            if (!task.isCompleted()) {
                                tasksForWeek.add(task);
                            }
                        }
                    }
                    showTasksForWeek(tasksForWeek);
                })
                .addOnFailureListener(e -> Log.e("CalendarFragment", "Error getting tasks for week", e));
    }

    private void showTasksForWeek(List<Task> tasksForWeek) {
        // Curăță decoratorii anteriori din calendar
        calendarView.removeDecorators();

        // Creează un set de zile care au task-uri
        HashSet<CalendarDay> daysWithTasks = new HashSet<>();
        for (Task task : tasksForWeek) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(task.getDueDate().getSeconds() * 1000);
            CalendarDay day = CalendarDay.from(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH));

            daysWithTasks.add(day);
        }

        // Aplicați decoratori pentru fiecare zi cu task-uri
        for (CalendarDay dayWithTask : daysWithTasks) {
            int taskColor = Color.RED; // Culoarea marker-ului
            TaskDayDecorator taskDayDecorator = new TaskDayDecorator(Color.MAGENTA, daysWithTasks);
            calendarView.addDecorator(taskDayDecorator);
        }
    }



    private void getTasksForDay(CalendarDay selectedDay) {
        long startOfDayMillis = getStartOfDayInMillis(selectedDay);
        long endOfDayMillis = getEndOfDayInMillis(selectedDay);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("tasks")
                .whereGreaterThanOrEqualTo("dueDate", new Timestamp(new Date(startOfDayMillis)))
                .whereLessThanOrEqualTo("dueDate", new Timestamp(new Date(endOfDayMillis)))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Task> tasksForSelectedDay = new ArrayList<>();
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Task task = document.toObject(Task.class);
                            if (!task.isCompleted()) {
                                tasksForSelectedDay.add(task);
                            }
                        }
                    }
                    showTasksForDay(tasksForSelectedDay);

                    // Actualizează decoratorii pentru toate zilele
                    FirebaseFirestore.getInstance().collection("tasks").get()
                            .addOnSuccessListener(querySnapshot -> {
                                List<Task> allTasks = new ArrayList<>();
                                for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                    Task taskFromDb = document.toObject(Task.class);
                                    if (taskFromDb != null) {
                                        allTasks.add(taskFromDb);
                                    }
                                }
                                updateDayDecorators(allTasks);
                            });
                })
                .addOnFailureListener(e -> Log.e("CalendarFragment", "Error getting tasks for day", e));
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

    private void showEditTaskBottomSheet(Task task) {
        EditTaskBottomSheet bottomSheet = new EditTaskBottomSheet();
        bottomSheet.setTask(task);
        bottomSheet.setOnTaskUpdatedListener(updatedTask -> {
            FirebaseFirestore.getInstance()
                    .collection("tasks")
                    .document(task.getTaskId())
                    .set(updatedTask)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Task updated", Toast.LENGTH_SHORT).show();
                        getTasksForDay(CalendarDay.today());
                    })

                    .addOnFailureListener(e -> Log.e("CalendarFragment", "Error updating task", e));
        });
        bottomSheet.show(getChildFragmentManager(), "EditTaskBottomSheet");
    }

    private void deleteTask(Task task) {
        FirebaseFirestore.getInstance()
                .collection("tasks")
                .document(task.getTaskId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Task deleted", Toast.LENGTH_SHORT).show();
                    getTasksForDay(CalendarDay.today());
                    FirebaseFirestore.getInstance().collection("tasks").get()
                            .addOnSuccessListener(querySnapshot -> {
                                List<Task> allTasks = new ArrayList<>();
                                for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                    Task taskFromDb = document.toObject(Task.class);
                                    if (taskFromDb != null) {
                                        allTasks.add(taskFromDb);
                                    }
                                }
                                updateDayDecorators(allTasks);
                            });
                })
                .addOnFailureListener(e -> Log.e("CalendarFragment", "Error deleting task", e));
    }

    private void updateDayDecorators(List<Task> tasks) {
        HashSet<CalendarDay> daysWithTasks = new HashSet<>();

        for (Task task : tasks) {
            if (task.getDueDate() != null) {
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
        //Curata decoratorii vechi si aplica unii noi
        calendarView.removeDecorators();
        if (!daysWithTasks.isEmpty()) {
            calendarView.addDecorator(new TaskDayDecorator(Color.MAGENTA, daysWithTasks));
        }
    }

}

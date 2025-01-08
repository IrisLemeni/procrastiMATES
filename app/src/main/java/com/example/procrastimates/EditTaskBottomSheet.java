package com.example.procrastimates;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Calendar;
import java.util.Date;

public class EditTaskBottomSheet extends BottomSheetDialogFragment {

    private EditText editTaskTitle;
    private Button saveButton;
    private Task task;
    private RadioGroup dateSelectionGroup;
    private RadioButton todayButton, tomorrowButton, pickDateButton;
    private Date selectedDate;
    private Spinner prioritySpinner;

    private OnTaskUpdatedListener onTaskUpdatedListener;

    public interface OnTaskUpdatedListener {
        void onTaskUpdated(Task updatedTask);
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public void setOnTaskUpdatedListener(OnTaskUpdatedListener listener) {
        this.onTaskUpdatedListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_edit_task, container, false);

        editTaskTitle = view.findViewById(R.id.editTaskTitle);
        saveButton = view.findViewById(R.id.saveButton);
        dateSelectionGroup = view.findViewById(R.id.dateSelectionGroup);
        todayButton = view.findViewById(R.id.todayButton);
        tomorrowButton = view.findViewById(R.id.tomorrowButton);
        pickDateButton = view.findViewById(R.id.pickDateButton);
        prioritySpinner = view.findViewById(R.id.prioritySpinner);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.priority_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prioritySpinner.setAdapter(adapter);

        // Pre-populează câmpurile
        if (task != null) {
            editTaskTitle.setText(task.getTitle());
            selectedDate = task.getDueDate();  // Setează data existentă

            // Pre-selectează opțiunea corespunzătoare
            if (isToday(selectedDate)) {
                todayButton.setChecked(true);
            } else if (isTomorrow(selectedDate)) {
                tomorrowButton.setChecked(true);
            } else {
                pickDateButton.setChecked(true);
            }

            if (task.getPriority() != null) {
                String priorityString = task.getPriority().toString().substring(0, 1).toUpperCase()
                        + task.getPriority().toString().substring(1).toLowerCase(); // Transformă enum-ul într-un string compatibil (ex. LOW -> Low)
                int spinnerPosition = adapter.getPosition(priorityString);
                prioritySpinner.setSelection(spinnerPosition);
            }
        }

        saveButton.setOnClickListener(v -> {
            Task updatedTask = getUpdatedTask(); // Creează un nou task cu valorile actualizate
            if (onTaskUpdatedListener != null) {
                onTaskUpdatedListener.onTaskUpdated(updatedTask);
            }
            dismiss();
        });

        dateSelectionGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.todayButton) {
                selectedDate = getTodayDate();
            } else if (checkedId == R.id.tomorrowButton) {
                selectedDate = getTomorrowDate();
            } else if (checkedId == R.id.pickDateButton) {
                showDatePickerDialog();
            }
        });

        return view;
    }

    private boolean isToday(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int day = calendar.get(Calendar.DAY_OF_YEAR);
        int year = calendar.get(Calendar.YEAR);

        Calendar today = Calendar.getInstance();
        return today.get(Calendar.YEAR) == year && today.get(Calendar.DAY_OF_YEAR) == day;
    }

    private boolean isTomorrow(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        return isToday(calendar.getTime());
    }

    private Date getTodayDate() {
        Calendar calendar = Calendar.getInstance();
        return calendar.getTime();  // Returnează un obiect Date cu data curentă
    }

    private Date getTomorrowDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        return calendar.getTime();  // Returnează un obiect Date cu data de mâine
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getActivity(),
                (view, year, month, dayOfMonth) -> {
                    // Setăm valoarea pentru data selectată
                    calendar.set(year, month, dayOfMonth);
                    selectedDate = calendar.getTime();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private Task getUpdatedTask() {
        if (task == null) {
            task = new Task(); // Creează un task nou dacă nu există unul
        }
        String updatedTitle = editTaskTitle.getText().toString().trim();

        if (TextUtils.isEmpty(updatedTitle)) {
            editTaskTitle.setError("Task title cannot be empty");
            return null; // Nu continua dacă titlul este gol
        }

        task.setTitle(updatedTitle);
        task.setDueDate(selectedDate); // Setează data actualizată

        String selectedPriority = (String) prioritySpinner.getSelectedItem();
        try {
            task.setPriority(Priority.valueOf(selectedPriority.toUpperCase()));
        } catch (IllegalArgumentException | NullPointerException e) {
            task.setPriority(Priority.LOW); // Default dacă prioritatea nu este validă
        }

        return task;
    }
}

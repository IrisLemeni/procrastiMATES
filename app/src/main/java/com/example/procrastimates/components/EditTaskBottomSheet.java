package com.example.procrastimates.components;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.procrastimates.enums.Priority;
import com.example.procrastimates.R;
import com.example.procrastimates.models.Task;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;

import java.util.Calendar;

public class EditTaskBottomSheet extends BottomSheetDialogFragment {

    private TextInputEditText editTaskTitle;
    private Button saveButton, cancelButton;
    private Task task;
    private ChipGroup dateChipGroup;
    private Chip todayChip, tomorrowChip, pickDateChip;
    private Timestamp selectedDate;
    private AutoCompleteTextView prioritySpinner;

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
        cancelButton = view.findViewById(R.id.cancelButton);
        dateChipGroup = view.findViewById(R.id.dateChipGroup);
        todayChip = view.findViewById(R.id.todayChip);
        tomorrowChip = view.findViewById(R.id.tomorrowChip);
        pickDateChip = view.findViewById(R.id.pickDateChip);
        prioritySpinner = view.findViewById(R.id.prioritySpinner);

        // Setup priority dropdown
        String[] priorities = getResources().getStringArray(R.array.priority_options);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_dropdown_item_1line, priorities);
        prioritySpinner.setAdapter(adapter);

        // Pre-populate fields
        if (task != null) {
            editTaskTitle.setText(task.getTitle());
            selectedDate = task.getDueDate();

            // Pre-select appropriate date option
            if (isToday(selectedDate)) {
                todayChip.setChecked(true);
            } else if (isTomorrow(selectedDate)) {
                tomorrowChip.setChecked(true);
            } else {
                pickDateChip.setChecked(true);
            }

            // Set priority
            if (task.getPriority() != null) {
                String priorityString = task.getPriority().toString().substring(0, 1).toUpperCase()
                        + task.getPriority().toString().substring(1).toLowerCase();
                prioritySpinner.setText(priorityString, false);
            }
        } else {
            // Default values for new task
            selectedDate = getTodayTimestamp();
            todayChip.setChecked(true);
        }

        // Setup button listeners
        saveButton.setOnClickListener(v -> {
            Task updatedTask = getUpdatedTask();
            if (updatedTask != null && onTaskUpdatedListener != null) {
                onTaskUpdatedListener.onTaskUpdated(updatedTask);
            }
            dismiss();
        });

        cancelButton.setOnClickListener(v -> dismiss());

        // FIXED: Setup chip group listener - folosim OnCheckedChangeListener în loc de OnCheckedStateChangeListener
        dateChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.todayChip) {
                selectedDate = getTodayTimestamp();
            } else if (checkedId == R.id.tomorrowChip) {
                selectedDate = getTomorrowTimestamp();
            } else if (checkedId == R.id.pickDateChip) {
                showDatePickerDialog();
            }
        });

        return view;
    }

    private boolean isToday(Timestamp timestamp) {
        if (timestamp == null) return false;

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp.getSeconds() * 1000);
        int day = calendar.get(Calendar.DAY_OF_YEAR);
        int year = calendar.get(Calendar.YEAR);

        Calendar today = Calendar.getInstance();
        return today.get(Calendar.YEAR) == year && today.get(Calendar.DAY_OF_YEAR) == day;
    }

    private boolean isTomorrow(Timestamp timestamp) {
        if (timestamp == null) return false;

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        Calendar taskDate = Calendar.getInstance();
        taskDate.setTimeInMillis(timestamp.getSeconds() * 1000);

        return taskDate.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
                taskDate.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR);
    }

    private Timestamp getTodayTimestamp() {
        Calendar calendar = Calendar.getInstance();
        // Set time to start of day
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return new Timestamp(calendar.getTime());
    }

    private Timestamp getTomorrowTimestamp() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        // Set time to start of day
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return new Timestamp(calendar.getTime());
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        if (selectedDate != null) {
            calendar.setTimeInMillis(selectedDate.getSeconds() * 1000);
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getActivity(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    // Set time to start of day
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 0);
                    selectedDate = new Timestamp(calendar.getTime());
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        // Set minimum date to today
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private Task getUpdatedTask() {
        if (task == null) {
            task = new Task();
        }

        String updatedTitle = editTaskTitle.getText().toString().trim();

        if (TextUtils.isEmpty(updatedTitle)) {
            editTaskTitle.setError("Task title cannot be empty");
            return null;
        }

        task.setTitle(updatedTitle);
        task.setDueDate(selectedDate);

        // Set priority
        String selectedPriority = prioritySpinner.getText().toString();
        try {
            if (!TextUtils.isEmpty(selectedPriority)) {
                task.setPriority(Priority.valueOf(selectedPriority.toUpperCase()));
            } else {
                task.setPriority(Priority.LOW); // Default priority
            }
        } catch (IllegalArgumentException e) {
            task.setPriority(Priority.LOW); // Default if invalid
        }

        return task;
    }
}
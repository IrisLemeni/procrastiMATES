package com.example.procrastimates;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;

import androidx.lifecycle.ViewModelProvider;

import com.example.procrastimates.models.Task;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.Timestamp;

import java.util.Calendar;

public class AddTaskBottomSheet extends BottomSheetDialogFragment {

    private EditText taskEditText;
    private Button saveButton;
    private TaskViewModel taskViewModel;
    private OnTaskAddedListener onTaskAddedListener;
    private ChipGroup dateChipGroup;
    private Chip todayChip, tomorrowChip, pickDateChip;
    private Timestamp selectedTimestamp;
    private AutoCompleteTextView prioritySpinner;

    public interface OnTaskAddedListener {
        void onTaskAdded(Task newTask);
    }

    public void setOnTaskAddedListener(OnTaskAddedListener listener) {
        this.onTaskAddedListener = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.new_task, container, false);

        taskEditText = view.findViewById(R.id.newTaskText);
        saveButton = view.findViewById(R.id.saveButton);
        dateChipGroup = view.findViewById(R.id.dateChipGroup);
        todayChip = view.findViewById(R.id.todayChip);
        tomorrowChip = view.findViewById(R.id.tomorrowChip);
        pickDateChip = view.findViewById(R.id.pickDateChip);
        prioritySpinner = view.findViewById(R.id.prioritySpinner);

        taskViewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);

        saveButton.setOnClickListener(v -> saveTask());

        dateChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.todayChip) {
                selectedTimestamp = getTodayTimestamp();
            } else if (checkedId == R.id.tomorrowChip) {
                selectedTimestamp = getTomorrowTimestamp();
            } else if (checkedId == R.id.pickDateChip) {
                showDatePickerDialog();
            }
        });

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.priority_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prioritySpinner.setAdapter(adapter);

        return view;
    }

    private Timestamp getTodayTimestamp() {
        Calendar calendar = Calendar.getInstance();
        return new Timestamp(calendar.getTime());  // Returnează un Timestamp cu data curentă
    }

    private Timestamp getTomorrowTimestamp() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        return new Timestamp(calendar.getTime());  // Returnează un Timestamp cu data de mâine
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getActivity(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    selectedTimestamp = new Timestamp(calendar.getTime());
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void saveTask() {
        String taskTitle = taskEditText.getText().toString().trim();

        if (!taskTitle.isEmpty()) {
            Task task = new Task();
            task.setTitle(taskTitle);
            task.setPriority(getSelectedPriority());
            task.setDueDate(getDueDateAsTimestamp());

            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            task.setUserId(userId);

            // Nu mai apelăm taskViewModel direct aici
            // taskViewModel.addTask(task, userId); <- Elimină această linie

            if (onTaskAddedListener != null) {
                onTaskAddedListener.onTaskAdded(task);
            }
            dismiss();
        } else {
            taskEditText.setError("Please enter a task title");
        }
    }

    // Metoda direct cu Timestamp
    private Timestamp getDueDateAsTimestamp() {
        if (todayChip.isChecked()) {
            return getTodayTimestamp();
        } else if (tomorrowChip.isChecked()) {
            return getTomorrowTimestamp();
        } else if (pickDateChip.isChecked()) {
            return selectedTimestamp;
        }
        return getTodayTimestamp();  // Default to today if no date selected
    }

    private Priority getSelectedPriority() {
        String selectedPriority = prioritySpinner.getText().toString();

        if (selectedPriority.equalsIgnoreCase("Low")) {
            return Priority.LOW;
        } else if (selectedPriority.equalsIgnoreCase("Medium")) {
            return Priority.MEDIUM;
        } else if (selectedPriority.equalsIgnoreCase("High")) {
            return Priority.HIGH;
        }
        return Priority.LOW;
    }
}

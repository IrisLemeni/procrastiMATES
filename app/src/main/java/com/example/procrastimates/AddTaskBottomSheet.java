package com.example.procrastimates;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.procrastimates.TaskViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;

import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;

public class AddTaskBottomSheet extends BottomSheetDialogFragment {

    private EditText taskEditText;
    private Button saveButton;
    private TaskViewModel taskViewModel;
    private OnTaskAddedListener onTaskAddedListener;
    private RadioGroup dateSelectionGroup;
    private RadioButton todayButton, tomorrowButton, pickDateButton;
    private Date selectedDate;
    private Spinner prioritySpinner;


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
        dateSelectionGroup = view.findViewById(R.id.dateSelectionGroup);
        todayButton = view.findViewById(R.id.todayButton);
        tomorrowButton = view.findViewById(R.id.tomorrowButton);
        pickDateButton = view.findViewById(R.id.pickDateButton);
        prioritySpinner = view.findViewById(R.id.prioritySpinner);

        taskViewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);

        saveButton.setOnClickListener(v -> saveTask());

        dateSelectionGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.todayButton) {
                selectedDate = getTodayDate();
            } else if (checkedId == R.id.tomorrowButton) {
                selectedDate = getTomorrowDate();
            } else if (checkedId == R.id.pickDateButton) {
                showDatePickerDialog();
            }
        });

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.priority_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prioritySpinner.setAdapter(adapter);

        return view;
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
                    calendar.set(year, month, dayOfMonth);
                    selectedDate = calendar.getTime();
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

            task.setDueDate(getDueDate());

            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            task.setUserId(userId);

            taskViewModel.addTask(task, userId);
            if (onTaskAddedListener != null) {
                onTaskAddedListener.onTaskAdded(task);
            }

            dismiss();
        } else {
            taskEditText.setError("Please enter a task title");
        }
    }

    private Date getDueDate() {
        if (todayButton.isChecked()) {
            return getTodayDate();
        } else if (tomorrowButton.isChecked()) {
            return getTomorrowDate();
        } else if (pickDateButton.isChecked()) {
            return selectedDate;
        }
        return getTodayDate();
    }

    private Priority getSelectedPriority() {
        String selectedPriority = (String) prioritySpinner.getSelectedItem();

        if (selectedPriority.equals("Low")) {
            return Priority.LOW;
        } else if (selectedPriority.equals("Medium")) {
            return Priority.MEDIUM;
        } else if (selectedPriority.equals("High")) {
            return Priority.HIGH;
        }
        return Priority.LOW;  // Default
    }

}

package com.example.procrastimates;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.procrastimates.TaskViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;

public class AddTaskBottomSheet extends BottomSheetDialogFragment {

    private EditText taskEditText;
    private Button saveButton;
    private TaskViewModel taskViewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.new_task, container, false);

        taskEditText = view.findViewById(R.id.newTaskText);
        saveButton = view.findViewById(R.id.saveButton);

        taskViewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);

        saveButton.setOnClickListener(v -> saveTask());

        return view;
    }

    private void saveTask() {
        String taskTitle = taskEditText.getText().toString().trim();

        if (!taskTitle.isEmpty()) {
            Task task = new Task();
            task.setTitle(taskTitle);
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            taskViewModel.addTask(task, userId);
            dismiss();
        } else {
            taskEditText.setError("Please enter a task title");
        }
    }
}

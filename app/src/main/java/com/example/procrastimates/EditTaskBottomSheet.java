package com.example.procrastimates;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class EditTaskBottomSheet extends BottomSheetDialogFragment {

    private EditText editTaskTitle;
    private Button saveButton;
    private Task task;
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

        // Pre-populează câmpurile
        if (task != null) {
            editTaskTitle.setText(task.getTitle());
        }

        saveButton.setOnClickListener(v -> {
            Task updatedTask = getUpdatedTask(); // Creează un nou task cu valorile actualizate
            if (onTaskUpdatedListener != null) {
                onTaskUpdatedListener.onTaskUpdated(updatedTask);
            }
            dismiss();
        });

        return view;
    }

    private Task getUpdatedTask() {
        if (task == null) {
            task = new Task(); // Creează un task nou dacă nu există unul
        }

        String updatedTitle = editTaskTitle.getText().toString().trim();

        // Verifică dacă titlul este gol
        if (TextUtils.isEmpty(updatedTitle)) {
            editTaskTitle.setError("Task title cannot be empty");
            return null; // Nu continua dacă titlul este gol
        }

        // Actualizează task-ul existent cu noile valori
        task.setTitle(updatedTitle);

        return task;
    }
}

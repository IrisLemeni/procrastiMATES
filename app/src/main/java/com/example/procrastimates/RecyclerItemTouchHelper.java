package com.example.procrastimates;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.procrastimates.repositories.TaskRepository;
import com.example.procrastimates.models.Task;

public class RecyclerItemTouchHelper extends ItemTouchHelper.SimpleCallback {

    private TaskAdapter adapter;
    private TaskRepository taskRepository;

    // Constructor modificat pentru a accepta doar TaskAdapter
    public RecyclerItemTouchHelper(TaskAdapter adapter) {
        super(0, ItemTouchHelper.LEFT);
        this.adapter = adapter;
        this.taskRepository = TaskRepository.getInstance();  // Obține instanța TaskRepository
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull final RecyclerView.ViewHolder viewHolder, int direction) {
        final int position = viewHolder.getAdapterPosition();
        Context context = viewHolder.itemView.getContext();

        if (direction == ItemTouchHelper.LEFT) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Delete Task");
            builder.setMessage("Are you sure you want to delete this task?");
            builder.setPositiveButton("Confirm", (dialog, which) -> {
                Task taskToDelete = adapter.getTaskAt(position);
                taskRepository.deleteTask(taskToDelete.getTaskId(), new TaskRepository.OnTaskActionListener() {
                    @Override
                    public void onSuccess(Object result) {
                        adapter.removeTask(position);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        // Afișează un mesaj de eroare
                        AlertDialog.Builder errorDialog = new AlertDialog.Builder(context);
                        errorDialog.setTitle("Error");
                        errorDialog.setMessage("Failed to delete task: " + e.getMessage());
                        errorDialog.setPositiveButton("OK", null);
                        errorDialog.show();
                        adapter.notifyItemChanged(position); // Restaurează task-ul în caz de eșec
                    }
                });
            });

            builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                adapter.notifyItemChanged(viewHolder.getAdapterPosition());
            });

            builder.show();
        }
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

        // Folosim contextul din itemView
        Context context = viewHolder.itemView.getContext();

        if (dX > 0) {
            return; // Nu facem nimic pentru swipe la dreapta
        }

        Drawable icon = ContextCompat.getDrawable(context, R.drawable.baseline_delete);
        ColorDrawable background = new ColorDrawable(Color.RED);

        View itemView = viewHolder.itemView;
        int backgroundCornerOffset = 20;

        // Swipe la stânga
        int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
        int iconTop = itemView.getTop() + (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
        int iconBottom = iconTop + icon.getIntrinsicHeight();

        int iconLeft = itemView.getRight() - iconMargin - icon.getIntrinsicWidth();
        int iconRight = itemView.getRight() - iconMargin;
        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);

        background.setBounds(itemView.getRight() + ((int) dX) - backgroundCornerOffset,
                itemView.getTop(), itemView.getRight(), itemView.getBottom());

        background.draw(c);
        icon.draw(c);
    }
}


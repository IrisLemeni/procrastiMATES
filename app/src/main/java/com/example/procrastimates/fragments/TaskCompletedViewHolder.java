package com.example.procrastimates.fragments;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.procrastimates.Message;
import com.example.procrastimates.MessageType;
import com.example.procrastimates.Notification;
import com.example.procrastimates.NotificationSender;
import com.example.procrastimates.Objection;
import com.example.procrastimates.ObjectionStatus;
import com.example.procrastimates.R;
import com.example.procrastimates.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

class TaskCompletedViewHolder extends RecyclerView.ViewHolder {
    TextView taskTitle, completedBy, timestamp;
    Button objectButton;
    private final String currentUserId;
    private final Context context;
    private final FirebaseFirestore db;


    TaskCompletedViewHolder(View itemView, Context context, String currentUserId, FirebaseFirestore db) {
        super(itemView);
        this.context = context;
        this.currentUserId = currentUserId;
        this.db = db;
        taskTitle = itemView.findViewById(R.id.task_title);
        completedBy = itemView.findViewById(R.id.completed_by);
        timestamp = itemView.findViewById(R.id.timestamp);
        objectButton = itemView.findViewById(R.id.btn_objection);
    }

    void bind(Message message) {
        // Încarcă detaliile task-ului
        if (message.getTaskId() != null) {
            db.collection("tasks").document(message.getTaskId())
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            Task task = document.toObject(Task.class);
                            if (task != null) {
                                taskTitle.setText(task.getTitle());

                                // Verifică dacă utilizatorul poate face obiecție
                                checkObjectionEligibility(task, message);
                            }
                        }
                    });
        }

        // Încarcă numele expeditorului
        db.collection("users").document(message.getSenderId())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String name = document.getString("username");
                        completedBy.setText(name != null ? name + " a finalizat un task" : "Task finalizat");
                    }
                });

        // Formatează timestamp-ul
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm, dd MMM", Locale.getDefault());
        timestamp.setText(sdf.format(message.getTimestamp().toDate()));
    }

    private void checkObjectionEligibility(Task task, Message message) {
        if (task.getCompletedAt() == null) {
            objectButton.setVisibility(View.GONE);
            return;
        }

        long timeDifference = System.currentTimeMillis() - task.getCompletedAt().toDate().getTime();
        boolean isWithin10Minutes = timeDifference <= 10 * 60 * 1000;

        // Verifică dacă există deja o obiecție pentru acest task
        db.collection("objections")
                .whereEqualTo("taskId", task.getTaskId())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean hasObjection = !queryDocumentSnapshots.isEmpty();

                    // Verifică câte obiecții a făcut utilizatorul astăzi
                    Calendar calendar = Calendar.getInstance();
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    Timestamp startOfDay = new Timestamp(calendar.getTime());

                    db.collection("objections")
                            .whereEqualTo("objectorUserId", currentUserId)
                            .whereGreaterThanOrEqualTo("createdAt", startOfDay)
                            .get()
                            .addOnSuccessListener(objectionSnapshots -> {
                                boolean hasReachedDailyLimit = objectionSnapshots.size() >= 2;

                                boolean canObject = isWithin10Minutes && !hasObjection && !hasReachedDailyLimit &&
                                        !task.getUserId().equals(currentUserId);

                                objectButton.setVisibility(canObject ? View.VISIBLE : View.GONE);
                                objectButton.setOnClickListener(v -> raiseObjection(task));
                            });
                });
    }

    private void raiseObjection(Task task) {
        // Creează obiecția
        Objection objection = new Objection();
        objection.setObjectionId(UUID.randomUUID().toString());
        objection.setTaskId(task.getTaskId());
        objection.setTaskTitle(task.getTitle());
        objection.setTargetUserId(task.getUserId());
        objection.setObjectorUserId(currentUserId);
        objection.setCreatedAt(new Timestamp(new Date()));
        objection.setStatus(ObjectionStatus.PENDING);
        objection.setCircleId(task.getCircleId());

        // Salvează obiecția în Firebase
        db.collection("objections").document(objection.getObjectionId())
                .set(objection)
                .addOnSuccessListener(aVoid -> {
                    // Creează un mesaj de obiecție
                    sendObjectionMessage(task, objection);

                    // Trimite notificare utilizatorului care a completat task-ul
                    sendObjectionNotification(task, objection);
                });
    }

    private void sendObjectionMessage(Task task, Objection objection) {
        Message objectionMessage = new Message();
        objectionMessage.setMessageId(UUID.randomUUID().toString());
        objectionMessage.setCircleId(task.getCircleId());
        objectionMessage.setSenderId(currentUserId);
        objectionMessage.setText("A contestat completarea task-ului: " + task.getTitle());
        objectionMessage.setType(MessageType.OBJECTION_RAISED);
        objectionMessage.setTaskId(task.getTaskId());
        objectionMessage.setTimestamp(new Timestamp(new Date()));

        db.collection("messages").document(objectionMessage.getMessageId())
                .set(objectionMessage);
    }

    private void sendObjectionNotification(Task task, Objection objection) {
        // Caută utilizatorul care a realizat task-ul
        db.collection("users").document(task.getUserId())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String targetUserName = document.getString("username");

                        // Caută numele celui care face obiecția
                        db.collection("users").document(currentUserId)
                                .get()
                                .addOnSuccessListener(objectorDoc -> {
                                    if (objectorDoc.exists()) {
                                        String objectorName = objectorDoc.getString("username");

                                        // Creează notificarea
                                        Notification notification = new Notification();
                                        notification.setNotificationId(UUID.randomUUID().toString());
                                        notification.setUserId(task.getUserId());
                                        notification.setTitle("Obiecție primită");
                                        notification.setBody(objectorName + " a contestat task-ul tău: " + task.getTitle());
                                        notification.setCircleId(task.getCircleId());
                                        notification.setTaskId(task.getTaskId());
                                        notification.setRead(false);
                                        notification.setCreatedAt(new Timestamp(new Date()));

                                        // Salvează notificarea
                                        db.collection("notifications").document(notification.getNotificationId())
                                                .set(notification)
                                                .addOnSuccessListener(aVoid -> {
                                                    // Trimite notificarea push
                                                    NotificationSender.sendPushNotification(
                                                            task.getUserId(),
                                                            "Obiecție primită",
                                                            objectorName + " a contestat task-ul tău: " + task.getTitle()
                                                    );
                                                });
                                    }
                                });
                    }
                });
    }
}
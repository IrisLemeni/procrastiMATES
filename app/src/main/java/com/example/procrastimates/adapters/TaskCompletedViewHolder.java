package com.example.procrastimates.adapters;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.procrastimates.models.Message;
import com.example.procrastimates.enums.MessageType;
import com.example.procrastimates.models.Notification;
import com.example.procrastimates.services.NotificationSender;
import com.example.procrastimates.enums.NotificationType;
import com.example.procrastimates.models.Objection;
import com.example.procrastimates.enums.ObjectionStatus;
import com.example.procrastimates.R;
import com.example.procrastimates.models.Task;
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

    private static final String COLLECTION_USERS = "users";
    private static final String FIELD_USERNAME = "username";
    private static final String COLLECTION_OBJECTIONS = "objections";
    private static final String COLLECTION_TASKS = "tasks";
    private static final String COLLECTION_MESSAGES = "messages";
    private static final String COLLECTION_NOTIFICATIONS = "notifications";

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
        // Load task details
        if (message.getTaskId() != null) {
            db.collection(COLLECTION_TASKS).document(message.getTaskId())
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            Task task = document.toObject(Task.class);
                            if (task != null) {
                                taskTitle.setText(task.getTitle());

                                // Check if user can raise objection
                                checkObjectionEligibility(task);
                            }
                        }
                    });
        }

        // Load sender's name
        db.collection(COLLECTION_USERS).document(message.getSenderId())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String name = document.getString(FIELD_USERNAME);
                        completedBy.setText(name != null ? name : "Unknown user");
                    }
                });

        // Format timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm, dd MMM", Locale.getDefault());
        timestamp.setText(sdf.format(message.getTimestamp().toDate()));
    }

    private void checkObjectionEligibility(Task task) {
        if (task.getCompletedAt() == null) {
            objectButton.setVisibility(View.GONE);
            return;
        }

        long timeDifference = System.currentTimeMillis() - task.getCompletedAt().toDate().getTime();
        boolean isWithin10Minutes = timeDifference <= 10 * 60 * 1000;

        // Check if there's already an objection for this task
        db.collection(COLLECTION_OBJECTIONS)
                .whereEqualTo("taskId", task.getTaskId())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean hasObjection = !queryDocumentSnapshots.isEmpty();

                    // Check how many objections the user has made today
                    Calendar calendar = Calendar.getInstance();
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    Timestamp startOfDay = new Timestamp(calendar.getTime());

                    db.collection(COLLECTION_OBJECTIONS)
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
        // Create objection
        Objection objection = new Objection();
        objection.setObjectionId(UUID.randomUUID().toString());
        objection.setTaskId(task.getTaskId());
        objection.setTaskTitle(task.getTitle());
        objection.setTargetUserId(task.getUserId());
        objection.setObjectorUserId(currentUserId);
        objection.setCreatedAt(new Timestamp(new Date()));
        objection.setStatus(ObjectionStatus.PENDING);
        objection.setCircleId(task.getCircleId());

        // Save objection to Firebase
        db.collection(COLLECTION_OBJECTIONS).document(objection.getObjectionId())
                .set(objection)
                .addOnSuccessListener(aVoid -> {
                    // Create objection message
                    sendObjectionMessage(task, objection);

                    // Send notification to user who completed the task
                    sendObjectionNotification(task);
                });
    }

    private void sendObjectionMessage(Task task, Objection objection) {
        Message objectionMessage = new Message();
        objectionMessage.setMessageId(UUID.randomUUID().toString());
        objectionMessage.setCircleId(task.getCircleId());
        objectionMessage.setSenderId(currentUserId);
        objectionMessage.setText("Challenged task completion: " + task.getTitle());
        objectionMessage.setType(MessageType.OBJECTION_RAISED);
        objectionMessage.setTaskId(task.getTaskId());
        objectionMessage.setTimestamp(new Timestamp(new Date()));

        db.collection(COLLECTION_MESSAGES).document(objectionMessage.getMessageId())
                .set(objectionMessage);
    }

    private void sendObjectionNotification(Task task) {
        // Find user who completed the task
        db.collection(COLLECTION_USERS).document(task.getUserId())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        // Find name of the objector
                        db.collection(COLLECTION_USERS).document(currentUserId)
                                .get()
                                .addOnSuccessListener(objectorDoc -> {
                                    if (objectorDoc.exists()) {
                                        String objectorName = objectorDoc.getString(FIELD_USERNAME);

                                        // Create notification
                                        Notification notification = new Notification();
                                        notification.setNotificationId(UUID.randomUUID().toString());
                                        notification.setUserId(task.getUserId());
                                        notification.setTitle("Objection Received");
                                        notification.setBody(objectorName + " challenged your task: " + task.getTitle());
                                        notification.setCircleId(task.getCircleId());
                                        notification.setTaskId(task.getTaskId());
                                        notification.setType(NotificationType.OBJECTION_RAISED);
                                        notification.setRead(false);
                                        notification.setCreatedAt(new Timestamp(new Date()));

                                        // Save notification
                                        db.collection(COLLECTION_NOTIFICATIONS).document(notification.getNotificationId())
                                                .set(notification)
                                                .addOnSuccessListener(aVoid -> {
                                                    // Send push notification
                                                    NotificationSender.sendPushNotification(
                                                            task.getUserId(),
                                                            "Objection Received",
                                                            objectorName + " challenged your task: " + task.getTitle(),
                                                            task.getTaskId(),
                                                            task.getCircleId(),
                                                            NotificationType.OBJECTION_RAISED
                                                    );
                                                });
                                    }
                                });
                    }
                });
    }
}


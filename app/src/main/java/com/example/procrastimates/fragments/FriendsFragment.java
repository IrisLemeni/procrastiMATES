package com.example.procrastimates.fragments;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.procrastimates.models.Circle;
import com.example.procrastimates.models.Friend;
import com.example.procrastimates.adapters.FriendsAdapter;
import com.example.procrastimates.adapters.LeaderboardAdapter;
import com.example.procrastimates.R;
import com.example.procrastimates.models.Task;
import com.example.procrastimates.activities.CircleChatActivity;
import com.example.procrastimates.activities.NotificationsActivity;
import com.example.procrastimates.activities.SearchFriendsActivity;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class FriendsFragment extends Fragment {
    private Button btnAddFriend, btnObjections;
    private ImageButton btnNotifications;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserId;
    private RecyclerView friendsRecyclerView, top3RecyclerView, othersRecyclerView;
    private TextView userName;
    private FriendsAdapter friendsAdapter;
    private LeaderboardAdapter top3Adapter, othersAdapter;
    private ImageView userProfileImage;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friends, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        currentUserId = mAuth.getCurrentUser().getUid();
        userName = view.findViewById(R.id.userName);
        userProfileImage = view.findViewById(R.id.userProfileImage);
        loadUserData(currentUserId);

        btnNotifications = view.findViewById(R.id.btnNotifications);
        btnAddFriend = view.findViewById(R.id.btnAddFriend);
        btnObjections = view.findViewById(R.id.btnObjections);

        friendsRecyclerView = view.findViewById(R.id.friendsRecyclerView);
        top3RecyclerView = view.findViewById(R.id.top3RecyclerView);
        othersRecyclerView = view.findViewById(R.id.othersRecyclerView);

        friendsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        top3RecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        othersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        friendsAdapter = new FriendsAdapter(new ArrayList<>());
        top3Adapter = new LeaderboardAdapter(new ArrayList<>(), true);
        othersAdapter = new LeaderboardAdapter(new ArrayList<>(), false);

        friendsRecyclerView.setAdapter(friendsAdapter);
        top3RecyclerView.setAdapter(top3Adapter);
        othersRecyclerView.setAdapter(othersAdapter);

        btnAddFriend.setOnClickListener(v -> startActivity(new Intent(getContext(), SearchFriendsActivity.class)));
        btnNotifications.setOnClickListener(v -> showNotifications());
        btnObjections.setOnClickListener(v -> launchCircleChatActivity());

        // Load both daily progress and monthly leaderboard
        loadDailyTasks();
        loadMonthlyLeaderboard();

        return view;
    }

    private void launchCircleChatActivity() {
        db.collection("circles")
                .whereArrayContains("members", currentUserId)
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        String circleId = query.getDocuments().get(0).getId();
                        Intent intent = new Intent(getContext(), CircleChatActivity.class);
                        intent.putExtra("circleId", circleId);
                        startActivity(intent);
                    }
                });
    }

    private void loadDailyTasks() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startOfDay = calendar.getTimeInMillis();

        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        long endOfDay = calendar.getTimeInMillis();

        Timestamp startTimestamp = new Timestamp(startOfDay / 1000, 0);
        Timestamp endTimestamp = new Timestamp(endOfDay / 1000, 0);

        db.collection("circles")
                .whereArrayContains("members", currentUserId)
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        DocumentSnapshot circleDoc = query.getDocuments().get(0);
                        Circle circle = circleDoc.toObject(Circle.class);
                        if (circle != null) {
                            List<String> members = circle.getMembers();
                            loadDailyProgressForMembers(members, startTimestamp, endTimestamp);
                        }
                    }
                });
    }

    private void loadDailyProgressForMembers(List<String> members, Timestamp startTimestamp, Timestamp endTimestamp) {
        List<Friend> dailyFriendsList = new ArrayList<>();
        AtomicInteger completedRequests = new AtomicInteger(0);

        for (String friendId : members) {
            db.collection("users").document(friendId).get()
                    .addOnSuccessListener(userDoc -> {
                        if (userDoc.exists()) {
                            String friendName = userDoc.getString("username");
                            String profileImageUrl = userDoc.getString("profileImageUrl");

                            db.collection("tasks")
                                    .whereEqualTo("userId", friendId)
                                    .whereGreaterThanOrEqualTo("dueDate", startTimestamp)
                                    .whereLessThanOrEqualTo("dueDate", endTimestamp)
                                    .get()
                                    .addOnSuccessListener(tasks -> {
                                        int completed = 0;
                                        int total = tasks.size();

                                        for (DocumentSnapshot doc : tasks.getDocuments()) {
                                            Task task = doc.toObject(Task.class);
                                            if (task != null && task.isCompleted()) {
                                                completed++;
                                            }
                                        }

                                        dailyFriendsList.add(new Friend(friendId, friendName, completed, total, profileImageUrl));

                                        // Check if all requests are completed
                                        if (completedRequests.incrementAndGet() == members.size()) {
                                            // Sort by completion rate (percentage)
                                            dailyFriendsList.sort((f1, f2) -> {
                                                float rate1 = f1.getTotalTasks() > 0 ? (float) f1.getCompletedTasks() / f1.getTotalTasks() : 0;
                                                float rate2 = f2.getTotalTasks() > 0 ? (float) f2.getCompletedTasks() / f2.getTotalTasks() : 0;
                                                return Float.compare(rate2, rate1);
                                            });

                                            friendsAdapter.setFriends(dailyFriendsList);
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        // Handle error - add friend with 0 tasks
                                        dailyFriendsList.add(new Friend(friendId, friendName, 0, 0, profileImageUrl));
                                        if (completedRequests.incrementAndGet() == members.size()) {
                                            friendsAdapter.setFriends(dailyFriendsList);
                                        }
                                    });
                        } else {
                            // User doesn't exist, still increment counter
                            if (completedRequests.incrementAndGet() == members.size()) {
                                friendsAdapter.setFriends(dailyFriendsList);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Handle error
                        if (completedRequests.incrementAndGet() == members.size()) {
                            friendsAdapter.setFriends(dailyFriendsList);
                        }
                    });
        }
    }

    private void loadMonthlyLeaderboard() {
        db.collection("circles")
                .whereArrayContains("members", currentUserId)
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        DocumentSnapshot circleDoc = query.getDocuments().get(0);
                        Circle circle = circleDoc.toObject(Circle.class);
                        if (circle != null) {
                            loadMonthlyProgressForMembers(circle.getMembers());
                        }
                    }
                });
    }

    private void loadMonthlyProgressForMembers(List<String> members) {
        // Get current month's start and end dates
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startOfMonth = calendar.getTimeInMillis();

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        long endOfMonth = calendar.getTimeInMillis();

        Timestamp startTimestamp = new Timestamp(startOfMonth / 1000, 0);
        Timestamp endTimestamp = new Timestamp(endOfMonth / 1000, 0);

        List<Friend> monthlyFriendsList = new ArrayList<>();
        AtomicInteger completedRequests = new AtomicInteger(0);

        for (String friendId : members) {
            db.collection("users").document(friendId).get()
                    .addOnSuccessListener(userDoc -> {
                        if (userDoc.exists()) {
                            String friendName = userDoc.getString("username");
                            String profileImageUrl = userDoc.getString("profileImageUrl");

                            db.collection("tasks")
                                    .whereEqualTo("userId", friendId)
                                    .whereGreaterThanOrEqualTo("dueDate", startTimestamp)
                                    .whereLessThanOrEqualTo("dueDate", endTimestamp)
                                    .get()
                                    .addOnSuccessListener(tasks -> {
                                        int completed = 0;
                                        int total = tasks.size();

                                        for (DocumentSnapshot doc : tasks.getDocuments()) {
                                            Task task = doc.toObject(Task.class);
                                            if (task != null && task.isCompleted()) {
                                                completed++;
                                            }
                                        }

                                        monthlyFriendsList.add(new Friend(friendId, friendName, completed, total, profileImageUrl));

                                        // Check if all requests are completed
                                        if (completedRequests.incrementAndGet() == members.size()) {
                                            // Sort by completed tasks count (descending)
                                            monthlyFriendsList.sort((f1, f2) -> Integer.compare(f2.getCompletedTasks(), f1.getCompletedTasks()));
                                            updateLeaderboard(monthlyFriendsList);
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        // Handle error - add friend with 0 tasks
                                        monthlyFriendsList.add(new Friend(friendId, friendName, 0, 0, profileImageUrl));
                                        if (completedRequests.incrementAndGet() == members.size()) {
                                            monthlyFriendsList.sort((f1, f2) -> Integer.compare(f2.getCompletedTasks(), f1.getCompletedTasks()));
                                            updateLeaderboard(monthlyFriendsList);
                                        }
                                    });
                        } else {
                            // User doesn't exist, still increment counter
                            if (completedRequests.incrementAndGet() == members.size()) {
                                monthlyFriendsList.sort((f1, f2) -> Integer.compare(f2.getCompletedTasks(), f1.getCompletedTasks()));
                                updateLeaderboard(monthlyFriendsList);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Handle error
                        if (completedRequests.incrementAndGet() == members.size()) {
                            monthlyFriendsList.sort((f1, f2) -> Integer.compare(f2.getCompletedTasks(), f1.getCompletedTasks()));
                            updateLeaderboard(monthlyFriendsList);
                        }
                    });
        }
    }

    private void updateLeaderboard(List<Friend> allFriends) {
        if (allFriends.isEmpty()) {
            return;
        }

        // Get top 3 and others
        List<Friend> top3 = allFriends.size() <= 3 ? new ArrayList<>(allFriends) : allFriends.subList(0, 3);
        List<Friend> others = allFriends.size() <= 3 ? new ArrayList<>() : allFriends.subList(3, allFriends.size());

        // Rearrange top 3 for podium display (2nd, 1st, 3rd)
        if (top3.size() >= 2) {
            List<Friend> rearrangedTop3 = new ArrayList<>();
            if (top3.size() >= 2) {
                rearrangedTop3.add(top3.get(1)); // 2nd place
                rearrangedTop3.add(top3.get(0)); // 1st place
                if (top3.size() >= 3) {
                    rearrangedTop3.add(top3.get(2)); // 3rd place
                }
            } else {
                rearrangedTop3.addAll(top3);
            }
            top3 = rearrangedTop3;
        }

        // Update adapters
        top3Adapter.setFriends(top3);
        othersAdapter.setFriends(others);

        // Notify adapters that data has changed
        top3Adapter.notifyDataSetChanged();
        othersAdapter.notifyDataSetChanged();
    }

    // Keep the old method names for compatibility but make them call the new methods
    public void loadFriendsProgress() {
        loadMonthlyLeaderboard();
    }

    public void loadProgressForFriends(List<String> members) {
        loadMonthlyProgressForMembers(members);
    }

    private void showNotifications() {
        startActivity(new Intent(getActivity(), NotificationsActivity.class));
    }

    private void loadUserData(String userId) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        userName.setText(doc.getString("username") != null ? doc.getString("username") : "Your username");
                        String imageUrl = doc.getString("profileImageUrl");
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Glide.with(FriendsFragment.this).load(imageUrl).circleCrop().into(userProfileImage);
                        }
                    }
                });
    }
}
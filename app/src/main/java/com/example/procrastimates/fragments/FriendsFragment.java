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

import com.bumptech.glide.Glide;
import com.example.procrastimates.activities.InvitationsActivity;
import com.example.procrastimates.models.Circle;
import com.example.procrastimates.models.Friend;
import com.example.procrastimates.adapters.FriendsAdapter;
import com.example.procrastimates.adapters.LeaderboardAdapter;
import com.example.procrastimates.R;
import com.example.procrastimates.models.Task;
import com.example.procrastimates.activities.CircleChatActivity;
import com.example.procrastimates.activities.SearchFriendsActivity;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
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

    private static final String COLLECTION_CIRCLES = "circles";
    private static final String FIELD_MEMBERS = "members";
    private static final String COLLECTION_USERS = "users";
    private static final String FIELD_USERNAME = "username";
    private static final String FIELD_PROFILE_IMAGE_URL = "profileImageUrl";
    private static final String FIELD_DUE_DATE = "dueDate";
    private static final String COLLECTION_TASKS = "tasks";
    private static final String FIELD_USER_ID = "userId";

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
        db.collection(COLLECTION_CIRCLES)
                .whereArrayContains(FIELD_MEMBERS, currentUserId)
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

        db.collection(COLLECTION_CIRCLES)
                .whereArrayContains(FIELD_MEMBERS, currentUserId)
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
        int totalMembers = members.size();

        for (String friendId : members) {
            loadFriendData(friendId, dailyFriendsList, completedRequests, totalMembers, startTimestamp, endTimestamp, true);
        }
    }

    private void loadFriendData(String friendId, List<Friend> friendsList, AtomicInteger completedRequests, int totalMembers, Timestamp startTimestamp, Timestamp endTimestamp, boolean isDaily) {
        db.collection(COLLECTION_USERS).document(friendId).get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        String friendName = userDoc.getString(FIELD_USERNAME);
                        String profileImageUrl = userDoc.getString(FIELD_PROFILE_IMAGE_URL);
                        loadFriendTasks(friendId, friendName, profileImageUrl, friendsList, completedRequests, totalMembers, startTimestamp, endTimestamp, isDaily);
                    } else {
                        friendsList.add(new Friend(friendId, "Unknown", 0, 0, null));
                        checkIfAllLoaded(friendsList, completedRequests, totalMembers, isDaily);
                    }
                })
                .addOnFailureListener(e -> {
                    friendsList.add(new Friend(friendId, "Unknown", 0, 0, null));
                    checkIfAllLoaded(friendsList, completedRequests, totalMembers, isDaily);
                });
    }

    private void loadFriendTasks(String friendId, String friendName, String profileImageUrl, List<Friend> friendsList, AtomicInteger completedRequests, int totalMembers, Timestamp startTimestamp, Timestamp endTimestamp, boolean isDaily) {
        db.collection(COLLECTION_TASKS)
                .whereEqualTo(FIELD_USER_ID, friendId)
                .whereGreaterThanOrEqualTo(FIELD_DUE_DATE, startTimestamp)
                .whereLessThanOrEqualTo(FIELD_DUE_DATE, endTimestamp)
                .get()
                .addOnSuccessListener(tasks -> {
                    int completed = countCompletedTasks(tasks.getDocuments());
                    int total = tasks.size();
                    friendsList.add(new Friend(friendId, friendName, completed, total, profileImageUrl));
                    checkIfAllLoaded(friendsList, completedRequests, totalMembers, isDaily);
                })
                .addOnFailureListener(e -> {
                    friendsList.add(new Friend(friendId, friendName, 0, 0, profileImageUrl));
                    checkIfAllLoaded(friendsList, completedRequests, totalMembers, isDaily);
                });
    }

    private int countCompletedTasks(List<DocumentSnapshot> taskDocs) {
        int completed = 0;
        for (DocumentSnapshot doc : taskDocs) {
            Task task = doc.toObject(Task.class);
            if (task != null && task.isCompleted()) {
                completed++;
            }
        }
        return completed;
    }

    private void checkIfAllLoaded(List<Friend> friendsList, AtomicInteger completedRequests, int totalMembers, boolean isDaily) {
        if (completedRequests.incrementAndGet() == totalMembers) {
            if (isDaily) {
                sortAndSetDailyFriends(friendsList);
            } else {
                sortAndSetMonthlyFriends(friendsList);
            }
        }
    }

    private void sortAndSetDailyFriends(List<Friend> friendsList) {
        friendsList.sort((f1, f2) -> {
            float rate1 = f1.getTotalTasks() > 0 ? (float) f1.getCompletedTasks() / f1.getTotalTasks() : 0;
            float rate2 = f2.getTotalTasks() > 0 ? (float) f2.getCompletedTasks() / f2.getTotalTasks() : 0;
            return Float.compare(rate2, rate1);
        });
        friendsAdapter.setFriends(friendsList);
    }

    private void loadMonthlyLeaderboard() {
        db.collection(COLLECTION_CIRCLES)
                .whereArrayContains(FIELD_MEMBERS, currentUserId)
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
        int totalMembers = members.size();

        for (String friendId : members) {
            loadFriendData(friendId, monthlyFriendsList, completedRequests, totalMembers, startTimestamp, endTimestamp, false);
        }
    }

    private void sortAndSetMonthlyFriends(List<Friend> friendsList) {
        friendsList.sort((f1, f2) -> Integer.compare(f2.getCompletedTasks(), f1.getCompletedTasks()));
        updateLeaderboard(friendsList);
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
    }

    // Keep the old method names for compatibility but make them call the new methods
    public void loadFriendsProgress() {
        loadMonthlyLeaderboard();
    }

    public void loadProgressForFriends(List<String> members) {
        loadMonthlyProgressForMembers(members);
    }

    private void showNotifications() {
        startActivity(new Intent(getActivity(), InvitationsActivity.class));
    }

    private void loadUserData(String userId) {
        db.collection(COLLECTION_USERS).document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        userName.setText(doc.getString(FIELD_USERNAME) != null ? doc.getString(FIELD_USERNAME) : "Your username");
                        String imageUrl = doc.getString(FIELD_PROFILE_IMAGE_URL);
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Glide.with(FriendsFragment.this).load(imageUrl).circleCrop().into(userProfileImage);
                        }
                    }
                });
    }
}
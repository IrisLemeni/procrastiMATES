package com.example.procrastimates.fragments;

import android.content.Intent;
import android.media.Image;
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
import com.example.procrastimates.Circle;
import com.example.procrastimates.Friend;
import com.example.procrastimates.FriendsAdapter;
import com.example.procrastimates.LeaderboardAdapter;
import com.example.procrastimates.R;
import com.example.procrastimates.Task;
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

        // Inițializează RecyclerView-urile
        friendsRecyclerView = view.findViewById(R.id.friendsRecyclerView);
        top3RecyclerView = view.findViewById(R.id.top3RecyclerView);
        othersRecyclerView = view.findViewById(R.id.othersRecyclerView);

        // Setează layout managers
        friendsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // Grid cu 3 coloane pentru poziționarea celor 3 de pe podium
        top3RecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        othersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Inițializează adaptoarele
        friendsAdapter = new FriendsAdapter(new ArrayList<>());
        top3Adapter = new LeaderboardAdapter(new ArrayList<>(), true); // true pentru view-ul de podium
        othersAdapter = new LeaderboardAdapter(new ArrayList<>(), false);

        // Setează adaptoarele pentru RecyclerView-uri
        friendsRecyclerView.setAdapter(friendsAdapter);
        top3RecyclerView.setAdapter(top3Adapter);
        othersRecyclerView.setAdapter(othersAdapter);

        btnAddFriend.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), SearchFriendsActivity.class);
            startActivity(intent);
        });

        btnNotifications.setOnClickListener(v -> showNotifications());

        // Set click listener for the ObjectionsActivity button
        btnObjections.setOnClickListener(v -> launchCircleChatActivity());

        // Încarcă datele
        loadFriendsProgress();
        loadDailyTasks();

        return view;
    }

    // Method to launch the ObjectionsActivity
    private void launchCircleChatActivity() {
        FirebaseFirestore.getInstance()
                .collection("circles")
                .whereArrayContains("members", FirebaseAuth.getInstance().getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String circleId = queryDocumentSnapshots.getDocuments().get(0).getId();
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
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot circleDoc = queryDocumentSnapshots.getDocuments().get(0);
                        Circle circle = circleDoc.toObject(Circle.class);
                        if (circle != null) {
                            List<String> members = circle.getMembers();
                            List<Friend> dailyFriendsList = new ArrayList<>();

                            for (String friendId : members) {
                                db.collection("users")
                                        .document(friendId)
                                        .get()
                                        .addOnSuccessListener(documentSnapshot -> {
                                            if (documentSnapshot.exists()) {
                                                String friendName = documentSnapshot.getString("username");

                                                db.collection("tasks")
                                                        .whereEqualTo("userId", friendId)
                                                        .whereGreaterThanOrEqualTo("dueDate", startTimestamp)
                                                        .whereLessThanOrEqualTo("dueDate", endTimestamp)
                                                        .get()
                                                        .addOnSuccessListener(taskSnapshots -> {
                                                            int completedTasks = 0;
                                                            int totalTasks = taskSnapshots.size();

                                                            for (DocumentSnapshot taskDoc : taskSnapshots) {
                                                                Task task = taskDoc.toObject(Task.class);
                                                                if (task != null && task.isCompleted()) {
                                                                    completedTasks++;
                                                                }
                                                            }

                                                            dailyFriendsList.add(new Friend(friendId, friendName, completedTasks, totalTasks));

                                                            if (dailyFriendsList.size() == members.size()) {
                                                                // Sortează după rata de completare (procent), apoi după numărul absolut
                                                                Collections.sort(dailyFriendsList, (f1, f2) -> {
                                                                    float rate1 = f1.getTotalTasks() > 0 ? (float) f1.getCompletedTasks() / f1.getTotalTasks() : 0;
                                                                    float rate2 = f2.getTotalTasks() > 0 ? (float) f2.getCompletedTasks() / f2.getTotalTasks() : 0;

                                                                    if (Float.compare(rate2, rate1) == 0) {
                                                                        // Dacă procentele sunt egale, sortează după numărul de task-uri completate
                                                                        return Integer.compare(f2.getCompletedTasks(), f1.getCompletedTasks());
                                                                    }
                                                                    return Float.compare(rate2, rate1);
                                                                });

                                                                friendsAdapter.setFriends(dailyFriendsList);
                                                            }
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            Toast.makeText(getContext(), "Failed to load daily tasks for friend", Toast.LENGTH_SHORT).show();
                                                        });
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(getContext(), "Failed to get friend name", Toast.LENGTH_SHORT).show();
                                        });
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to get circle", Toast.LENGTH_SHORT).show();
                });
    }

    public void loadFriendsProgress() {
        db.collection("circles")
                .whereArrayContains("members", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot circleDoc = queryDocumentSnapshots.getDocuments().get(0);
                        Circle circle = circleDoc.toObject(Circle.class);
                        if (circle != null) {
                            List<String> members = circle.getMembers();
                            loadProgressForFriends(members);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to get circle", Toast.LENGTH_SHORT).show();
                });
    }

    public void loadProgressForFriends(List<String> members) {
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

        List<Friend> updatedFriendsList = new ArrayList<>();

        for (String friendId : members) {
            db.collection("users")
                    .document(friendId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String friendName = documentSnapshot.getString("username");

                            db.collection("tasks")
                                    .whereEqualTo("userId", friendId)
                                    .whereGreaterThanOrEqualTo("dueDate", new Timestamp(startOfMonth / 1000, 0))
                                    .whereLessThanOrEqualTo("dueDate", new Timestamp(endOfMonth / 1000, 0))
                                    .get()
                                    .addOnSuccessListener(queryDocumentSnapshots -> {
                                        int completedTasks = 0;
                                        int totalTasks = 0;

                                        for (DocumentSnapshot taskDoc : queryDocumentSnapshots) {
                                            Task task = taskDoc.toObject(Task.class);
                                            if (task != null) {
                                                totalTasks++;
                                                if (task.isCompleted()) {
                                                    completedTasks++;
                                                }
                                            }
                                        }

                                        updatedFriendsList.add(new Friend(friendId, friendName, completedTasks, totalTasks));

                                        if (updatedFriendsList.size() == members.size()) {
                                            // Sortează lista după numărul de task-uri completate
                                            Collections.sort(updatedFriendsList, (f1, f2) -> Integer.compare(f2.getCompletedTasks(), f1.getCompletedTasks()));

                                            // Actualizează lista prietenilor pentru progresul zilnic
                                            friendsAdapter = new FriendsAdapter(updatedFriendsList);
                                            friendsRecyclerView.setAdapter(friendsAdapter);

                                            // Actualizează leaderboard-ul
                                            updateLeaderboard(updatedFriendsList);
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(getContext(), "Failed to load tasks for friend", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to get friend name", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void updateLeaderboard(List<Friend> allFriends) {
        if (allFriends.size() > 0) {
            List<Friend> top3;
            List<Friend> others;

            if (allFriends.size() <= 3) {
                top3 = new ArrayList<>(allFriends);
                others = new ArrayList<>();
            } else {
                top3 = new ArrayList<>(allFriends.subList(0, 3));
                others = new ArrayList<>(allFriends.subList(3, allFriends.size()));
            }

            if (top3.size() >= 2) {
                Friend first = top3.get(0);  // Locul 1
                Friend second = top3.get(1); // Locul 2

                // Rearanjează pentru a se potrivi cu aranjamentul vizual al podiumului
                top3.set(0, second);  // Pune locul 2 pe prima poziție
                top3.set(1, first);   // Pune locul 1 pe a doua poziție
            }

            // Actualizează RecyclerView-urile
            top3Adapter.setFriends(top3);
            othersAdapter.setFriends(others);
        }
    }

    private void showNotifications() {
        Intent intent = new Intent(getActivity(), NotificationsActivity.class);
        startActivity(intent);
    }

    private void loadUserData(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("username");
                        if (username != null) {
                            userName.setText(username);
                        } else {
                            userName.setText("Your username");
                        }
                        String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            Glide.with(FriendsFragment.this)
                                    .load(profileImageUrl)
                                    .circleCrop()
                                    .into(userProfileImage);
                        }
                    }
                });
    }
}
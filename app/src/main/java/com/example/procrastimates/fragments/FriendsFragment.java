package com.example.procrastimates.fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.example.procrastimates.Circle;
import com.example.procrastimates.Friend;
import com.example.procrastimates.FriendsAdapter;
import com.example.procrastimates.Invitation;
import com.example.procrastimates.InvitationStatus;
import com.example.procrastimates.LeaderboardAdapter;
import com.example.procrastimates.R;
import com.example.procrastimates.Task;
import com.example.procrastimates.activities.NotificationsActivity;
import com.example.procrastimates.activities.SearchFriendsActivity;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class FriendsFragment extends Fragment {
    private Button btnAddFriend;
    private ImageButton btnNotifications;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserId;
    private RecyclerView recyclerView, top3RecyclerView, othersRecyclerView;
    private TextView userName;
    private FriendsAdapter friendsAdapter;
    private List<Friend> friendsList = new ArrayList<>();
    private LeaderboardAdapter top3Adapter, othersAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friends, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        currentUserId = mAuth.getCurrentUser().getUid();
        userName = view.findViewById(R.id.userName);
        loadUserData(currentUserId);

        btnNotifications = view.findViewById(R.id.btnNotifications);
        btnAddFriend = view.findViewById(R.id.btnAddFriend);

        // Inițializează RecyclerView-ul și adapter-ul
        recyclerView = view.findViewById(R.id.friendsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        friendsAdapter = new FriendsAdapter(friendsList);
        recyclerView.setAdapter(friendsAdapter); // Setează adapter-ul pentru RecyclerView
        top3RecyclerView = view.findViewById(R.id.top3RecyclerView);
        othersRecyclerView = view.findViewById(R.id.othersRecyclerView);

        top3RecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        othersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        top3Adapter = new LeaderboardAdapter(new ArrayList<>());
        othersAdapter = new LeaderboardAdapter(new ArrayList<>());


        btnAddFriend.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), SearchFriendsActivity.class);
            startActivity(intent);
        });

        btnNotifications.setOnClickListener(v -> showNotifications());
        loadFriendsProgress(); // Încarcă progresul prietenilor

        return view;
    }

    public void loadFriendsProgress() {
        db.collection("circles")
                .whereArrayContains("members", currentUserId) // Verifică dacă utilizatorul curent este în cerc
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot circleDoc = queryDocumentSnapshots.getDocuments().get(0);
                        Circle circle = circleDoc.toObject(Circle.class);
                        if (circle != null) {
                            List<String> members = circle.getMembers();
                            loadProgressForFriends(members); // Încarcă progresul tuturor membrilor
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

        // Folosește o listă temporară pentru a adăuga toți prietenii
        List<Friend> updatedFriendsList = new ArrayList<>();

        for (String friendId : members) {
            db.collection("users")
                    .document(friendId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String friendName = documentSnapshot.getString("username");

                            // Filtrează task-urile din luna curentă
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

                                        // Adaugă prietenul în lista de prieteni
                                        updatedFriendsList.add(new Friend(friendId, friendName, completedTasks, totalTasks));

                                        // După ce s-au adăugat toți prietenii, sortează și separă
                                        if (updatedFriendsList.size() == members.size()) {
                                            Collections.sort(updatedFriendsList, (f1, f2) -> Integer.compare(f2.getCompletedTasks(), f1.getCompletedTasks()));
                                            List<Friend> top3 = updatedFriendsList.subList(0, Math.min(3, updatedFriendsList.size()));
                                            List<Friend> others = updatedFriendsList.size() > 3 ? updatedFriendsList.subList(3, updatedFriendsList.size()) : new ArrayList<>();

                                            // Actualizează UI cu RecyclerView-uri
                                            top3Adapter.setFriends(top3);
                                            othersAdapter.setFriends(others);
                                            top3RecyclerView.setAdapter(top3Adapter);
                                            othersRecyclerView.setAdapter(othersAdapter);
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
                    }
                });
    }
}
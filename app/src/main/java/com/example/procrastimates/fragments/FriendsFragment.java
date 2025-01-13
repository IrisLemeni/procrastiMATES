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
import android.widget.Toast;

import com.example.procrastimates.Circle;
import com.example.procrastimates.Friend;
import com.example.procrastimates.FriendsAdapter;
import com.example.procrastimates.Invitation;
import com.example.procrastimates.InvitationStatus;
import com.example.procrastimates.R;
import com.example.procrastimates.Task;
import com.example.procrastimates.activities.NotificationsActivity;
import com.example.procrastimates.activities.SearchFriendsActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FriendsFragment extends Fragment {
    private Button btnAddFriend;
    private ImageButton btnNotifications;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserId;
    private RecyclerView recyclerView;
    private FriendsAdapter friendsAdapter;
    private List<Friend> friendsList = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friends, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        currentUserId = mAuth.getCurrentUser().getUid(); // Get the current user's ID

        btnNotifications = view.findViewById(R.id.btnNotifications);
        btnAddFriend = view.findViewById(R.id.btnAddFriend);

        // Inițializează RecyclerView-ul și adapter-ul
        recyclerView = view.findViewById(R.id.friendsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        friendsAdapter = new FriendsAdapter(friendsList);
        recyclerView.setAdapter(friendsAdapter); // Setează adapter-ul pentru RecyclerView

        btnAddFriend.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), SearchFriendsActivity.class);
            startActivity(intent);
        });

        btnNotifications.setOnClickListener(v -> showNotifications());
        loadFriendsProgress(); // Încarcă progresul prietenilor

        return view;
    }

    public void loadFriendsProgress() {
        // Obține cercul curent al utilizatorului
        db.collection("circles")
                .whereEqualTo("userId", currentUserId)
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
        for (String friendId : members) {
            if (friendId.equals(mAuth.getCurrentUser().getUid())) continue;

            db.collection("users")
                    .document(friendId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String friendName = documentSnapshot.getString("username");

                            // Apoi, obține task-urile prietenului
                            db.collection("tasks")
                                    .whereEqualTo("userId", friendId)
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

                                        // Adaugă prietenul și numele său în lista de prieteni
                                        friendsList.add(new Friend(friendId, friendName, completedTasks, totalTasks));

                                        // Actualizează adapter-ul
                                        friendsAdapter.notifyDataSetChanged(); // Actualizează RecyclerView-ul
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
}
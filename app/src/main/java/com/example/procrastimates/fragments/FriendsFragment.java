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

import com.example.procrastimates.FriendsAdapter;
import com.example.procrastimates.Invitation;
import com.example.procrastimates.InvitationStatus;
import com.example.procrastimates.R;
import com.example.procrastimates.activities.NotificationsActivity;
import com.example.procrastimates.activities.SearchFriendsActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class FriendsFragment extends Fragment {
    private Button btnAddFriend;
    private ImageButton btnNotifications;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friends, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        currentUserId = mAuth.getCurrentUser().getUid(); // Get the current user's ID

        btnNotifications = view.findViewById(R.id.btnNotifications);
        btnAddFriend = view.findViewById(R.id.btnAddFriend);
        RecyclerView friendsRecyclerView = view.findViewById(R.id.friendsRecyclerView);
        FriendsAdapter adapter = new FriendsAdapter(friendsList);
        friendsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        friendsRecyclerView.setAdapter(adapter);


        btnAddFriend.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), SearchFriendsActivity.class);
            startActivity(intent);
        });

        btnNotifications.setOnClickListener(v -> showNotifications());

        return view;
    }

    private void showNotifications() {
        Intent intent = new Intent(getActivity(), NotificationsActivity.class);
        startActivity(intent);
    }
}
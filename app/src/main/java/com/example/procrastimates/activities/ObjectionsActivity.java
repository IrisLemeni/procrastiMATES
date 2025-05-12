package com.example.procrastimates.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.procrastimates.Objection;
import com.example.procrastimates.R;
import com.example.procrastimates.ObjectionsAdapter;
import com.example.procrastimates.RecentTasksAdapter;
import com.example.procrastimates.ObjectionRepository;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObjectionsActivity extends AppCompatActivity implements
        RecentTasksAdapter.OnObjectionClickListener,
        ObjectionsAdapter.OnObjectionInteractionListener {

    private static final int PICK_IMAGE_REQUEST = 1;

    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvEmptyState;
    private Button btnBackToHome;

    private RecentTasksAdapter recentTasksAdapter;
    private ObjectionsAdapter objectionsAdapter;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ObjectionRepository objectionRepository;

    private String currentUserId;
    private String currentCircleId;
    private Map<String, String> usernames;

    private String selectedObjectionId; // For keeping track of which objection to upload proof for

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_objections);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        objectionRepository = ObjectionRepository.getInstance();

        currentUserId = mAuth.getCurrentUser().getUid();
        usernames = new HashMap<>();

        // Find current circle
        findUserCircle();

        // Initialize views
        tabLayout = findViewById(R.id.tabLayout);
        recyclerView = findViewById(R.id.recyclerView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        btnBackToHome = findViewById(R.id.btnBackToHome);

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize adapters
        recentTasksAdapter = new RecentTasksAdapter(this, this);
        objectionsAdapter = new ObjectionsAdapter(this, this);

        // Set default adapter
        recyclerView.setAdapter(recentTasksAdapter);

        // Set up tab selection listener
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                updateContent(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Set up swipe refresh
        swipeRefreshLayout.setOnRefreshListener(this::refreshContent);

        // Set up back button
        btnBackToHome.setOnClickListener(v -> finish());

        // Load initial data
        loadInitialData();
    }

    private void findUserCircle() {
        db.collection("circles")
                .whereArrayContains("members", currentUserId)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        currentCircleId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        loadInitialData();
                    } else {
                        showEmptyState("You are not part of any circle yet");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to find your circle", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadInitialData() {
        if (currentCircleId == null) {
            return; // Wait for circle ID to be loaded
        }

        // Show loading indicator
        swipeRefreshLayout.setRefreshing(true);

        // Load data based on current tab
        updateContent(tabLayout.getSelectedTabPosition());
    }

    private void refreshContent() {
        loadInitialData();
    }

    private void updateContent(int tabPosition) {
        if (currentCircleId == null) {
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        switch (tabPosition) {
            case 0: // Recent tasks tab
                loadRecentTasks();
                break;
            case 1: // My objections tab
                loadUserObjections();
                break;
            case 2: // Circle objections tab
                loadCircleObjections();
                break;
        }
    }

    private void loadRecentTasks() {
        recyclerView.setAdapter(recentTasksAdapter);

        objectionRepository.getRecentlyCompletedTasks(currentCircleId, currentUserId,
                new ObjectionRepository.OnObjectionActionListener() {
                    @Override
                    public void onSuccess(Object result) {
                        swipeRefreshLayout.setRefreshing(false);

                        @SuppressWarnings("unchecked")
                        List<ObjectionRepository.TaskWithUser> tasks = (List<ObjectionRepository.TaskWithUser>) result;

                        if (tasks.isEmpty()) {
                            showEmptyState("No recent completed tasks");
                        } else {
                            hideEmptyState();
                            recentTasksAdapter.setTasks(tasks);
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(ObjectionsActivity.this,
                                "Failed to load recent tasks: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        showEmptyState("Failed to load recent tasks");
                    }
                });
    }

    private void loadUserObjections() {
        recyclerView.setAdapter(objectionsAdapter);

        // Load objections raised by or against the current user
        objectionRepository.getObjectionsForUser(currentUserId, true,
                new ObjectionRepository.OnObjectionActionListener() {
                    @Override
                    public void onSuccess(Object result) {
                        @SuppressWarnings("unchecked")
                        List<Objection> objections = (List<Objection>) result;

                        objectionRepository.getObjectionsForUser(currentUserId, false,
                                new ObjectionRepository.OnObjectionActionListener() {
                                    @Override
                                    public void onSuccess(Object innerResult) {
                                        swipeRefreshLayout.setRefreshing(false);

                                        @SuppressWarnings("unchecked")
                                        List<Objection> raisedObjections = (List<Objection>) innerResult;

                                        // Combine both lists
                                        List<Objection> allObjections = new ArrayList<>(objections);
                                        allObjections.addAll(raisedObjections);

                                        if (allObjections.isEmpty()) {
                                            showEmptyState("No objections yet");
                                        } else {
                                            hideEmptyState();
                                            objectionsAdapter.setObjections(allObjections);
                                            loadUsernames(allObjections);
                                        }
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        swipeRefreshLayout.setRefreshing(false);
                                        Toast.makeText(ObjectionsActivity.this,
                                                "Failed to load objections: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(ObjectionsActivity.this,
                                "Failed to load objections: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        showEmptyState("Failed to load objections");
                    }
                });
    }

    private void loadCircleObjections() {
        recyclerView.setAdapter(objectionsAdapter);

        objectionRepository.getObjectionsForCircle(currentCircleId,
                new ObjectionRepository.OnObjectionActionListener() {
                    @Override
                    public void onSuccess(Object result) {
                        swipeRefreshLayout.setRefreshing(false);

                        @SuppressWarnings("unchecked")
                        List<Objection> objections = (List<Objection>) result;

                        if (objections.isEmpty()) {
                            showEmptyState("No objections in your circle yet");
                        } else {
                            hideEmptyState();
                            objectionsAdapter.setObjections(objections);
                            loadUsernames(objections);
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(ObjectionsActivity.this,
                                "Failed to load circle objections: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        showEmptyState("Failed to load objections");
                    }
                });
    }

    private void loadUsernames(List<Objection> objections) {
        // Load usernames for all users involved in objections
        java.util.Set<String> userIds = new java.util.HashSet<>();

        for (Objection objection : objections) {
            userIds.add(objection.getTargetUserId());
            userIds.add(objection.getObjectorUserId());
        }

        for (String userId : userIds) {
            if (!usernames.containsKey(userId)) {
                db.collection("users").document(userId).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String username = documentSnapshot.getString("username");
                                if (username != null) {
                                    usernames.put(userId, username);
                                    objectionsAdapter.setUsername(userId, username);
                                }
                            }
                        });
            }
        }
    }

    private void showEmptyState(String message) {
        tvEmptyState.setText(message);
        tvEmptyState.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        tvEmptyState.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onRaiseObjectionClick(ObjectionRepository.TaskWithUser task) {
        // Check if user can raise an objection today
        objectionRepository.canUserRaiseObjection(currentUserId,
                new ObjectionRepository.OnObjectionActionListener() {
                    @Override
                    public void onSuccess(Object result) {
                        boolean canRaiseObjection = (boolean) result;

                        if (canRaiseObjection) {
                            createObjection(task);
                        } else {
                            Toast.makeText(ObjectionsActivity.this,
                                    "You've already used your objection for today",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(ObjectionsActivity.this,
                                "Failed to check objection status: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createObjection(ObjectionRepository.TaskWithUser task) {
        objectionRepository.raiseObjection(
                task.getTaskId(),
                task.getTitle(),
                task.getUserId(),
                currentUserId,
                currentCircleId,
                new ObjectionRepository.OnObjectionActionListener() {
                    @Override
                    public void onSuccess(Object result) {
                        Toast.makeText(ObjectionsActivity.this,
                                "Objection raised successfully",
                                Toast.LENGTH_SHORT).show();

                        // Switch to objections tab
                        tabLayout.selectTab(tabLayout.getTabAt(1));
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(ObjectionsActivity.this,
                                "Failed to raise objection: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onUploadProofClick(Objection objection) {
        selectedObjectionId = objection.getObjectionId();

        // Open gallery to select image
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            uploadProof(imageUri);
        }
    }

    private void uploadProof(Uri imageUri) {
        if (selectedObjectionId == null) {
            Toast.makeText(this, "No objection selected", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Uploading proof...", Toast.LENGTH_SHORT).show();

        objectionRepository.uploadProofImage(imageUri, selectedObjectionId,
                new ObjectionRepository.OnObjectionActionListener() {
                    @Override
                    public void onSuccess(Object result) {
                        Toast.makeText(ObjectionsActivity.this,
                                "Proof uploaded successfully",
                                Toast.LENGTH_SHORT).show();

                        // Refresh objections
                        updateContent(tabLayout.getSelectedTabPosition());
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(ObjectionsActivity.this,
                                "Failed to upload proof: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });

        selectedObjectionId = null;
    }
}
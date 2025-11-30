package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView; // Import TextView
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class I_Profile_UploadContent extends Fragment {

    private RecyclerView recyclerView;
    private I_ProfileUploadContent_GridAdapter adapter;
    // CHANGED: List of Objects, not Strings
    private List<I_NewPost_Event> postList;
    private DatabaseReference databaseReference;

    // Define TextViews for stats
    private TextView postsNum, followersNum, followingsNum;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.i2_upload_content, container, false);

        // Initialize TextViews
        postsNum = rootView.findViewById(R.id.posts_num);
        followersNum = rootView.findViewById(R.id.followers_num);
        followingsNum = rootView.findViewById(R.id.followings_num);

        // Fetch User Stats
        fetchUserStats();

        recyclerView = rootView.findViewById(R.id.favcontentRecyclerView);

        if (recyclerView != null && getContext() != null) {
            recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));

            // Initialize List
            postList = new ArrayList<>();

            // Pass the list of Objects to the Adapter
            adapter = new I_ProfileUploadContent_GridAdapter(getContext(), postList);

            // Handle clicks (Updated to match new Interface)
            adapter.setOnItemClickListener(new I_ProfileUploadContent_GridAdapter.OnItemClickListener() {
                @Override
                public void onAddClick() {
                    // Go to New Post Activity
                    Intent intent = new Intent(getActivity(), I_NewPost_UploadActivity.class);
                    startActivity(intent);
                }

                @Override
                public void onImageClick(I_NewPost_Event post, int position) {
                    // Navigation is handled inside the Adapter's onBindViewHolder now
                    // so this can remain empty or be used for logging.
                }
            });

            recyclerView.setAdapter(adapter);
            fetchUserImages();
        }

        return rootView;
    }

    private void fetchUserStats() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);

        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Get values as Objects first to handle both Integer and String types safely
                    Object postsObj = snapshot.child("posts").getValue();
                    Object followersObj = snapshot.child("followers").getValue();
                    Object followingObj = snapshot.child("following").getValue();

                    // Convert to String or default to "0"
                    String postsCount = (postsObj != null) ? String.valueOf(postsObj) : "0";
                    String followersCount = (followersObj != null) ? String.valueOf(followersObj) : "0";
                    String followingCount = (followingObj != null) ? String.valueOf(followingObj) : "0";

                    // Update UI
                    if (postsNum != null) postsNum.setText(postsCount);
                    if (followersNum != null) followersNum.setText(followersCount);
                    if (followingsNum != null) followingsNum.setText(followingCount);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle potential errors if needed
            }
        });
    }

    private void fetchUserImages() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String currentUserId = currentUser.getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference("PostEvents");

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (postList == null) return;
                postList.clear();

                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    // Get the raw User ID string to check ownership first
                    String userId = postSnapshot.child("userId").getValue(String.class);

                    if (userId != null && userId.equals(currentUserId)) {
                        // Convert snapshot to Object
                        I_NewPost_Event post = postSnapshot.getValue(I_NewPost_Event.class);

                        if (post != null) {
                            // --- CRITICAL FIX ---
                            // Firebase doesn't store the key inside the object by default.
                            // We must manually set it so the Adapter can send it later.
                            post.setPostId(postSnapshot.getKey());

                            // Only add if it has images
                            if (post.getImageUrls() != null && !post.getImageUrls().isEmpty()) {
                                postList.add(post);
                            }
                        }
                    }
                }

                Collections.reverse(postList); // Show newest first

                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error loading images", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}

package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class D1_FeedActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private D_FeedAdapter postAdapter;
    private List<I_PostUpload_Event> postList;
    private DatabaseReference postsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.d1_feed); // Ensure your layout is correct

        // Initialize the RecyclerView
        recyclerView = findViewById(R.id.feedrecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize the list and adapter
        postList = new ArrayList<>();
        postAdapter = new D_FeedAdapter(this, postList);
        recyclerView.setAdapter(postAdapter);

        // Initialize Firebase reference
        postsRef = FirebaseDatabase.getInstance().getReference("PostEvents");

        // Fetch the posts from Firebase
        fetchPostsFromFirebase();
    }

    // Fetch data from Firebase and populate the RecyclerView
    private void fetchPostsFromFirebase() {
        // Reference to the posts in Firebase (all posts, not user-specific)
        DatabaseReference postsRef = FirebaseDatabase.getInstance().getReference("PostEvents");

        postsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                postList.clear(); // Clear existing posts

                // Iterate through each post in the snapshot
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    I_PostUpload_Event postEvent = postSnapshot.getValue(I_PostUpload_Event.class);

                    if (postEvent != null) {
                        postList.add(postEvent);  // Add post to list
                    }
                }

                // Notify the adapter to update the RecyclerView
                postAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle error
                Toast.makeText(D1_FeedActivity.this, "Failed to load posts", Toast.LENGTH_SHORT).show();
            }
        });
    }


    public void onButtonClicked(View view) {
        Intent intent = null;
        int viewId = view.getId();

        if (viewId == R.id.home_menu) {
            intent = new Intent(this, D1_FeedActivity.class);
        } else if (viewId == R.id.calendar_menu) {
            return;
        } else if (viewId == R.id.camera_menu) {
            intent = new Intent(this, F1_CameraActivity.class);
        } else if (viewId == R.id.closet_menu) {
            intent = new Intent(this, G1_ClosetActivity.class);
        } else if (viewId == R.id.profile_menu) {
            intent = new Intent(this, I1_ProfileActivity.class);
        }

        if (intent != null) {
            startActivity(intent);
            finish();
        }
    }
}
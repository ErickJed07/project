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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

                // Sort the postList by postDate field (most recent first)
                sortPostsByDate();

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

    // Method to sort posts by their date (most recent first)
    private void sortPostsByDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);

        // Sort posts by postDate
        Collections.sort(postList, (post1, post2) -> {
            try {
                // Parse the postDate from each post
                Date date1 = dateFormat.parse(post1.getDate());
                Date date2 = dateFormat.parse(post2.getDate());

                // Compare the dates: most recent first
                return date2.compareTo(date1);
            } catch (Exception e) {
                e.printStackTrace();
                return 0; // If parsing fails, no sorting
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
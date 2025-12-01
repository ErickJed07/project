package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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
    private List<I_NewPost_Event> postList;
    private DatabaseReference postsRef;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.d1_feed);

        // Initialize SwipeRefreshLayout
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        recyclerView = findViewById(R.id.feedrecyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        postList = new ArrayList<>();
        postAdapter = new D_FeedAdapter(this, postList);
        recyclerView.setAdapter(postAdapter);

        // --- REMOVED THE MANUAL SCROLL LISTENER ---
        // Modern SwipeRefreshLayout automatically detects when RecyclerView is at the top.
        // Manually enabling/disabling it causes the "sticky" scroll feeling.

        postsRef = FirebaseDatabase.getInstance().getReference("PostEvents");

        // Set up the Refresh Listener
        swipeRefreshLayout.setOnRefreshListener(() -> {
            fetchPostsFromFirebase();
        });

        // Optional: Set a distance to trigger to avoid accidental refreshes
        // This requires a slightly longer pull to activate
        swipeRefreshLayout.setDistanceToTriggerSync(300);

        fetchPostsFromFirebase();
    }

    private void fetchPostsFromFirebase() {
        postsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                postList.clear();

                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    I_NewPost_Event postEvent = postSnapshot.getValue(I_NewPost_Event.class);
                    if (postEvent != null) {
                        postList.add(postEvent);
                    }
                }

                sortPostsByDate();
                postAdapter.notifyDataSetChanged();

                // Stop the refreshing animation when data loads
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(D1_FeedActivity.this, "Failed to load posts", Toast.LENGTH_SHORT).show();

                // Stop animation even if it fails
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        });
    }

    private void sortPostsByDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);

        Collections.sort(postList, (post1, post2) -> {
            try {
                Date date1 = dateFormat.parse(post1.getDate());
                Date date2 = dateFormat.parse(post2.getDate());
                return date2.compareTo(date1);
            } catch (Exception e) {
                e.printStackTrace();
                return 0;
            }
        });
    }

    public void onButtonClicked(View view) {
        Intent intent = null;
        int viewId = view.getId();

        if (viewId == R.id.home_menu) {
            intent = new Intent(this, D1_FeedActivity.class);
        } else if (viewId == R.id.calendar_menu) {
            intent = new Intent(this, E1_CalendarActivity.class);
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

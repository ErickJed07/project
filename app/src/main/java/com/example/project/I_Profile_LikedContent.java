package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

public class I_Profile_LikedContent extends Fragment {

    private RecyclerView recyclerView;
    private I_ProfileLikedContent_GridAdapter gridAdapter;
    private List<I_NewPost_Event> likedPostList;
    private FirebaseUser firebaseUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.i3_liked_content, container, false);

        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.likedContentRecyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));

        // Initialize List
        likedPostList = new ArrayList<>();


        // Initialize the correct Adapter with the click listener
        gridAdapter = new I_ProfileLikedContent_GridAdapter(getContext(), likedPostList, new I_ProfileLikedContent_GridAdapter.OnItemClickListener() {
            @Override
            public void onImageClick(I_NewPost_Event post, int position) {
                Intent intent = new Intent(getContext(), I_ProfileLikedContent_GridViewer.class);
                intent.putExtra("POST_ID", post.getPostId());
                startActivity(intent);
            }
        }); // <--- ADD THIS LINE (closing brace, parenthesis, and semicolon)

        recyclerView.setAdapter(gridAdapter);


        // Get current user
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        // Fetch the posts
        readLikedPosts();

        return view;
    }

    private void readLikedPosts() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("PostEvents");

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                likedPostList.clear();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    I_NewPost_Event post = snapshot.getValue(I_NewPost_Event.class);

                    if (isValidPost(post, snapshot.getKey())) {

                        // 1. Check if current user Liked the post (ID is in heartLiked map)
                        boolean isLiked = isLikedByCurrentUser(post);

                        // 2. Check if current user Created the post (UserID matches current user)
                        boolean isCreator = isCreatedByCurrentUser(post);

                        // 3. Enforce the rule:
                        // "if userid is same id as heartliked true (isCreator), don't show it"
                        // Therefore: Only show if Liked is TRUE and Creator is FALSE
                        if (isLiked && !isCreator) {
                            likedPostList.add(post);
                        }
                    }
                }

                Collections.reverse(likedPostList);
                gridAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("LikedContent", "Database error: " + databaseError.getMessage());
            }
        });
    }


    // Helper method to validate if the post object and user session exist
    private boolean isValidPost(I_NewPost_Event post, String key) {
        if (post == null || firebaseUser == null) {
            return false;
        }
        post.setPostId(key);
        return true;
    }

    // Helper method to check if the post was created by the current user
    private boolean isCreatedByCurrentUser(I_NewPost_Event post) {
        return post.getUserId() != null && post.getUserId().equals(firebaseUser.getUid());
    }

    // Helper method to check if the post is liked by the current user
    private boolean isLikedByCurrentUser(I_NewPost_Event post) {
        return post.getHeartLiked() != null &&
                post.getHeartLiked().containsKey(firebaseUser.getUid()) &&
                Boolean.TRUE.equals(post.getHeartLiked().get(firebaseUser.getUid()));
    }

}

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

public class I_Profile_FavContent extends Fragment {

    private RecyclerView recyclerView;
    private I_ProfileFavContent_GridAdapter gridAdapter;
    private List<I_NewPost_Event> favPostList;
    private FirebaseUser firebaseUser;

    public I_Profile_FavContent() {
        // Required empty public constructor
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.i4_fav_content, container, false);

        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.favcontentRecyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));

        // Initialize List
        favPostList = new ArrayList<>();

        // Initialize the Adapter
        gridAdapter = new I_ProfileFavContent_GridAdapter(getContext(), favPostList, new I_ProfileFavContent_GridAdapter.OnItemClickListener() {
            @Override
            public void onImageClick(I_NewPost_Event post, int position) {
                // Fixed: Changed intent target from Adapter.class to GridViewer.class
                Intent intent = new Intent(getContext(), I_ProfileFavContent_GridViewer.class);
                intent.putExtra("POST_ID", post.getPostId());
                startActivity(intent);
            }
        });

        recyclerView.setAdapter(gridAdapter);

        // Get current user
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        // Fetch the posts
        readFavPosts();

        return view;
    }

    private void readFavPosts() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("PostEvents");

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                favPostList.clear();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    I_NewPost_Event post = snapshot.getValue(I_NewPost_Event.class);

                    if (isValidPost(post, snapshot.getKey())) {

                        // 1. Check if current user Favorited the post (ID is in favlist map)
                        boolean isFavorited = isFavoritedByCurrentUser(post);

                        // 2. Check if current user Created the post
                        boolean isCreator = isCreatedByCurrentUser(post);

                        // 3. Enforce rule: Show if Favorited is TRUE and Creator is FALSE
                        // (Assuming you want the exact same logic as Liked Content)
                        if (isFavorited && !isCreator) {
                            favPostList.add(post);
                        }
                    }
                }

                Collections.reverse(favPostList);
                gridAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("FavContent", "Database error: " + databaseError.getMessage());
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

    // Helper method to check if the post is favorited by the current user
    private boolean isFavoritedByCurrentUser(I_NewPost_Event post) {
        // logic adapted for favlist
        return post.getFavList() != null &&
                post.getFavList().containsKey(firebaseUser.getUid()) &&
                Boolean.TRUE.equals(post.getFavList().get(firebaseUser.getUid()));
    }
}

package com.example.project;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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

public class I_Profile_AddModels extends AppCompatActivity {

    private SearchView searchView;
    private RecyclerView recyclerView;
    private ImageButton backButton; // Added based on XML
    private List<User> userList;
    private UserAdapter userAdapter;
    private List<String> myFollowingList; // List to store IDs of people I follow

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.i_profile_addmodel);

        // Initialize Views
        searchView = findViewById(R.id.search_view);
        recyclerView = findViewById(R.id.recycler_view_following);
        backButton = findViewById(R.id.imageButton2);

        // Setup Back Button
        backButton.setOnClickListener(v -> finish());

        // Setup RecyclerView
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // List to hold users
        userList = new ArrayList<>();
        myFollowingList = new ArrayList<>();

        // Adapter setup
        userAdapter = new UserAdapter(this, userList);
        recyclerView.setAdapter(userAdapter);

        // 1. First, get the list of people I already follow
        getFollowing();

        // --- SEARCH FUNCTIONALITY ---
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Filter the list inside the adapter
                filterList(newText, userList, userAdapter);
                return true;
            }
        });
    }

    // Method to fetch who I am following first
    private void getFollowing() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) return;

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users")
                .child(firebaseUser.getUid()).child("ModelsList");

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@androidx.annotation.NonNull DataSnapshot snapshot) {
                myFollowingList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    myFollowingList.add(ds.getKey());
                }
                // After getting following list, fetch all users to populate the recycler view
                getAllUsers();
            }

            @Override
            public void onCancelled(@androidx.annotation.NonNull DatabaseError error) {
            }
        });
    }

    // --- FETCH USERS FROM FIREBASE ---
    private void getAllUsers() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");

        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@androidx.annotation.NonNull DataSnapshot snapshot) {
                userList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    User user = ds.getValue(User.class);

                    // FIX: Manually set the ID using the node key
                    if (user != null) {
                        user.setId(ds.getKey());

                        // LOGIC: Remove if already following OR if it's me
                        if (firebaseUser != null && !user.getId().equals(firebaseUser.getUid())) {

                            // ** KEY CHANGE: Only add to list if NOT in myFollowingList **
                            if (!myFollowingList.contains(user.getId())) {
                                userList.add(user);
                            }
                        }
                    }
                }
                userAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@androidx.annotation.NonNull DatabaseError error) {
                // Handle errors
            }
        });
    }

    // --- FIXED FILTER LIST METHOD ---
    private void filterList(String text, List<User> originalList, UserAdapter adapter) {
        // 1. CRITICAL FIX: Check if text is null or empty immediately
        if (text == null || text.trim().isEmpty()) {
            // If the search bar is empty, we MUST restore the original list.
            // We pass 'originalList' (which contains all users) back to the adapter.
            adapter.setFilteredList(originalList);
            return; // Stop here, no need to filter
        }

        // 2. If text exists, perform the filter loop
        List<User> filteredList = new ArrayList<>();
        String searchText = text.toLowerCase().trim();

        for (User item : originalList) {
            if (item.getUsername() != null && item.getUsername().toLowerCase().contains(searchText)) {
                filteredList.add(item);
            }
        }

        // 3. Update the adapter with the results
        adapter.setFilteredList(filteredList);
    }

    // --- USER CLASS ---
    public static class User {
        private String id;
        private String username;
        private String profileImageUrl;

        public User() {}

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getProfileImageUrl() { return profileImageUrl; }
        public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    }


    // --- ADAPTER CLASS ---
    public static class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

        private android.content.Context mContext;
        private List<User> mUsers;
        private FirebaseUser firebaseUser;

        public UserAdapter(android.content.Context mContext, List<User> mUsers) {
            this.mContext = mContext;
            this.mUsers = mUsers;
            this.firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        }

        public void setFilteredList(List<User> filteredList) {
            this.mUsers = filteredList;
            notifyDataSetChanged();
        }

        @androidx.annotation.NonNull
        @Override
        public ViewHolder onCreateViewHolder(@androidx.annotation.NonNull android.view.ViewGroup parent, int viewType) {
            android.view.View view = android.view.LayoutInflater.from(mContext).inflate(R.layout.i_profile_modelfan_user, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@androidx.annotation.NonNull ViewHolder holder, int position) {
            final User user = mUsers.get(position);

            holder.username.setText(user.getUsername());
            holder.btn_follow.setVisibility(View.VISIBLE);
            holder.btn_follow.setText("Follow Model");

            // Load Profile Image if available (Safety check)
            if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                Glide.with(mContext).load(user.getProfileImageUrl()).into(holder.image_profile);
            } else {
                holder.image_profile.setImageResource(R.drawable.profile); // Default placeholder
            }

            // --- NEW LOGIC: CHECK POSTS & LOAD PREVIEWS ---
            checkPostsAndLoadPreviews(user.getId(), holder);


            // Handle Follow Button Click
            holder.btn_follow.setOnClickListener(view -> {
                // 1. ADD to "ModelsList" for You (Following)
                FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid())
                        .child("ModelsList").child(user.getId()).setValue(true);

                // 2. ADD to "FansList" for the Target User (Follower)
                FirebaseDatabase.getInstance().getReference("Users").child(user.getId())
                        .child("FansList").child(firebaseUser.getUid()).setValue(true);

                // 3. UPDATE "Models" COUNT (+1) for You
                updateCount(firebaseUser.getUid(), "Models", 1);

                // 4. UPDATE "Fans" COUNT (+1) for the Target User
                updateCount(user.getId(), "Fans", 1);

                Toast.makeText(mContext, "You are now following " + user.getUsername(), Toast.LENGTH_SHORT).show();
            });
        }

        private void checkPostsAndLoadPreviews(String userId, ViewHolder holder) {
            // 1. Change reference to "PostEvents"
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("PostEvents");

            reference.addListenerForSingleValueEvent(new ValueEventListener() { // Use SingleValueEvent for lists to avoid constant reloading
                @Override
                public void onDataChange(@androidx.annotation.NonNull DataSnapshot snapshot) {
                    List<String> postImages = new ArrayList<>();

                    // 2. Loop through all posts in PostEvents
                    for (DataSnapshot postSnap : snapshot.getChildren()) {

                        // 3. Get the userId of the poster
                        String publisherId = postSnap.child("userId").getValue(String.class);

                        // 4. Check if this post belongs to the user we are looking at
                        if (publisherId != null && publisherId.equals(userId)) {

                            // 5. Handle "imageUrls" - get the first image available for this post
                            DataSnapshot imagesSnap = postSnap.child("imageUrls");
                            if (imagesSnap.exists()) {
                                // Loop through the images (it's a list/map) and take the first one
                                for (DataSnapshot imgSnap : imagesSnap.getChildren()) {
                                    String imgUrl = imgSnap.getValue(String.class);
                                    if (imgUrl != null && !imgUrl.isEmpty()) {
                                        postImages.add(imgUrl);
                                        break; // Found the thumbnail for this post, move to next post
                                    }
                                }
                            }
                        }
                    }

                    // 6. Check count: Show preview only if 4 or more posts exist
                    if (postImages.size() >= 4) {
                        // UPDATED: Visibility is now controlled on the CardView
                        holder.card_post_previews.setVisibility(View.VISIBLE);

                        // Reverse list to show latest posts first
                        Collections.reverse(postImages);

                        if (postImages.size() >= 1) loadImage(holder.post_img_1, postImages.get(0));
                        if (postImages.size() >= 2) loadImage(holder.post_img_2, postImages.get(1));
                        if (postImages.size() >= 3) loadImage(holder.post_img_3, postImages.get(2));
                        if (postImages.size() >= 4) loadImage(holder.post_img_4, postImages.get(3));
                    } else {
                        // UPDATED: Hide the CardView
                        holder.card_post_previews.setVisibility(View.GONE);
                    }

                }

                @Override
                public void onCancelled(@androidx.annotation.NonNull DatabaseError error) { }
            });
        }

        private void loadImage(ImageView imageView, String url) {
            Glide.with(mContext).load(url).into(imageView);
        }

        // Helper to safely increment counts
        private void updateCount(String userId, String field, int increment) {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(userId).child(field);
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@androidx.annotation.NonNull DataSnapshot snapshot) {
                    long currentCount = 0;
                    if (snapshot.exists()) {
                        try {
                            currentCount = Long.parseLong(snapshot.getValue().toString());
                        } catch (Exception e) { currentCount = 0; }
                    }
                    ref.setValue(Math.max(0, currentCount + increment));
                }
                @Override
                public void onCancelled(@androidx.annotation.NonNull DatabaseError error) { }
            });
        }

        @Override
        public int getItemCount() {
            return mUsers.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {

            public TextView username;
            public com.google.android.material.imageview.ShapeableImageView image_profile;
            public android.widget.Button btn_follow;

            // CHANGED: Now referencing the CardView wrapper
            public com.google.android.material.card.MaterialCardView card_post_previews;

            public ImageView post_img_1, post_img_2, post_img_3, post_img_4;

            public ViewHolder(@androidx.annotation.NonNull android.view.View itemView) {
                super(itemView);
                username = itemView.findViewById(R.id.username);
                image_profile = itemView.findViewById(R.id.img_profile);
                btn_follow = itemView.findViewById(R.id.btn_follow);

                // CHANGED: Find the CardView by its new ID
                card_post_previews = itemView.findViewById(R.id.card_post_previews);

                post_img_1 = itemView.findViewById(R.id.post_img_1);
                post_img_2 = itemView.findViewById(R.id.post_img_2);
                post_img_3 = itemView.findViewById(R.id.post_img_3);
                post_img_4 = itemView.findViewById(R.id.post_img_4);
            }
        }


    }
}

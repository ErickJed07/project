package com.example.project;

import android.content.Intent;
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

                    if (user != null) {
                        user.setId(ds.getKey());

                        // --- FIX IS HERE ---
                        // Just get the String URL here. Do not use Glide or circleCrop here.
                        if (ds.child("profilePhoto").exists()) {
                            user.setProfileImageUrl(ds.child("profilePhoto").getValue(String.class));
                        }

                        // Logic: Remove if already following OR if it's me
                        if (firebaseUser != null && !user.getId().equals(firebaseUser.getUid())) {
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
            holder.btn_follow.setText("Follow");

                // --- NEW GLIDE LOGIC HERE ---
                String profilePhotoUrl = user.getProfileImageUrl();

                if (isValidContextForGlide(mContext)) {
                    if (profilePhotoUrl != null && !profilePhotoUrl.isEmpty() && !profilePhotoUrl.equals("default")) {
                        try {
                            Glide.with(mContext)
                                    .load(profilePhotoUrl)
                                    .placeholder(R.drawable.ic_placeholder_2) // Ensure you have this drawable or use R.drawable.profile
                                    .error(R.drawable.ic_placeholder_2)
                                    .circleCrop() // This makes the image round
                                    .into(holder.image_profile);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        // Load default image if URL is missing or "default"
                        Glide.with(mContext)
                                .load(R.drawable.ic_placeholder_2)
                                .circleCrop()
                                .into(holder.image_profile);
                    }
                }

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

            View.OnClickListener profileClick = v -> {
                if (user.getId().equals(firebaseUser.getUid())) {
                    mContext.startActivity(new Intent(mContext, I_ProfileActivity.class));
                } else {
                    Intent intent = new Intent(mContext, I_UserProfileActivity.class);
                    intent.putExtra(I_UserProfileActivity.EXTRA_USER_ID, user.getId());
                    mContext.startActivity(intent);
                }
            };

            holder.itemView.setOnClickListener(profileClick);
            holder.username.setOnClickListener(profileClick);
            holder.image_profile.setOnClickListener(profileClick);
        }

        private void updateCount(String userId, String field, int increment) {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(userId).child(field);
            ref.runTransaction(new com.google.firebase.database.Transaction.Handler() {
                @androidx.annotation.NonNull
                @Override
                public com.google.firebase.database.Transaction.Result doTransaction(@androidx.annotation.NonNull com.google.firebase.database.MutableData mutableData) {
                    Long count = mutableData.getValue(Long.class);
                    if (count == null) {
                        mutableData.setValue(Math.max(0L, (long) increment));
                    } else {
                        mutableData.setValue(Math.max(0L, count + increment));
                    }
                    return com.google.firebase.database.Transaction.success(mutableData);
                }

                @Override
                public void onComplete(@androidx.annotation.Nullable com.google.firebase.database.DatabaseError databaseError, boolean committed, @androidx.annotation.Nullable com.google.firebase.database.DataSnapshot dataSnapshot) {
                }
            });
        }

        @Override
        public int getItemCount() {
            return mUsers.size();
        }

        private boolean isValidContextForGlide(android.content.Context context) {
            if (context == null) return false;
            if (context instanceof android.app.Activity) {
                android.app.Activity activity = (android.app.Activity) context;
                return !activity.isDestroyed() && !activity.isFinishing();
            }
            return true;
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {

            public TextView username;
            public android.widget.Button btn_follow;
            public ImageView image_profile;

            public ViewHolder(@androidx.annotation.NonNull android.view.View itemView) {
                super(itemView);
                username = itemView.findViewById(R.id.username);
                image_profile = itemView.findViewById(R.id.img_profile);
                btn_follow = itemView.findViewById(R.id.btn_follow);
            }
        }


    }
}

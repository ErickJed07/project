package com.example.project;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class I_ProfileUploadContent_GridViewer extends AppCompatActivity {

    private ImageView heartButton, favButton, shareButton; // Added shareButton
    private TextView heartNumTextView, favNumTextView;
    private ImageView backButton; // Added backButton
    private String postId;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.i_profile_postupload_gridview);

        // 1. Setup User
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = (user != null) ? user.getUid() : "";

        // 2. Retrieve the data passed via Intent
        ArrayList<String> imageUrls = getIntent().getStringArrayListExtra("URLS");
        String username = getIntent().getStringExtra("USERNAME");
        String caption = getIntent().getStringExtra("CAPTION");

        // --- CRITICAL FIX ---
        // We get the ID using the exact same key we used in Step 1
        postId = getIntent().getStringExtra("POST_ID");
        // --------------------

        // Debugging Check: If this toast shows up, Step 1 is still wrong.
        if (postId == null) {
            Toast.makeText(this, "ERROR: Post ID is missing from Adapter!", Toast.LENGTH_LONG).show();
        }

        // 3. Create the I_NewPost_Event object for the adapter
        I_NewPost_Event postEvent = new I_NewPost_Event();
        postEvent.setUsername(username);
        postEvent.setCaption(caption);
        postEvent.setImageUrls(imageUrls != null ? imageUrls : new ArrayList<>());

        // 4. Setup ViewPager
        ViewPager2 viewPager = findViewById(R.id.postcontentview2);
        viewPager.setAdapter(new PostEventAdapter(this, postEvent));

        // 5. Setup Buttons
        heartButton = findViewById(R.id.heartPVButton);
        favButton = findViewById(R.id.favPVButton);
        heartNumTextView = findViewById(R.id.heartNumPVTextView);
        favNumTextView = findViewById(R.id.favNumPVTextView);
        shareButton = findViewById(R.id.btnShare);
        backButton = findViewById(R.id.postcontentback_button);

        // 6. Load Data
        if (postId != null && !currentUserId.isEmpty()) {
            loadPostData();
            setupClickListeners();
        }
    }



    // Load Post Data from Firebase (Changed to Single Value Event to prevent conflicts)
    private void loadPostData() {
        DatabaseReference postRef = FirebaseDatabase.getInstance().getReference("PostEvents").child(postId);
        postRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    I_NewPost_Event post = snapshot.getValue(I_NewPost_Event.class);
                    if (post != null) {
                        updateUI(post);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(I_ProfileUploadContent_GridViewer.this, "Failed to load data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Update the UI with Post Data (Heart and Fav counts, etc.)
    private void updateUI(I_NewPost_Event post) {
        // Update Heart UI
        Map<String, Boolean> heartLiked = post.getHeartLiked();
        if (heartLiked == null) heartLiked = new HashMap<>();
        heartNumTextView.setText(String.valueOf(heartLiked.size()));
        updateHeartIcon(heartButton, heartLiked, currentUserId);

        // Update Fav UI
        Map<String, Boolean> favList = post.getFavList();
        if (favList == null) favList = new HashMap<>();
        favNumTextView.setText(String.valueOf(favList.size()));
        updateFavIcon(favButton, favList, currentUserId);
    }

    // Setup click listeners for Heart and Fav buttons
    private void setupClickListeners() {
        heartButton.setOnClickListener(v -> toggleInteraction("Heart"));
        favButton.setOnClickListener(v -> toggleInteraction("Fav"));

        if (backButton != null) {
            backButton.setOnClickListener(v -> finish()); // Close activity on X button
        }

        if (shareButton != null) {
            shareButton.setOnClickListener(v -> {
                // Basic share intent logic
                android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Check out this post!");
                shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Check out this post!"); // You can add link here
                startActivity(android.content.Intent.createChooser(shareIntent, "Share post via"));
            });
        }
    }

    // --- HELPER METHODS ---

    private void updateHeartIcon(ImageView heartButton, Map<String, Boolean> heartLiked, String userId) {
        if (heartLiked != null && heartLiked.containsKey(userId)) {
            heartButton.setImageResource(R.drawable.heart2); // Filled heart
        } else {
            heartButton.setImageResource(R.drawable.heart); // Empty heart
        }
    }

    private void updateFavIcon(ImageView favButton, Map<String, Boolean> favList, String userId) {
        if (favList != null && favList.containsKey(userId)) {
            favButton.setImageResource(R.drawable.fav2); // Filled star
        } else {
            favButton.setImageResource(R.drawable.fav); // Empty star
        }
    }

    // Toggle Like/Heart or Favorite button
    private void toggleInteraction(String type) {
        // 1. Safety Checks
        if (postId == null) {
            Toast.makeText(this, "Error: Post ID not found", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "Please log in to interact", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference postRef = FirebaseDatabase.getInstance().getReference("PostEvents").child(postId);

        // 2. Read current data once
        postRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    I_NewPost_Event post = snapshot.getValue(I_NewPost_Event.class);
                    if (post == null) return;

                    if (type.equals("Heart")) {
                        // --- HEART LOGIC ---
                        Map<String, Boolean> heartLiked = post.getHeartLiked();
                        if (heartLiked == null) heartLiked = new HashMap<>();

                        if (heartLiked.containsKey(currentUserId)) {
                            heartLiked.remove(currentUserId);
                        } else {
                            heartLiked.put(currentUserId, true);
                        }

                        // Update Data Object
                        post.setHeartLiked(heartLiked);
                        post.setHeartCount(heartLiked.size());

                        // Save to Firebase
                        postRef.setValue(post);

                        // Update UI Immediately
                        heartNumTextView.setText(String.valueOf(heartLiked.size()));
                        updateHeartIcon(heartButton, heartLiked, currentUserId);

                    } else if (type.equals("Fav")) {
                        // --- FAV LOGIC ---
                        Map<String, Boolean> favList = post.getFavList();
                        if (favList == null) favList = new HashMap<>();

                        if (favList.containsKey(currentUserId)) {
                            favList.remove(currentUserId);
                            Toast.makeText(I_ProfileUploadContent_GridViewer.this, "Removed from favorites", Toast.LENGTH_SHORT).show();
                        } else {
                            favList.put(currentUserId, true);
                            Toast.makeText(I_ProfileUploadContent_GridViewer.this, "Saved to favorites", Toast.LENGTH_SHORT).show();
                        }

                        // Update Data Object
                        post.setFavList(favList);
                        post.setFavCount(favList.size());

                        // Save to Firebase
                        postRef.setValue(post);

                        // Update UI Immediately
                        favNumTextView.setText(String.valueOf(favList.size()));
                        updateFavIcon(favButton, favList, currentUserId);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(I_ProfileUploadContent_GridViewer.this, "Action failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- SIMPLIFIED ADAPTER FOR IMAGES ONLY ---
    private static class PostEventAdapter extends RecyclerView.Adapter<PostEventAdapter.ViewHolder> {
        private final I_NewPost_Event postEvent;
        private final Context context;

        PostEventAdapter(Context context, I_NewPost_Event postEvent) {
            this.context = context;
            this.postEvent = postEvent;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView imageView = new ImageView(context);
            imageView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            return new ViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (postEvent.getImageUrls() != null && position < postEvent.getImageUrls().size()) {
                String imageUrl = postEvent.getImageUrls().get(position);
                Glide.with(context)
                        .load(imageUrl)
                        .into(holder.imageView);
            }
        }

        @Override
        public int getItemCount() {
            return (postEvent.getImageUrls() != null) ? postEvent.getImageUrls().size() : 0;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final ImageView imageView;

            ViewHolder(View itemView) {
                super(itemView);
                this.imageView = (ImageView) itemView;
            }
        }
    }
}

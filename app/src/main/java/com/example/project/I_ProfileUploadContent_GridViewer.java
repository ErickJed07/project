package com.example.project;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
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
import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator;

import java.util.ArrayList;
import java.util.Collections; // Imported for reversing the list
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class I_ProfileUploadContent_GridViewer extends AppCompatActivity {

    private ViewPager2 mainVerticalViewPager;
    private VerticalPostAdapter verticalAdapter;

    private List<I_NewPost_Event> allUserPosts = new ArrayList<>();
    private List<String> allUserPostIds = new ArrayList<>();

    private ImageView backButton;
    private String startPostId;
    private String currentUserId;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.i_profile_contents_gridview);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = (user != null) ? user.getUid() : "";

        startPostId = getIntent().getStringExtra("POST_ID");

        backButton = findViewById(R.id.postcontentback_button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        // 3. Setup ViewPager for Vertical Scrolling
        mainVerticalViewPager = findViewById(R.id.postcontentview2);

        // NULL CHECK: Prevents the crash if the View ID is missing or wrong
        if (mainVerticalViewPager != null) {
            mainVerticalViewPager.setOrientation(ViewPager2.ORIENTATION_VERTICAL);

            // --- FIX: UPDATED TRANSFORMER FOR NATURAL SCROLLING ---
            mainVerticalViewPager.setPageTransformer((page, position) -> {
                // position < -1 : Page is off-screen to the top
                // position = 0  : Page is centered (visible)
                // position > 1  : Page is off-screen to the bottom

                if (position < -1) {
                    page.setAlpha(0f);
                } else if (position <= 1) {
                    // This is the visible page or the one entering/leaving

                    // 1. Reset any rotation/scaling that might feel "flipped"
                    page.setAlpha(1f);
                    page.setTranslationY(0f);
                    page.setTranslationX(0f);
                    page.setScaleX(1f);
                    page.setScaleY(1f);

                    // 2. Optional: Simple Fade Effect (Standard)
                    // As the page moves up (position goes negative), it fades out slightly
                    // As the page moves down (position goes positive), it fades out slightly
                    float minAlpha = 0.5f;
                    float alpha = Math.max(minAlpha, 1 - Math.abs(position));
                    page.setAlpha(alpha);

                } else {
                    page.setAlpha(0f);
                }
            });


            verticalAdapter = new VerticalPostAdapter(this, allUserPosts, allUserPostIds, currentUserId);
            mainVerticalViewPager.setAdapter(verticalAdapter);
        }

        if (!currentUserId.isEmpty()) {
            loadAllUserPosts();
        }
    }

    private void loadAllUserPosts() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("PostEvents");

        ref.orderByChild("userId").equalTo(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allUserPosts.clear();
                allUserPostIds.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    I_NewPost_Event post = ds.getValue(I_NewPost_Event.class);
                    if (post != null) {
                        allUserPosts.add(post);
                        allUserPostIds.add(ds.getKey());
                    }
                }

                // --- KEY FIX: REVERSE THE LIST ---
                // This swaps the "Up" and "Down" posts.
                Collections.reverse(allUserPosts);
                Collections.reverse(allUserPostIds);

                if (verticalAdapter != null) {
                    verticalAdapter.notifyDataSetChanged();
                }

                if (startPostId != null && mainVerticalViewPager != null) {
                    int startIndex = allUserPostIds.indexOf(startPostId);
                    if (startIndex != -1) {
                        mainVerticalViewPager.setCurrentItem(startIndex, false);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(I_ProfileUploadContent_GridViewer.this, "Error loading posts", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static class VerticalPostAdapter extends RecyclerView.Adapter<VerticalPostAdapter.PostViewHolder> {

        private List<I_NewPost_Event> posts;
        private List<String> postIds;
        private AppCompatActivity context;
        private String currentUserId;

        public VerticalPostAdapter(AppCompatActivity context, List<I_NewPost_Event> posts, List<String> postIds, String currentUserId) {
            this.context = context;
            this.posts = posts;
            this.postIds = postIds;
            this.currentUserId = currentUserId;
        }

        @NonNull
        @Override
        public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.i_profile_contents_verticalgridview, parent, false);
            return new PostViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PostViewHolder holder, @SuppressLint("RecyclerView") int position) {
            I_NewPost_Event post = posts.get(position);
            String postId = postIds.get(position);

            if (holder.imagesViewPager != null) {
                // 1. Set up the Adapter
                ImageSliderAdapter imageAdapter = new ImageSliderAdapter(context, post.getImageUrls());
                holder.imagesViewPager.setAdapter(imageAdapter);
                holder.imagesViewPager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);

                // --- INDICATOR LOGIC ADDED HERE ---
                int totalPhotos = imageAdapter.getItemCount();

                // Bind Dots Indicator
                if (holder.dotsIndicator != null) {
                    holder.dotsIndicator.setViewPager2(holder.imagesViewPager);
                }

                // Initial Page Number Display
                if (holder.photoIndicator != null) {
                    holder.photoIndicator.setText("1/" + totalPhotos);
                }

                // Page Change Callback for Number Display
                holder.imagesViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        if (holder.photoIndicator != null) {
                            holder.photoIndicator.setText((position + 1) + "/" + totalPhotos);
                        }
                    }
                });

                // Visibility Logic
                if (totalPhotos <= 1) {
                    if (holder.dotsIndicator != null) holder.dotsIndicator.setVisibility(View.GONE);
                    if (holder.photoIndicator != null) holder.photoIndicator.setVisibility(View.GONE);
                } else {
                    if (holder.dotsIndicator != null) holder.dotsIndicator.setVisibility(View.VISIBLE);
                    if (holder.photoIndicator != null) holder.photoIndicator.setVisibility(View.VISIBLE);
                }
            }

            if (holder.heartButton != null) {
                updateHeartIcon(holder.heartButton, post.getHeartLiked(), currentUserId);
                holder.heartButton.setOnClickListener(v -> toggleInteraction(holder, post, postId, "Heart"));
            }

            if (holder.favButton != null) {
                updateFavIcon(holder.favButton, post.getFavList(), currentUserId);
                holder.favButton.setOnClickListener(v -> toggleInteraction(holder, post, postId, "Fav"));
            }

            if (holder.deleteButton != null) {
                holder.deleteButton.setOnClickListener(v -> deletePost(position, postId));
            }

            if (holder.shareButton != null) {
                holder.shareButton.setOnClickListener(v -> {
                    android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Check out this post!");
                    shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Check out this post!");
                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share post via"));
                });
            }
        }

        @Override
        public int getItemCount() {
            return posts.size();
        }

        private void deletePost(int position, String postId) {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("PostEvents").child(postId);

            // Remove from PostEvents
            ref.removeValue().addOnSuccessListener(aVoid -> {
                Toast.makeText(context, "Post Deleted", Toast.LENGTH_SHORT).show();

                // 1. Update the UI List
                try {
                    posts.remove(position);
                    postIds.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, posts.size());
                } catch (IndexOutOfBoundsException e) {
                    // Handle potential index errors
                }

                // 2. Update Firebase User Post Count (-1)
                if (currentUserId != null && !currentUserId.isEmpty()) {
                    DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUserId);

                    userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                // Get current count
                                Integer currentCount = snapshot.child("posts").getValue(Integer.class);

                                // Safety check for null
                                if (currentCount == null) {
                                    currentCount = 0;
                                }

                                // Decrement only if greater than 0
                                if (currentCount > 0) {
                                    userRef.child("posts").setValue(currentCount - 1);
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            // Handle database error if necessary
                        }
                    });
                }
                context.finish();
            });
        }


        private void toggleInteraction(PostViewHolder holder, I_NewPost_Event post, String postId, String type) {
            DatabaseReference postRef = FirebaseDatabase.getInstance().getReference("PostEvents").child(postId);

            if (type.equals("Heart")) {
                Map<String, Boolean> likes = post.getHeartLiked();
                if (likes == null) likes = new HashMap<>();

                if (likes.containsKey(currentUserId)) {
                    likes.remove(currentUserId);
                } else {
                    likes.put(currentUserId, true);
                }

                post.setHeartLiked(likes);
                post.setHeartCount(likes.size());
                updateHeartIcon(holder.heartButton, likes, currentUserId);

            } else if (type.equals("Fav")) {
                Map<String, Boolean> favs = post.getFavList();
                if (favs == null) favs = new HashMap<>();

                if (favs.containsKey(currentUserId)) {
                    favs.remove(currentUserId);
                    Toast.makeText(context, "Removed from favorites", Toast.LENGTH_SHORT).show();
                } else {
                    favs.put(currentUserId, true);
                    Toast.makeText(context, "Saved to favorites", Toast.LENGTH_SHORT).show();
                }

                post.setFavList(favs);
                post.setFavCount(favs.size());
                updateFavIcon(holder.favButton, favs, currentUserId);
            }
            postRef.setValue(post);
        }

        private void updateHeartIcon(ConstraintLayout btn, Map<String, Boolean> map, String uid) {
            if (btn == null || btn.getChildCount() == 0) return;
            ImageView icon = (ImageView) btn.getChildAt(0);
            if (map != null && map.containsKey(uid)) {
                icon.setImageResource(R.drawable.heart2);
            } else {
                icon.setImageResource(R.drawable.heart);
            }
        }

        private void updateFavIcon(ConstraintLayout btn, Map<String, Boolean> map, String uid) {
            if (btn == null || btn.getChildCount() == 0) return;
            ImageView icon = (ImageView) btn.getChildAt(0);
            if (map != null && map.containsKey(uid)) {
                icon.setImageResource(R.drawable.fav2);
            } else {
                icon.setImageResource(R.drawable.fav);
            }
        }

        static class PostViewHolder extends RecyclerView.ViewHolder {
            ViewPager2 imagesViewPager;
            ConstraintLayout heartButton, favButton, shareButton;
            ImageButton deleteButton;

            // Indicators
            WormDotsIndicator dotsIndicator;
            TextView photoIndicator;

            PostViewHolder(@NonNull View itemView) {
                super(itemView);
                imagesViewPager = itemView.findViewById(R.id.inner_images_viewpager);
                heartButton = itemView.findViewById(R.id.Heartbutton);
                favButton = itemView.findViewById(R.id.Favbutton);
                shareButton = itemView.findViewById(R.id.Sharebutton);
                deleteButton = itemView.findViewById(R.id.deletebutton);

                // Fixed the caret issue here:
                dotsIndicator = itemView.findViewById(R.id.dotindicatoruploadcontent);
                photoIndicator = itemView.findViewById(R.id.photoindicatoruploadcontent);
            }
        }

    }

    private static class ImageSliderAdapter extends RecyclerView.Adapter<ImageSliderAdapter.ImageViewHolder> {
        private Context context;
        private List<String> imageUrls;

        ImageSliderAdapter(Context context, List<String> imageUrls) {
            this.context = context;
            this.imageUrls = imageUrls;
        }

        @NonNull
        @Override
        public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView imageView = new ImageView(context);
            imageView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            return new ImageViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
            if (imageUrls != null && position < imageUrls.size()) {
                Glide.with(context).load(imageUrls.get(position)).into(holder.imageView);
            }
        }

        @Override
        public int getItemCount() {
            return (imageUrls != null) ? imageUrls.size() : 0;
        }

        static class ImageViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;

            ImageViewHolder(View itemView) {
                super(itemView);
                this.imageView = (ImageView) itemView;
            }
        }
    }
}

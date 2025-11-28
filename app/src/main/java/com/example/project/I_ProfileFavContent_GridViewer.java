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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class I_ProfileFavContent_GridViewer extends AppCompatActivity {

    private ViewPager2 mainVerticalViewPager;
    private VerticalPostAdapter verticalAdapter;

    // Changed list names to reflect context (Favorites)
    private List<I_NewPost_Event> favPosts = new ArrayList<>();
    private List<String> favPostIds = new ArrayList<>();

    private ImageView backButton;
    private String startPostId;
    private String currentUserId;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Reusing the same layout as the upload/liked viewer since the UI is identical
        setContentView(R.layout.i_profile_contents_gridview);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = (user != null) ? user.getUid() : "";

        startPostId = getIntent().getStringExtra("POST_ID");

        backButton = findViewById(R.id.postcontentback_button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        // Setup ViewPager for Vertical Scrolling
        mainVerticalViewPager = findViewById(R.id.postcontentview2);

        if (mainVerticalViewPager != null) {
            mainVerticalViewPager.setOrientation(ViewPager2.ORIENTATION_VERTICAL);

            // Standard page transformer logic
            mainVerticalViewPager.setPageTransformer((page, position) -> {
                if (position < -1) {
                    page.setAlpha(0f);
                } else if (position <= 1) {
                    page.setAlpha(1f);
                    page.setTranslationY(0f);
                    page.setTranslationX(0f);
                    page.setScaleX(1f);
                    page.setScaleY(1f);

                    float minAlpha = 0.5f;
                    float alpha = Math.max(minAlpha, 1 - Math.abs(position));
                    page.setAlpha(alpha);
                } else {
                    page.setAlpha(0f);
                }
            });

            verticalAdapter = new VerticalPostAdapter(this, favPosts, favPostIds, currentUserId);
            mainVerticalViewPager.setAdapter(verticalAdapter);
        }

        if (!currentUserId.isEmpty()) {
            loadFavPosts();
        }
    }

    private void loadFavPosts() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("PostEvents");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                favPosts.clear();
                favPostIds.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    I_NewPost_Event post = ds.getValue(I_NewPost_Event.class);

                    if (post != null) {
                        // 1. Check if the current user Favorites (Saved) the post
                        Map<String, Boolean> favs = post.getFavList();
                        boolean isFav = (favs != null && Boolean.TRUE.equals(favs.get(currentUserId)));

                        // 2. Check if the current user Created the post
                        boolean isCreator = (post.getUserId() != null && post.getUserId().equals(currentUserId));

                        // 3. Add if isFav is TRUE AND isCreator is FALSE.
                        // This matches the logic used in "Liked" content to avoid showing own posts.
                        if (isFav && !isCreator) {
                            favPosts.add(post);
                            favPostIds.add(ds.getKey());
                        }
                    }
                }

                // Reverse to show newest favorites first
                Collections.reverse(favPosts);
                Collections.reverse(favPostIds);

                if (verticalAdapter != null) {
                    verticalAdapter.notifyDataSetChanged();
                }

                // Handle jumping to a specific post if passed via intent
                if (startPostId != null && mainVerticalViewPager != null) {
                    int startIndex = favPostIds.indexOf(startPostId);
                    if (startIndex != -1) {
                        mainVerticalViewPager.setCurrentItem(startIndex, false);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(I_ProfileFavContent_GridViewer.this, "Error loading favorites", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Using a static inner class for the adapter, identical to the LikedViewer but independent
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

            // --- Image Slider Setup ---
            if (holder.imagesViewPager != null) {
                ImageSliderAdapter imageAdapter = new ImageSliderAdapter(context, post.getImageUrls());
                holder.imagesViewPager.setAdapter(imageAdapter);
                holder.imagesViewPager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);

                int totalPhotos = imageAdapter.getItemCount();

                if (holder.dotsIndicator != null) {
                    holder.dotsIndicator.setViewPager2(holder.imagesViewPager);
                }

                if (holder.photoIndicator != null) {
                    holder.photoIndicator.setText("1/" + totalPhotos);
                }

                holder.imagesViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        if (holder.photoIndicator != null) {
                            holder.photoIndicator.setText((position + 1) + "/" + totalPhotos);
                        }
                    }
                });

                if (totalPhotos <= 1) {
                    if (holder.dotsIndicator != null) holder.dotsIndicator.setVisibility(View.GONE);
                    if (holder.photoIndicator != null) holder.photoIndicator.setVisibility(View.GONE);
                } else {
                    if (holder.dotsIndicator != null) holder.dotsIndicator.setVisibility(View.VISIBLE);
                    if (holder.photoIndicator != null) holder.photoIndicator.setVisibility(View.VISIBLE);
                }
            }

            // --- Interaction Buttons ---

            if (holder.heartButton != null) {
                updateHeartIcon(holder.heartButton, post.getHeartLiked(), currentUserId);
                holder.heartButton.setOnClickListener(v -> toggleInteraction(holder, post, postId, "Heart", position));
            }

            if (holder.favButton != null) {
                updateFavIcon(holder.favButton, post.getFavList(), currentUserId);
                holder.favButton.setOnClickListener(v -> toggleInteraction(holder, post, postId, "Fav", position));
            }

            // --- Delete Button Logic ---
            if (holder.deleteButton != null) {
                // Only allow deletion if the current user OWNS the post
                if (post.getUserId() != null && post.getUserId().equals(currentUserId)) {
                    holder.deleteButton.setVisibility(View.VISIBLE);
                    holder.deleteButton.setOnClickListener(v -> deletePost(position, postId));
                } else {
                    holder.deleteButton.setVisibility(View.GONE);
                }
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
            ref.removeValue().addOnSuccessListener(aVoid -> {
                Toast.makeText(context, "Post Deleted", Toast.LENGTH_SHORT).show();
                try {
                    posts.remove(position);
                    postIds.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, posts.size());
                } catch (IndexOutOfBoundsException e) {
                    // Handle race condition
                }
            });
        }

        private void toggleInteraction(PostViewHolder holder, I_NewPost_Event post, String postId, String type, int position) {
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
                    // Optional: remove from list immediately if un-favorited in this view
                    // posts.remove(position); postIds.remove(position); notifyItemRemoved(position);
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
            if (map != null && Boolean.TRUE.equals(map.get(uid))) {
                icon.setImageResource(R.drawable.heart2);
            } else {
                icon.setImageResource(R.drawable.heart);
            }
        }

        private void updateFavIcon(ConstraintLayout btn, Map<String, Boolean> map, String uid) {
            if (btn == null || btn.getChildCount() == 0) return;
            ImageView icon = (ImageView) btn.getChildAt(0);
            if (map != null && Boolean.TRUE.equals(map.get(uid))) {
                icon.setImageResource(R.drawable.fav2);
            } else {
                icon.setImageResource(R.drawable.fav);
            }
        }

        static class PostViewHolder extends RecyclerView.ViewHolder {
            ViewPager2 imagesViewPager;
            ConstraintLayout heartButton, favButton, shareButton;
            ImageButton deleteButton;

            WormDotsIndicator dotsIndicator;
            TextView photoIndicator;

            PostViewHolder(@NonNull View itemView) {
                super(itemView);
                imagesViewPager = itemView.findViewById(R.id.inner_images_viewpager);
                heartButton = itemView.findViewById(R.id.Heartbutton);
                favButton = itemView.findViewById(R.id.Favbutton);
                shareButton = itemView.findViewById(R.id.Sharebutton);
                deleteButton = itemView.findViewById(R.id.deletebutton);

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

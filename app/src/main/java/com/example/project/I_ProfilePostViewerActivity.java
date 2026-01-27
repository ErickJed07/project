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

public class I_ProfilePostViewerActivity extends AppCompatActivity {

    public static final String EXTRA_TYPE = "VIEW_TYPE";
    public static final String TYPE_UPLOAD = "UPLOAD";
    public static final String TYPE_LIKED = "LIKED";
    public static final String TYPE_FAVORITE = "FAVORITE";
    public static final String EXTRA_POST_ID = "POST_ID";

    private ViewPager2 mainVerticalViewPager;
    private VerticalPostAdapter verticalAdapter;
    private List<I_NewPost_Event> posts = new ArrayList<>();
    private List<String> postIds = new ArrayList<>();

    private String viewType;
    private String startPostId;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.i_profile_contents_gridview);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = (user != null) ? user.getUid() : "";

        viewType = getIntent().getStringExtra(EXTRA_TYPE);
        startPostId = getIntent().getStringExtra(EXTRA_POST_ID);

        ImageView backButton = findViewById(R.id.postcontentback_button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        mainVerticalViewPager = findViewById(R.id.postcontentview2);
        if (mainVerticalViewPager != null) {
            mainVerticalViewPager.setOrientation(ViewPager2.ORIENTATION_VERTICAL);
            mainVerticalViewPager.setPageTransformer((page, position) -> {
                if (position < -1) {
                    page.setAlpha(0f);
                } else if (position <= 1) {
                    page.setAlpha(1f);
                    page.setTranslationY(0f);
                    page.setTranslationX(0f);
                    page.setScaleX(1f);
                    page.setScaleY(1f);
                    float alpha = Math.max(0.5f, 1 - Math.abs(position));
                    page.setAlpha(alpha);
                } else {
                    page.setAlpha(0f);
                }
            });

            verticalAdapter = new VerticalPostAdapter(this, posts, postIds, currentUserId);
            mainVerticalViewPager.setAdapter(verticalAdapter);
        }

        if (!currentUserId.isEmpty()) {
            loadPosts();
        }
    }

    private void loadPosts() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("PostEvents");
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                posts.clear();
                postIds.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    I_NewPost_Event post = ds.getValue(I_NewPost_Event.class);
                    if (post == null) continue;
                    post.setPostId(ds.getKey());

                    boolean isCreator = post.getUserId() != null && post.getUserId().equals(currentUserId);
                    boolean shouldAdd = false;

                    if (TYPE_UPLOAD.equals(viewType)) {
                        shouldAdd = isCreator;
                    } else if (TYPE_LIKED.equals(viewType)) {
                        Map<String, Boolean> likes = post.getHeartLiked();
                        shouldAdd = (likes != null && Boolean.TRUE.equals(likes.get(currentUserId))) && !isCreator;
                    } else if (TYPE_FAVORITE.equals(viewType)) {
                        Map<String, Boolean> favs = post.getFavList();
                        shouldAdd = (favs != null && Boolean.TRUE.equals(favs.get(currentUserId))) && !isCreator;
                    }

                    if (shouldAdd) {
                        posts.add(post);
                        postIds.add(ds.getKey());
                    }
                }

                Collections.reverse(posts);
                Collections.reverse(postIds);

                if (verticalAdapter != null) verticalAdapter.notifyDataSetChanged();

                if (startPostId != null && mainVerticalViewPager != null) {
                    int startIndex = postIds.indexOf(startPostId);
                    if (startIndex != -1) {
                        mainVerticalViewPager.setCurrentItem(startIndex, false);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(I_ProfilePostViewerActivity.this, "Error loading posts", Toast.LENGTH_SHORT).show();
            }
        };

        if (TYPE_UPLOAD.equals(viewType)) {
            ref.orderByChild("userId").equalTo(currentUserId).addListenerForSingleValueEvent(listener);
        } else {
            ref.addListenerForSingleValueEvent(listener);
        }
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
                ImageSliderAdapter imageAdapter = new ImageSliderAdapter(context, post.getImageUrls());
                holder.imagesViewPager.setAdapter(imageAdapter);
                holder.imagesViewPager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);

                int totalPhotos = imageAdapter.getItemCount();
                if (holder.dotsIndicator != null) holder.dotsIndicator.setViewPager2(holder.imagesViewPager);
                if (holder.photoIndicator != null) holder.photoIndicator.setText("1/" + totalPhotos);

                holder.imagesViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int pos) {
                        if (holder.photoIndicator != null) holder.photoIndicator.setText((pos + 1) + "/" + totalPhotos);
                    }
                });

                boolean showIndicators = totalPhotos > 1;
                if (holder.dotsIndicator != null) holder.dotsIndicator.setVisibility(showIndicators ? View.VISIBLE : View.GONE);
                if (holder.photoIndicator != null) holder.photoIndicator.setVisibility(showIndicators ? View.VISIBLE : View.GONE);
            }

            if (holder.heartButton != null) {
                updateIcon(holder.heartButton, post.getHeartLiked(), currentUserId, R.drawable.heart2, R.drawable.heart);
                holder.heartButton.setOnClickListener(v -> toggleInteraction(holder, post, postId, "Heart"));
            }

            if (holder.favButton != null) {
                updateIcon(holder.favButton, post.getFavList(), currentUserId, R.drawable.fav2, R.drawable.fav);
                holder.favButton.setOnClickListener(v -> toggleInteraction(holder, post, postId, "Fav"));
            }

            if (holder.deleteButton != null) {
                boolean isOwner = post.getUserId() != null && post.getUserId().equals(currentUserId);
                holder.deleteButton.setVisibility(isOwner ? View.VISIBLE : View.GONE);
                holder.deleteButton.setOnClickListener(v -> deletePost(position, postId));
            }

            if (holder.shareButton != null) {
                holder.shareButton.setOnClickListener(v -> {
                    android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Check out this post!");
                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share post via"));
                });
            }
        }

        @Override
        public int getItemCount() { return posts.size(); }

        private void deletePost(int position, String postId) {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("PostEvents").child(postId);
            ref.removeValue().addOnSuccessListener(aVoid -> {
                Toast.makeText(context, "Post Deleted", Toast.LENGTH_SHORT).show();
                posts.remove(position);
                postIds.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, posts.size());
                
                // Decrement post count if it was user's own post
                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUserId).child("posts");
                userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Long count = snapshot.getValue(Long.class);
                        if (count != null && count > 0) userRef.setValue(count - 1);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            });
        }

        private void toggleInteraction(PostViewHolder holder, I_NewPost_Event post, String postId, String type) {
            DatabaseReference postRef = FirebaseDatabase.getInstance().getReference("PostEvents").child(postId);
            if ("Heart".equals(type)) {
                Map<String, Boolean> likes = post.getHeartLiked();
                if (likes == null) likes = new HashMap<>();
                if (likes.containsKey(currentUserId)) likes.remove(currentUserId);
                else likes.put(currentUserId, true);
                post.setHeartLiked(likes);
                post.setHeartCount(likes.size());
                updateIcon(holder.heartButton, likes, currentUserId, R.drawable.heart2, R.drawable.heart);
            } else {
                Map<String, Boolean> favs = post.getFavList();
                if (favs == null) favs = new HashMap<>();
                if (favs.containsKey(currentUserId)) favs.remove(currentUserId);
                else favs.put(currentUserId, true);
                post.setFavList(favs);
                post.setFavCount(favs.size());
                updateIcon(holder.favButton, favs, currentUserId, R.drawable.fav2, R.drawable.fav);
            }
            postRef.setValue(post);
        }

        private void updateIcon(ConstraintLayout btn, Map<String, Boolean> map, String uid, int activeRes, int inactiveRes) {
            if (btn == null || btn.getChildCount() == 0) return;
            ImageView icon = (ImageView) btn.getChildAt(0);
            icon.setImageResource(map != null && Boolean.TRUE.equals(map.get(uid)) ? activeRes : inactiveRes);
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
        ImageSliderAdapter(Context context, List<String> imageUrls) { this.context = context; this.imageUrls = imageUrls; }
        @NonNull @Override public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView iv = new ImageView(context);
            iv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            return new ImageViewHolder(iv);
        }
        @Override public void onBindViewHolder(@NonNull ImageViewHolder holder, int pos) { Glide.with(context).load(imageUrls.get(pos)).into(holder.imageView); }
        @Override public int getItemCount() { return imageUrls != null ? imageUrls.size() : 0; }
        static class ImageViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            ImageViewHolder(View v) { super(v); imageView = (ImageView) v; }
        }
    }
}

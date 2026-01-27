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

public class I_ProfilePostActivity extends AppCompatActivity {

    public static final String EXTRA_TYPE = "VIEW_TYPE";
    public static final String TYPE_UPLOAD = "UPLOAD";
    public static final String TYPE_LIKED = "LIKED";
    public static final String TYPE_FAVORITE = "FAVORITE";
    public static final String EXTRA_POST_ID = "POST_ID";

    private ViewPager2 mainVerticalViewPager;
    private VerticalPostAdapter verticalAdapter;
    private List<I_PostEvent> posts = new ArrayList<>();
    private List<String> postIds = new ArrayList<>();
    private String viewType, startPostId, currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.i_profile_grid);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        viewType = getIntent().getStringExtra(EXTRA_TYPE);
        startPostId = getIntent().getStringExtra(EXTRA_POST_ID);

        findViewById(R.id.postcontentback_button).setOnClickListener(v -> finish());
        mainVerticalViewPager = findViewById(R.id.postcontentview2);

        if (mainVerticalViewPager != null) {
            mainVerticalViewPager.setOrientation(ViewPager2.ORIENTATION_VERTICAL);
            mainVerticalViewPager.setPageTransformer((page, position) -> {
                if (position < -1) page.setAlpha(0f);
                else if (position <= 1) {
                    page.setAlpha(Math.max(0.5f, 1 - Math.abs(position)));
                    page.setTranslationY(0f);
                    page.setTranslationX(0f);
                    page.setScaleX(1f);
                    page.setScaleY(1f);
                } else page.setAlpha(0f);
            });
            verticalAdapter = new VerticalPostAdapter(this, posts, postIds, currentUserId);
            mainVerticalViewPager.setAdapter(verticalAdapter);
        }

        if (!currentUserId.isEmpty()) loadPosts();
    }

    private void loadPosts() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("PostEvents");
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                posts.clear();
                postIds.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    I_PostEvent post = ds.getValue(I_PostEvent.class);
                    if (post == null) continue;
                    post.setPostId(ds.getKey());
                    boolean isCreator = post.getUserId() != null && post.getUserId().equals(currentUserId);
                    boolean shouldAdd = false;
                    if (TYPE_UPLOAD.equals(viewType)) shouldAdd = isCreator;
                    else if (TYPE_LIKED.equals(viewType)) shouldAdd = (post.getHeartLiked() != null && Boolean.TRUE.equals(post.getHeartLiked().get(currentUserId))) && !isCreator;
                    else if (TYPE_FAVORITE.equals(viewType)) shouldAdd = (post.getFavList() != null && Boolean.TRUE.equals(post.getFavList().get(currentUserId))) && !isCreator;
                    if (shouldAdd) { posts.add(post); postIds.add(ds.getKey()); }
                }
                Collections.reverse(posts);
                Collections.reverse(postIds);
                if (verticalAdapter != null) verticalAdapter.notifyDataSetChanged();
                if (startPostId != null && mainVerticalViewPager != null) {
                    int index = postIds.indexOf(startPostId);
                    if (index != -1) mainVerticalViewPager.setCurrentItem(index, false);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        if (TYPE_UPLOAD.equals(viewType)) ref.orderByChild("userId").equalTo(currentUserId).addListenerForSingleValueEvent(listener);
        else ref.addListenerForSingleValueEvent(listener);
    }

    public static class VerticalPostAdapter extends RecyclerView.Adapter<VerticalPostAdapter.PostViewHolder> {
        private List<I_PostEvent> posts;
        private List<String> postIds;
        private AppCompatActivity context;
        private String currentUserId;

        public VerticalPostAdapter(AppCompatActivity context, List<I_PostEvent> posts, List<String> postIds, String currentUserId) {
            this.context = context; this.posts = posts; this.postIds = postIds; this.currentUserId = currentUserId;
        }

        @NonNull @Override public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new PostViewHolder(LayoutInflater.from(context).inflate(R.layout.i_profile_contents_verticalgridview, parent, false));
        }

        @Override public void onBindViewHolder(@NonNull PostViewHolder holder, @SuppressLint("RecyclerView") int position) {
            I_PostEvent post = posts.get(position);
            String postId = postIds.get(position);
            if (holder.imagesViewPager != null) {
                ImageSliderAdapter adapter = new ImageSliderAdapter(context, post.getImageUrls());
                holder.imagesViewPager.setAdapter(adapter);
                holder.imagesViewPager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
                int total = adapter.getItemCount();
                if (holder.dotsIndicator != null) holder.dotsIndicator.setViewPager2(holder.imagesViewPager);
                if (holder.photoIndicator != null) holder.photoIndicator.setText("1/" + total);
                holder.imagesViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                    @Override public void onPageSelected(int pos) { if (holder.photoIndicator != null) holder.photoIndicator.setText((pos + 1) + "/" + total); }
                });
                boolean indicators = total > 1;
                if (holder.dotsIndicator != null) holder.dotsIndicator.setVisibility(indicators ? View.VISIBLE : View.GONE);
                if (holder.photoIndicator != null) holder.photoIndicator.setVisibility(indicators ? View.VISIBLE : View.GONE);
            }
            updateIcon(holder.heartButton, post.getHeartLiked(), currentUserId, R.drawable.heart2, R.drawable.heart);
            holder.heartButton.setOnClickListener(v -> toggle(post, postId, "Heart", holder));
            updateIcon(holder.favButton, post.getFavList(), currentUserId, R.drawable.fav2, R.drawable.fav);
            holder.favButton.setOnClickListener(v -> toggle(post, postId, "Fav", holder));
            holder.deleteButton.setVisibility(currentUserId.equals(post.getUserId()) ? View.VISIBLE : View.GONE);
            holder.deleteButton.setOnClickListener(v -> delete(position, postId));
            holder.shareButton.setOnClickListener(v -> {
                context.startActivity(android.content.Intent.createChooser(new android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, "Check out this post!"), "Share via"));
            });
        }

        @Override public int getItemCount() { return posts.size(); }

        private void delete(int pos, String id) {
            FirebaseDatabase.getInstance().getReference("PostEvents").child(id).removeValue().addOnSuccessListener(aVoid -> {
                posts.remove(pos); postIds.remove(pos); notifyItemRemoved(pos); notifyItemRangeChanged(pos, posts.size());
                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUserId).child("posts");
                userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot s) { Long c = s.getValue(Long.class); if (c != null && c > 0) userRef.setValue(c - 1); }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
            });
        }

        private void toggle(I_PostEvent post, String id, String type, PostViewHolder holder) {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("PostEvents").child(id);
            if ("Heart".equals(type)) {
                Map<String, Boolean> map = post.getHeartLiked() != null ? post.getHeartLiked() : new HashMap<>();
                if (map.containsKey(currentUserId)) map.remove(currentUserId); else map.put(currentUserId, true);
                post.setHeartLiked(map); post.setHeartCount(map.size());
                updateIcon(holder.heartButton, map, currentUserId, R.drawable.heart2, R.drawable.heart);
            } else {
                Map<String, Boolean> map = post.getFavList() != null ? post.getFavList() : new HashMap<>();
                if (map.containsKey(currentUserId)) map.remove(currentUserId); else map.put(currentUserId, true);
                post.setFavList(map); post.setFavCount(map.size());
                updateIcon(holder.favButton, map, currentUserId, R.drawable.fav2, R.drawable.fav);
            }
            ref.setValue(post);
        }

        private void updateIcon(ConstraintLayout btn, Map<String, Boolean> map, String uid, int a, int i) {
            if (btn != null && btn.getChildCount() > 0) ((ImageView) btn.getChildAt(0)).setImageResource(map != null && Boolean.TRUE.equals(map.get(uid)) ? a : i);
        }

        static class PostViewHolder extends RecyclerView.ViewHolder {
            ViewPager2 imagesViewPager; ConstraintLayout heartButton, favButton, shareButton; ImageButton deleteButton; WormDotsIndicator dotsIndicator; TextView photoIndicator;
            PostViewHolder(View v) { super(v); imagesViewPager = v.findViewById(R.id.inner_images_viewpager); heartButton = v.findViewById(R.id.Heartbutton); favButton = v.findViewById(R.id.Favbutton); shareButton = v.findViewById(R.id.Sharebutton); deleteButton = v.findViewById(R.id.deletebutton); dotsIndicator = v.findViewById(R.id.dotindicatoruploadcontent); photoIndicator = v.findViewById(R.id.photoindicatoruploadcontent); }
        }
    }

    private static class ImageSliderAdapter extends RecyclerView.Adapter<ImageSliderAdapter.ViewHolder> {
        private Context c; private List<String> urls;
        ImageSliderAdapter(Context c, List<String> urls) { this.c = c; this.urls = urls; }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) { ImageView iv = new ImageView(c); iv.setLayoutParams(new ViewGroup.LayoutParams(-1, -1)); iv.setScaleType(ImageView.ScaleType.FIT_CENTER); return new ViewHolder(iv); }
        @Override public void onBindViewHolder(@NonNull ViewHolder h, int p) { Glide.with(c).load(urls.get(p)).into(h.iv); }
        @Override public int getItemCount() { return urls != null ? urls.size() : 0; }
        static class ViewHolder extends RecyclerView.ViewHolder { ImageView iv; ViewHolder(View v) { super(v); iv = (ImageView) v; } }
    }
}

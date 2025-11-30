package com.example.project;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class D_FeedAdapter extends RecyclerView.Adapter<D_FeedAdapter.PostViewHolder> {

    private Context context;
    private List<I_NewPost_Event> postList;
    private Handler handler;

    public D_FeedAdapter(Context context, List<I_NewPost_Event> postList) {
        this.context = context;
        this.postList = postList;
        this.handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public PostViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.d2_feed_item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(PostViewHolder holder, int position) {
        I_NewPost_Event postEvent = postList.get(position);

        // --- 1. Set Basic Data ---
        holder.userNameTextView.setText(postEvent.getUsername());
        holder.captionTextView.setText(postEvent.getCaption());
        holder.dateTextView.setText(getRelativeTime(postEvent.getDate()));

        // --- 2. Get IDs Safely ---
        String currentUserId = "";
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        final String finalCurrentUserId = currentUserId;
        String postId = postEvent.getPostId();

        // --- 3. DYNAMIC IMAGE RESIZING (WARP CONTENT LOGIC) ---
        if (postEvent.getImageUrls() != null && !postEvent.getImageUrls().isEmpty()) {

            // Get the first image URL to determine the aspect ratio
            String firstImageUrl = postEvent.getImageUrls().get(0);

            // Reset ViewPager height temporarily to avoid layout glitches
            ViewGroup.LayoutParams resetParams = holder.viewPager2.getLayoutParams();
            resetParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            holder.viewPager2.setLayoutParams(resetParams);

            // Use Glide to download the bitmap size BEFORE displaying it fully
            Glide.with(context)
                    .asBitmap()
                    .load(firstImageUrl)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            int imageWidth = resource.getWidth();
                            int imageHeight = resource.getHeight();

                            // Prevent division by zero
                            if (imageWidth > 0) {
                                // Calculate Aspect Ratio
                                float aspectRatio = (float) imageHeight / imageWidth;

                                // Get the screen width (or width of the viewpager)
                                int viewPagerWidth = holder.viewPager2.getWidth();

                                // If viewPager hasn't laid out yet, use screen width as fallback
                                if (viewPagerWidth == 0) {
                                    viewPagerWidth = context.getResources().getDisplayMetrics().widthPixels;
                                }

                                // Calculate the new height for the ViewPager
                                int newHeight = (int) (viewPagerWidth * aspectRatio);

                                // Apply the height
                                ViewGroup.LayoutParams layoutParams = holder.viewPager2.getLayoutParams();
                                layoutParams.height = newHeight;
                                holder.viewPager2.setLayoutParams(layoutParams);
                            }
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            // Do nothing
                        }
                    });

            // Set the internal Adapter for the ViewPager
            D_Feed_ImageViewAdapter imageAdapter = new D_Feed_ImageViewAdapter(context, postEvent.getImageUrls());
            holder.viewPager2.setAdapter(imageAdapter);

            // Bind indicator
            holder.dotsIndicator.setViewPager2(holder.viewPager2);

            // Page number display
            holder.viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    int totalPhotos = imageAdapter.getItemCount();
                    holder.photoIndicator.setText((position + 1) + "/" + totalPhotos);
                }
            });

            // Visibility logic
            if (imageAdapter.getItemCount() <= 1) {
                holder.dotsIndicator.setVisibility(View.GONE);
                holder.photoIndicator.setVisibility(View.GONE);
            } else {
                holder.dotsIndicator.setVisibility(View.VISIBLE);
                holder.photoIndicator.setVisibility(View.VISIBLE);
            }
        } else {
            // Handle posts with no images (optional: hide viewpager)
            holder.viewPager2.setVisibility(View.GONE);
        }


        // --- 4. Initial UI State Setup ---
        updateHeartIcon(holder.heartButton, postEvent.getHeartLiked(), currentUserId);
        if (postEvent.getHeartLiked() != null) {
            holder.heartNumTextView.setText(String.valueOf(postEvent.getHeartLiked().size()));
        } else {
            holder.heartNumTextView.setText("0");
        }

        updateFavIcon(holder.favButton, postEvent.getFavList(), currentUserId);
        if (postEvent.getFavList() != null) {
            holder.favNumTextView.setText(String.valueOf(postEvent.getFavList().size()));
        } else {
            holder.favNumTextView.setText("0");
        }

        // --- 5. Click Listeners ---

        // Heart Button
        holder.heartButton.setOnClickListener(v -> {
            if (postId == null || finalCurrentUserId.isEmpty()) return;

            DatabaseReference postRef = FirebaseDatabase.getInstance().getReference("PostEvents").child(postId);
            postRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        I_NewPost_Event post = dataSnapshot.getValue(I_NewPost_Event.class);
                        if (post == null) return;

                        Map<String, Boolean> heartLiked = post.getHeartLiked();
                        if (heartLiked == null) heartLiked = new HashMap<>();

                        if (heartLiked.containsKey(finalCurrentUserId)) {
                            heartLiked.remove(finalCurrentUserId);
                        } else {
                            heartLiked.put(finalCurrentUserId, true);
                        }

                        post.setHeartCount(heartLiked.size());
                        post.setHeartLiked(heartLiked);
                        postRef.setValue(post);

                        holder.heartNumTextView.setText(String.valueOf(heartLiked.size()));
                        updateHeartIcon(holder.heartButton, heartLiked, finalCurrentUserId);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(context, "Failed to update like", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Fav Button
        holder.favButton.setOnClickListener(v -> {
            if (postId == null || finalCurrentUserId.isEmpty()) {
                Toast.makeText(context, "Please log in to save", Toast.LENGTH_SHORT).show();
                return;
            }

            DatabaseReference postRef = FirebaseDatabase.getInstance().getReference("PostEvents").child(postId);
            postRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        I_NewPost_Event post = dataSnapshot.getValue(I_NewPost_Event.class);
                        if (post == null) return;

                        Map<String, Boolean> favList = post.getFavList();
                        if (favList == null) favList = new HashMap<>();

                        if (favList.containsKey(finalCurrentUserId)) {
                            favList.remove(finalCurrentUserId);
                            Toast.makeText(context, "Removed from favorites", Toast.LENGTH_SHORT).show();
                        } else {
                            favList.put(finalCurrentUserId, true);
                            Toast.makeText(context, "Saved to favorites", Toast.LENGTH_SHORT).show();
                        }

                        post.setFavCount(favList.size());
                        post.setFavList(favList);
                        postRef.setValue(post);

                        holder.favNumTextView.setText(String.valueOf(favList.size()));
                        updateFavIcon(holder.favButton, favList, finalCurrentUserId);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(context, "Failed to update favorites", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Share Button
        holder.shareButton.setOnClickListener(v -> {
            String shareContent = postEvent.getUsername() + " posted: " + "\n" + postEvent.getCaption();
            android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Check out this post!");
            shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareContent);
            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share post via"));
        });
    }

    // --- HELPER METHODS ---

    private void updateHeartIcon(ImageView heartButton, Map<String, Boolean> heartLiked, String userId) {
        if (heartLiked != null && heartLiked.containsKey(userId)) {
            heartButton.setImageResource(R.drawable.heart2);
        } else {
            heartButton.setImageResource(R.drawable.heart);
        }
    }

    private void updateFavIcon(ImageView favButton, Map<String, Boolean> favList, String userId) {
        if (favList != null && favList.containsKey(userId)) {
            favButton.setImageResource(R.drawable.fav2);
        } else {
            favButton.setImageResource(R.drawable.fav);
        }
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    private String getRelativeTime(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) return "";
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            Date postDate = format.parse(timestamp);
            if (postDate == null) return "";

            long diffInMillis = System.currentTimeMillis() - postDate.getTime();
            long seconds = diffInMillis / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;

            if (seconds < 60) return "Just now";
            else if (minutes < 60) return minutes + " min ago";
            else if (hours < 24) return hours + " h";
            else if (days == 1) return "Yesterday";
            else if (days < 30) return new SimpleDateFormat("MMM d", Locale.US).format(postDate);
            else if (days < 365) return new SimpleDateFormat("MMM d", Locale.US).format(postDate);
            else return new SimpleDateFormat("MMM d, yyyy", Locale.US).format(postDate);
        } catch (ParseException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView userNameTextView, captionTextView, dateTextView, photoIndicator;
        TextView favNumTextView, heartNumTextView;
        ViewPager2 viewPager2;
        WormDotsIndicator dotsIndicator;
        ImageView heartButton, favButton , shareButton;

        public PostViewHolder(View itemView) {
            super(itemView);
            userNameTextView = itemView.findViewById(R.id.userName_post);
            captionTextView = itemView.findViewById(R.id.caption_post);
            dateTextView = itemView.findViewById(R.id.postdate);
            viewPager2 = itemView.findViewById(R.id.post_Pic);
            photoIndicator = itemView.findViewById(R.id.photoIndicator);
            dotsIndicator = itemView.findViewById(R.id.dotsIndicator);

            heartButton = itemView.findViewById(R.id.heart_post);
            heartNumTextView = itemView.findViewById(R.id.heart_num);
            favButton = itemView.findViewById(R.id.fav_post);
            favNumTextView = itemView.findViewById(R.id.fav_num);
            shareButton = itemView.findViewById(R.id.share_post);
        }
    }
}

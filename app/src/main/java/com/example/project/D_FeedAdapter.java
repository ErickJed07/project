package com.example.project;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.HashMap;

import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

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
        // Use this final variable inside listeners
        final String finalCurrentUserId = currentUserId;
        String postId = postEvent.getPostId();

        // --- 3. Initial UI State Setup ---

        // Set Heart Icon
        updateHeartIcon(holder.heartButton, postEvent.getHeartLiked(), currentUserId);

        // Set Heart Count Text (NEW)
        if (postEvent.getHeartLiked() != null) {
            holder.heartNumTextView.setText(String.valueOf(postEvent.getHeartLiked().size()));
        } else {
            holder.heartNumTextView.setText("0");
        }

        // Set Fav Icon
        updateFavIcon(holder.favButton, postEvent.getFavList(), currentUserId);

        // Set Fav Count Text
        if (postEvent.getFavList() != null) {
            holder.favNumTextView.setText(String.valueOf(postEvent.getFavList().size()));
        } else {
            holder.favNumTextView.setText("0");
        }

        // ============================================================
        // CLICK LISTENER 1: HEART BUTTON (LIKES)
        // ============================================================
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

                        // Toggle Like Logic
                        if (heartLiked.containsKey(finalCurrentUserId)) {
                            heartLiked.remove(finalCurrentUserId);
                            // Optional: Toast.makeText(context, "Unliked", Toast.LENGTH_SHORT).show();
                        } else {
                            heartLiked.put(finalCurrentUserId, true);
                            // Optional: Toast.makeText(context, "Liked", Toast.LENGTH_SHORT).show();
                        }

                        // Update Data
                        post.setHeartCount(heartLiked.size());
                        post.setHeartLiked(heartLiked);

                        // Save to Firebase
                        postRef.setValue(post);

                        // Update UI Immediately
                        holder.heartNumTextView.setText(String.valueOf(heartLiked.size())); // Update text
                        updateHeartIcon(holder.heartButton, heartLiked, finalCurrentUserId); // Update icon
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(context, "Failed to update like", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // ============================================================
        // CLICK LISTENER 2: FAV BUTTON (FAVORITES)
        // ============================================================
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

                        // Toggle Fav Logic
                        if (favList.containsKey(finalCurrentUserId)) {
                            favList.remove(finalCurrentUserId);
                            Toast.makeText(context, "Removed from favorites", Toast.LENGTH_SHORT).show();
                        } else {
                            favList.put(finalCurrentUserId, true);
                            Toast.makeText(context, "Saved to favorites", Toast.LENGTH_SHORT).show();
                        }

                        // Update Data
                        post.setFavCount(favList.size());
                        post.setFavList(favList);

                        // Save to Firebase
                        postRef.setValue(post);

                        // Update UI Immediately
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

        // ============================================================
        // CLICK LISTENER 3: SHARE BUTTON
        // ============================================================
        holder.shareButton.setOnClickListener(v -> {
            // Create the content to share (e.g., Caption + Link or App Name)
            String shareContent = postEvent.getUsername() + " posted: " + "\n" +
                    postEvent.getCaption();

            // Create the Intent
            android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Check out this post!");
            shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareContent);

            // Launch the Share Sheet
            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share post via"));
        });

        // --- 4. Date Updater ---
        Runnable updateDateRunnable = new Runnable() {
            @Override
            public void run() {
                // Use safe relative time method
                holder.dateTextView.setText(getRelativeTime(postEvent.getDate()));
                handler.postDelayed(this, 10000);
            }
        };
        handler.postDelayed(updateDateRunnable, 10000);

        // --- 5. Image ViewPager Logic ---
        if (postEvent.getImageUrls() != null && !postEvent.getImageUrls().isEmpty()) {
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
        }
    }

    // --- HELPER METHODS ---

    // Heart Icon Logic
    private void updateHeartIcon(ImageView heartButton, Map<String, Boolean> heartLiked, String userId) {
        if (heartLiked != null && heartLiked.containsKey(userId)) {
            heartButton.setImageResource(R.drawable.heart2); // Filled
        } else {
            heartButton.setImageResource(R.drawable.heart); // Outline
        }
    }

    // Fav Icon Logic
    private void updateFavIcon(ImageView favButton, Map<String, Boolean> favList, String userId) {
        if (favList != null && favList.containsKey(userId)) {
            favButton.setImageResource(R.drawable.fav2); // Filled
        } else {
            favButton.setImageResource(R.drawable.fav); // Outline
        }
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    // Safe Relative Time Method
    private String getRelativeTime(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return "";
        }
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            Date postDate = format.parse(timestamp);

            if (postDate == null) return "";

            long diffInMillis = System.currentTimeMillis() - postDate.getTime();
            long seconds = diffInMillis / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;

            if (seconds < 60) {
                return "Just now";
            } else if (minutes < 60) {
                return minutes + " min ago";
            } else if (hours < 24) {
                return hours + " h";
            } else if (days == 1) {
                return "Yesterday";
            } else if (days < 30) {
                return new SimpleDateFormat("MMM d", Locale.US).format(postDate);
            } else if (days < 365) {
                return new SimpleDateFormat("MMM d", Locale.US).format(postDate);
            } else {
                return new SimpleDateFormat("MMM d, yyyy", Locale.US).format(postDate);
            }
        } catch (ParseException e) {
            e.printStackTrace();
            return "";
        }
    }

    // --- VIEW HOLDER ---
    public static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView userNameTextView, captionTextView, dateTextView, photoIndicator;
        TextView favNumTextView, heartNumTextView; // Added heartNumTextView
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
            heartNumTextView = itemView.findViewById(R.id.heart_num); // Find the heart count view

            favButton = itemView.findViewById(R.id.fav_post);
            favNumTextView = itemView.findViewById(R.id.fav_num);

            shareButton = itemView.findViewById(R.id.share_post);
        }
    }
}

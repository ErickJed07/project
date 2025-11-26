package com.example.project;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Scroller;
import android.widget.TextView;
import android.widget.Toast;
import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class D_FeedAdapter extends RecyclerView.Adapter<D_FeedAdapter.PostViewHolder> {

    private Context context;
    private List<I_PostUpload_Event> postList;
    private Handler handler;

    public D_FeedAdapter(Context context, List<I_PostUpload_Event> postList) {
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
        I_PostUpload_Event postEvent = postList.get(position);

        // Set data on the views
        holder.userNameTextView.setText(postEvent.getUsername());
        holder.captionTextView.setText(postEvent.getCaption());

        // Set initial relative time for the post date
        String relativeTime = getRelativeTime(postEvent.getDate());
        holder.dateTextView.setText(relativeTime);

        // Heart button logic
        holder.heartButton.setOnClickListener(v -> {
            // Get the current user's ID
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

            // Get the current post's postId (this should come from the postEvent object)
            String postId = postEvent.getPostId();  // Assuming you have a `getPostId()` method in I_PostUpload_Event

            // Fetch the post from Firebase
            DatabaseReference postRef = FirebaseDatabase.getInstance().getReference("PostEvents").child(postId);
            postRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        I_PostUpload_Event post = dataSnapshot.getValue(I_PostUpload_Event.class);
                        if (post == null) return; // Add a null check for safety

                        Map<String, Boolean> heartLiked = post.getHeartLiked();

                        if (heartLiked == null) {
                            heartLiked = new HashMap<>();
                        }

                        if (heartLiked.containsKey(currentUserId)) {
                            // User has already liked the post, so remove the like
                            heartLiked.remove(currentUserId);
                            Toast.makeText(context, "Removed like", Toast.LENGTH_SHORT).show();
                        } else {
                            // User hasn't liked the post yet, so add the like
                            heartLiked.put(currentUserId, true);
                            Toast.makeText(context, "Liked the post", Toast.LENGTH_SHORT).show();
                        }

                        // --- START OF FIX ---
                        // Update the heart count to match the number of likes
                        post.setHeartCount(heartLiked.size());
                        // --- END OF FIX ---

                        // Save the updated map and count back to Firebase
                        post.setHeartLiked(heartLiked);
                        postRef.setValue(post);
                    }
                }


                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Handle error if necessary
                    Toast.makeText(context, "Failed to update like", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Update the date every 10 seconds
        Runnable updateDateRunnable = new Runnable() {
            @Override
            public void run() {
                String newRelativeTime = getRelativeTime(postEvent.getDate());
                holder.dateTextView.setText(newRelativeTime);  // Update the date on the screen
                handler.postDelayed(this, 10000); // Repeat every 10 seconds (10,000 milliseconds)
            }
        };
        handler.postDelayed(updateDateRunnable, 10000);  // Initial update in 10 seconds

        // Set smooth scrolling for ViewPager2
        setViewPagerSmoothScroll(holder.viewPager2);

        // Check if there are images and set ViewPager2 adapter
        if (postEvent.getImageUrls() != null && !postEvent.getImageUrls().isEmpty()) {
            D_Feed_ImageViewAdapter imageAdapter = new D_Feed_ImageViewAdapter(context, postEvent.getImageUrls());
            holder.viewPager2.setAdapter(imageAdapter);

            // Bind the circle indicator to the ViewPager2
            holder.dotsIndicator.setViewPager2(holder.viewPager2);

            // Update the page number display (e.g., "1/5")
            holder.viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    int totalPhotos = imageAdapter.getItemCount();
                    holder.photoIndicator.setText((position + 1) + "/" + totalPhotos);
                }
            });

            // If there is only one image, hide the dots indicator
            if (imageAdapter.getItemCount() <= 1) {
                holder.dotsIndicator.setVisibility(View.GONE); // Hide dots indicator if only one image
                holder.photoIndicator.setVisibility(View.GONE); // Hide photo page indicator if only one image
            } else {
                holder.dotsIndicator.setVisibility(View.VISIBLE); // Show dots indicator if more than one image
                holder.photoIndicator.setVisibility(View.VISIBLE); // Show photo page indicator if more than one image
            }
        }
    }


    @Override
    public int getItemCount() {
        return postList.size();
    }

    // Method to calculate the relative time (e.g., "2 min ago", "1 hour ago")
    private String getRelativeTime(String timestamp) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            Date postDate = format.parse(timestamp);
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
            return timestamp;
        }
    }


    // Set smooth scroll for ViewPager2
    private void setViewPagerSmoothScroll(ViewPager2 viewPager2) {
        try {
            Field scrollerField = ViewPager2.class.getDeclaredField("mScroller");
            scrollerField.setAccessible(true);
            Scroller scroller = (Scroller) scrollerField.get(viewPager2);
            Method setDurationMethod = scroller.getClass().getDeclaredMethod("setDuration", int.class);
            setDurationMethod.setAccessible(true);
            setDurationMethod.invoke(scroller, 800);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView userNameTextView, captionTextView, dateTextView, photoIndicator;
        ViewPager2 viewPager2;
        WormDotsIndicator dotsIndicator;
        ImageView heartButton;  // Heart and Fav Post icons

        public PostViewHolder(View itemView) {
            super(itemView);
            userNameTextView = itemView.findViewById(R.id.userName_post);
            captionTextView = itemView.findViewById(R.id.caption_post);
            dateTextView = itemView.findViewById(R.id.postdate);
            viewPager2 = itemView.findViewById(R.id.post_Pic);
            photoIndicator = itemView.findViewById(R.id.photoIndicator);
            dotsIndicator = itemView.findViewById(R.id.dotsIndicator); // Find the Fav Post icon
            heartButton  = itemView.findViewById(R.id.heart_post);
        }
    }
}

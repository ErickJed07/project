package com.example.project;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Scroller;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class D_FeedAdapter extends RecyclerView.Adapter<D_FeedAdapter.PostViewHolder> {

    private Context context;
    private List<I_PostUpload_Event> postList;
    private Handler handler;

    public D_FeedAdapter(Context context, List<I_PostUpload_Event> postList) {
        this.context = context;
        this.postList = postList;
        this.handler = new Handler(Looper.getMainLooper()); // Create a handler for the main thread
        sortPostsByTimeDifference(); // Sort posts by time difference initially
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
            // Firebase timestamp format: "2025-11-24T07:18:26Z"
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            Date postDate = format.parse(timestamp);

            // Get the difference between the current time and post time
            long diffInMillis = System.currentTimeMillis() - postDate.getTime();
            long seconds = diffInMillis / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;

            // Return relative time in a readable format
            if (seconds < 60) {
                return seconds + " sec ago";
            } else if (minutes < 60) {
                return minutes + " min ago";
            } else if (hours < 24) {
                return hours + " hour ago";
            } else if (days < 30) {
                return days + " day ago";
            } else if (days < 365) {
                return days / 30 + " month ago";
            } else {
                return days / 365 + " year ago";
            }
        } catch (ParseException e) {
            e.printStackTrace();
            return timestamp; // Fallback to raw timestamp if parsing fails
        }
    }

    // Sort posts by their time difference (most recent first)
    private void sortPostsByTimeDifference() {
        Collections.sort(postList, (post1, post2) -> {
            try {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                Date date1 = format.parse(post1.getDate());
                Date date2 = format.parse(post2.getDate());

                // Compare the time differences
                return Long.compare(date2.getTime(), date1.getTime()); // Sort in descending order (most recent first)
            } catch (ParseException e) {
                e.printStackTrace();
                return 0; // If parsing fails, no sorting
            }
        });
    }

    // Set smooth scroll for ViewPager2 (duration modification)
    private void setViewPagerSmoothScroll(ViewPager2 viewPager2) {
        try {
            // Access the mScroller field using reflection to modify scroll duration
            Field scrollerField = ViewPager2.class.getDeclaredField("mScroller");
            scrollerField.setAccessible(true);
            Scroller scroller = (Scroller) scrollerField.get(viewPager2);

            // Set smooth scrolling speed (lower value means faster scrolling)
            Method setDurationMethod = scroller.getClass().getDeclaredMethod("setDuration", int.class);
            setDurationMethod.setAccessible(true);
            setDurationMethod.invoke(scroller, 800); // Duration in milliseconds (default is around 300ms)
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView userNameTextView, captionTextView, dateTextView, photoIndicator;
        ViewPager2 viewPager2;
        WormDotsIndicator dotsIndicator;

        public PostViewHolder(View itemView) {
            super(itemView);
            userNameTextView = itemView.findViewById(R.id.userName_post);
            captionTextView = itemView.findViewById(R.id.caption_post);
            dateTextView = itemView.findViewById(R.id.postdate);
            viewPager2 = itemView.findViewById(R.id.post_Pic);
            photoIndicator = itemView.findViewById(R.id.photoIndicator); // Photo page indicator
            dotsIndicator = itemView.findViewById(R.id.dotsIndicator);  // Circle dots indicator
        }
    }
}

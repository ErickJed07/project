package com.example.project;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
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
import java.util.HashMap;
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

        // --- 1. MENU LOGIC (Follow/Report) ---
        holder.menuOptions.setOnClickListener(v -> {
            showPopupMenu(holder.menuOptions, postEvent);
        });

        // --- 2. Set Basic Data ---
        holder.userNameTextView.setText(postEvent.getUsername());
        holder.captionTextView.setText(postEvent.getCaption());
        holder.dateTextView.setText(getRelativeTime(postEvent.getDate()));

        // --- 3. Get IDs Safely ---
        String currentUserId = "";
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        final String finalCurrentUserId = currentUserId;
        String postId = postEvent.getPostId();
        String postAuthorId = postEvent.getUserId(); // Get the author's ID

        // --- 4. FETCH PROFILE PHOTO FROM FIREBASE USERS NODE ---
        if (postAuthorId != null && !postAuthorId.isEmpty()) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(postAuthorId);
            userRef.child("profilePhoto").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String profilePhotoUrl = snapshot.getValue(String.class);

                        // Check if URL is valid (not null, empty, or "default")
                        if (profilePhotoUrl != null && !profilePhotoUrl.isEmpty() && !profilePhotoUrl.equals("default")) {
                            try {
                                Glide.with(context)
                                        .load(profilePhotoUrl)
                                        .placeholder(R.drawable.ic_placeholder_2)
                                        .error(R.drawable.ic_placeholder_2)
                                        .circleCrop()
                                        .into(holder.profilePic);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            Glide.with(context).load(R.drawable.ic_placeholder_2).circleCrop().into(holder.profilePic);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        }

        // --- 5. DYNAMIC IMAGE RESIZING (WARP CONTENT LOGIC) ---
        if (postEvent.getImageUrls() != null && !postEvent.getImageUrls().isEmpty()) {

            String firstImageUrl = postEvent.getImageUrls().get(0);

            ViewGroup.LayoutParams resetParams = holder.viewPager2.getLayoutParams();
            resetParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            holder.viewPager2.setLayoutParams(resetParams);

            Glide.with(context)
                    .asBitmap()
                    .load(firstImageUrl)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            int imageWidth = resource.getWidth();
                            int imageHeight = resource.getHeight();

                            if (imageWidth > 0) {
                                float aspectRatio = (float) imageHeight / imageWidth;
                                int viewPagerWidth = holder.viewPager2.getWidth();

                                if (viewPagerWidth == 0) {
                                    viewPagerWidth = context.getResources().getDisplayMetrics().widthPixels;
                                }

                                int newHeight = (int) (viewPagerWidth * aspectRatio);

                                ViewGroup.LayoutParams layoutParams = holder.viewPager2.getLayoutParams();
                                layoutParams.height = newHeight;
                                holder.viewPager2.setLayoutParams(layoutParams);
                            }
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                        }
                    });

            D_Feed_ImageViewAdapter imageAdapter = new D_Feed_ImageViewAdapter(context, postEvent.getImageUrls());
            holder.viewPager2.setAdapter(imageAdapter);
            holder.dotsIndicator.setViewPager2(holder.viewPager2);

            holder.viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    int totalPhotos = imageAdapter.getItemCount();
                    holder.photoIndicator.setText((position + 1) + "/" + totalPhotos);
                }
            });

            if (imageAdapter.getItemCount() <= 1) {
                holder.dotsIndicator.setVisibility(View.GONE);
                holder.photoIndicator.setVisibility(View.GONE);
            } else {
                holder.dotsIndicator.setVisibility(View.VISIBLE);
                holder.photoIndicator.setVisibility(View.VISIBLE);
            }
        } else {
            holder.viewPager2.setVisibility(View.GONE);
        }

        // --- 6. Initial UI State Setup ---
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

        // --- 7. Click Listeners ---

        // Profile Picture Click
        holder.profilePic.setOnClickListener(v -> {
            if (postAuthorId != null && !postAuthorId.isEmpty()) {
                Intent intent = new Intent(context, I1_ProfileActivity.class);
                intent.putExtra("USER_ID", postAuthorId);
                context.startActivity(intent);
            }
        });

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

        // Favorite Button
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
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Check out this post!");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareContent);
            context.startActivity(Intent.createChooser(shareIntent, "Share post via"));
        });
    }

    // --- HELPER METHODS FOR MENU & ACTIONS ---
    private void showPopupMenu(android.view.View view, I_NewPost_Event post) {
        // USE THIS CONSTRUCTOR (API 22+):
        // Arguments: Context, Anchor, Gravity, Attribute, STYLE RESOURCE
        // This forces the R.style.PurplePopupMenu to apply.
        PopupMenu popupMenu = new PopupMenu(context, view);

        // Constants
        final int ID_FOLLOW = 1;
        final int ID_REPORT = 2;

        // Get User ID
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        // Check Database (ModelsList)
        DatabaseReference followRef = FirebaseDatabase.getInstance().getReference("Users")
                .child(currentUserId)
                .child("ModelsList")
                .child(post.getUserId());

        followRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String followText = snapshot.exists() ? "Unfollow" : "Follow";

                // 1. Add Follow Item
                popupMenu.getMenu().add(0, ID_FOLLOW, 0, followText);

                // 2. Add Report Item with RED Text
                android.text.SpannableString reportSpan = new android.text.SpannableString("Report");
                reportSpan.setSpan(new android.text.style.ForegroundColorSpan(android.graphics.Color.RED), 0, reportSpan.length(), 0);
                popupMenu.getMenu().add(0, ID_REPORT, 1, reportSpan);

                popupMenu.show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                popupMenu.getMenu().add(0, ID_FOLLOW, 0, "Follow");

                android.text.SpannableString reportSpan = new android.text.SpannableString("Report");
                reportSpan.setSpan(new android.text.style.ForegroundColorSpan(android.graphics.Color.RED), 0, reportSpan.length(), 0);
                popupMenu.getMenu().add(0, ID_REPORT, 1, reportSpan);

                popupMenu.show();
            }
        });

        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == ID_FOLLOW) {
                followUser(post.getUserId());
                return true;
            } else if (id == ID_REPORT) {
                reportPost(post.getPostId());
                return true;
            }
            return false;
        });
    }

    private void followUser(String userToFollowId) {
        if (userToFollowId == null || userToFollowId.isEmpty()) return;

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (currentUserId.equals(userToFollowId)) {
            Toast.makeText(context, "You cannot follow yourself.", Toast.LENGTH_SHORT).show();
            return;
        }


        // Change reference to check 'ModelsList' inside the current user's node
        DatabaseReference followRef = FirebaseDatabase.getInstance().getReference("Users")
                .child(currentUserId)
                .child("ModelsList")
                .child(userToFollowId);

        followRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // --- CASE: ALREADY FOLLOWING -> UNFOLLOW ---

                    // 1. Remove from "ModelsList" (Me -> Them)
                    followRef.removeValue();

                    // 2. Remove from "FansList" (Them -> Me)
                    FirebaseDatabase.getInstance().getReference().child("Users").child(userToFollowId)
                            .child("FansList").child(currentUserId).removeValue();

                    // 3. Update Counts (-1)
                    updateCount(currentUserId, "Models", -1);
                    updateCount(userToFollowId, "Fans", -1);

                    Toast.makeText(context, "Unfollowed user.", Toast.LENGTH_SHORT).show();

                } else {
                    // --- CASE: NOT FOLLOWING -> FOLLOW ---

                    // 1. Add to "ModelsList" (Me -> Them)
                    followRef.setValue(true);

                    // 2. Add to "FansList" (Them -> Me)
                    FirebaseDatabase.getInstance().getReference().child("Users").child(userToFollowId)
                            .child("FansList").child(currentUserId).setValue(true);

                    // 3. Update Counts (+1)
                    updateCount(currentUserId, "Models", 1);
                    updateCount(userToFollowId, "Fans", 1);

                    Toast.makeText(context, "User followed!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, "Action failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCount(String userId, String field, int increment) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(userId).child(field);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long currentCount = 0;
                if (snapshot.exists()) {
                    try {
                        currentCount = Long.parseLong(snapshot.getValue().toString());
                    } catch (Exception e) {
                        currentCount = 0;
                    }
                }
                // Don't allow negative numbers
                long newCount = Math.max(0, currentCount + increment);
                ref.setValue(newCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void reportPost(String postId) {
        if (postId == null || postId.isEmpty()) return;

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        // CHANGED: Now saving reports inside PostEvents -> [postId] -> reports
        DatabaseReference reportRef = FirebaseDatabase.getInstance().getReference("PostEvents")
                .child(postId)
                .child("reports");

        // Check if user already reported this post
        reportRef.child(currentUserId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                Toast.makeText(context, "You have already reported this post.", Toast.LENGTH_SHORT).show();
            } else {
                // Add new report
                reportRef.child(currentUserId).setValue(true).addOnCompleteListener(reportTask -> {
                    if (reportTask.isSuccessful()) {
                        Toast.makeText(context, "Post reported. Thank you.", Toast.LENGTH_SHORT).show();
                        // Check if we should delete
                        checkReportCount(postId);
                    }
                });
            }
        });
    }

    private void checkReportCount(String postId) {
        // CHANGED: Checking count inside PostEvents -> [postId] -> reports
        DatabaseReference reportRef = FirebaseDatabase.getInstance().getReference("PostEvents")
                .child(postId)
                .child("reports");

        reportRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                long reportCount = task.getResult().getChildrenCount();

                // Logic: If 10 or more users reported it, delete the post
                if (reportCount >= 10) {
                    deletePost(postId);
                }
            }
        });
    }

    private void deletePost(String postId) {
        DatabaseReference postRef = FirebaseDatabase.getInstance().getReference("PostEvents").child(postId);
        postRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Reports are now child nodes of the post, so they are deleted automatically when the post is deleted.
                Toast.makeText(context, "Post removed due to community reports.", Toast.LENGTH_LONG).show();
            }
        });
    }


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
        ImageView profilePic;
        TextView userNameTextView, captionTextView, dateTextView, photoIndicator;
        TextView favNumTextView, heartNumTextView;
        ViewPager2 viewPager2;
        WormDotsIndicator dotsIndicator;
        ImageView heartButton, favButton, shareButton, menuOptions; // Added menuOptions

        public PostViewHolder(View itemView) {
            super(itemView);
            profilePic = itemView.findViewById(R.id.profile_pic);
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
            menuOptions = itemView.findViewById(R.id.menu_options); // Bind menuOptions
        }
    }
}

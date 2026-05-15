package com.example.project;

import android.content.Context;
import android.content.Intent;
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
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class D_FeedAdapter extends RecyclerView.Adapter<D_FeedAdapter.PostViewHolder> {

    private Context context;
    private List<I_PostEvent> postList;
    private Handler handler;

    public D_FeedAdapter(Context context, List<I_PostEvent> postList) {
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
        I_PostEvent postEvent = postList.get(position);

        holder.menuOptions.setOnClickListener(v -> showPopupMenu(holder.menuOptions, postEvent));
        holder.userNameTextView.setText(postEvent.getUsername());
        if (holder.captionTextView != null) {
            String caption = postEvent.getCaption();
            if (caption != null && !caption.trim().isEmpty()) {
                holder.captionTextView.setText(caption);
                holder.captionTextView.setVisibility(View.VISIBLE);
            } else {
                holder.captionTextView.setVisibility(View.GONE);
            }
        }
        if (holder.dateTextView != null) holder.dateTextView.setText(getRelativeTime(postEvent.getDate()));

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        final String finalCurrentUserId = currentUserId;
        String postId = postEvent.getPostId();
        String postAuthorId = postEvent.getUserId();

        if (postAuthorId != null && !postAuthorId.isEmpty()) {
            FirebaseDatabase.getInstance().getReference("Users").child(postAuthorId).child("profilePhoto").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!isValidContextForGlide(context)) return;
                    if (snapshot.exists()) {
                        String url = snapshot.getValue(String.class);
                        Glide.with(context).load(url != null && !url.isEmpty() && !url.equals("default") ? url : R.drawable.ic_placeholder_2).circleCrop().into(holder.profilePic);
                    }
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });
        }

        if (postEvent.getImageUrls() != null && !postEvent.getImageUrls().isEmpty()) {
            String firstImageUrl = postEvent.getImageUrls().get(0);
            
            // Grid mode: use single ImageView
            if (holder.postMainImage != null) {
                Glide.with(context).load(firstImageUrl).centerCrop().into(holder.postMainImage);
                holder.postMainImage.setOnClickListener(v -> showPostDetailDialog(postEvent));
            }

            // Compatibility with legacy viewpager if present
            if (holder.viewPager2 != null && holder.viewPager2.getVisibility() == View.VISIBLE) {
                D_Feed_ImageViewAdapter imageAdapter = new D_Feed_ImageViewAdapter(context, postEvent.getImageUrls());
                holder.viewPager2.setAdapter(imageAdapter);
                if (holder.dotsIndicator != null) holder.dotsIndicator.setViewPager2(holder.viewPager2);
                holder.viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                    @Override public void onPageSelected(int pos) { if (holder.photoIndicator != null) holder.photoIndicator.setText((pos + 1) + "/" + imageAdapter.getItemCount()); }
                });
            }
        }

        updateHeartIcon(holder.heartButton, holder.heartNumTextView, postEvent.getHeartLiked(), currentUserId);
        holder.heartNumTextView.setText(formatCount(postEvent.getHeartLiked() != null ? postEvent.getHeartLiked().size() : 0));
        updateFavIcon(holder.favButton, holder.favNumTextView, postEvent.getFavList(), currentUserId);
        holder.favNumTextView.setText(formatCount(postEvent.getFavList() != null ? postEvent.getFavList().size() : 0));

        holder.profilePic.setOnClickListener(v -> {
            if (postAuthorId != null) {
                if (postAuthorId.equals(finalCurrentUserId)) {
                    context.startActivity(new Intent(context, I_ProfileActivity.class));
                } else {
                    context.startActivity(new Intent(context, I_UserProfileActivity.class).putExtra(I_UserProfileActivity.EXTRA_USER_ID, postAuthorId));
                }
            }
        });

        holder.userNameTextView.setOnClickListener(v -> {
            if (postAuthorId != null) {
                if (postAuthorId.equals(finalCurrentUserId)) {
                    context.startActivity(new Intent(context, I_ProfileActivity.class));
                } else {
                    context.startActivity(new Intent(context, I_UserProfileActivity.class).putExtra(I_UserProfileActivity.EXTRA_USER_ID, postAuthorId));
                }
            }
        });

        holder.heartButton.setOnClickListener(v -> {
            if (postId == null || finalCurrentUserId.isEmpty()) return;
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("PostEvents").child(postId);
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override public void onDataChange(DataSnapshot s) {
                    I_PostEvent p = s.getValue(I_PostEvent.class);
                    if (p == null) return;
                    Map<String, Boolean> likes = p.getHeartLiked() == null ? new HashMap<>() : p.getHeartLiked();
                    boolean isAdding = !likes.containsKey(finalCurrentUserId);
                    if (likes.containsKey(finalCurrentUserId)) likes.remove(finalCurrentUserId); else likes.put(finalCurrentUserId, true);
                    p.setHeartCount(likes.size()); p.setHeartLiked(likes); ref.setValue(p);
                    holder.heartNumTextView.setText(formatCount(likes.size()));
                    updateHeartIcon(holder.heartButton, holder.heartNumTextView, likes, finalCurrentUserId);
                    
                    if (postAuthorId != null && !postAuthorId.equals(finalCurrentUserId)) {
                        FirebaseDatabase.getInstance().getReference("Users").child(finalCurrentUserId).child("username").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override public void onDataChange(@NonNull DataSnapshot s) {
                                String myUsername = s.getValue(String.class);
                                String name = (myUsername != null ? myUsername : "Someone");
                                if (isAdding) {
                                    NotificationHelper.sendNotification(postAuthorId, "Post Liked", name + " liked your post!", finalCurrentUserId);
                                } else {
                                    NotificationHelper.sendNotification(postAuthorId, "Post Unliked", name + " removed like from your post.", finalCurrentUserId);
                                }
                            }
                            @Override public void onCancelled(@NonNull DatabaseError e) {}
                        });
                    }
                }
                @Override public void onCancelled(DatabaseError e) {}
            });
        });

        holder.favButton.setOnClickListener(v -> {
            if (postId == null || finalCurrentUserId.isEmpty()) return;
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("PostEvents").child(postId);
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override public void onDataChange(DataSnapshot s) {
                    I_PostEvent p = s.getValue(I_PostEvent.class);
                    if (p == null) return;
                    Map<String, Boolean> favs = p.getFavList() == null ? new HashMap<>() : p.getFavList();
                    boolean isAdding = !favs.containsKey(finalCurrentUserId);
                    if (favs.containsKey(finalCurrentUserId)) favs.remove(finalCurrentUserId); else favs.put(finalCurrentUserId, true);
                    p.setFavCount(favs.size()); p.setFavList(favs); ref.setValue(p);
                    holder.favNumTextView.setText(formatCount(favs.size()));
                    updateFavIcon(holder.favButton, holder.favNumTextView, favs, finalCurrentUserId);

                    if (postAuthorId != null && !postAuthorId.equals(finalCurrentUserId)) {
                        FirebaseDatabase.getInstance().getReference("Users").child(finalCurrentUserId).child("username").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override public void onDataChange(@NonNull DataSnapshot s) {
                                String myUsername = s.getValue(String.class);
                                String name = (myUsername != null ? myUsername : "Someone");
                                if (isAdding) {
                                    NotificationHelper.sendNotification(postAuthorId, "Post Favorited", name + " added your post to favorites!", finalCurrentUserId);
                                } else {
                                    NotificationHelper.sendNotification(postAuthorId, "Post Unfavorited", name + " removed your post from favorites.", finalCurrentUserId);
                                }
                            }
                            @Override public void onCancelled(@NonNull DatabaseError e) {}
                        });
                    }
                }
                @Override public void onCancelled(DatabaseError e) {}
            });
        });

        holder.shareButton.setOnClickListener(v -> context.startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, postEvent.getUsername() + " posted: " + postEvent.getCaption()), "Share")));
    }

    private void showPopupMenu(View view, I_PostEvent post) {
        PopupMenu menu = new PopupMenu(context, view);
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        FirebaseDatabase.getInstance().getReference("Users").child(uid).child("ModelsList").child(post.getUserId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                menu.getMenu().add(0, 1, 0, s.exists() ? "Unfollow" : "Follow");
                android.text.SpannableString report = new android.text.SpannableString("Report");
                report.setSpan(new android.text.style.ForegroundColorSpan(android.graphics.Color.RED), 0, report.length(), 0);
                menu.getMenu().add(0, 2, 1, report);
                menu.show();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) followUser(post.getUserId());
            else if (item.getItemId() == 2) reportPost(post.getPostId());
            return true;
        });
    }

    private void followUser(String targetId) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (uid.equals(targetId)) return;
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("ModelsList").child(targetId);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                if (s.exists()) {
                    ref.removeValue();
                    FirebaseDatabase.getInstance().getReference("Users").child(targetId).child("FansList").child(uid).removeValue();
                    updateCount(uid, "Models", -1); updateCount(targetId, "Fans", -1);

                    // Add notification for Unfollow
                    FirebaseDatabase.getInstance().getReference("Users").child(uid).child("username").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot s) {
                            String myUsername = s.getValue(String.class);
                            NotificationHelper.sendNotification(targetId, "Lost a Fan", (myUsername != null ? myUsername : "Someone") + " unfollowed you.", uid);
                        }
                        @Override public void onCancelled(@NonNull DatabaseError e) {}
                    });
                } else {
                    ref.setValue(true);
                    FirebaseDatabase.getInstance().getReference("Users").child(targetId).child("FansList").child(uid).setValue(true);
                    updateCount(uid, "Models", 1); updateCount(targetId, "Fans", 1);
                    
                    // Add notification
                    FirebaseDatabase.getInstance().getReference("Users").child(uid).child("username").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot s) {
                            String myUsername = s.getValue(String.class);
                            NotificationHelper.sendNotification(targetId, "New Fan", (myUsername != null ? myUsername : "Someone") + " started following you!", uid);
                        }
                        @Override public void onCancelled(@NonNull DatabaseError e) {}
                    });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private void updateCount(String uid, String field, int inc) {
        DatabaseReference r = FirebaseDatabase.getInstance().getReference("Users").child(uid).child(field);
        r.runTransaction(new com.google.firebase.database.Transaction.Handler() {
            @NonNull
            @Override
            public com.google.firebase.database.Transaction.Result doTransaction(@NonNull com.google.firebase.database.MutableData mutableData) {
                Long count = mutableData.getValue(Long.class);
                if (count == null) {
                    mutableData.setValue(Math.max(0L, (long) inc));
                } else {
                    mutableData.setValue(Math.max(0L, count + inc));
                }
                return com.google.firebase.database.Transaction.success(mutableData);
            }

            @Override
            public void onComplete(@androidx.annotation.Nullable DatabaseError databaseError, boolean committed, @androidx.annotation.Nullable DataSnapshot dataSnapshot) {
            }
        });
    }

    private void reportPost(String id) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference r = FirebaseDatabase.getInstance().getReference("PostEvents").child(id).child("reports");
        r.child(uid).get().addOnCompleteListener(t -> {
            if (t.isSuccessful() && !t.getResult().exists()) {
                r.child(uid).setValue(true).addOnCompleteListener(rt -> { if (rt.isSuccessful()) checkReportCount(id); });
            }
        });
    }

    private void checkReportCount(String id) {
        DatabaseReference r = FirebaseDatabase.getInstance().getReference("PostEvents").child(id).child("reports");
        r.get().addOnCompleteListener(t -> { if (t.isSuccessful() && t.getResult().getChildrenCount() >= 10) FirebaseDatabase.getInstance().getReference("PostEvents").child(id).removeValue(); });
    }

    private boolean isValidContextForGlide(Context context) {
        if (context == null) return false;
        if (context instanceof android.app.Activity) {
            android.app.Activity activity = (android.app.Activity) context;
            return !activity.isDestroyed() && !activity.isFinishing();
        }
        return true;
    }

    private void showPostDetailDialog(I_PostEvent post) {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_feed_post_detail);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        ViewPager2 viewPager = dialog.findViewById(R.id.vp_full_post_images);
        WormDotsIndicator dotsIndicator = dialog.findViewById(R.id.dots_full_post);
        TextView captionText = dialog.findViewById(R.id.tv_full_post_caption);
        View closeBtn = dialog.findViewById(R.id.btn_close_dialog);
        
        // New interactive elements
        TextView usernameText = dialog.findViewById(R.id.tv_username_dialog);
        ImageView profilePic = dialog.findViewById(R.id.iv_user_profile_dialog);
        ImageView likeBtn = dialog.findViewById(R.id.iv_like_dialog);
        TextView likeCount = dialog.findViewById(R.id.tv_likes_count_dialog);
        ImageView favBtn = dialog.findViewById(R.id.iv_fav_dialog);
        TextView favCount = dialog.findViewById(R.id.tv_favs_count_dialog);
        ImageView shareBtn = dialog.findViewById(R.id.iv_share_dialog);

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        // Set User Info
        if (usernameText != null) usernameText.setText(post.getUsername());
        if (profilePic != null && post.getUserId() != null) {
            FirebaseDatabase.getInstance().getReference("Users").child(post.getUserId()).child("profilePhoto").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override public void onDataChange(@NonNull DataSnapshot s) {
                    if (s.exists()) {
                        String url = s.getValue(String.class);
                        Glide.with(context).load(url != null && !url.isEmpty() && !url.equals("default") ? url : R.drawable.ic_placeholder_2).circleCrop().into(profilePic);
                    }
                }
                @Override public void onCancelled(@NonNull DatabaseError e) {}
            });
        }

        // Set Interaction States
        if (likeCount != null) likeCount.setText(formatCount(post.getHeartLiked() != null ? post.getHeartLiked().size() : 0));
        if (favCount != null) favCount.setText(formatCount(post.getFavList() != null ? post.getFavList().size() : 0));
        updateHeartIcon(likeBtn, likeCount, post.getHeartLiked(), currentUserId);
        updateFavIcon(favBtn, favCount, post.getFavList(), currentUserId);

        // Interaction Listeners
        if (likeBtn != null) {
            likeBtn.setOnClickListener(v -> {
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("PostEvents").child(post.getPostId());
                ref.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot s) {
                        I_PostEvent p = s.getValue(I_PostEvent.class);
                        if (p == null) return;
                        Map<String, Boolean> likes = p.getHeartLiked() == null ? new HashMap<>() : p.getHeartLiked();
                        boolean isAdding = !likes.containsKey(currentUserId);
                        if (likes.containsKey(currentUserId)) likes.remove(currentUserId); else likes.put(currentUserId, true);
                        p.setHeartCount(likes.size()); p.setHeartLiked(likes); ref.setValue(p);
                        if (likeCount != null) likeCount.setText(formatCount(likes.size()));
                        updateHeartIcon(likeBtn, likeCount, likes, currentUserId);
                        notifyDataSetChanged(); // Sync with main feed

                        if (post.getUserId() != null && !post.getUserId().equals(currentUserId)) {
                            FirebaseDatabase.getInstance().getReference("Users").child(currentUserId).child("username").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override public void onDataChange(@NonNull DataSnapshot s) {
                                    String myUsername = s.getValue(String.class);
                                    String name = (myUsername != null ? myUsername : "Someone");
                                    if (isAdding) NotificationHelper.sendNotification(post.getUserId(), "Post Liked", name + " liked your post!", currentUserId);
                                    else NotificationHelper.sendNotification(post.getUserId(), "Post Unliked", name + " removed like from your post.", currentUserId);
                                }
                                @Override public void onCancelled(@NonNull DatabaseError e) {}
                            });
                        }
                    }
                    @Override public void onCancelled(DatabaseError e) {}
                });
            });
        }

        if (favBtn != null) {
            favBtn.setOnClickListener(v -> {
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("PostEvents").child(post.getPostId());
                ref.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot s) {
                        I_PostEvent p = s.getValue(I_PostEvent.class);
                        if (p == null) return;
                        Map<String, Boolean> favs = p.getFavList() == null ? new HashMap<>() : p.getFavList();
                        boolean isAdding = !favs.containsKey(currentUserId);
                        if (favs.containsKey(currentUserId)) favs.remove(currentUserId); else favs.put(currentUserId, true);
                        p.setFavCount(favs.size()); p.setFavList(favs); ref.setValue(p);
                        if (favCount != null) favCount.setText(formatCount(favs.size()));
                        updateFavIcon(favBtn, favCount, favs, currentUserId);
                        notifyDataSetChanged(); // Sync with main feed

                        if (post.getUserId() != null && !post.getUserId().equals(currentUserId)) {
                            FirebaseDatabase.getInstance().getReference("Users").child(currentUserId).child("username").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override public void onDataChange(@NonNull DataSnapshot s) {
                                    String myUsername = s.getValue(String.class);
                                    String name = (myUsername != null ? myUsername : "Someone");
                                    if (isAdding) NotificationHelper.sendNotification(post.getUserId(), "Post Favorited", name + " added your post to favorites!", currentUserId);
                                    else NotificationHelper.sendNotification(post.getUserId(), "Post Unfavorited", name + " removed your post from favorites.", currentUserId);
                                }
                                @Override public void onCancelled(@NonNull DatabaseError e) {}
                            });
                        }
                    }
                    @Override public void onCancelled(DatabaseError e) {}
                });
            });
        }

        if (shareBtn != null) {
            shareBtn.setOnClickListener(v -> context.startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, post.getUsername() + " posted: " + post.getCaption()), "Share")));
        }

        // Load More Posts from User
        RecyclerView rvMore = dialog.findViewById(R.id.rv_more_posts);
        TextView moreLabel = dialog.findViewById(R.id.tv_more_from_user);
        if (moreLabel != null) moreLabel.setText("More from " + post.getUsername());
        
        if (rvMore != null && post.getUserId() != null) {
            List<I_PostEvent> morePosts = new ArrayList<>();
            RecyclerView.Adapter moreAdapter = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
                    View item = LayoutInflater.from(context).inflate(R.layout.i_profile_contents_gridimage, p, false);
                    // Adjust size for horizontal preview
                    ViewGroup.LayoutParams lp = item.getLayoutParams();
                    lp.width = (int) android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_DIP, 100, context.getResources().getDisplayMetrics());
                    item.setLayoutParams(lp);
                    return new RecyclerView.ViewHolder(item) {};
                }
                @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int p) {
                    I_PostEvent pe = morePosts.get(p);
                    ImageView iv = h.itemView.findViewById(R.id.gridImageView);
                    if (pe.getImageUrls() != null && !pe.getImageUrls().isEmpty()) {
                        Glide.with(context).load(pe.getImageUrls().get(0)).centerCrop().into(iv);
                    }
                    iv.setOnClickListener(v -> {
                        dialog.dismiss();
                        showPostDetailDialog(pe);
                    });
                }
                @Override public int getItemCount() { return morePosts.size(); }
            };
            rvMore.setAdapter(moreAdapter);
            rvMore.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));

            FirebaseDatabase.getInstance().getReference("PostEvents")
                    .orderByChild("userId").equalTo(post.getUserId())
                    .limitToLast(10)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                            morePosts.clear();
                            for (DataSnapshot ds : snapshot.getChildren()) {
                                I_PostEvent p = ds.getValue(I_PostEvent.class);
                                if (p != null) {
                                    p.setPostId(ds.getKey());
                                    if (!p.getPostId().equals(post.getPostId())) morePosts.add(p);
                                }
                            }
                            java.util.Collections.reverse(morePosts);
                            moreAdapter.notifyDataSetChanged();
                            if (morePosts.isEmpty()) {
                                if (moreLabel != null) moreLabel.setVisibility(View.GONE);
                                rvMore.setVisibility(View.GONE);
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
        }

        if (post.getImageUrls() != null && !post.getImageUrls().isEmpty()) {
            viewPager.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
                    ImageView iv = new ImageView(context);
                    iv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    return new RecyclerView.ViewHolder(iv) {};
                }
                @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int p) {
                    Glide.with(context).load(post.getImageUrls().get(p)).into((ImageView) h.itemView);
                }
                @Override public int getItemCount() { return post.getImageUrls().size(); }
            });

            if (post.getImageUrls().size() > 1) {
                dotsIndicator.setViewPager2(viewPager);
                dotsIndicator.setVisibility(View.VISIBLE);
            } else {
                dotsIndicator.setVisibility(View.GONE);
            }
        }

        if (post.getCaption() != null && !post.getCaption().trim().isEmpty()) {
            captionText.setText(post.getCaption());
            captionText.setVisibility(View.VISIBLE);
        } else {
            captionText.setVisibility(View.GONE);
        }

        closeBtn.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private String formatCount(int count) {
        if (count < 1000) return String.valueOf(count);
        return String.format(Locale.US, "%.1fk", count / 1000.0);
    }

    private void updateHeartIcon(ImageView b, TextView t, Map<String, Boolean> m, String u) {
        boolean liked = m != null && m.containsKey(u);
        b.setImageResource(liked ? R.drawable.heart2 : R.drawable.heart);
        if (liked) {
            b.setColorFilter(android.graphics.Color.parseColor("#D946EF"));
            if (t != null) t.setTextColor(android.graphics.Color.parseColor("#D946EF"));
        } else {
            b.setColorFilter(android.graphics.Color.BLACK);
            if (t != null) t.setTextColor(android.graphics.Color.BLACK);
        }
    }

    private void updateFavIcon(ImageView b, TextView t, Map<String, Boolean> m, String u) {
        boolean faved = m != null && m.containsKey(u);
        b.setImageResource(faved ? R.drawable.closet_2 : R.drawable.closet);
        b.setColorFilter(android.graphics.Color.BLACK);
        if (t != null) t.setTextColor(android.graphics.Color.BLACK);
    }

    @Override public int getItemCount() { return postList.size(); }

    private String getRelativeTime(String ts) {
        if (ts == null || ts.isEmpty()) return "";
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).parse(ts);
            if (d == null) return "";
            long diff = (System.currentTimeMillis() - d.getTime()) / 1000;
            if (diff < 60) return "Just now";
            if (diff < 3600) return (diff / 60) + " min ago";
            if (diff < 86400) return (diff / 3600) + " h";
            return new SimpleDateFormat("MMM d", Locale.US).format(d);
        } catch (Exception e) { return ""; }
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView profilePic, heartButton, favButton, shareButton, menuOptions, postMainImage;
        TextView userNameTextView, captionTextView, dateTextView, photoIndicator, favNumTextView, heartNumTextView;
        ViewPager2 viewPager2; WormDotsIndicator dotsIndicator;
        public PostViewHolder(View v) {
            super(v);
            profilePic = v.findViewById(R.id.profile_pic); userNameTextView = v.findViewById(R.id.userName_post); captionTextView = v.findViewById(R.id.caption_post);
            dateTextView = v.findViewById(R.id.postdate); viewPager2 = v.findViewById(R.id.post_Pic); photoIndicator = v.findViewById(R.id.photoIndicator);
            dotsIndicator = v.findViewById(R.id.dotsIndicator); heartButton = v.findViewById(R.id.heart_post); heartNumTextView = v.findViewById(R.id.heart_num);
            favButton = v.findViewById(R.id.fav_post); favNumTextView = v.findViewById(R.id.fav_num); shareButton = v.findViewById(R.id.share_post); menuOptions = v.findViewById(R.id.menu_options);
            postMainImage = v.findViewById(R.id.post_main_image);
        }
    }
}

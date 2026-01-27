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
        holder.captionTextView.setText(postEvent.getCaption());
        holder.dateTextView.setText(getRelativeTime(postEvent.getDate()));

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        final String finalCurrentUserId = currentUserId;
        String postId = postEvent.getPostId();
        String postAuthorId = postEvent.getUserId();

        if (postAuthorId != null && !postAuthorId.isEmpty()) {
            FirebaseDatabase.getInstance().getReference("Users").child(postAuthorId).child("profilePhoto").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
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
            holder.viewPager2.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
            Glide.with(context).asBitmap().load(firstImageUrl).into(new CustomTarget<Bitmap>() {
                @Override public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                    int w = resource.getWidth();
                    int h = resource.getHeight();
                    if (w > 0) {
                        float aspect = (float) h / w;
                        int vpW = holder.viewPager2.getWidth() == 0 ? context.getResources().getDisplayMetrics().widthPixels : holder.viewPager2.getWidth();
                        holder.viewPager2.getLayoutParams().height = (int) (vpW * aspect);
                        holder.viewPager2.requestLayout();
                    }
                }
                @Override public void onLoadCleared(@Nullable Drawable p) {}
            });

            D_Feed_ImageViewAdapter imageAdapter = new D_Feed_ImageViewAdapter(context, postEvent.getImageUrls());
            holder.viewPager2.setAdapter(imageAdapter);
            holder.dotsIndicator.setViewPager2(holder.viewPager2);
            holder.viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override public void onPageSelected(int pos) { holder.photoIndicator.setText((pos + 1) + "/" + imageAdapter.getItemCount()); }
            });
            boolean multi = imageAdapter.getItemCount() > 1;
            holder.dotsIndicator.setVisibility(multi ? View.VISIBLE : View.GONE);
            holder.photoIndicator.setVisibility(multi ? View.VISIBLE : View.GONE);
        } else holder.viewPager2.setVisibility(View.GONE);

        updateHeartIcon(holder.heartButton, postEvent.getHeartLiked(), currentUserId);
        holder.heartNumTextView.setText(String.valueOf(postEvent.getHeartLiked() != null ? postEvent.getHeartLiked().size() : 0));
        updateFavIcon(holder.favButton, postEvent.getFavList(), currentUserId);
        holder.favNumTextView.setText(String.valueOf(postEvent.getFavList() != null ? postEvent.getFavList().size() : 0));

        holder.profilePic.setOnClickListener(v -> { if (postAuthorId != null) context.startActivity(new Intent(context, I_ProfileActivity.class).putExtra("USER_ID", postAuthorId)); });

        holder.heartButton.setOnClickListener(v -> {
            if (postId == null || finalCurrentUserId.isEmpty()) return;
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("PostEvents").child(postId);
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override public void onDataChange(DataSnapshot s) {
                    I_PostEvent p = s.getValue(I_PostEvent.class);
                    if (p == null) return;
                    Map<String, Boolean> likes = p.getHeartLiked() == null ? new HashMap<>() : p.getHeartLiked();
                    if (likes.containsKey(finalCurrentUserId)) likes.remove(finalCurrentUserId); else likes.put(finalCurrentUserId, true);
                    p.setHeartCount(likes.size()); p.setHeartLiked(likes); ref.setValue(p);
                    holder.heartNumTextView.setText(String.valueOf(likes.size()));
                    updateHeartIcon(holder.heartButton, likes, finalCurrentUserId);
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
                    if (favs.containsKey(finalCurrentUserId)) favs.remove(finalCurrentUserId); else favs.put(finalCurrentUserId, true);
                    p.setFavCount(favs.size()); p.setFavList(favs); ref.setValue(p);
                    holder.favNumTextView.setText(String.valueOf(favs.size()));
                    updateFavIcon(holder.favButton, favs, finalCurrentUserId);
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
                } else {
                    ref.setValue(true);
                    FirebaseDatabase.getInstance().getReference("Users").child(targetId).child("FansList").child(uid).setValue(true);
                    updateCount(uid, "Models", 1); updateCount(targetId, "Fans", 1);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private void updateCount(String uid, String field, int inc) {
        DatabaseReference r = FirebaseDatabase.getInstance().getReference("Users").child(uid).child(field);
        r.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                long c = 0;
                try { if (s.exists()) c = Long.parseLong(s.getValue().toString()); } catch (Exception e) {}
                r.setValue(Math.max(0, c + inc));
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
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

    private void updateHeartIcon(ImageView b, Map<String, Boolean> m, String u) { b.setImageResource(m != null && m.containsKey(u) ? R.drawable.heart2 : R.drawable.heart); }
    private void updateFavIcon(ImageView b, Map<String, Boolean> m, String u) { b.setImageResource(m != null && m.containsKey(u) ? R.drawable.fav2 : R.drawable.fav); }

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
        ImageView profilePic, heartButton, favButton, shareButton, menuOptions;
        TextView userNameTextView, captionTextView, dateTextView, photoIndicator, favNumTextView, heartNumTextView;
        ViewPager2 viewPager2; WormDotsIndicator dotsIndicator;
        public PostViewHolder(View v) {
            super(v);
            profilePic = v.findViewById(R.id.profile_pic); userNameTextView = v.findViewById(R.id.userName_post); captionTextView = v.findViewById(R.id.caption_post);
            dateTextView = v.findViewById(R.id.postdate); viewPager2 = v.findViewById(R.id.post_Pic); photoIndicator = v.findViewById(R.id.photoIndicator);
            dotsIndicator = v.findViewById(R.id.dotsIndicator); heartButton = v.findViewById(R.id.heart_post); heartNumTextView = v.findViewById(R.id.heart_num);
            favButton = v.findViewById(R.id.fav_post); favNumTextView = v.findViewById(R.id.fav_num); shareButton = v.findViewById(R.id.share_post); menuOptions = v.findViewById(R.id.menu_options);
        }
    }
}

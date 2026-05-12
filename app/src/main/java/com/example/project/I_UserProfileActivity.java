package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class I_UserProfileActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "USER_ID";
    private String userId;
    private ImageView profileImageView;
    private TextView usernameText, emailText;
    private TextView postsNumText, followersNumText, followingNumText;
    private MaterialButton followBtn;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.i_profile_user_view);

        userId = getIntent().getStringExtra(EXTRA_USER_ID);
        if (userId == null) {
            finish();
            return;
        }

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (userId.equals(currentUser.getUid())) {
            // If it's my own profile, just go to I_ProfileActivity or show I_ProfileActivity
            startActivity(new Intent(this, I_ProfileActivity.class));
            finish();
            return;
        }

        profileImageView = findViewById(R.id.profileimage);
        usernameText = findViewById(R.id.username);
        emailText = findViewById(R.id.email_profile);
        postsNumText = findViewById(R.id.posts_num);
        followersNumText = findViewById(R.id.followers_num);
        followingNumText = findViewById(R.id.followings_num);
        followBtn = findViewById(R.id.btn_follow_user);
        tabLayout = findViewById(R.id.profile_tab_layout);
        viewPager = findViewById(R.id.profile_view_pager);
        ImageButton backBtn = findViewById(R.id.back_button);
        ImageButton menuBtn = findViewById(R.id.menubutton);

        backBtn.setOnClickListener(v -> finish());
        menuBtn.setOnClickListener(v -> showPopupMenu(v));

        findViewById(R.id.modellist).setOnClickListener(v -> openSocialList(0));
        findViewById(R.id.fanslist).setOnClickListener(v -> openSocialList(1));

        ProfilePagerAdapter pagerAdapter = new ProfilePagerAdapter(this, userId);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0: tab.setText("Posts"); break;
                case 1: tab.setText("Liked"); break;
                case 2: tab.setText("Favorite"); break;
            }
        }).attach();

        loadUserInfo();
        checkFollowing();

        followBtn.setOnClickListener(v -> toggleFollow());
    }

    private void loadUserInfo() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(userId);
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    usernameText.setText(snapshot.child("username").getValue(String.class));
                    emailText.setText(snapshot.child("email").getValue(String.class));
                    
                    Long postsVal = snapshot.child("posts").getValue(Long.class);
                    postsNumText.setText(String.valueOf(postsVal != null ? postsVal : 0L));

                    long modelsCount = snapshot.child("ModelsList").getChildrenCount();
                    followersNumText.setText(String.valueOf(modelsCount));

                    long fansCount = snapshot.child("FansList").getChildrenCount();
                    followingNumText.setText(String.valueOf(fansCount));

                    String photoUrl = snapshot.child("profilePhoto").getValue(String.class);
                    if (photoUrl != null && !photoUrl.equals("default") && !photoUrl.isEmpty()) {
                        if (!isDestroyed() && !isFinishing()) {
                            Glide.with(I_UserProfileActivity.this).load(photoUrl).circleCrop().into(profileImageView);
                        }
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void checkFollowing() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users")
                .child(currentUser.getUid()).child("ModelsList");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.child(userId).exists()) {
                    followBtn.setText("Following");
                } else {
                    followBtn.setText("Follow");
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void toggleFollow() {
        DatabaseReference myRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);

        if (followBtn.getText().toString().equalsIgnoreCase("Follow")) {
            myRef.child("ModelsList").child(userId).setValue(true);
            userRef.child("FansList").child(currentUser.getUid()).setValue(true);
            updateCount(currentUser.getUid(), "Models", 1);
            updateCount(userId, "Fans", 1);
        } else {
            myRef.child("ModelsList").child(userId).removeValue();
            userRef.child("FansList").child(currentUser.getUid()).removeValue();
            updateCount(currentUser.getUid(), "Models", -1);
            updateCount(userId, "Fans", -1);
        }
    }

    private void showPopupMenu(View v) {
        android.widget.PopupMenu popup = new android.widget.PopupMenu(this, v);
        popup.getMenu().add(0, 1, 0, "Block");
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                android.widget.Toast.makeText(this, "Coming soon", android.widget.Toast.LENGTH_SHORT).show();
            }
            return true;
        });
        popup.show();
    }

    private void updateCount(String uid, String field, int inc) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(uid).child(field);
        ref.runTransaction(new com.google.firebase.database.Transaction.Handler() {
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

    private void openSocialList(int startTab) {
        Intent intent = new Intent(this, I_ProfileSocialActivity.class);
        intent.putExtra("profileid", userId);
        intent.putExtra("title", startTab == 0 ? "models" : "fans");
        startActivity(intent);
    }

    private static class ProfilePagerAdapter extends FragmentStateAdapter {
        private final String targetUserId;
        public ProfilePagerAdapter(@NonNull FragmentActivity fa, String userId) {
            super(fa);
            this.targetUserId = userId;
        }
        @NonNull @Override public Fragment createFragment(int pos) {
            switch (pos) {
                case 1: return I_ProfileContentFragment.newInstance(I_ProfileContentFragment.TYPE_LIKED, targetUserId);
                case 2: return I_ProfileContentFragment.newInstance(I_ProfileContentFragment.TYPE_FAVORITE, targetUserId);
                default: return I_ProfileContentFragment.newInstance(I_ProfileContentFragment.TYPE_UPLOAD, targetUserId);
            }
        }
        @Override public int getItemCount() { return 3; }
    }

    public void onButtonClicked(View view) {
        Intent intent = null;
        int viewId = view.getId();
        if (viewId == R.id.home_menu) {
            intent = new Intent(this, D_FeedActivity.class);
        } else if (viewId == R.id.wardrobe_menu) {
            intent = new Intent(this, WardrobeActivity.class);
        } else if (viewId == R.id.calendar_menu) {
            intent = new Intent(this, E_CalendarActivity.class);
        } else if (viewId == R.id.ai_menu) {
            intent = new Intent(this, AiActivity.class);
        } else if (viewId == R.id.profile_menu) {
            intent = new Intent(this, I_ProfileActivity.class);
        }

        if (intent != null) {
            startActivity(intent);
            finish();
        }
    }
}

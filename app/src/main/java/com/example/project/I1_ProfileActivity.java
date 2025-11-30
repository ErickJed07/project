package com.example.project;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

public class I1_ProfileActivity extends AppCompatActivity {

    private TextView usernameText, emailText;
    private TextView postsNumText, followersNumText, followingNumText;

    // REPLACED: TextViews and FrameLayout with TabLayout and ViewPager2
    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    private FirebaseAuth mAuth;
    private DatabaseReference databaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.i1_profile);

        // Firebase init
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Find views
        usernameText = findViewById(R.id.username);
        emailText = findViewById(R.id.email_profile);

        // Find views for the stats
        postsNumText = findViewById(R.id.posts_num);
        followersNumText = findViewById(R.id.followers_num);
        followingNumText = findViewById(R.id.followings_num);

        // --- VIEW PAGER & TAB LAYOUT SETUP START ---
        tabLayout = findViewById(R.id.profile_tab_layout);
        viewPager = findViewById(R.id.profile_view_pager);

        // Set the adapter
        ProfilePagerAdapter pagerAdapter = new ProfilePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // Connect TabLayout and ViewPager2 using TabLayoutMediator
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            android.text.SpannableString boldText;
            switch (position) {
                case 0:
                    boldText = new android.text.SpannableString("Upload");
                    boldText.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, boldText.length(), 0);
                    tab.setText(boldText);
                    break;
                case 1:
                    boldText = new android.text.SpannableString("Liked");
                    boldText.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, boldText.length(), 0);
                    tab.setText(boldText);
                    break;
                case 2:
                    boldText = new android.text.SpannableString("Favorite");
                    boldText.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, boldText.length(), 0);
                    tab.setText(boldText);
                    break;
            }
        }).attach();
        // --- VIEW PAGER & TAB LAYOUT SETUP END ---

        // --- MENU BUTTON LOGIC START ---
        View menuButton = findViewById(R.id.menubutton);
        View addFollowingBtn = findViewById(R.id.add_following);

        addFollowingBtn.setOnClickListener(v -> {
            Intent intent = new Intent(I1_ProfileActivity.this, I_Profile_AddModels.class);
            startActivity(intent);
        });

        LinearLayout modelListBtn = findViewById(R.id.modellist);
        LinearLayout fansListBtn = findViewById(R.id.fanslist);

        modelListBtn.setOnClickListener(v -> {
            Intent intent = new Intent(I1_ProfileActivity.this, I_ProfileModelFans_List.class);
            intent.putExtra("START_TAB", 0);
            startActivity(intent);
        });

        fansListBtn.setOnClickListener(v -> {
            Intent intent = new Intent(I1_ProfileActivity.this, I_ProfileModelFans_List.class);
            intent.putExtra("START_TAB", 1);
            startActivity(intent);
        });

        if (menuButton != null) {
            menuButton.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(I1_ProfileActivity.this, v);

                final int LOGOUT_ID = 1;
                final int DELETE_ID = 2;

                popup.getMenu().add(0, LOGOUT_ID, 0, "Logout");
                MenuItem deleteItem = popup.getMenu().add(0, DELETE_ID, 1, "Delete Account");

                SpannableString s = new SpannableString(deleteItem.getTitle());
                s.setSpan(new ForegroundColorSpan(Color.RED), 0, s.length(), 0);
                deleteItem.setTitle(s);

                popup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == LOGOUT_ID) {
                        mAuth.signOut();
                        Intent intent = new Intent(I1_ProfileActivity.this, A_HomeActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                        return true;
                    } else if (item.getItemId() == DELETE_ID) {
                        // ... (Your existing delete logic remains unchanged) ...
                        handleDeleteAccount();
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        }

        if (currentUser != null) {
            String uid = currentUser.getUid();
            emailText.setText(currentUser.getEmail());

            databaseRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);
            databaseRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String username = snapshot.child("username").getValue(String.class);
                        usernameText.setText(username != null ? username : "Unknown User");

                        Long posts = snapshot.child("posts").getValue(Long.class);
                        postsNumText.setText(String.valueOf(posts != null ? posts : 0));

                        Long Models = snapshot.child("Models").getValue(Long.class);
                        followersNumText.setText(String.valueOf(Models != null ? Models : 0));

                        Long Fans = snapshot.child("Fans").getValue(Long.class);
                        followingNumText.setText(String.valueOf(Fans != null ? Fans : 0));
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) { }
            });
        }
    }

    // Helper method to keep onCreate clean (moved your delete logic here)
    private void handleDeleteAccount() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            DatabaseReference postsRef = FirebaseDatabase.getInstance().getReference("PostEvents");
            Query query = postsRef.orderByChild("userId").equalTo(uid);

            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                        postSnapshot.getRef().removeValue();
                    }
                    DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);
                    userRef.removeValue().addOnCompleteListener(dbTask -> {
                        if (dbTask.isSuccessful()) {
                            user.delete().addOnCompleteListener(authTask -> {
                                if (authTask.isSuccessful()) {
                                    Toast.makeText(I1_ProfileActivity.this, "Account Deleted", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(I1_ProfileActivity.this, A_HomeActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                }
                            });
                        }
                    });
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) { }
            });
        }
    }

    public void onButtonClicked(View view) {
        Intent intent = null;
        int viewId = view.getId();

        if (viewId == R.id.home_menu) {
            intent = new Intent(this, D1_FeedActivity.class);
        } else if (viewId == R.id.calendar_menu) {
            intent = new Intent(this, E1_CalendarActivity.class);
        } else if (viewId == R.id.add_menu) {
            intent = new Intent(this, F1_CameraActivity.class);
        } else if (viewId == R.id.closet_menu) {
            intent = new Intent(this, G1_ClosetActivity.class);
        } else if (viewId == R.id.profile_menu) {
            return;
        }

        if (intent != null) {
            startActivity(intent);
            finish();
        }
    }

    // --- INNER CLASS ADAPTER ---
    private static class ProfilePagerAdapter extends FragmentStateAdapter {
        public ProfilePagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new I_Profile_UploadContent();
                case 1:
                    return new I_Profile_LikedContent();
                case 2:
                    return new I_Profile_FavContent();
                default:
                    return new I_Profile_UploadContent();
            }
        }

        @Override
        public int getItemCount() {
            return 3; // We have 3 tabs
        }
    }
}

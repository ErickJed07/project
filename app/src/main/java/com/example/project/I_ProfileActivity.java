package com.example.project;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.HashMap;
import java.util.Map;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

public class I_ProfileActivity extends AppCompatActivity {

    private ImageView profileImageView;
    private ImageButton changePhotoBtn;
    private TextView usernameText, emailText;
    private TextView postsNumText, followersNumText, followingNumText;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseRef;
    private static final int IMAGE_REQ_CODE = 101;
    private String currentPhotoUrl = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.i_profile);

        initCloudinary();
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        profileImageView = findViewById(R.id.profileimage);
        changePhotoBtn = findViewById(R.id.change_photo);
        usernameText = findViewById(R.id.username);
        emailText = findViewById(R.id.email_profile);
        postsNumText = findViewById(R.id.posts_num);
        followersNumText = findViewById(R.id.followers_num);
        followingNumText = findViewById(R.id.followings_num);
        tabLayout = findViewById(R.id.profile_tab_layout);
        viewPager = findViewById(R.id.profile_view_pager);

        changePhotoBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, IMAGE_REQ_CODE);
        });

        ProfilePagerAdapter pagerAdapter = new ProfilePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            String title = "";
            switch (position) {
                case 0: title = "Upload"; break;
                case 1: title = "Liked"; break;
                case 2: title = "Favorite"; break;
            }
            SpannableString boldText = new SpannableString(title);
            boldText.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, boldText.length(), 0);
            tab.setText(boldText);
        }).attach();

        findViewById(R.id.menubutton).setOnClickListener(v -> showPopupMenu(v));
        findViewById(R.id.add_following).setOnClickListener(v -> startActivity(new Intent(this, I_Profile_AddModels.class)));
        findViewById(R.id.modellist).setOnClickListener(v -> openSocialList(0));
        findViewById(R.id.fanslist).setOnClickListener(v -> openSocialList(1));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }


        if (currentUser != null) {
            String uid = currentUser.getUid();
            emailText.setText(currentUser.getEmail());
            databaseRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);
            databaseRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        usernameText.setText(snapshot.child("username").getValue(String.class));
                        
                        Long postsVal = snapshot.child("posts").getValue(Long.class);
                        postsNumText.setText(String.valueOf(postsVal != null ? postsVal : 0L));

                        long modelsCount = snapshot.child("ModelsList").getChildrenCount();
                        followersNumText.setText(String.valueOf(modelsCount));

                        long fansCount = snapshot.child("FansList").getChildrenCount();
                        followingNumText.setText(String.valueOf(fansCount));
                        
                        currentPhotoUrl = snapshot.child("profilePhoto").getValue(String.class);
                        if (currentPhotoUrl != null && !currentPhotoUrl.equals("default") && !currentPhotoUrl.isEmpty()) {
                            if (!isDestroyed() && !isFinishing()) {
                                Glide.with(I_ProfileActivity.this).load(currentPhotoUrl).circleCrop().placeholder(R.drawable.profile_upload).into(profileImageView);
                            }
                        } else {
                            profileImageView.setImageResource(R.drawable.profile_upload);
                        }
                    }
                }
                @Override public void onCancelled(DatabaseError error) { }
            });
        }
    }

    private void initCloudinary() {
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("cloud_name", BuildConfig.CLOUDINARY_CLOUD_NAME);
            config.put("api_key", BuildConfig.CLOUDINARY_API_KEY);
            config.put("api_secret", BuildConfig.CLOUDINARY_API_SECRET);
            MediaManager.init(this, config);
        } catch (IllegalStateException e) { }
    }

    private void sendTestForcedNotification() {
        String channelId = "fcm_default_channel";
        android.app.NotificationManager notificationManager =
                (android.app.NotificationManager) getSystemService(android.content.Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(channelId,
                    "Urgent Notifications",
                    android.app.NotificationManager.IMPORTANCE_HIGH);
            channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(this, I_ProfileActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(this, 0, intent,
                android.app.PendingIntent.FLAG_ONE_SHOT | android.app.PendingIntent.FLAG_IMMUTABLE);

        androidx.core.app.NotificationCompat.Builder builder = new androidx.core.app.NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Test Forced Notification")
                .setContentText("This is how it looks even when the app is exited!")
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MAX)
                .setCategory(androidx.core.app.NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setDefaults(android.app.Notification.DEFAULT_ALL)
                .setColor(getResources().getColor(R.color.purple_primary, null))
                .setContentIntent(pendingIntent);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        Toast.makeText(this, "Notification sent! Exit the app to see the effect.", Toast.LENGTH_LONG).show();
    }

    private void openSocialList(int startTab) {
        Intent intent = new Intent(this, I_ProfileSocialActivity.class);
        intent.putExtra("title", startTab == 0 ? "models" : "fans");
        startActivity(intent);
    }

    private void showPopupMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenu().add(0, 1, 0, "Logout");
        popup.getMenu().add(0, 4, 1, "Theme Settings");
        popup.getMenu().add(0, 3, 2, "Version: " + BuildConfig.VERSION_NAME).setEnabled(false);
        MenuItem deleteItem = popup.getMenu().add(0, 2, 3, "Delete Account");
        SpannableString s = new SpannableString(deleteItem.getTitle());
        s.setSpan(new ForegroundColorSpan(Color.RED), 0, s.length(), 0);
        deleteItem.setTitle(s);

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                mAuth.signOut();
                startActivity(new Intent(this, A_HomeActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                finish();
            } else if (item.getItemId() == 2) {
                handleDeleteAccount();
            } else if (item.getItemId() == 4) {
                showThemeSelectionDialog();
            }
            return true;
        });
        popup.show();
    }

    private void showThemeSelectionDialog() {
        String[] themes = {"Light Mode", "Dark Mode", "System Default"};
        int currentTheme = ThemeHelper.getSavedTheme(this);
        int checkedItem = 2; // Default to System
        if (currentTheme == ThemeHelper.THEME_LIGHT) checkedItem = 0;
        else if (currentTheme == ThemeHelper.THEME_DARK) checkedItem = 1;

        new AlertDialog.Builder(this)
                .setTitle("Choose Theme")
                .setSingleChoiceItems(themes, checkedItem, (dialog, which) -> {
                    int selectedTheme = ThemeHelper.THEME_SYSTEM;
                    if (which == 0) selectedTheme = ThemeHelper.THEME_LIGHT;
                    else if (which == 1) selectedTheme = ThemeHelper.THEME_DARK;

                    ThemeHelper.saveTheme(this, selectedTheme);
                    ThemeHelper.applyTheme(selectedTheme);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_REQ_CODE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            uploadToCloudinary(data.getData());
        }
    }

    private void uploadToCloudinary(Uri uri) {
        Toast.makeText(this, "Uploading...", Toast.LENGTH_SHORT).show();
        MediaManager.get().upload(uri).option("folder", "ProfilePhoto").callback(new UploadCallback() {
            @Override public void onSuccess(String requestId, Map resultData) {
                String newUrl = (String) resultData.get("secure_url");
                String oldUrl = currentPhotoUrl;
                updateFirebaseProfilePhoto(newUrl, oldUrl);
            }
            @Override public void onError(String requestId, ErrorInfo error) { Toast.makeText(I_ProfileActivity.this, "Error: " + error.getDescription(), Toast.LENGTH_SHORT).show(); }
            @Override public void onStart(String requestId) {}
            @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
            @Override public void onReschedule(String requestId, ErrorInfo error) {}
        }).dispatch();
    }

    private void updateFirebaseProfilePhoto(String newUrl, String oldUrl) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(user.getUid());
            ref.child("profilePhoto").setValue(newUrl).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    if (oldUrl != null && !oldUrl.equals("default")) deleteOldPhotoFromCloudinary(oldUrl);
                }
            });
        }
    }

    private void deleteOldPhotoFromCloudinary(String oldUrl) {
        new Thread(() -> {
            try {
                String fileName = oldUrl.substring(oldUrl.lastIndexOf("/") + 1);
                String publicId = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf(".")) : fileName;
                Map<String, Object> config = new HashMap<>();
                config.put("cloud_name", BuildConfig.CLOUDINARY_CLOUD_NAME);
                config.put("api_key", BuildConfig.CLOUDINARY_API_KEY);
                config.put("api_secret", BuildConfig.CLOUDINARY_API_SECRET);
                new Cloudinary(config).uploader().destroy(publicId, ObjectUtils.asMap("invalidate", true));
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void handleDeleteAccount() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            FirebaseDatabase.getInstance().getReference("PostEvents").orderByChild("userId").equalTo(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()) ds.getRef().removeValue();
                        FirebaseDatabase.getInstance().getReference("Users").child(uid).removeValue().addOnCompleteListener(task -> {
                            user.delete().addOnCompleteListener(authTask -> {
                                startActivity(new Intent(I_ProfileActivity.this, A_HomeActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                                finish();
                            });
                        });
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) { }
                });
        }
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
            // Already here
            return;
        }
        
        if (intent != null) {
            startActivity(intent);
            finish();
        }
    }

    private static class ProfilePagerAdapter extends FragmentStateAdapter {
        public ProfilePagerAdapter(@NonNull FragmentActivity fa) { super(fa); }
        @NonNull @Override public Fragment createFragment(int pos) {
            switch (pos) {
                case 1: return I_ProfileContentFragment.newInstance(I_ProfileContentFragment.TYPE_LIKED);
                case 2: return I_ProfileContentFragment.newInstance(I_ProfileContentFragment.TYPE_FAVORITE);
                default: return I_ProfileContentFragment.newInstance(I_ProfileContentFragment.TYPE_UPLOAD);
            }
        }
        @Override public int getItemCount() { return 3; }
    }
}

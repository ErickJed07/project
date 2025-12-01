package com.example.project;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri; // Required for image path
import android.os.Bundle;
import android.provider.MediaStore; // Required for gallery
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton; // Required for the button
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable; // Required for onActivityResult
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager; // Cloudinary
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.HashMap;
import java.util.Map;
// Use the pure Java Cloudinary class for admin operations (deletion)
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

public class I1_ProfileActivity extends AppCompatActivity {

    private ImageView profileImageView;
    private ImageButton changePhotoBtn; // The small button to change photo
    private TextView usernameText, emailText;
    private TextView postsNumText, followersNumText, followingNumText;

    // Tabs
    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    private FirebaseAuth mAuth;
    private DatabaseReference databaseRef;

    // Image Upload Variables
    private static final int IMAGE_REQ_CODE = 101;
    private Uri imageUri;

    // Store the current photo URL so we can delete it later
    private String currentPhotoUrl = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.i1_profile);

        // 1. Initialize Cloudinary
        initCloudinary();

        // Firebase init
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Find views
        profileImageView = findViewById(R.id.profileimage);
        changePhotoBtn = findViewById(R.id.change_photo); // Initialize the change button
        usernameText = findViewById(R.id.username);
        emailText = findViewById(R.id.email_profile);

        postsNumText = findViewById(R.id.posts_num);
        followersNumText = findViewById(R.id.followers_num);
        followingNumText = findViewById(R.id.followings_num);

        tabLayout = findViewById(R.id.profile_tab_layout);
        viewPager = findViewById(R.id.profile_view_pager);

        // --- CHANGE PHOTO BUTTON LOGIC ---
        changePhotoBtn.setOnClickListener(v -> {
            // Open Gallery
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, IMAGE_REQ_CODE);
        });

        // --- VIEW PAGER SETUP ---
        ProfilePagerAdapter pagerAdapter = new ProfilePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

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

        // --- MENU & BUTTONS LOGIC ---
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
                        handleDeleteAccount();
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        }

        // --- FIREBASE DATA LISTENER ---
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

                        // Load Photo
                        String photoUrl = snapshot.child("profilePhoto").getValue(String.class);

                        // Keep track of the current URL for deletion purposes later
                        currentPhotoUrl = photoUrl;

                        if (photoUrl != null && !photoUrl.equals("default") && !photoUrl.isEmpty()) {
                            // Check if activity is destroyed before loading to prevent crashes
                            if (!isDestroyed() && !isFinishing()) {
                                Glide.with(I1_ProfileActivity.this)
                                        .load(photoUrl)
                                        .circleCrop()
                                        .placeholder(R.drawable.profile_upload)
                                        .error(R.drawable.profile_upload)
                                        .into(profileImageView);
                            }
                        } else {
                            profileImageView.setImageResource(R.drawable.profile_upload);
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) { }
            });
        }
    }

    // --- 2. CLOUDINARY INIT ---
    private void initCloudinary() {
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("cloud_name", BuildConfig.CLOUDINARY_CLOUD_NAME);
            config.put("api_key", BuildConfig.CLOUDINARY_API_KEY);
            config.put("api_secret", BuildConfig.CLOUDINARY_API_SECRET);
            MediaManager.init(this, config);
        } catch (IllegalStateException e) {
            // Already initialized
        }
    }

    // --- 3. HANDLE IMAGE SELECTION ---
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_REQ_CODE && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            if (imageUri != null) {
                uploadToCloudinary(imageUri);
            }
        }
    }

    // --- 4. UPLOAD TO CLOUDINARY ---
    // --- 4. UPLOAD TO CLOUDINARY ---
    private void uploadToCloudinary(Uri uri) {
        Toast.makeText(this, "Uploading new photo...", Toast.LENGTH_SHORT).show();

        MediaManager.get().upload(uri)
                .option("folder", "ProfilePhoto") // <--- Added this line to specify the folder
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        // Get URL of the NEW photo
                        String downloadUrl = (String) resultData.get("secure_url");

                        // Capture the OLD photo URL from the variable we set in onDataChange
                        String oldUrlToDelete = currentPhotoUrl;

                        // Update Firebase first
                        updateFirebaseProfilePhoto(downloadUrl, oldUrlToDelete);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Toast.makeText(I1_ProfileActivity.this, "Upload Error: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }


    // --- 5. UPDATE FIREBASE DATABASE & DELETE OLD PHOTO ---
    private void updateFirebaseProfilePhoto(String newUrl, String oldUrl) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(user.getUid());

            // Update the database with the NEW URL
            ref.child("profilePhoto").setValue(newUrl)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(I1_ProfileActivity.this, "Profile Photo Updated!", Toast.LENGTH_SHORT).show();

                            // DATABASE UPDATED SUCCESSFULLY.
                            // NOW DELETE THE OLD IMAGE from Cloudinary.
                            if (oldUrl != null && !oldUrl.equals("default")) {
                                deleteOldPhotoFromCloudinary(oldUrl);
                            }
                        } else {
                            Toast.makeText(I1_ProfileActivity.this, "Database Update Failed", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }


    // --- Helper to delete old photo ---
    private void deleteOldPhotoFromCloudinary(String oldUrl) {
        // 1. Validation check
        if (oldUrl == null || oldUrl.isEmpty() || oldUrl.equals("default")) {
            return;
        }

        // 2. Run network operation in a background thread
        new Thread(() -> {
            try {
                // --- A. Extract the Public ID from the URL ---
                // Example: https://res.cloudinary.com/.../upload/v1764529443/xrfxwaxyukfvdfrzlxey.jpg

                // Get the part after the last slash: "xrfxwaxyukfvdfrzlxey.jpg"
                String fileName = oldUrl.substring(oldUrl.lastIndexOf("/") + 1);

                // Remove the extension to get the Public ID: "xrfxwaxyukfvdfrzlxey"
                String publicId = fileName;
                if (fileName.contains(".")) {
                    publicId = fileName.substring(0, fileName.lastIndexOf("."));
                }

                // --- B. Configure a raw Cloudinary instance ---
                // We create a new map specifically for this operation because we need the pure Java class logic
                Map<String, Object> config = new HashMap<>();
                config.put("cloud_name", BuildConfig.CLOUDINARY_CLOUD_NAME);
                config.put("api_key", BuildConfig.CLOUDINARY_API_KEY);
                config.put("api_secret", BuildConfig.CLOUDINARY_API_SECRET);

                Cloudinary cloudinary = new Cloudinary(config);

                // --- C. Execute Destroy Command ---
                // "invalidate": true helps clear the CDN cache
                cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("invalidate", true));

                // Optional: Show a small message on success (must run on UI thread)
                runOnUiThread(() ->
                        Toast.makeText(I1_ProfileActivity.this, "Old photo deleted from cloud", Toast.LENGTH_SHORT).show()
                );

            } catch (Exception e) {
                e.printStackTrace();
                // Optional: Log failure
            }
        }).start();
    }

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

    private static class ProfilePagerAdapter extends FragmentStateAdapter {
        public ProfilePagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0: return new I_Profile_UploadContent();
                case 1: return new I_Profile_LikedContent();
                case 2: return new I_Profile_FavContent();
                default: return new I_Profile_UploadContent();
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}

package com.example.project;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class G4_Closet_Category_PhotoViewerActivity extends AppCompatActivity {

    private static final String TAG = "ClosetPhotoViewer";
    private ViewPager2 viewPager;
    private List<String> imageUrls;
    private G5_Closet_Category_PhotoPagerAdapter adapter;

    private String categoryId;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.g3_closet_category_photo_viewer);

        // 1. Get Data from Intent
        imageUrls = getIntent().getStringArrayListExtra("IMAGES");

        // --- FIX: Robust Category ID Retrieval ---
        // Try all common key variations
        if (getIntent().hasExtra("CATEGORY_ID")) {
            categoryId = getIntent().getStringExtra("CATEGORY_ID");
        } else if (getIntent().hasExtra("categoryId")) {
            categoryId = getIntent().getStringExtra("categoryId");
        } else if (getIntent().hasExtra("category_id")) {
            categoryId = getIntent().getStringExtra("category_id");
        } else if (getIntent().hasExtra("id")) {
            categoryId = getIntent().getStringExtra("id");
        }

        // 2. Get UID safely
        uid = getIntent().getStringExtra("UID");
        if (uid == null) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                uid = user.getUid();
            }
        }

        // Debug Log to confirm we have data now
        Log.d(TAG, "onCreate: UID=" + uid + ", CategoryID=" + categoryId);

        if (categoryId == null) {
            // If still null, we can't delete from Firebase later
            Log.e(TAG, "CRITICAL ERROR: Category ID is MISSING. Deletion will fail.");
            Toast.makeText(this, "Error: Category info missing.", Toast.LENGTH_LONG).show();
        }

        if (imageUrls == null) imageUrls = new ArrayList<>();

        if (imageUrls.isEmpty()) {
            Toast.makeText(this, "No images to display", Toast.LENGTH_SHORT).show();
            finishWithAnimation();
            return;
        }

        int startIndex = getIntent().getIntExtra("START_INDEX", 0);
        if (startIndex < 0 || startIndex >= imageUrls.size()) startIndex = 0;

        viewPager = findViewById(R.id.viewPager);
        adapter = new G5_Closet_Category_PhotoPagerAdapter(this, imageUrls);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(startIndex, false);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finishWithAnimation());

        Button btnDelete = findViewById(R.id.btnDelete);
        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> deleteCurrentImage());
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishWithAnimation();
            }
        });
    }

    private void deleteCurrentImage() {
        try {
            int position = viewPager.getCurrentItem();
            if (position < 0 || position >= imageUrls.size()) return;

            String urlToDelete = imageUrls.get(position);

            Log.d(TAG, "Attempting to delete URL: " + urlToDelete);

            // -------------------------------------------------------
            // 1. DELETE FROM CLOUDINARY (Background Thread)
            // -------------------------------------------------------
            new Thread(() -> {
                try {
                    String publicId = null;
                    if (urlToDelete.contains("/upload/")) {
                        String[] parts = urlToDelete.split("/upload/");
                        if (parts.length > 1) {
                            String temp = parts[1];
                            if (temp.matches("^v\\d+/.*")) {
                                temp = temp.substring(temp.indexOf("/") + 1);
                            }
                            if (temp.contains(".")) {
                                temp = temp.substring(0, temp.lastIndexOf("."));
                            }
                            publicId = temp;
                        }
                    }

                    if (publicId != null) {
                        Map<String, Object> config = new HashMap<>();
                        config.put("cloud_name", BuildConfig.CLOUDINARY_CLOUD_NAME);
                        config.put("api_key", BuildConfig.CLOUDINARY_API_KEY);
                        config.put("api_secret", BuildConfig.CLOUDINARY_API_SECRET);

                        com.cloudinary.Cloudinary cloudinary = new com.cloudinary.Cloudinary(config);
                        cloudinary.uploader().destroy(publicId, com.cloudinary.utils.ObjectUtils.emptyMap());
                        Log.d(TAG, "Cloudinary delete success: " + publicId);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Cloudinary delete failed.", e);
                }
            }).start();

            // -------------------------------------------------------
            // 2. DELETE FROM FIREBASE
            // -------------------------------------------------------
            if (uid != null && categoryId != null) {
                // Path matches your JSON: Users -> {uid} -> categories -> {categoryId} -> photos
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users")
                        .child(uid)
                        .child("categories")
                        .child(categoryId) // e.g. "Bag"
                        .child("photos");

                Log.d(TAG, "Checking Firebase Path: " + ref.toString());
                findAndDeleteFirebaseNode(ref, urlToDelete);
            } else {
                Log.e(TAG, "Cannot delete: UID or CategoryID is null! UID: " + uid + ", CatID: " + categoryId);
                Toast.makeText(this, "Error: Cannot update database (ID missing)", Toast.LENGTH_SHORT).show();
                // We continue to update UI so user doesn't feel stuck, even if DB update failed
            }

            // -------------------------------------------------------
            // 3. UPDATE UI
            // -------------------------------------------------------
            imageUrls.remove(position);
            adapter.notifyDataSetChanged();

            if (imageUrls.isEmpty()) {
                finishWithAnimation();
            } else {
                int newPos = Math.min(position, imageUrls.size() - 1);
                viewPager.setCurrentItem(newPos, false);
            }

            Toast.makeText(this, "Image deleted", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, "Error deleting image", e);
            Toast.makeText(this, "Error deleting image", Toast.LENGTH_SHORT).show();
        }
    }

    private void findAndDeleteFirebaseNode(DatabaseReference ref, String urlToDelete) {
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Log.e(TAG, "No photos found at path: " + ref.toString());
                    return;
                }

                boolean found = false;
                for (DataSnapshot child : snapshot.getChildren()) {
                    String dbUrl = null;

                    // Check variations based on your JSON structure
                    if (child.hasChild("imageUrl")) {
                        dbUrl = child.child("imageUrl").getValue(String.class);
                    } else if (child.hasChild("url")) {
                        dbUrl = child.child("url").getValue(String.class);
                    } else {
                        Object val = child.getValue();
                        if (val instanceof String) {
                            dbUrl = (String) val;
                        }
                    }

                    if (dbUrl != null && dbUrl.equals(urlToDelete)) {
                        Log.d(TAG, "Found match! Deleting node: " + child.getKey());
                        child.getRef().removeValue()
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Firebase remove success"))
                                .addOnFailureListener(e -> Log.e(TAG, "Firebase remove failed", e));
                        found = true;
                    }
                }

                if (!found) {
                    Log.w(TAG, "Finished searching. Could not find URL in Firebase: " + urlToDelete);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
            }
        });
    }

    private void finishWithAnimation() {
        finish();
        overridePendingTransition(R.anim.no_animation, R.anim.slide_out_down);
    }
}

package com.example.project;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class G2_Closet_CategoryActivity extends AppCompatActivity {

    private RecyclerView galleryRecyclerView;
    private G3_Closet_CategoryAdapter adapter;

    // CHANGED: Now storing URLs (Strings) instead of Files
    private final List<String> imageUrlList = new ArrayList<>();

    // CHANGED: Map to link URL -> Firebase Key (needed for deletion)
    private final Map<String, String> urlToKeyMap = new HashMap<>();

    // CHANGED: Selection set now stores URLs
    private final Set<String> selectedUrls = new HashSet<>();

    private boolean isMultiSelectMode = false;
    private FloatingActionButton deleteFab;
    private String categoryName;
    private String categoryId; // New: We need ID to query Firebase

    private FirebaseAuth mAuth;
    private DatabaseReference categoryRef; // Points to specific category
    private String uid;

    // NEW: Track sort order (Default: true = Latest/Newest first)
    private boolean isLatestFirst = true;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.g2_closet_gallery);


        // --- NEW CODE: Sort Button ---
        View sortBtn = findViewById(R.id.sortButton);
        if (sortBtn != null) {
            sortBtn.setOnClickListener(v -> {
                isLatestFirst = !isLatestFirst; // Toggle the boolean
                sortImages(); // Apply sort
            });
        }
        // -----------------------------


        // --- NEW CODE: Handle Back Arrow Click ---
        findViewById(R.id.btnbackcloset).setOnClickListener(v -> {
            // Triggers the same logic as the hardware back button (goes to Feed)
            getOnBackPressedDispatcher().onBackPressed();
        });
        // -----------------------------------------

        // 1. Get Data from Intent
        categoryName = getIntent().getStringExtra("CATEGORY_NAME");
        categoryId = getIntent().getStringExtra("CATEGORY_ID");

        TextView titleView = findViewById(R.id.categoryTitle);
        if (categoryName != null) titleView.setText(categoryName);

        mAuth = FirebaseAuth.getInstance();
        uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        if (uid == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // ---------------------------------------------------------
        // UPDATE: Path excludes "closetData"
        // NEW: Users -> uid -> categories -> [id]
        // ---------------------------------------------------------
        if (categoryId != null) {
            categoryRef = FirebaseDatabase.getInstance().getReference("Users")
                    .child(uid)
                    .child("categories") // Looks inside the specific user's categories
                    .child(categoryId);  // Looks inside the specific category (e.g., "Tops")
        } else {
            Toast.makeText(this, "Error: Category ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        deleteFab = findViewById(R.id.fabDelete);
        deleteFab.hide();

        galleryRecyclerView = findViewById(R.id.galleryRecyclerView);

        androidx.recyclerview.widget.GridLayoutManager layoutManager =
                new androidx.recyclerview.widget.GridLayoutManager(this, 3); // 3 columns
        galleryRecyclerView.setLayoutManager(layoutManager);

        // 3. Initialize Adapter
        adapter = new G3_Closet_CategoryAdapter(
                this,
                imageUrlList, // Passing Strings now
                this::onImageClicked,
                this::onImageLongClicked,
                this::isUrlSelected,
                this::isMultiSelectMode
        );

        galleryRecyclerView.setAdapter(adapter);
        // 4. Load Images from Firebase
        loadImagesFromFirebase();

        // 5. Delete Logic
        deleteFab.setOnClickListener(v -> {
            deleteSelectedImages();
        });
    }

    private void loadImagesFromFirebase() {
        categoryRef.child("photos").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                imageUrlList.clear();
                urlToKeyMap.clear();

                for (DataSnapshot photoSnap : snapshot.getChildren()) {
                    String key = photoSnap.getKey();
                    String url = null;

                    if (photoSnap.hasChild("imageUrl")) {
                        url = photoSnap.child("imageUrl").getValue(String.class);
                    } else if (photoSnap.hasChild("url")) {
                        url = photoSnap.child("url").getValue(String.class);
                    }

                    if (url != null && !url.isEmpty()) {
                        imageUrlList.add(url);
                        urlToKeyMap.put(url, key);
                    }
                }

                // --- NEW: Apply Sort immediately after loading ---
                sortImages();
                // Note: sortImages calls notifyDataSetChanged, so we don't need to call it twice
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    // ---------------------------------------------------------
    // INTERACTION LOGIC (Updated for URLs)
    // ---------------------------------------------------------
    private void onImageClicked(String url) {
        if (isMultiSelectMode) {
            toggleSelection(url);
        } else {
            // Pass list of URLs to the viewer
            Intent intent = new Intent(this, G4_Closet_Category_PhotoViewerActivity.class);
            intent.putStringArrayListExtra("IMAGES", (ArrayList<String>) imageUrlList);

            // Find index of clicked URL
            int index = imageUrlList.indexOf(url);
            intent.putExtra("START_INDEX", index);

            startActivity(intent);
        }
    }

    private void onImageLongClicked(String url) {
        if (!isMultiSelectMode) {
            isMultiSelectMode = true;
            selectedUrls.add(url);
            adapter.notifyDataSetChanged();
            deleteFab.show();
        } else {
            toggleSelection(url);
        }
    }

    private void toggleSelection(String url) {
        if (selectedUrls.contains(url)) {
            selectedUrls.remove(url);
        } else {
            selectedUrls.add(url);
        }

        if (selectedUrls.isEmpty()) {
            exitMultiSelectMode();
        }
        adapter.notifyDataSetChanged();
    }

    private void exitMultiSelectMode() {
        isMultiSelectMode = false;
        selectedUrls.clear();
        adapter.notifyDataSetChanged();
        deleteFab.hide();
    }

    private boolean isUrlSelected(String url) {
        return selectedUrls.contains(url);
    }

    private boolean isMultiSelectMode() {
        return isMultiSelectMode;
    }

    // ---------------------------------------------------------
    // DELETION LOGIC (Firebase)
    // ---------------------------------------------------------
    // ---------------------------------------------------------
    // DELETION LOGIC (Firebase + Cloudinary)
    // ---------------------------------------------------------
    private void deleteSelectedImages() {
        if (selectedUrls.isEmpty()) return;

        // Create a copy of the set to iterate over safely
        Set<String> urlsToDelete = new HashSet<>(selectedUrls);

        for (String url : urlsToDelete) {
            String key = urlToKeyMap.get(url);

            // 1. DELETE FROM FIREBASE
            if (key != null) {
                categoryRef.child("photos").child(key).removeValue();
            }

            // 2. DELETE FROM CLOUDINARY (Run in background thread)
            new Thread(() -> {
                try {
                    String publicId = extractPublicId(url);
                    if (publicId != null) {
                        // This requires MediaManager to be initialized with api_secret
                        com.cloudinary.android.MediaManager.get().getCloudinary()
                                .uploader().destroy(publicId, com.cloudinary.utils.ObjectUtils.emptyMap());

                        android.util.Log.d("Delete", "Deleted from Cloudinary: " + publicId);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    android.util.Log.e("Delete", "Cloudinary delete failed (Check API Secret): " + e.getMessage());
                }
            }).start();
        }

        selectedUrls.clear();
        exitMultiSelectMode();
        Toast.makeText(this, "Images deleted", Toast.LENGTH_SHORT).show();
    }

    // HELPER: Extract the 'public_id' from a Cloudinary URL
    private String extractPublicId(String url) {
        try {
            // URL format example: .../upload/v12345/CategoriesPhotos/image_name.png
            if (url.contains("/upload/")) {
                String[] parts = url.split("/upload/");
                if (parts.length > 1) {
                    String temp = parts[1];

                    // Remove version number (e.g., v16789/) if present
                    if (temp.matches("^v\\d+/.*")) {
                        temp = temp.substring(temp.indexOf("/") + 1);
                    }

                    // Remove file extension (e.g., .png)
                    if (temp.contains(".")) {
                        temp = temp.substring(0, temp.lastIndexOf("."));
                    }
                    return temp; // Returns "CategoriesPhotos/image_name"
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private void sortImages() {
        if (imageUrlList.isEmpty()) return;

        java.util.Collections.sort(imageUrlList, (url1, url2) -> {
            String key1 = urlToKeyMap.get(url1);
            String key2 = urlToKeyMap.get(url2);

            if (key1 == null || key2 == null) return 0;

            // Firebase Keys are time-based.
            if (isLatestFirst) {
                return key2.compareTo(key1); // Descending (Newest first)
            } else {
                return key1.compareTo(key2); // Ascending (Oldest first)
            }
        });

        adapter.notifyDataSetChanged();

        String message = isLatestFirst ? "Sorted by Latest" : "Sorted by Oldest";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }


    // ---------------------------------------------------------
    // NAVIGATION
    // ---------------------------------------------------------
    public void onCloset_backClicked(View view) {
        finish();
    }

    public void onButtonClicked(View view) {
        Intent intent = null;
        int id = view.getId();

        if (id == R.id.home_menu) intent = new Intent(this, D1_FeedActivity.class);
        else if (id == R.id.calendar_menu) intent = new Intent(this, E1_CalendarActivity.class);
        else if (id == R.id.camera_menu) intent = new Intent(this, F1_CameraActivity.class);
        else if (id == R.id.closet_menu) intent = new Intent(this, G1_ClosetActivity.class);
        else if (id == R.id.profile_menu) intent = new Intent(this, I1_ProfileActivity.class);

        if (intent != null) {
            startActivity(intent);
            finish();
        }
    }
}

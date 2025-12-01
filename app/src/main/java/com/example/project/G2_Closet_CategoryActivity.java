package com.example.project;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.g2_closet_gallery);

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

        // 2. Set Firebase Reference: Users -> uid -> closetData -> categories -> [id]
        // If ID is missing (legacy), we might need a fallback, but G1 should pass it.
        if (categoryId != null) {
            categoryRef = FirebaseDatabase.getInstance().getReference("Users")
                    .child(uid)
                    .child("closetData")
                    .child("categories")
                    .child(categoryId);
        } else {
            Toast.makeText(this, "Error: Category ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        deleteFab = findViewById(R.id.fabDelete);
        deleteFab.hide();

        galleryRecyclerView = findViewById(R.id.galleryRecyclerView);

// --- ADD THIS ---
        androidx.recyclerview.widget.GridLayoutManager layoutManager =
                new androidx.recyclerview.widget.GridLayoutManager(this, 3); // 3 columns
        galleryRecyclerView.setLayoutManager(layoutManager);



        // 3. Initialize Adapter
        // NOTE: You must update G3_Closet_CategoryAdapter to accept List<String> instead of List<File>
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

    // ---------------------------------------------------------
    // FIREBASE LOADING
    // ---------------------------------------------------------
    private void loadImagesFromFirebase() {
        // Listen to the "photos" node inside this category
        categoryRef.child("photos").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                imageUrlList.clear();
                urlToKeyMap.clear();

                for (DataSnapshot photoSnap : snapshot.getChildren()) {
                    // The key (e.g. -OfOd...)
                    String key = photoSnap.getKey();
                    // The value (e.g. https://cloudinary...)
                    String url = photoSnap.getValue(String.class);

                    if (url != null) {
                        imageUrlList.add(url);
                        urlToKeyMap.put(url, key);
                    }
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(G2_Closet_CategoryActivity.this, "Failed to load images", Toast.LENGTH_SHORT).show();
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
    private void deleteSelectedImages() {
        if (selectedUrls.isEmpty()) return;

        for (String url : selectedUrls) {
            String key = urlToKeyMap.get(url);
            if (key != null) {
                // Remove from Firebase: categories/[id]/photos/[key]
                categoryRef.child("photos").child(key).removeValue();
            }
        }

        // Cloudinary Note:
        // Ideally, you should also delete the image from Cloudinary using its API
        // to save storage space, but that usually requires backend logic or a secure server.
        // For now, we just remove the reference from the Database.

        selectedUrls.clear();
        exitMultiSelectMode();
        Toast.makeText(this, "Images deleted", Toast.LENGTH_SHORT).show();
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
        else if (id == R.id.camera_menu) intent = new Intent(this, F1_CameraActivity.class); // Fixed ID name
        else if (id == R.id.closet_menu) intent = new Intent(this, G1_ClosetActivity.class);
        else if (id == R.id.profile_menu) intent = new Intent(this, I1_ProfileActivity.class);

        if (intent != null) {
            startActivity(intent);
            finish();
        }
    }
}

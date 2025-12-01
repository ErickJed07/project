package com.example.project;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class G4_Closet_Category_PhotoViewerActivity extends AppCompatActivity {

    private static final String TAG = "ClosetPhotoViewer";
    private ViewPager2 viewPager;
    private List<String> imageUrls; // Changed variable name to reflect content
    private G5_Closet_Category_PhotoPagerAdapter adapter;

    // New variables needed for Firebase deletion
    private String categoryId;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.g3_closet_category_photo_viewer); // Keeping original layout name

        // 1. Get Data from Intent
        imageUrls = getIntent().getStringArrayListExtra("IMAGES");
        categoryId = getIntent().getStringExtra("CATEGORY_ID");
        uid = getIntent().getStringExtra("UID");

        if (imageUrls == null) imageUrls = new ArrayList<>();

        // Defensive: if no images, close viewer
        if (imageUrls.isEmpty()) {
            Toast.makeText(this, "No images to display", Toast.LENGTH_SHORT).show();
            finishWithAnimation();
            return;
        }

        int startIndex = getIntent().getIntExtra("START_INDEX", 0);
        if (startIndex < 0 || startIndex >= imageUrls.size()) startIndex = 0;

        viewPager = findViewById(R.id.viewPager);

        // Ensure G5 Adapter is updated to handle Strings (URLs) + Glide
        adapter = new G5_Closet_Category_PhotoPagerAdapter(this, imageUrls);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(startIndex, false);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finishWithAnimation());

        Button btnDelete = findViewById(R.id.btnDelete);
        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> deleteCurrentImage());
        }

        // Optional: Modern back handling
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

            // 1. Delete from Firebase
            if (uid != null && categoryId != null) {
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users")
                        .child(uid)
                        .child("closetData")
                        .child("categories")
                        .child(categoryId)
                        .child("photos");

                // We need to find the KEY associated with this URL to delete it
                Query query = ref.orderByValue().equalTo(urlToDelete);
                query.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        for (DataSnapshot appleSnapshot : snapshot.getChildren()) {
                            appleSnapshot.getRef().removeValue(); // Delete the node
                        }
                        Log.d(TAG, "Deleted from Firebase: " + urlToDelete);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e(TAG, "Firebase delete error", error.toException());
                    }
                });
            }

            // 2. Update UI
            imageUrls.remove(position);
            adapter.notifyItemRemoved(position);

            if (imageUrls.isEmpty()) {
                // no more images -> close viewer
                finishWithAnimation();
            } else {
                // ensure current item is valid
                int newPos = Math.min(position, imageUrls.size() - 1);
                // Re-setting adapter is often smoother for ViewPager2 dynamic removal than notifyItemRemoved alone
                // But for simple lists, standard notification is fine.
                // To be safe with ViewPager2 index issues:
                adapter.notifyDataSetChanged();
                viewPager.setCurrentItem(newPos, false);
            }

            Toast.makeText(this, "Image deleted", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, "Error deleting image", e);
            Toast.makeText(this, "Error deleting image", Toast.LENGTH_SHORT).show();
        }
    }

    private void finishWithAnimation() {
        finish();
        overridePendingTransition(R.anim.no_animation, R.anim.slide_out_down);
    }
}

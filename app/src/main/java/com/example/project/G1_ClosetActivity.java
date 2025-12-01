package com.example.project;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide; // Ensure you have Glide in build.gradle
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class G1_ClosetActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;
    private FirebaseStorage storage;

    private GridLayout gridLayout;
    private View addButtonView;
    private final Set<String> existingCategoryIds = new HashSet<>();
    private final Map<String, View> categoryViews = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.g1_closet);

        mAuth = FirebaseAuth.getInstance();
        // Point to Users root
        dbRef = FirebaseDatabase.getInstance().getReference("Users");
        storage = FirebaseStorage.getInstance();
        gridLayout = findViewById(R.id.galleryGrid);
        addButtonView = findViewById(R.id.AddCategory);

        loadCategoriesFromFirebase();

        addButtonView.setOnClickListener(v -> onAddCategoryClicked(v));

        addPermanentCategoryIfNeeded();

        findViewById(R.id.newoutfit).setOnClickListener(view ->
                startActivity(new Intent(G1_ClosetActivity.this, H1_DressActivity.class))
        );
    }

    private void loadCategoriesFromFirebase() {
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();

        // Read from Users -> uid -> closetData -> categories
        dbRef.child(uid).child("closetData").child("categories").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                // Clear existing views to prevent duplication during updates
                for (View view : categoryViews.values()) {
                    gridLayout.removeView(view);
                }
                categoryViews.clear();
                existingCategoryIds.clear();

                addPermanentCategoryToUI();

                // ... inside loadCategoriesFromFirebase method ...

                for (DataSnapshot child : snapshot.getChildren()) {
                    String categoryId = child.getKey();
                    String name = child.child("name").getValue(String.class);

                    String firstImageUrl = "";

                    // CHECK: Make sure "photos" exists and has children
                    if (child.hasChild("photos")) {
                        // We iterate through the generated keys (e.g. -OfOds83...)
                        for (DataSnapshot photoSnap : child.child("photos").getChildren()) {
                            // GET VALUE: The value at that key is the URL string
                            Object value = photoSnap.getValue();

                            if (value instanceof String) {
                                String url = (String) value;
                                if (url != null && !url.isEmpty()) {
                                    firstImageUrl = url;
                                    break; // Stop after finding the first valid URL
                                }
                            }
                        }
                    }

                    if (categoryId != null && name != null && !categoryId.equals("permanent_category_id")) {
                        addCategoryToUI(categoryId, name, firstImageUrl);
                    }
                }


                // Ensure "Add Button" is always at the end
                gridLayout.removeView(addButtonView);
                gridLayout.addView(addButtonView);
            }

            @Override
            public void onCancelled(DatabaseError error) { }
        });
    }

    public void onAddCategoryClicked(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Category Name");

        final EditText input = new EditText(this);
        input.setHint("Category name");
        builder.setView(input);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String categoryName = input.getText().toString().trim();
            if (!categoryName.isEmpty()) {
                saveCategoryToFirebase(categoryName, "");
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void saveCategoryToFirebase(String categoryName, String imageUrl) {
        String uid = mAuth.getCurrentUser().getUid();

        // Save to Users -> uid -> closetData -> categories
        DatabaseReference userCategoryRef = dbRef.child(uid).child("closetData").child("categories").push();
        String categoryId = userCategoryRef.getKey();

        Map<String, Object> catData = new HashMap<>();
        catData.put("id", categoryId);
        catData.put("name", categoryName);
        // Note: We don't save "imageUrl" directly on the category object anymore,
        // images live in the "photos" sub-node, but we pass it here for UI consistency.

        userCategoryRef.setValue(catData);

        // UI update will happen automatically via the ValueEventListener in loadCategoriesFromFirebase
    }

    private void addCategoryToUI(String categoryId, String categoryName, String imageUrl) {
        if (existingCategoryIds.contains(categoryId)) return;
        existingCategoryIds.add(categoryId);

        float scale = getResources().getDisplayMetrics().density;
        int cardSize = (int) (160 * scale);

        CardView card = new CardView(this);
        card.setRadius(24f);
        card.setCardElevation(6f);
        card.setUseCompatPadding(true);
        card.setCardBackgroundColor(getResources().getColor(R.color.white));

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(12, 12, 12, 12);
        card.setLayoutParams(params);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(8, 8, 8, 8);
        container.setGravity(Gravity.CENTER);

        ImageView imagePreview = new ImageView(this);
        imagePreview.setLayoutParams(new FrameLayout.LayoutParams(cardSize, cardSize));
        imagePreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imagePreview.setClipToOutline(true);

        // ---------------------------------------------------------
        // IMAGE LOADING LOGIC
        // ---------------------------------------------------------
        if (imageUrl != null && !imageUrl.isEmpty()) {
            // Use Glide to load the Cloudinary URL
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.box_background) // Make sure this drawable exists
                    .into(imagePreview);
        } else {
            // Fallback: No image uploaded yet
            imagePreview.setImageResource(R.drawable.box_background);
        }

        TextView label = new TextView(this);
        label.setText(categoryName);
        label.setTextColor(getResources().getColor(R.color.black));
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setTextSize(16f);
        label.setGravity(Gravity.CENTER);
        label.setPadding(0, 8, 0, 0);

        container.addView(imagePreview);
        container.addView(label);
        card.addView(container);

        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, G2_Closet_CategoryActivity.class);
            intent.putExtra("CATEGORY_NAME", categoryName);
            intent.putExtra("CATEGORY_ID", categoryId); // Pass ID for better querying in next screen
            startActivity(intent);
        });

        card.setOnLongClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Category")
                    .setMessage("Are you sure you want to delete \"" + categoryName + "\"?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        // Remove from UI (though listener will do it too)
                        gridLayout.removeView(card);
                        existingCategoryIds.remove(categoryId);
                        categoryViews.remove(categoryId);

                        // Remove from Firebase
                        dbRef.child(mAuth.getCurrentUser().getUid())
                                .child("closetData")
                                .child("categories")
                                .child(categoryId)
                                .removeValue();

                        // Optional: Call Cloudinary API to delete images if needed (requires backend usually)
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });

        gridLayout.addView(card);
        categoryViews.put(categoryId, card);
    }

    private void addPermanentCategoryIfNeeded() {
        String permanentCategoryName = "PermanentCategory";
        String permanentCategoryId = "permanent_category_id";

        DatabaseReference categoryRef = dbRef.child(mAuth.getCurrentUser().getUid())
                .child("closetData")
                .child("categories")
                .child(permanentCategoryId);

        categoryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    Map<String, Object> catData = new HashMap<>();
                    catData.put("id", permanentCategoryId);
                    catData.put("name", permanentCategoryName);
                    categoryRef.setValue(catData);

                    addCategoryToUI(permanentCategoryId, permanentCategoryName, "");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    private void addPermanentCategoryToUI() {
        String permanentCategoryName = "PermanentCategory";
        String permanentCategoryId = "permanent_category_id";

        if (!existingCategoryIds.contains(permanentCategoryId)) {
            addCategoryToUI(permanentCategoryId, permanentCategoryName, "");
        }
    }

    public void onButtonClicked(View view) {
        Intent intent = null;
        int viewId = view.getId();

        if (viewId == R.id.home_menu) {
            intent = new Intent(this, D1_FeedActivity.class);
        } else if (viewId == R.id.calendar_menu) {
            intent = new Intent(this, E1_CalendarActivity.class);
        } else if (viewId == R.id.camera_menu) {
            intent = new Intent(this, F1_CameraActivity.class);
        } else if (viewId == R.id.closet_menu) {
            return;
        } else if (viewId == R.id.profile_menu) {
            intent = new Intent(this, I1_ProfileActivity.class);
        }

        if (intent != null) {
            startActivity(intent);
            finish();
        }
    }
}

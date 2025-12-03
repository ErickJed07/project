package com.example.project;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;

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
        dbRef = FirebaseDatabase.getInstance().getReference("Users");
        storage = FirebaseStorage.getInstance();

        gridLayout = findViewById(R.id.galleryGrid);
        gridLayout.setColumnCount(3);

        addButtonView = findViewById(R.id.AddCategory);

        loadCategoriesFromFirebase();

        addButtonView.setOnClickListener(v -> onAddCategoryClicked(v));

        initializeDefaultCategories();

        findViewById(R.id.newoutfit).setOnClickListener(view ->
                startActivity(new Intent(G1_ClosetActivity.this, H1_DressActivity.class))
        );
    }

    private void loadCategoriesFromFirebase() {
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();

        dbRef.child(uid).child("categories").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (View view : categoryViews.values()) {
                    gridLayout.removeView(view);
                }
                categoryViews.clear();
                existingCategoryIds.clear();

                    String preOutfitKey = "PreOutfit";

                    if (snapshot.hasChild(preOutfitKey)) {
                    processCategorySnapshot(snapshot.child(preOutfitKey));
                }

                for (DataSnapshot child : snapshot.getChildren()) {
                    if (child.getKey().equals(preOutfitKey)) {
                        continue;
                    }
                    processCategorySnapshot(child);
                }

                gridLayout.removeView(addButtonView);
                gridLayout.addView(addButtonView);
            }

            @Override
            public void onCancelled(DatabaseError error) { }
        });
    }

    private void processCategorySnapshot(DataSnapshot child) {
        String categoryId = child.getKey();
        String name = child.child("name").getValue(String.class);

        if (name == null) {
            name = categoryId;
        }

        String firstImageUrl = "";

        if (child.hasChild("photos")) {
            for (DataSnapshot photoSnap : child.child("photos").getChildren()) {
                if (photoSnap.hasChild("url")) {
                    String url = photoSnap.child("url").getValue(String.class);
                    if (url != null && !url.isEmpty()) {
                        firstImageUrl = url;
                        break;
                    }
                } else {
                    Object value = photoSnap.getValue();
                    if (value instanceof String) {
                        String url = (String) value;
                        if (url != null && !url.isEmpty()) {
                            firstImageUrl = url;
                            break;
                        }
                    }
                }
            }
        }

        if (categoryId != null && name != null) {
            addCategoryToUI(categoryId, name, firstImageUrl);
        }
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
        String sanitizedName = categoryName.replaceAll("[.#$\\[\\]]", "");

        if (sanitizedName.isEmpty()) {
            Toast.makeText(this, "Invalid Category Name", Toast.LENGTH_SHORT).show();
            return;
        }

        String safeId = sanitizedName;
        DatabaseReference userCategoryRef = dbRef.child(uid).child("categories").child(safeId);

        Map<String, Object> catData = new HashMap<>();
        catData.put("id", safeId);
        catData.put("name", categoryName);

        userCategoryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Toast.makeText(G1_ClosetActivity.this, "Category already exists", Toast.LENGTH_SHORT).show();
                } else {
                    userCategoryRef.setValue(catData);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) { }
        });
    }

    private void addCategoryToUI(String categoryId, String categoryName, String imageUrl) {
        if (existingCategoryIds.contains(categoryId)) return;
        existingCategoryIds.add(categoryId);

        float scale = getResources().getDisplayMetrics().density;
        int cardSize = (int) (100 * scale);

        CardView card = new CardView(this);
//        card.setRadius(24f);
        card.setCardElevation(6f);
        card.setUseCompatPadding(true);
        card.setCardBackgroundColor(getResources().getColor(R.color.white));

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(0, 0, 0, 0);
        card.setLayoutParams(params);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, 20, 0, 0);
        container.setGravity(Gravity.CENTER);

        ImageView imagePreview = new ImageView(this);
        imagePreview.setLayoutParams(new FrameLayout.LayoutParams(cardSize, cardSize));
        imagePreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imagePreview.setClipToOutline(true);

        // CHANGED: Logic to show default icons if no user photo exists
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_placeholder_2)
                    .into(imagePreview);
        } else {
            // Use the helper method to get the specific drawable
            int defaultIconResId = getDefaultCategoryIcon(categoryName);
            imagePreview.setImageResource(defaultIconResId);
        }

        TextView label = new TextView(this);
        label.setText(categoryName);
        label.setTextColor(getResources().getColor(R.color.black));
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setTextSize(14f);
        label.setGravity(Gravity.CENTER);
        label.setPadding(0, 0, 0, 0);

        container.addView(imagePreview);
        container.addView(label);
        card.addView(container);

        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, G2_Closet_CategoryActivity.class);
            intent.putExtra("CATEGORY_NAME", categoryName);
            intent.putExtra("CATEGORY_ID", categoryId);
            startActivity(intent);
        });

        card.setOnLongClickListener(v -> {
            if (isDefaultCategory(categoryName)) {
                Toast.makeText(this, "Cannot delete Default Category", Toast.LENGTH_SHORT).show();
                return true;
            }

            new AlertDialog.Builder(this)
                    .setTitle("Delete Category")
                    .setMessage("Are you sure you want to delete \"" + categoryName + "\"?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        gridLayout.removeView(card);
                        existingCategoryIds.remove(categoryId);
                        categoryViews.remove(categoryId);

                        dbRef.child(mAuth.getCurrentUser().getUid())
                                .child("categories")
                                .child(categoryId)
                                .removeValue();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });

        gridLayout.addView(card);
        categoryViews.put(categoryId, card);
    }

    private boolean isDefaultCategory(String name) {
        String[] defaults = {
                "PreOutfit", "Hat", "Accessories", "Outer",
                "Top", "Bag", "Bottom", "Shoes", "Dress"
        };
        for (String s : defaults) {
            if (s.equals(name)) return true;
        }
        return false;
    }

    // NEW HELPER METHOD: Assigns drawables based on category name
    // IMPORTANT: Change 'R.drawable.box_background' to your actual image names
    private int getDefaultCategoryIcon(String categoryName) {
        switch (categoryName) {
            case "PreOutfit":
                return R.drawable.box_background; // e.g. R.drawable.ic_pre_outfit
            case "Hat":
                return R.drawable.box_background; // e.g. R.drawable.ic_hat
            case "Accessories":
                return R.drawable.box_background; // e.g. R.drawable.ic_accessories
            case "Outer":
                return R.drawable.box_background; // e.g. R.drawable.ic_outer
            case "Top":
                return R.drawable.box_background; // e.g. R.drawable.ic_top
            case "Bag":
                return R.drawable.box_background; // e.g. R.drawable.ic_bag
            case "Bottom":
                return R.drawable.box_background; // e.g. R.drawable.ic_bottom
            case "Shoes":
                return R.drawable.box_background; // e.g. R.drawable.ic_shoes
            case "Dress":
                return R.drawable.box_background; // e.g. R.drawable.ic_dress
            default:
                return R.drawable.box_background;
        }
    }

    private void initializeDefaultCategories() {
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();
        DatabaseReference categoriesRef = dbRef.child(uid).child("categories");

        String[] defaultCategories = {
                "PreOutfit", "Hat", "Accessories", "Outer",
                "Top", "Bag", "Bottom", "Shoes", "Dress"
        };

        for (String categoryName : defaultCategories) {
            String categoryId = categoryName.replaceAll("[.#$\\[\\]-]", "");

            DatabaseReference specificCatRef = categoriesRef.child(categoryId);
            final String finalId = categoryId;
            final String finalName = categoryName;

            specificCatRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (!dataSnapshot.exists()) {
                        Map<String, Object> catData = new HashMap<>();
                        catData.put("id", finalId);
                        catData.put("name", finalName);
                        specificCatRef.setValue(catData);
                    }
                }
                @Override
                public void onCancelled(DatabaseError databaseError) { }
            });
        }
    }
}

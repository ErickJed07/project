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
        dbRef = FirebaseDatabase.getInstance().getReference("ClosetData");
        storage = FirebaseStorage.getInstance();
        gridLayout = findViewById(R.id.galleryGrid);
        addButtonView = findViewById(R.id.AddCategory);

        // Existing categories from Firebase
        loadCategoriesFromFirebase();

        // Add listener for category addition
        addButtonView.setOnClickListener(v -> onAddCategoryClicked(v));

        // Always add the permanent category if it's missing
        addPermanentCategoryIfNeeded();

        // Navigate to new outfit activity
        findViewById(R.id.newoutfit).setOnClickListener(view ->
                startActivity(new Intent(G1_ClosetActivity.this, H1_DressActivity.class))
        );
    }

    private void loadCategoriesFromFirebase() {
        String uid = mAuth.getCurrentUser().getUid();
        dbRef.child(uid).child("categories").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (View view : categoryViews.values()) {
                    gridLayout.removeView(view);
                }
                categoryViews.clear();
                existingCategoryIds.clear();

                // Add permanent category first
                addPermanentCategoryToUI();

                // Add other categories after the permanent category
                for (DataSnapshot child : snapshot.getChildren()) {
                    String categoryId = child.getKey();
                    String name = child.child("name").getValue(String.class);
                    if (categoryId != null && name != null && !categoryId.equals("permanent_category_id")) {
                        addCategoryToUI(categoryId, name);
                    }
                }

                // Add the "Add Category" button back
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
                saveCategoryToFirebase(categoryName);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void saveCategoryToFirebase(String categoryName) {
        String uid = mAuth.getCurrentUser().getUid();
        DatabaseReference userCategoryRef = dbRef.child(uid).child("categories").push();
        String categoryId = userCategoryRef.getKey();
        G6_Closet_CategoryFirebase category = new G6_Closet_CategoryFirebase(categoryId, categoryName);
        userCategoryRef.setValue(category);
        addCategoryToUI(categoryId, categoryName);

        gridLayout.removeView(addButtonView);
        gridLayout.addView(addButtonView);
    }

    private void addCategoryToUI(String categoryId, String categoryName) {
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

        // Local images now stored per user
        File categoryDir = new File(getFilesDir(), "ClosetImages/" + mAuth.getCurrentUser().getUid() + "/" + categoryName);
        File[] imageFiles = categoryDir.listFiles((dir, name) -> name.endsWith(".jpg") || name.endsWith(".png"));
        if (imageFiles != null && imageFiles.length > 0) {
            File latest = imageFiles[0];
            for (File file : imageFiles) {
                if (file.lastModified() > latest.lastModified()) latest = file;
            }
            imagePreview.setImageURI(Uri.fromFile(latest));
        } else {
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
            startActivity(intent);
        });

        card.setOnLongClickListener(v -> {
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

                        storage.getReference()
                                .child("ClosetImages")
                                .child(mAuth.getCurrentUser().getUid())
                                .child(categoryName)
                                .listAll()
                                .addOnSuccessListener(listResult -> {
                                    for (var item : listResult.getItems()) {
                                        item.delete();
                                    }
                                });

                        deleteCategoryLocalFolder(categoryName);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });

        gridLayout.addView(card);
        categoryViews.put(categoryId, card);
    }

    private void deleteCategoryLocalFolder(String categoryName) {
        File categoryDir = new File(getFilesDir(), "ClosetImages/" + mAuth.getCurrentUser().getUid() + "/" + categoryName);
        if (categoryDir.exists() && categoryDir.isDirectory()) {
            File[] files = categoryDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            categoryDir.delete();
        }
    }

    private void addPermanentCategoryIfNeeded() {
        String permanentCategoryName = "PermanentCategory";
        String permanentCategoryId = "permanent_category_id"; // A fixed ID

        DatabaseReference categoryRef = dbRef.child(mAuth.getCurrentUser().getUid()).child("categories").child(permanentCategoryId);

        // Check if the permanent category already exists in the database
        categoryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    // If the permanent category doesn't exist, add it
                    G6_Closet_CategoryFirebase category = new G6_Closet_CategoryFirebase(permanentCategoryId, permanentCategoryName);
                    categoryRef.setValue(category);
                    addCategoryToUI(permanentCategoryId, permanentCategoryName);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle error
            }
        });
    }

    private void addPermanentCategoryToUI() {
        String permanentCategoryName = "PermanentCategory";
        String permanentCategoryId = "permanent_category_id"; // A fixed ID

        // Check if the permanent category is already displayed
        if (!existingCategoryIds.contains(permanentCategoryId)) {
            addCategoryToUI(permanentCategoryId, permanentCategoryName);
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

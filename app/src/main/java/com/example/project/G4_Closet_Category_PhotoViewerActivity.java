package com.example.project;

import android.os.Bundle;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import androidx.annotation.NonNull;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.tabs.TabLayout;

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

        // Update details for the initial item
        updateItemDetails(startIndex);

        // Register callback to update details on swipe
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateItemDetails(position);
            }
        });

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finishWithAnimation());

        ImageButton btnMore = findViewById(R.id.btnMore);
        if (btnMore != null) {
            btnMore.setOnClickListener(this::showMoreMenu);
        }

        // --- NEW: Connect Photo Movement to Bottom Sheet ---
        View detailsCard = findViewById(R.id.detailsCard);
        View photoContainer = findViewById(R.id.photoContainer);
        View uiOverlay = findViewById(R.id.uiOverlay);
        if (detailsCard != null && photoContainer != null) {
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(detailsCard);
            behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                @Override
                public void onStateChanged(@NonNull View bottomSheet, int newState) {}

                @Override
                public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                    if (slideOffset > 0) {
                        // 1. Scale/Move the Photo Container (now just the ViewPager)
                        // This shrinks the image into the top part of the screen
                        float scale = 1.0f - (slideOffset * 0.4f); 
                        photoContainer.setScaleX(scale);
                        photoContainer.setScaleY(scale);
                        photoContainer.setPivotY(0);
                        photoContainer.setPivotX(photoContainer.getWidth() / 2f);
                        photoContainer.setTranslationY(-slideOffset * 20f);

                        // 2. Handle the UI Overlay (Buttons)
                        // We keep the buttons fixed at the top, or move them much less
                        if (uiOverlay != null) {
                            uiOverlay.setTranslationY(-slideOffset * 10f); // Minimal movement
                        }
                    } else {
                        photoContainer.setScaleX(1.0f);
                        photoContainer.setScaleY(1.0f);
                        photoContainer.setTranslationY(0);
                        if (uiOverlay != null) uiOverlay.setTranslationY(0);
                    }
                }
            });
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

    private void showMoreMenu(View v) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.layout_bottom_sheet_more, null);
        bottomSheetDialog.setContentView(sheetView);

        sheetView.findViewById(R.id.menuEdit).setOnClickListener(view -> {
            bottomSheetDialog.dismiss();
            showEditItemSheet();
        });

        sheetView.findViewById(R.id.menuDelete).setOnClickListener(view -> {
            deleteCurrentImage();
            bottomSheetDialog.dismiss();
        });

        sheetView.findViewById(R.id.btnCancel).setOnClickListener(view -> {
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    private void showEditItemSheet() {
        BottomSheetDialog editSheet = new BottomSheetDialog(this);
        View editView = LayoutInflater.from(this).inflate(R.layout.layout_bottom_sheet_edit_item, null);
        editSheet.setContentView(editView);

        TextView tvSize = editView.findViewById(R.id.tvSize);
        TextView tvColorName = editView.findViewById(R.id.tvColorName);
        View viewColorCircle = editView.findViewById(R.id.viewColorCircle);
        
        // Populate with current data
        int currentPos = viewPager.getCurrentItem();
        // etName.setText(imageUrls.get(currentPos).contains("hoodie") ? "Classic Hoodie" : "Item " + (currentPos + 1));

        // Size Selection
        View sizeContainer = (View) tvSize.getParent();
        if (sizeContainer.getParent() instanceof View) {
            ((View) sizeContainer.getParent()).setOnClickListener(v -> {
                SizeSelectionBottomSheet sizeSheet = SizeSelectionBottomSheet.newInstance(tvSize.getText().toString(), tvSize::setText);
                sizeSheet.show(getSupportFragmentManager(), "SizeSelection");
            });
        }

        // Color Selection
        if (tvColorName.getParent() instanceof View) {
            ((View) tvColorName.getParent()).setOnClickListener(v -> {
                ColorSelectionBottomSheet colorSheet = ColorSelectionBottomSheet.newInstance((selectedColors, isMultiple) -> {
                    if (!selectedColors.isEmpty()) {
                        ColorOption color = selectedColors.get(0);
                        tvColorName.setText(color.getName());
                        viewColorCircle.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor(color.getHexCode())));
                    }
                });
                colorSheet.show(getSupportFragmentManager(), "ColorSelection");
            });
        }

        TabLayout tabLayout = editView.findViewById(R.id.tabLayoutTags);
        TextView tvTagTypeLabel = editView.findViewById(R.id.tvTagTypeLabel);
        ChipGroup cgOccasions = editView.findViewById(R.id.cgOccasions);
        ChipGroup cgSeasons = editView.findViewById(R.id.cgSeasons);

        // Tab selection logic
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    tvTagTypeLabel.setText("Occasions");
                    cgOccasions.setVisibility(View.VISIBLE);
                    cgSeasons.setVisibility(View.GONE);
                } else {
                    tvTagTypeLabel.setText("Seasons");
                    cgOccasions.setVisibility(View.GONE);
                    cgSeasons.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Add Tag
        editView.findViewById(R.id.btnAddTag).setOnClickListener(v -> {
            boolean isOccasion = tabLayout.getSelectedTabPosition() == 0;
            showAddTagDialog(isOccasion ? cgOccasions : cgSeasons, isOccasion ? "Occasion" : "Season");
        });

        // Initialize with some mock tags
        addTagChip(cgOccasions, "Streetwear");
        addTagChip(cgOccasions, "Minimalist");
        addTagChip(cgSeasons, "Winter");
        addTagChip(cgSeasons, "Spring");

        editView.findViewById(R.id.btnSheetSave).setOnClickListener(v -> {
            /*
            String newName = etName.getText().toString().trim();
            if (newName.isEmpty()) {
                etName.setError("Name required");
                return;
            }
            */
            // In a real app, update Firebase here
            Toast.makeText(this, "Item updated", Toast.LENGTH_SHORT).show();
            editSheet.dismiss();
            updateItemDetails(currentPos); // Refresh UI
        });

        editSheet.show();
    }

    private void showAddTagDialog(ChipGroup chipGroup, String type) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_tag_enhanced, null);
        builder.setView(dialogView);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        TextView tvTitle = dialogView.findViewById(R.id.tvAddTagTitle);
        com.google.android.material.textfield.TextInputLayout tilName = dialogView.findViewById(R.id.tilTagName);
        EditText etName = dialogView.findViewById(R.id.etTagName);
        View btnAdd = dialogView.findViewById(R.id.btnSubmitTag);

        tvTitle.setText("Add New " + type);
        tilName.setHint("e.g. " + (type.equals("Occasion") ? "Vacation" : "Winter"));
        etName.requestFocus();

        btnAdd.setOnClickListener(v -> {
            String tag = etName.getText().toString().trim();
            if (!tag.isEmpty()) {
                addTagChip(chipGroup, tag);
                dialog.dismiss();
            } else {
                etName.setError("Name required");
            }
        });

        dialog.show();
    }

    private void addTagChip(ChipGroup chipGroup, String text) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setCloseIconVisible(true);
        chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#F5F5F7")));
        chip.setOnCloseIconClickListener(v -> chipGroup.removeView(chip));
        chipGroup.addView(chip);
    }

    private void updateItemDetails(int position) {
        if (imageUrls == null || position < 0 || position >= imageUrls.size()) return;
        String currentUrl = imageUrls.get(position);
        fetchItemMetadata(currentUrl);
    }

    private void fetchItemMetadata(String url) {
        if (uid == null) return;

        DatabaseReference ref;
        if ("all_clothes".equals(categoryId)) {
            ref = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("categories");
        } else if (categoryId != null) {
            ref = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("categories").child(categoryId).child("photos");
        } else {
            return;
        }

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if ("all_clothes".equals(categoryId)) {
                    for (DataSnapshot catSnap : snapshot.getChildren()) {
                        if (catSnap.hasChild("photos")) {
                            for (DataSnapshot photoSnap : catSnap.child("photos").getChildren()) {
                                if (isMatch(photoSnap, url)) {
                                    updateUIWithMetadata(photoSnap);
                                    return;
                                }
                            }
                        }
                    }
                } else {
                    for (DataSnapshot photoSnap : snapshot.getChildren()) {
                        if (isMatch(photoSnap, url)) {
                            updateUIWithMetadata(photoSnap);
                            return;
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Metadata fetch cancelled: " + error.getMessage());
            }
        });
    }

    private boolean isMatch(DataSnapshot photoSnap, String url) {
        String dbUrl = null;
        if (photoSnap.hasChild("imageUrl")) {
            dbUrl = photoSnap.child("imageUrl").getValue(String.class);
        } else if (photoSnap.hasChild("url")) {
            dbUrl = photoSnap.child("url").getValue(String.class);
        } else if (photoSnap.getValue() instanceof String) {
            dbUrl = photoSnap.getValue(String.class);
        }
        return url != null && url.equals(dbUrl);
    }

    private void updateUIWithMetadata(DataSnapshot photoSnap) {
        TextView tvCategoryName = findViewById(R.id.tvcategory);
        TextView tvSizeValue = findViewById(R.id.tvSizeValue);
        View viewColor = findViewById(R.id.viewColor);
        TextView tvColorValue = findViewById(R.id.tvColorValue);
        ChipGroup chipGroupTags = findViewById(R.id.chipGroupTags);

        // Category
        String category = photoSnap.child("category").getValue(String.class);
        if (category == null && !"all_clothes".equals(categoryId)) {
            category = categoryId; // Fallback to categoryId from intent
        }
        if (tvCategoryName != null) {
            tvCategoryName.setText(category != null ? category : "Unknown");
        }

        // Size
        String size = photoSnap.child("size").getValue(String.class);
        if (tvSizeValue != null) {
            tvSizeValue.setText(size != null ? size : "N/A");
        }

        // Color
        if (viewColor != null) {
            DataSnapshot colorsSnap = photoSnap.child("colors");
            if (colorsSnap.exists()) {
                List<String> colors = new ArrayList<>();
                Object colorsValue = colorsSnap.getValue();
                if (colorsValue instanceof List) {
                    List<?> list = (List<?>) colorsValue;
                    for (Object item : list) {
                        if (item instanceof String) colors.add((String) item);
                    }
                } else if (colorsValue instanceof Map) {
                    Map<?, ?> colorMap = (Map<?, ?>) colorsValue;
                    for (Object item : colorMap.values()) {
                        if (item instanceof String) colors.add((String) item);
                    }
                }

                if (!colors.isEmpty()) {
                    String colorName = colors.get(0);
                    int colorHex = getColorHexFromName(colorName);
                    viewColor.setBackgroundTintList(android.content.res.ColorStateList.valueOf(colorHex));
                    if (tvColorValue != null) {
                        tvColorValue.setText(colorName);
                    }
                }
            }
        }

        // Tags (Occasions + Seasons)
        if (chipGroupTags != null) {
            chipGroupTags.removeAllViews();
            addTagsFromSnap(photoSnap.child("occasions"), chipGroupTags);
            addTagsFromSnap(photoSnap.child("season"), chipGroupTags);
        }
    }

    private void addTagsFromSnap(DataSnapshot snap, ChipGroup group) {
        if (!snap.exists()) return;
        Object snapValue = snap.getValue();
        if (snapValue instanceof List) {
            List<?> tags = (List<?>) snapValue;
            for (Object tag : tags) {
                if (tag instanceof String) addTagToGroup(group, (String) tag);
            }
        } else if (snapValue instanceof Map) {
            Map<?, ?> tags = (Map<?, ?>) snapValue;
            for (Object tag : tags.values()) {
                if (tag instanceof String) addTagToGroup(group, (String) tag);
            }
        }
    }

    private void addTagToGroup(ChipGroup group, String text) {
        TextView tagView = new TextView(this);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        tagView.setLayoutParams(params);
        tagView.setBackgroundResource(R.drawable.bg_tag);
        tagView.setPadding(32, 16, 32, 16);
        tagView.setText(text);
        tagView.setTextColor(Color.BLACK);
        tagView.setTypeface(null, android.graphics.Typeface.BOLD);
        group.addView(tagView);
    }

    private int getColorHexFromName(String name) {
        if (name == null) return Color.LTGRAY;
        switch (name) {
            case "Black": return Color.BLACK;
            case "White": return Color.WHITE;
            case "Red": return Color.RED;
            case "Blue": return Color.BLUE;
            case "Green": return Color.GREEN;
            case "Yellow": return Color.YELLOW;
            case "Orange": return Color.parseColor("#FFA500");
            case "Purple": return Color.parseColor("#800080");
            case "Pink": return Color.parseColor("#FFC0CB");
            case "Brown": return Color.parseColor("#A52A2A");
            case "Gray": return Color.GRAY;
            case "Beige": return Color.parseColor("#F5F5DC");
            default: return Color.LTGRAY;
        }
    }
}


package com.example.project;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.RelativeCornerSize;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseLandmark;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;

public class AiActivity extends AppCompatActivity {

    private ViewGroup llAvatars;
    private View avatarMain, btnAddAvatar;
    private ImageView ivMainModel;
    private boolean isExpanded = false;
    private Uri currentPhotoUri;

    private RecyclerView rvCategories, rvItems, rvSelectedPreview;
    private View llEmptyState, llPreviewContainer, btnTogglePreview;
    private ImageView ivPreviewToggleIcon;
    private AiCategoryAdapter categoryAdapter;
    private AiItemAdapter itemAdapter;
    private AiPreviewAdapter previewAdapter;

    private List<ViewCategoriesActivity.CategoryModel> categoryList = new ArrayList<>();
    private List<ClothingItem> itemList = new ArrayList<>();
    private List<ClothingItem> originalItemList = new ArrayList<>();
    private Set<ClothingItem> selectedItems = new HashSet<>();
    private List<ClothingItem> previewList = new ArrayList<>();

    private String selectedSeason = "All";
    private String selectedOccasion = "All";
    private String selectedSort = "All";
    private String currentCategoryName = "";
    private String currentCategoryId = "all_clothes";
    private boolean isWomanSelected = true;

    private BottomSheetBehavior<View> bottomSheetBehavior;
    private int bottomInset = 0;

    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;

    private PoseDetector poseDetector;

    private final ActivityResultLauncher<String> cameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) openCamera();
                else Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            });

    private final ActivityResultLauncher<Uri> takePhotoLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), result -> {
                if (result) handleImageSelection(currentPhotoUri);
            });

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), this::handleImageSelection);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai);

        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference("Users");

        PoseDetectorOptions options = new PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.SINGLE_IMAGE_MODE)
                .build();
        poseDetector = PoseDetection.getClient(options);

        llAvatars = findViewById(R.id.ll_avatars);
        avatarMain = findViewById(R.id.avatar_main);
        btnAddAvatar = findViewById(R.id.btn_add_avatar);
        ivMainModel = findViewById(R.id.iv_main_model);
        llEmptyState = findViewById(R.id.ll_empty_state);
        llPreviewContainer = findViewById(R.id.ll_preview_container);
        btnTogglePreview = findViewById(R.id.btn_toggle_preview);
        ivPreviewToggleIcon = findViewById(R.id.iv_preview_toggle_icon);

        findViewById(R.id.btn_back_ai).setOnClickListener(v -> finish());

        View mainContent = findViewById(R.id.cl_main_content);
        ViewCompat.setOnApplyWindowInsetsListener(mainContent, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            bottomInset = systemBars.bottom;

            if (bottomSheetBehavior != null) {
                int currentHeight = (int) (bottomSheetBehavior.getPeekHeight() +
                        (findViewById(R.id.bottom_sheet_card).getHeight() - bottomSheetBehavior.getPeekHeight()) * bottomSheetBehavior.calculateSlideOffset());
                updateMainModelHeight(currentHeight);
            }

            return WindowInsetsCompat.CONSUMED;
        });

        avatarMain.setOnClickListener(v -> toggleAvatars());
        btnAddAvatar.setOnClickListener(v -> showAddAvatarOptions());

        findViewById(R.id.btn_filter).setOnClickListener(v -> showFilterBottomSheet());

        View.OnClickListener generateListener = v -> {
            // TODO: Implement generation logic or call existing one if any
            Toast.makeText(this, "Generating Outfit...", Toast.LENGTH_SHORT).show();
        };
        findViewById(R.id.btn_generate_collapsed).setOnClickListener(generateListener);
        findViewById(R.id.btn_generate_expanded).setOnClickListener(generateListener);

        setupRecyclerViews();
        setupBottomSheet();
        loadCategories();
        updatePreview();
    }

    private void setupBottomSheet() {
        View bottomSheet = findViewById(R.id.bottom_sheet_card);
        View grabber = findViewById(R.id.bottom_sheet_grabber);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        // Disable manual dragging
        bottomSheetBehavior.setDraggable(false);

        // Add click listener to grabber for toggling
        grabber.setOnClickListener(v -> {
            if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            } else {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });

        // Calculate half screen height
        bottomSheet.post(() -> {
            int screenHeight = getResources().getDisplayMetrics().heightPixels;
            int halfHeight = screenHeight / 2;

            ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
            params.height = halfHeight;
            bottomSheet.setLayoutParams(params);

            // Calculate peek height to show only the header (Wardrobe + Filter)
            View header = findViewById(R.id.cl_header);
            int peekHeight = header.getBottom() + (int) getResources().getDimension(R.dimen.spacing_small);
            bottomSheetBehavior.setPeekHeight(peekHeight);

            // Set initial state to expanded
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            grabber.setRotation(180); // Pointing down when expanded

            // Dynamically adjust preview container bottom margin to sit above bottom sheet
            updatePreviewContainerMargin(bottomSheetBehavior.getPeekHeight());
            updateMainModelHeight(bottomSheetBehavior.getPeekHeight());
        });

        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_DRAGGING || newState == BottomSheetBehavior.STATE_EXPANDED) {
                    if (!(rvItems.getLayoutManager() instanceof GridLayoutManager)) {
                        rvItems.setLayoutManager(new GridLayoutManager(AiActivity.this, 3));
                    }
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    rvItems.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(AiActivity.this, RecyclerView.HORIZONTAL, false));
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                View btnFilter = findViewById(R.id.btn_filter);
                View btnGenerateCollapsed = findViewById(R.id.btn_generate_collapsed);
                View btnGenerateExpanded = findViewById(R.id.btn_generate_expanded);

                // Rotate grabber icon (0 = up, 180 = down)
                grabber.setRotation(180 * slideOffset);

                // Transition alpha based on slideOffset (0.0 = collapsed, 1.0 = expanded)
                btnFilter.setAlpha(slideOffset);
                btnGenerateCollapsed.setAlpha(1.0f - slideOffset);
                btnGenerateExpanded.setAlpha(slideOffset);

                // Update visibility to prevent clicks when hidden
                if (slideOffset <= 0.05f) {
                    btnFilter.setVisibility(View.GONE);
                    btnGenerateExpanded.setVisibility(View.GONE);
                    btnGenerateCollapsed.setVisibility(View.VISIBLE);
                } else if (slideOffset >= 0.95f) {
                    btnFilter.setVisibility(View.VISIBLE);
                    btnGenerateExpanded.setVisibility(View.VISIBLE);
                    btnGenerateCollapsed.setVisibility(View.GONE);
                } else {
                    btnFilter.setVisibility(View.VISIBLE);
                    btnGenerateExpanded.setVisibility(View.VISIBLE);
                    btnGenerateCollapsed.setVisibility(View.VISIBLE);
                }

                // Update preview container position during slide
                int currentHeight = (int) (bottomSheetBehavior.getPeekHeight() +
                    (bottomSheet.getHeight() - bottomSheetBehavior.getPeekHeight()) * slideOffset);
                updatePreviewContainerMargin(currentHeight);
                updateMainModelHeight(currentHeight);
            }
        });
    }

    private void updateMainModelHeight(int bottomSheetHeight) {
        View mainContent = findViewById(R.id.cl_main_content);
        if (mainContent != null && mainContent.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) mainContent.getLayoutParams();
            // Subtract the bottomInset to compensate for fitsSystemWindows padding
            params.bottomMargin = Math.max(0, bottomSheetHeight - bottomInset);
            mainContent.setLayoutParams(params);
        }
    }

    private void updatePreviewContainerMargin(int margin) {
        if (llPreviewContainer.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) llPreviewContainer.getLayoutParams();
            // Connect with a 4dp gap
            int gap = (int) (4 * getResources().getDisplayMetrics().density);
            params.bottomMargin = margin + gap;
            llPreviewContainer.setLayoutParams(params);
        }
    }

    private void showFilterBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_filter_bottom_sheet, null);
        dialog.setContentView(view);

        RecyclerView rvSeasons = view.findViewById(R.id.rv_seasons);
        List<String> seasons = Arrays.asList("All", "Summer", "Winter", "Spring", "Autumn");
        AiFilterChipAdapter seasonAdapter = new AiFilterChipAdapter(this, seasons, (item, pos) -> selectedSeason = item);
        seasonAdapter.setSelected(selectedSeason);
        rvSeasons.setAdapter(seasonAdapter);

        RecyclerView rvOccasions = view.findViewById(R.id.rv_occasions);
        List<String> occasions = Arrays.asList("All", "Casual", "Formal", "Party", "Work", "Sport");
        AiFilterChipAdapter occasionAdapter = new AiFilterChipAdapter(this, occasions, (item, pos) -> selectedOccasion = item);
        occasionAdapter.setSelected(selectedOccasion);
        rvOccasions.setAdapter(occasionAdapter);

        RecyclerView rvSort = view.findViewById(R.id.rv_sort_options);
        List<String> sorts = Arrays.asList("All", "Faves", "Latest", "Oldest");
        AiFilterChipAdapter sortOptionAdapter = new AiFilterChipAdapter(this, sorts, (item, pos) -> selectedSort = item);
        sortOptionAdapter.setSelected(selectedSort);
        rvSort.setAdapter(sortOptionAdapter);

        view.findViewById(R.id.btn_reset).setOnClickListener(v -> {
            seasonAdapter.reset();
            occasionAdapter.reset();
            sortOptionAdapter.reset();
            selectedSeason = "All";
            selectedOccasion = "All";
            selectedSort = "All";
        });

        view.findViewById(R.id.btn_apply_filters).setOnClickListener(v -> {
            applyFilters();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void applyFilters() {
        List<ClothingItem> matchingItems = new ArrayList<>();
        List<ClothingItem> nonMatchingItems = new ArrayList<>();

        for (ClothingItem item : originalItemList) {
            boolean matchesSeason = selectedSeason.equals("All") || (item.getSeason() != null && item.getSeason().equalsIgnoreCase(selectedSeason));
            boolean matchesOccasion = selectedOccasion.equals("All") || (item.getOccasions() != null && item.getOccasions().contains(selectedOccasion));
            boolean matchesFave = !selectedSort.equals("Faves") || item.isFavorite();

            if (matchesSeason && matchesOccasion && matchesFave) {
                matchingItems.add(item);
            } else {
                nonMatchingItems.add(item);
            }
        }

        String sortOption = selectedSort.equals("Faves") ? "Latest" : selectedSort;
        sortItemList(matchingItems, sortOption);
        sortItemList(nonMatchingItems, sortOption);

        itemList.clear();
        itemList.addAll(matchingItems);
        itemList.addAll(nonMatchingItems);

        itemAdapter.updateFilters(selectedSeason, selectedOccasion, selectedSort);
        itemAdapter.updateList(new ArrayList<>(itemList));
    }

    private void sortItemList(List<ClothingItem> list, String option) {
        switch (option) {
            case "Oldest":
                Collections.sort(list, (o1, o2) -> Long.compare(o1.getTimestamp(), o2.getTimestamp()));
                break;
            case "Latest":
                Collections.sort(list, (o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
                break;
            default:
                Collections.sort(list, (o1, o2) -> o1.getId().compareTo(o2.getId()));
                break;
        }
    }

    private void setupRecyclerViews() {
        rvCategories = findViewById(R.id.rv_categories);
        categoryAdapter = new AiCategoryAdapter(this, categoryList, category -> {
            currentCategoryName = category.name;
            currentCategoryId = category.id;
            if (itemAdapter != null) {
                itemAdapter.setCurrentCategory(currentCategoryId, currentCategoryName);
            }
            loadItems(currentCategoryId);
        });
        rvCategories.setAdapter(categoryAdapter);

        rvItems = findViewById(R.id.rv_items);
        rvItems.setLayoutManager(new GridLayoutManager(this, 3));
        itemAdapter = new AiItemAdapter(this, itemList, (item, isSelected) -> {
            if (isSelected) {
                selectedItems.add(item);
            } else {
                selectedItems.remove(item);
            }
            updatePreview();
        }, this::showImagePreview, (categoryId) -> {
            // "Add clothes" card clicked
            Intent intent = new Intent(this, AddItemActivity.class);
            intent.putExtra("CATEGORY_ID", categoryId);
            startActivity(intent);
        });
        rvItems.setAdapter(itemAdapter);

        rvSelectedPreview = findViewById(R.id.rv_selected_preview);
        previewAdapter = new AiPreviewAdapter(this, previewList, item -> {
            selectedItems.remove(item);
            itemAdapter.notifyDataSetChanged();
            updatePreview();
        }, () -> {
            // Expansion logic when item is clicked in collapsed state
            previewAdapter.setCollapsed(false);
            ivPreviewToggleIcon.animate().rotation(180).setDuration(200).start();
        });
        rvSelectedPreview.setAdapter(previewAdapter);

        setupSwipeNavigation();

        // Remove setupPreviewSwipe() since it's now handled by the toggle listener
        btnTogglePreview.setOnClickListener(v -> {
            if (!previewAdapter.isCollapsed()) {
                // If expanded, collapse it
                previewAdapter.setCollapsed(true);
                ivPreviewToggleIcon.animate().rotation(0).setDuration(200).start();
            } else {
                // If collapsed, expand it
                previewAdapter.setCollapsed(false);
                ivPreviewToggleIcon.animate().rotation(180).setDuration(200).start();
            }
        });
    }

    private void loadCategories() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        dbRef.child(uid).child("categories").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                categoryList.clear();
                int totalItems = 0;

                for (CategoryManager.CategoryItem fixedItem : CategoryManager.getCategories(isWomanSelected)) {
                    long itemCount = 0;
                    DataSnapshot categorySnapshot = snapshot.child(fixedItem.id);
                    if (categorySnapshot.exists() && categorySnapshot.hasChild("photos")) {
                        itemCount = categorySnapshot.child("photos").getChildrenCount();
                        totalItems += itemCount;
                    }
                    categoryList.add(new ViewCategoriesActivity.CategoryModel(fixedItem.id, fixedItem.name, fixedItem.iconRes, (int) itemCount));
                }

                categoryList.add(0, new ViewCategoriesActivity.CategoryModel("all_clothes", "All Clothes", R.drawable.hanger, totalItems));

                categoryAdapter.updateList(new ArrayList<>(categoryList));

                // Find and restore selected category
                int selectedPos = 0;
                for (int i = 0; i < categoryList.size(); i++) {
                    if (categoryList.get(i).id.equals(currentCategoryId)) {
                        selectedPos = i;
                        break;
                    }
                }
                
                categoryAdapter.setSelectedPosition(selectedPos);

                if (!categoryList.isEmpty()) {
                    ViewCategoriesActivity.CategoryModel selectedCategory = categoryList.get(selectedPos);
                    currentCategoryName = selectedCategory.name;
                    currentCategoryId = selectedCategory.id;
                    if (itemAdapter != null) {
                        itemAdapter.setCurrentCategory(currentCategoryId, currentCategoryName);
                    }
                    loadItems(currentCategoryId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AiActivity.this, "Failed to load categories", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadItems(String categoryId) {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        DatabaseReference photosRef;
        if (categoryId.equals("all_clothes")) {
            // Special handling for All Clothes - we need to fetch from all categories
            loadAllItems();
            return;
        } else {
            photosRef = dbRef.child(uid).child("categories").child(categoryId).child("photos");
        }

        photosRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                itemList.clear();
                originalItemList.clear();
                for (DataSnapshot photoSnap : snapshot.getChildren()) {
                    ClothingItem item = parseClothingItem(photoSnap, categoryId);
                    if (item != null) {
                        itemList.add(item);
                        originalItemList.add(item);
                    }
                }
                updateItemsUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AiActivity.this, "Failed to load items", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadAllItems() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        dbRef.child(uid).child("categories").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                itemList.clear();
                originalItemList.clear();
                for (DataSnapshot categorySnap : snapshot.getChildren()) {
                    String catId = categorySnap.getKey();
                    DataSnapshot photosSnap = categorySnap.child("photos");
                    for (DataSnapshot photoSnap : photosSnap.getChildren()) {
                        ClothingItem item = parseClothingItem(photoSnap, catId);
                        if (item != null) {
                            itemList.add(item);
                            originalItemList.add(item);
                        }
                    }
                }
                updateItemsUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AiActivity.this, "Failed to load all items", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private ClothingItem parseClothingItem(DataSnapshot photoSnap, String categoryId) {
        String id = photoSnap.getKey();
        String url = photoSnap.child("imageUrl").getValue(String.class);
        if (url == null) url = photoSnap.child("url").getValue(String.class);
        if (url == null) return null;

        ClothingItem item = new ClothingItem(id, url, categoryId);
        Boolean isFav = photoSnap.child("favorite").getValue(Boolean.class);
        item.setFavorite(isFav != null && isFav);

        Object seasonObj = photoSnap.child("season").getValue();
        if (seasonObj instanceof String) {
            item.setSeason((String) seasonObj);
        } else if (seasonObj instanceof List) {
            List<?> seasons = (List<?>) seasonObj;
            if (!seasons.isEmpty() && seasons.get(0) instanceof String) {
                item.setSeason((String) seasons.get(0));
            }
        }

        DataSnapshot occasionsSnap = photoSnap.child("occasions");
        List<String> occasionsList = new ArrayList<>();
        if (occasionsSnap.exists()) {
            Object occasionsObj = occasionsSnap.getValue();
            if (occasionsObj instanceof List) {
                for (DataSnapshot occasion : occasionsSnap.getChildren()) {
                    String val = occasion.getValue(String.class);
                    if (val != null) occasionsList.add(val);
                }
            } else if (occasionsObj instanceof String) {
                occasionsList.add((String) occasionsObj);
            }
        }
        item.setOccasions(occasionsList);

        Long ts = photoSnap.child("timestamp").getValue(Long.class);
        item.setTimestamp(ts != null ? ts : 0L);
        return item;
    }

    private void updateItemsUI() {
        boolean isAllClothes = "all_clothes".equals(currentCategoryId);
        
        // Show empty state only if "All Clothes" is empty.
        // For specific categories, the adapter will show the "+" card even if empty.
        if (itemList.isEmpty() && isAllClothes) {
            rvItems.setVisibility(View.GONE);
            llEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvItems.setVisibility(View.VISIBLE);
            llEmptyState.setVisibility(View.GONE);
        }

        itemAdapter.updateList(new ArrayList<>(itemList));
        itemAdapter.setSelectedItems(selectedItems);
        applyFilters();
    }

    private void updatePreview() {
        previewList.clear();
        previewList.addAll(selectedItems);
        previewAdapter.updateList(new ArrayList<>(previewList));

        // Show entire container if there is at least 1 item
        if (previewList.size() >= 1) {
            llPreviewContainer.setVisibility(View.VISIBLE);

            // Show toggle button ONLY if there are 2 or more items
            if (previewList.size() >= 2) {
                btnTogglePreview.setVisibility(View.VISIBLE);
                float rotation = previewAdapter.isCollapsed() ? 0 : 180;
                ivPreviewToggleIcon.setRotation(rotation);
            } else {
                btnTogglePreview.setVisibility(View.GONE);
                // If only 1 item remains, ensure it's expanded
                previewAdapter.setCollapsed(false);
            }
        } else {
            // Hide container if no items are selected
            llPreviewContainer.setVisibility(View.GONE);
            llPreviewContainer.setTranslationX(0);
            ivPreviewToggleIcon.setRotation(0);
            previewAdapter.setCollapsed(false);
            btnTogglePreview.setVisibility(View.GONE);
        }
    }

    private void showImagePreview(ClothingItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_image_preview, null);
        builder.setView(dialogView);

        ImageView ivFull = dialogView.findViewById(R.id.iv_preview_full);
        Glide.with(this)
                .load(item.getImageUrl())
                .placeholder(R.drawable.box_background)
                .into(ivFull);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
    }

    private void toggleAvatars() {
        TransitionManager.beginDelayedTransition(llAvatars, new AutoTransition());
        isExpanded = !isExpanded;
        
        // Toggle btnAddAvatar and any other children except the main avatar
        for (int i = 0; i < llAvatars.getChildCount(); i++) {
            View child = llAvatars.getChildAt(i);
            if (child.getId() != R.id.avatar_main) {
                child.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            }
        }
    }

    private void showAddAvatarOptions() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_new_model, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialogView.findViewById(R.id.btn_camera).setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.btn_gallery).setOnClickListener(v -> {
            pickImageLauncher.launch("image/*");
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void openCamera() {
        try {
            File photoFile = createImageFile();
            currentPhotoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
            takePhotoLauncher.launch(currentPhotoUri);
        } catch (IOException e) {
            Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void handleImageSelection(Uri uri) {
        if (uri != null) {
            Toast.makeText(this, "Scanning for full body...", Toast.LENGTH_SHORT).show();
            validateFullBody(uri, isFullBody -> {
                if (isFullBody) {
                    // Add to the list of selectable avatars
                    addAvatarToList(uri);
                    Toast.makeText(this, "New model image added!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Error: Please select a full body image (showing from head to toe).", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void validateFullBody(Uri uri, Consumer<Boolean> callback) {
        try {
            InputImage image = InputImage.fromFilePath(this, uri);
            poseDetector.process(image)
                    .addOnSuccessListener(pose -> {
                        boolean isFullBody = checkPoseForFullBody(pose);
                        callback.accept(isFullBody);
                    })
                    .addOnFailureListener(e -> {
                        callback.accept(false);
                    });
        } catch (IOException e) {
            e.printStackTrace();
            callback.accept(false);
        }
    }

    private boolean checkPoseForFullBody(Pose pose) {
        // Required landmarks for a full body
        int[] requiredLandmarks = {
                PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER,
                PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP,
                PoseLandmark.LEFT_KNEE, PoseLandmark.RIGHT_KNEE,
                PoseLandmark.LEFT_ANKLE, PoseLandmark.RIGHT_ANKLE
        };

        for (int landmarkType : requiredLandmarks) {
            PoseLandmark landmark = pose.getPoseLandmark(landmarkType);
            // Check if the landmark is present and has reasonable likelihood (in frame)
            if (landmark == null || landmark.getInFrameLikelihood() < 0.5f) {
                return false;
            }
        }
        return true;
    }

    private void addAvatarToList(Uri uri) {
        ShapeableImageView newAvatar = new ShapeableImageView(this);
        int size = (int) (40 * getResources().getDisplayMetrics().density);
        int margin = (int) (8 * getResources().getDisplayMetrics().density);
        int padding = (int) (2 * getResources().getDisplayMetrics().density);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.setMarginStart(margin);
        newAvatar.setLayoutParams(params);

        newAvatar.setPadding(padding, padding, padding, padding);
        newAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        newAvatar.setStrokeColor(ColorStateList.valueOf(Color.WHITE));
        newAvatar.setStrokeWidth(2 * getResources().getDisplayMetrics().density);
        newAvatar.setShapeAppearanceModel(ShapeAppearanceModel.builder()
                .setAllCornerSizes(new RelativeCornerSize(0.5f))
                .build());

        Glide.with(this).load(uri).into(newAvatar);

        newAvatar.setOnClickListener(v -> Glide.with(this).load(uri).into(ivMainModel));

        // Insert at index 1 (between Plus button and Main avatar)
        // Order: [Plus Button] [Added Avatars...] [Main Avatar]
        llAvatars.addView(newAvatar, 1);

        // Ensure it follows current expansion state
        newAvatar.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
    }

    private void setupSwipeNavigation() {
        GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                // Only allow category swipe when bottom sheet is expanded (vertical grid)
                if (bottomSheetBehavior == null || bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                    return false;
                }

                if (Math.abs(velocityX) > Math.abs(velocityY)) {
                    float diffX = e2.getX() - e1.getX();
                    if (Math.abs(diffX) > 150 && Math.abs(velocityX) > 150) {
                        navigateToCategory(diffX <= 0);
                        return true;
                    }
                }
                return false;
            }
        });

        rvItems.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                gestureDetector.onTouchEvent(e);
                return false;
            }

            @Override
            public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {}

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {}
        });
    }

    private void navigateToCategory(boolean next) {
        if (categoryAdapter == null || categoryList == null || categoryList.isEmpty()) return;

        int currentPos = categoryAdapter.getSelectedPosition();
        int newPos;
        if (next) {
            newPos = currentPos + 1;
            if (newPos >= categoryList.size()) return;
        } else {
            newPos = currentPos - 1;
            if (newPos < 0) return;
        }

        // Animate out
        float slideDist = 300f;
        rvItems.animate()
                .translationX(next ? -slideDist : slideDist)
                .alpha(0f)
                .setDuration(150)
                .withEndAction(() -> {
                    categoryAdapter.setSelectedPosition(newPos);
                    rvCategories.smoothScrollToPosition(newPos);

                    ViewCategoriesActivity.CategoryModel category = categoryList.get(newPos);
                    currentCategoryName = category.name;
                    currentCategoryId = category.id;
                    if (itemAdapter != null) {
                        itemAdapter.setCurrentCategory(currentCategoryId, currentCategoryName);
                    }
                    loadItems(currentCategoryId);

                    // Reset position to opposite side for "slide in" effect
                    rvItems.setTranslationX(next ? slideDist : -slideDist);
                    rvItems.animate()
                            .translationX(0)
                            .alpha(1f)
                            .setDuration(200)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator())
                            .start();
                })
                .start();
    }

    public void onButtonClicked(View view) {
        Intent intent = null;
        int viewId = view.getId();
        if (viewId == R.id.home_menu) intent = new Intent(this, D_FeedActivity.class);
        else if (viewId == R.id.calendar_menu) intent = new Intent(this, E_CalendarActivity.class);
        else if (viewId == R.id.camera_menu) intent = new Intent(this, F1_CameraActivity.class);
        else if (viewId == R.id.closet_menu) intent = new Intent(this, G1_ClosetActivity.class);
        else if (viewId == R.id.profile_menu) intent = new Intent(this, I_ProfileActivity.class);
        else if (viewId == R.id.wardrobe_menu) intent = new Intent(this, WardrobeActivity.class);
        else if (viewId == R.id.ai_menu) intent = new Intent(this, AiActivity.class);
        if (intent != null) { startActivity(intent); finish(); }
    }
}

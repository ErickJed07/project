package com.example.project;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.util.Log;
import android.util.TypedValue;
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

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseLandmark;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import java.util.HashMap;
import java.util.Map;

public class AiActivity extends AppCompatActivity {

    private ViewGroup llAvatars, llNoModelAvatars;
    private View avatarMain, btnAddAvatar, cvAvatarContainer;
    private ImageView ivMainModel;
    private Uri currentPhotoUri;

    private RecyclerView rvCategories, rvItems, rvSelectedPreview;
    private View llEmptyState, llPreviewContainer, btnTogglePreview, cvNoModelMessage, cvPreviewWrapper, btnClearAll;
    private TextView tvItemCount;
    private ImageView ivPreviewToggleIcon;
    private CircularProgressIndicator pbLoading;
    private AiCategoryAdapter categoryAdapter;
    private AiItemAdapter itemAdapter;
    private AiPreviewAdapter previewAdapter;

    private List<ViewCategoriesActivity.CategoryModel> categoryList = new ArrayList<>();
    private List<ClothingItem> itemList = new ArrayList<>();
    private List<ClothingItem> originalItemList = new ArrayList<>();
    private Set<ClothingItem> selectedItems = new LinkedHashSet<>();
    private List<ClothingItem> previewList = new ArrayList<>();

    private String selectedModelUrl = null;
    private Uri selectedModelUri = null;
    private static final String FAL_KEY = "14732117-ce26-436f-ac91-bb9d5d311539:244d86639b02ef3ed0d782990dc1a681";

    // Chain Generation State
    private List<ClothingItem> chainQueue = new ArrayList<>();
    private int currentChainIndex = 0;
    private String currentIntermediateModelUrl = null;

    private String selectedSeason = "All";
    private String selectedOccasion = "All";
    private String selectedColor = "All";
    private String selectedSort = "Latest";
    private String currentCategoryName = "";
    private String currentCategoryId = "all_clothes";
    private boolean isWomanSelected = true;

    private BottomSheetBehavior<View> bottomSheetBehavior;
    private int bottomInset = 0;

    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;

    private PoseDetector poseDetector;
    private FaceDetector faceDetector;
    private OkHttpClient httpClient;

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

        // Initialize HTTP Client with longer timeouts for AI generation
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .build();

        PoseDetectorOptions options = new PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.SINGLE_IMAGE_MODE)
                .build();
        poseDetector = PoseDetection.getClient(options);

        FaceDetectorOptions faceOptions = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .build();
        faceDetector = FaceDetection.getClient(faceOptions);

        llAvatars = findViewById(R.id.ll_avatars);
        avatarMain = findViewById(R.id.avatar_main);
        btnAddAvatar = findViewById(R.id.btn_add_avatar);
        cvAvatarContainer = findViewById(R.id.cv_avatar_container);
        ivMainModel = findViewById(R.id.iv_main_model);
        llEmptyState = findViewById(R.id.ll_empty_state);
        llPreviewContainer = findViewById(R.id.ll_preview_container);
        cvPreviewWrapper = findViewById(R.id.cv_preview_wrapper);
        tvItemCount = findViewById(R.id.tv_item_count);
        btnTogglePreview = findViewById(R.id.btn_toggle_preview);
        btnClearAll = findViewById(R.id.btn_clear_all);
        ivPreviewToggleIcon = findViewById(R.id.iv_preview_toggle_icon);
        cvNoModelMessage = findViewById(R.id.cv_no_model_message);
        llNoModelAvatars = findViewById(R.id.ll_no_model_avatars);
        pbLoading = findViewById(R.id.pb_loading);

        // Initialize Cloudinary
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("cloud_name", BuildConfig.CLOUDINARY_CLOUD_NAME);
            config.put("api_key", BuildConfig.CLOUDINARY_API_KEY);
            config.put("api_secret", BuildConfig.CLOUDINARY_API_SECRET);
            MediaManager.init(this, config);
        } catch (IllegalStateException e) {
            // Already initialized
        }

        // Initially show the message and hide the default model
        setNoModelVisible(true);
        ivMainModel.setImageDrawable(null);
        btnAddAvatar.setVisibility(View.GONE); // Default state
        btnAddAvatar.setAlpha(0f);

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

        avatarMain.setOnClickListener(v -> {
            selectMainAvatar();
        });
        btnAddAvatar.setOnClickListener(v -> showAddAvatarOptions());

        findViewById(R.id.btn_filter).setOnClickListener(v -> showFilterBottomSheet());

        View.OnClickListener generateListener = v -> {
            performVirtualTryOn();
        };
        findViewById(R.id.btn_generate_collapsed).setOnClickListener(generateListener);
        findViewById(R.id.btn_generate_expanded).setOnClickListener(generateListener);

        setupRecyclerViews();
        setupBottomSheet();
        loadCategories();
        updatePreview();
        loadUserModels();
    }

    private void loadUserModels() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        dbRef.child(uid).child("ai_models").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Clear existing dynamic avatars in both containers
                int childCount = llAvatars.getChildCount();
                for (int i = childCount - 2; i >= 1; i--) {
                    llAvatars.removeViewAt(i);
                }
                if (llNoModelAvatars != null) {
                    llNoModelAvatars.removeAllViews();
                    addPlusButtonToNoModel();
                }

                boolean hasCustomModels = snapshot.hasChildren();
                
                // Top-right container logic
                btnAddAvatar.setVisibility(View.VISIBLE);
                btnAddAvatar.setAlpha(1.0f);

                // Center "No Model" card container logic
                if (!hasCustomModels) {
                    addDefaultAvatarToNoModel();
                }

                for (DataSnapshot modelSnapshot : snapshot.getChildren()) {
                    AiModel model = modelSnapshot.getValue(AiModel.class);
                    if (model != null && model.url != null) {
                        addModelToAvatarList(model);
                    }
                }

                // Ensure everything in llAvatars is visible, but handle default avatar visibility
                for (int i = 0; i < llAvatars.getChildCount(); i++) {
                    View child = llAvatars.getChildAt(i);
                    if (child == avatarMain) {
                        child.setVisibility(hasCustomModels ? View.GONE : View.VISIBLE);
                    } else {
                        child.setVisibility(View.VISIBLE);
                    }
                    child.setAlpha(1.0f);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("AiActivity", "Failed to load models: " + error.getMessage());
            }
        });
    }

    private void addPlusButtonToNoModel() {
        if (llNoModelAvatars == null) return;
        
        com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(this);
        int size = (int) (44 * getResources().getDisplayMetrics().density);
        int margin = (int) (8 * getResources().getDisplayMetrics().density);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.setMarginEnd(margin);
        card.setLayoutParams(params);
        
        card.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#0D000000")));
        card.setRadius(22 * getResources().getDisplayMetrics().density);
        card.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#1A000000")));
        card.setStrokeWidth((int) (1 * getResources().getDisplayMetrics().density));
        card.setCardElevation(0);
        
        ImageView iv = new ImageView(this);
        int iconSize = (int) (20 * getResources().getDisplayMetrics().density);
        android.widget.FrameLayout.LayoutParams ivParams = new android.widget.FrameLayout.LayoutParams(iconSize, iconSize, android.view.Gravity.CENTER);
        iv.setLayoutParams(ivParams);
        iv.setImageResource(R.drawable.plus);
        iv.setImageTintList(ColorStateList.valueOf(Color.BLACK));
        
        card.addView(iv);
        card.setOnClickListener(v -> showAddAvatarOptions());
        
        llNoModelAvatars.addView(card);
    }

    private void addDefaultAvatarToNoModel() {
        if (llNoModelAvatars == null) return;
        ShapeableImageView defaultAvatar = createAvatarView();
        defaultAvatar.setImageResource(R.drawable.user_2);
        defaultAvatar.setOnClickListener(v -> selectMainAvatar());
        llNoModelAvatars.addView(defaultAvatar);
    }

    private void selectMainAvatar() {
        ivMainModel.setImageResource(R.drawable.user_2);
        selectedModelUrl = "https://idm-vton.github.io/inthewild/4/h/0.jpeg"; // Default public model URL
        selectedModelUri = null;
        
        setNoModelVisible(false);
        updateBottomSheetLockedState();

        resetAvatarBorders();
        highlightAvatarInContainer(llAvatars, avatarMain);
        highlightAvatarInContainer(llNoModelAvatars, null); // Highlight by drawable if needed, but simple for now
    }

    private void highlightAvatarInContainer(ViewGroup container, View selected) {
        if (container == null) return;
        for (int i = 0; i < container.getChildCount(); i++) {
            View v = container.getChildAt(i);
            if (v instanceof ShapeableImageView) {
                float width = (v == selected) ? 2 : 0;
                ((ShapeableImageView) v).setStrokeWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, width, getResources().getDisplayMetrics()));
            }
        }
    }

    private void addModelToAvatarList(AiModel model) {
        // Add to top-right container
        ShapeableImageView topAvatar = createAvatarView();
        Glide.with(this).load(model.url).into(topAvatar);
        topAvatar.setOnClickListener(v -> selectModel(model, topAvatar));
        topAvatar.setOnLongClickListener(v -> {
            showDeleteModelDialog(model);
            return true;
        });
        llAvatars.addView(topAvatar, 1);
        topAvatar.setVisibility(View.VISIBLE);
        topAvatar.setAlpha(1.0f);

        // Add to center "No Model" card container
        if (llNoModelAvatars != null) {
            ShapeableImageView cardAvatar = createAvatarView();
            Glide.with(this).load(model.url).into(cardAvatar);
            cardAvatar.setOnClickListener(v -> selectModel(model, topAvatar)); // Using topAvatar for highlighting ref
            llNoModelAvatars.addView(cardAvatar);
        }
    }

    private void selectModel(AiModel model, ShapeableImageView avatarView) {
        Glide.with(this).load(model.url).into(ivMainModel);
        selectedModelUrl = model.url;
        selectedModelUri = null;
        
        setNoModelVisible(false);
        updateBottomSheetLockedState();

        resetAvatarBorders();
        if (avatarView != null) {
            avatarView.setStrokeWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics()));
        }
    }

    private void resetAvatarBorders() {
        for (int i = 0; i < llAvatars.getChildCount(); i++) {
            View v = llAvatars.getChildAt(i);
            if (v instanceof ShapeableImageView) {
                ((ShapeableImageView) v).setStrokeWidth(0);
            }
        }
    }

    private ShapeableImageView createAvatarView() {
        ShapeableImageView newAvatar = new ShapeableImageView(this);
        int size = (int) (52 * getResources().getDisplayMetrics().density);
        int margin = (int) (8 * getResources().getDisplayMetrics().density);
        int padding = (int) (4 * getResources().getDisplayMetrics().density);

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
        return newAvatar;
    }

    private void showDeleteModelDialog(AiModel model) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Model")
                .setMessage("Are you sure you want to remove this model?")
                .setPositiveButton("Delete", (dialog, which) -> deleteModel(model))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteModel(AiModel model) {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        dbRef.child(uid).child("ai_models").child(model.id).removeValue()
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Model removed", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to remove model", Toast.LENGTH_SHORT).show());
    }

    private void setupBottomSheet() {
        View bottomSheet = findViewById(R.id.bottom_sheet_card);
        View grabber = findViewById(R.id.bottom_sheet_grabber);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        // Disable manual dragging
        bottomSheetBehavior.setDraggable(false);

        // Add click listener to grabber for toggling
        grabber.setOnClickListener(v -> {
            if (selectedModelUrl == null && selectedModelUri == null) {
                Toast.makeText(this, "Please select an avatar first", Toast.LENGTH_SHORT).show();
                return;
            }
            if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            } else {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });

        // Ensure llAvatars is visible
        llAvatars.setVisibility(View.VISIBLE);

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

            // Set initial state to collapsed
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            grabber.setRotation(0); // Pointing up when collapsed
            
            // Set initial disclaimer visibility
            findViewById(R.id.tv_ai_disclaimer_header).setVisibility(View.VISIBLE);
            findViewById(R.id.tv_ai_disclaimer_header).setAlpha(1.0f);
            findViewById(R.id.tv_ai_disclaimer).setVisibility(View.GONE);
            findViewById(R.id.tv_ai_disclaimer).setAlpha(0.0f);

            updateMainModelHeight(bottomSheetBehavior.getPeekHeight());
            
            // Initial lock state
            updateBottomSheetLockedState();
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
                View tvDisclaimerHeader = findViewById(R.id.tv_ai_disclaimer_header);
                View tvDisclaimerBottom = findViewById(R.id.tv_ai_disclaimer);

                // Rotate grabber icon (0 = up, 180 = down)
                grabber.setRotation(180 * slideOffset);

                // Transition alpha based on slideOffset (0.0 = collapsed, 1.0 = expanded)
                btnFilter.setAlpha(slideOffset);
                btnGenerateCollapsed.setAlpha(1.0f - slideOffset);
                btnGenerateExpanded.setAlpha(slideOffset);
                
                // Transition disclaimers
                tvDisclaimerHeader.setAlpha(1.0f - slideOffset);
                tvDisclaimerBottom.setAlpha(slideOffset);

                // Update visibility to prevent clicks when hidden
                if (slideOffset <= 0.05f) {
                    btnFilter.setVisibility(View.GONE);
                    btnGenerateExpanded.setVisibility(View.GONE);
                    btnGenerateCollapsed.setVisibility(View.VISIBLE);
                    tvDisclaimerHeader.setVisibility(View.VISIBLE);
                    tvDisclaimerBottom.setVisibility(View.GONE);
                } else if (slideOffset >= 0.95f) {
                    btnFilter.setVisibility(View.VISIBLE);
                    btnGenerateExpanded.setVisibility(View.VISIBLE);
                    btnGenerateCollapsed.setVisibility(View.GONE);
                    tvDisclaimerHeader.setVisibility(View.GONE);
                    tvDisclaimerBottom.setVisibility(View.VISIBLE);
                } else {
                    btnFilter.setVisibility(View.VISIBLE);
                    btnGenerateExpanded.setVisibility(View.VISIBLE);
                    btnGenerateCollapsed.setVisibility(View.VISIBLE);
                    tvDisclaimerHeader.setVisibility(View.VISIBLE);
                    tvDisclaimerBottom.setVisibility(View.VISIBLE);
                }

                // Update preview container position during slide
                int currentHeight = (int) (bottomSheetBehavior.getPeekHeight() +
                    (bottomSheet.getHeight() - bottomSheetBehavior.getPeekHeight()) * slideOffset);
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

        RecyclerView rvColors = view.findViewById(R.id.rv_colors);
        List<String> colors = Arrays.asList("All", "Black", "White", "Grey", "Beige", "Red", "Blue", "Green", "Yellow", "Orange", "Purple", "Pink", "Brown", "Multi");
        AiFilterChipAdapter colorAdapter = new AiFilterChipAdapter(this, colors, (item, pos) -> selectedColor = item);
        colorAdapter.setSelected(selectedColor);
        rvColors.setAdapter(colorAdapter);

        RecyclerView rvSort = view.findViewById(R.id.rv_sort_options);
        List<String> sorts = Arrays.asList("Latest", "Faves", "Oldest");
        AiFilterChipAdapter sortOptionAdapter = new AiFilterChipAdapter(this, sorts, (item, pos) -> selectedSort = item);
        sortOptionAdapter.setSelected(selectedSort);
        rvSort.setAdapter(sortOptionAdapter);

        view.findViewById(R.id.btn_reset).setOnClickListener(v -> {
            seasonAdapter.reset();
            occasionAdapter.reset();
            colorAdapter.reset();
            sortOptionAdapter.reset();
            selectedSeason = "All";
            selectedOccasion = "All";
            selectedColor = "All";
            selectedSort = "Latest";
            sortOptionAdapter.setSelected(selectedSort);
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
            boolean matchesColor = selectedColor.equals("All") || (item.getColor() != null && item.getColor().equalsIgnoreCase(selectedColor));
            boolean matchesFave = !selectedSort.equals("Faves") || item.isFavorite();

            if (matchesSeason && matchesOccasion && matchesColor && matchesFave) {
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

        itemAdapter.updateFilters(selectedSeason, selectedOccasion, selectedColor, selectedSort);
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
                // Enforce one item per category in Activity's set too
                ClothingItem existing = null;
                for (ClothingItem s : selectedItems) {
                    if (s.getCategoryId() != null && s.getCategoryId().equals(item.getCategoryId())) {
                        existing = s;
                        break;
                    }
                }
                if (existing != null) selectedItems.remove(existing);
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
            updatePreview();
        });
        rvSelectedPreview.setAdapter(previewAdapter);

        setupSwipeNavigation();

        btnTogglePreview.setOnClickListener(v -> {
            previewAdapter.setCollapsed(!previewAdapter.isCollapsed());
            updatePreview();
        });

        btnClearAll.setOnClickListener(v -> {
            selectedItems.clear();
            itemAdapter.notifyDataSetChanged();
            updatePreview();
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

        String color = photoSnap.child("color").getValue(String.class);
        item.setColor(color);

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

        int count = previewList.size();
        if (tvItemCount != null) {
            tvItemCount.setText(String.valueOf(count));
        }

        // Show entire container if there is at least 1 item
        if (count >= 1) {
            llPreviewContainer.setVisibility(View.VISIBLE);

            // Show Clear All and Toggle button ONLY if there are 2 or more items
            if (count >= 2) {
                btnTogglePreview.setVisibility(View.VISIBLE);
                boolean isColl = previewAdapter.isCollapsed();
                float rotation = isColl ? 0 : 180;
                ivPreviewToggleIcon.setRotation(rotation);

                // Show Clear All only when NOT collapsed
                btnClearAll.setVisibility(isColl ? View.GONE : View.VISIBLE);
                btnClearAll.animate().alpha(isColl ? 0f : 1f).setDuration(200).start();

                // Show counter only when collapsed
                findViewById(R.id.cv_item_counter).setVisibility(isColl ? View.VISIBLE : View.GONE);

                if (cvPreviewWrapper instanceof com.google.android.material.card.MaterialCardView) {
                    com.google.android.material.card.MaterialCardView wrapper = (com.google.android.material.card.MaterialCardView) cvPreviewWrapper;
                    wrapper.setStrokeWidth(isColl ? 0 : (int) (1 * getResources().getDisplayMetrics().density));
                    wrapper.setCardBackgroundColor(isColl ? Color.TRANSPARENT : Color.parseColor("#26FFFFFF"));
                }
            } else {
                btnTogglePreview.setVisibility(View.GONE);
                btnClearAll.setVisibility(View.GONE);
                findViewById(R.id.cv_item_counter).setVisibility(View.GONE);
                // If only 1 item remains, ensure it's expanded
                previewAdapter.setCollapsed(false);

                if (cvPreviewWrapper instanceof com.google.android.material.card.MaterialCardView) {
                    com.google.android.material.card.MaterialCardView wrapper = (com.google.android.material.card.MaterialCardView) cvPreviewWrapper;
                    wrapper.setStrokeWidth(0);
                    wrapper.setCardBackgroundColor(Color.TRANSPARENT);
                }
            }
        } else {
            // Hide container if no items are selected
            llPreviewContainer.setVisibility(View.GONE);
            btnClearAll.setVisibility(View.GONE);
            findViewById(R.id.cv_item_counter).setVisibility(View.GONE);
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

    private void updateBottomSheetLockedState() {
        boolean isLocked = (selectedModelUrl == null && selectedModelUri == null);
        View bottomSheet = findViewById(R.id.bottom_sheet_card);
        if (bottomSheet != null) {
            // Block all clicks to children when locked, but keep original appearance
            ViewGroup content = findViewById(R.id.bottom_sheet_content);
            if (content != null) {
                enableViewAndChildren(content, !isLocked);
            }
        }
    }

    private void enableViewAndChildren(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                enableViewAndChildren(group.getChildAt(i), enabled);
            }
        }
    }

    private void setNoModelVisible(boolean visible) {
        if (cvNoModelMessage == null) return;
        
        if (visible) {
            cvNoModelMessage.setAlpha(0f);
            cvNoModelMessage.setVisibility(View.VISIBLE);
            cvNoModelMessage.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .setListener(null);
            
            if (cvAvatarContainer != null) {
                cvAvatarContainer.animate().alpha(0f).setDuration(300).withEndAction(() -> cvAvatarContainer.setVisibility(View.GONE)).start();
            }
        } else {
            cvNoModelMessage.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> cvNoModelMessage.setVisibility(View.GONE))
                    .start();

            if (cvAvatarContainer != null) {
                cvAvatarContainer.setVisibility(View.VISIBLE);
                cvAvatarContainer.animate().alpha(1f).setDuration(300).start();
            }
        }
        updateBottomSheetLockedState();
    }

    // toggleAvatars removed as per user request to remove expand/collapse behavior

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
            if (!checkImageResolution(uri)) {
                Toast.makeText(this, getString(R.string.error_low_resolution), Toast.LENGTH_LONG).show();
                return;
            }

            Toast.makeText(this, "Scanning for face...", Toast.LENGTH_SHORT).show();
            validateFace(uri, faceValid -> {
                if (faceValid) {
                    Toast.makeText(this, "Scanning for full body...", Toast.LENGTH_SHORT).show();
                    validateFullBody(uri, validationResult -> {
                        if (validationResult == null) {
                            // Upload to Cloudinary then save to Firebase
                            uploadModelToCloudinary(uri);
                        } else {
                            // SHOW THE SPECIFIC ERROR REASON HERE
                            Toast.makeText(this, validationResult, Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    Toast.makeText(this, getString(R.string.error_no_face_detected), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void uploadModelToCloudinary(Uri uri) {
        pbLoading.setVisibility(View.VISIBLE);
        MediaManager.get().upload(uri)
                .option("folder", "Models")
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override public void onSuccess(String requestId, Map resultData) {
                        String url = (String) resultData.get("secure_url");
                        saveModelToFirebase(url);
                        runOnUiThread(() -> {
                            Glide.with(AiActivity.this).load(url).into(ivMainModel);
                            setNoModelVisible(false);
                            selectedModelUrl = url;
                            selectedModelUri = null;
                        });
                    }
                    @Override public void onError(String requestId, ErrorInfo error) {
                        runOnUiThread(() -> {
                            pbLoading.setVisibility(View.GONE);
                            Toast.makeText(AiActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
                        });
                    }
                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    private void saveModelToFirebase(String url) {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        String id = dbRef.child(uid).child("ai_models").push().getKey();
        if (id == null) return;
        AiModel model = new AiModel(id, url);

        dbRef.child(uid).child("ai_models").child(id).setValue(model)
                .addOnSuccessListener(aVoid -> runOnUiThread(() -> {
                    pbLoading.setVisibility(View.GONE);
                    Toast.makeText(this, "Model saved!", Toast.LENGTH_SHORT).show();
                }))
                .addOnFailureListener(e -> runOnUiThread(() -> {
                    pbLoading.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to save model", Toast.LENGTH_SHORT).show();
                }));
    }

    private boolean checkImageResolution(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            if (inputStream != null) inputStream.close();

            int width = options.outWidth;
            int height = options.outHeight;

            // Minimum 720p (1280x720 or 720x1280)
            return (width >= 720 && height >= 1280) || (width >= 1280 && height >= 720);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void validateFace(Uri uri, Consumer<Boolean> callback) {
        try {
            InputImage image = InputImage.fromFilePath(this, uri);
            faceDetector.process(image)
                    .addOnSuccessListener(faces -> {
                        boolean hasFace = !faces.isEmpty();
                        if (!hasFace) {
                            Log.d("AiActivity", "Face detection failed: No faces found");
                        }
                        callback.accept(hasFace);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("AiActivity", "Face detection process error", e);
                        callback.accept(false);
                    });
        } catch (IOException e) {
            Log.e("AiActivity", "Face detection file error", e);
            callback.accept(false);
        }
    }

    private void validateFullBody(Uri uri, Consumer<String> callback) {
        try {
            InputImage image = InputImage.fromFilePath(this, uri);
            poseDetector.process(image)
                    .addOnSuccessListener(pose -> {
                        if (checkPoseForFullBody(pose)) {
                            // Local pose check passed, accepted immediately (GPT validation removed)
                            callback.accept(null);
                        } else {
                            Log.d("AiActivity", "Pose detection failed: Not full body");
                            callback.accept(getString(R.string.error_not_full_body));
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("AiActivity", "Pose detection process error", e);
                        callback.accept(getString(R.string.error_obscured_joints));
                    });
        } catch (IOException e) {
            Log.e("AiActivity", "Pose detection file error", e);
            callback.accept("Error reading image: " + e.getMessage());
        }
    }


    private boolean checkPoseForFullBody(Pose pose) {
        // Required landmarks for a full body (head to toe)
        int[] requiredLandmarks = {
                PoseLandmark.NOSE,
                PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER,
                PoseLandmark.LEFT_ELBOW, PoseLandmark.RIGHT_ELBOW,
                PoseLandmark.LEFT_WRIST, PoseLandmark.RIGHT_WRIST,
                PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP,
                PoseLandmark.LEFT_KNEE, PoseLandmark.RIGHT_KNEE,
                PoseLandmark.LEFT_ANKLE, PoseLandmark.RIGHT_ANKLE
        };

        for (int landmarkType : requiredLandmarks) {
            PoseLandmark landmark = pose.getPoseLandmark(landmarkType);
            // Stricter check: Landmark must be present and high likelihood
            if (landmark == null || landmark.getInFrameLikelihood() < 0.7f) {
                return false;
            }
        }
        return true;
    }

    private void addAvatarToList(Uri uri) {
        // Method replaced by Cloudinary+Firebase flow
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

    private void performVirtualTryOn() {
        if (selectedItems.isEmpty()) {
            Toast.makeText(this, "Please select at least one garment", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedModelUrl == null && selectedModelUri == null) {
            Toast.makeText(this, "Please select a model image", Toast.LENGTH_SHORT).show();
            return;
        }

        pbLoading.setVisibility(View.VISIBLE);
        findViewById(R.id.btn_generate_collapsed).setEnabled(false);
        findViewById(R.id.btn_generate_expanded).setEnabled(false);

        // Initialize chain queue
        chainQueue.clear();
        chainQueue.addAll(selectedItems);
        currentChainIndex = 0;

        Log.d("AiActivity", "Starting Chain Virtual Try-On with " + chainQueue.size() + " items");

        if (selectedModelUrl != null) {
            startNextInChain(selectedModelUrl);
        } else {
            // Upload local URI to Cloudinary first
            Log.d("AiActivity", "Uploading local model URI to Cloudinary: " + selectedModelUri);
            MediaManager.get().upload(selectedModelUri)
                    .option("folder", "Models")
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {
                            Log.d("AiActivity", "Cloudinary upload started: " + requestId);
                        }
                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {}
                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            String modelUrl = (String) resultData.get("secure_url");
                            Log.d("AiActivity", "Cloudinary upload success! URL: " + modelUrl);
                            runOnUiThread(() -> startNextInChain(modelUrl));
                        }
                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            Log.e("AiActivity", "Cloudinary upload failed: " + error.getDescription());
                            runOnUiThread(() -> {
                                pbLoading.setVisibility(View.GONE);
                                findViewById(R.id.btn_generate_collapsed).setEnabled(true);
                                findViewById(R.id.btn_generate_expanded).setEnabled(true);
                                Toast.makeText(AiActivity.this, "Upload failed: " + error.getDescription(), Toast.LENGTH_LONG).show();
                            });
                        }
                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {}
                    }).dispatch();
        }
    }

    private void startNextInChain(String modelUrl) {
        if (currentChainIndex >= chainQueue.size()) {
            Log.d("AiActivity", "Chain generation completed.");
            runOnUiThread(() -> {
                pbLoading.setVisibility(View.GONE);
                findViewById(R.id.btn_generate_collapsed).setEnabled(true);
                findViewById(R.id.btn_generate_expanded).setEnabled(true);
            });
            return;
        }

        currentIntermediateModelUrl = modelUrl;
        ClothingItem garment = chainQueue.get(currentChainIndex);
        String garmentUrl = garment.getImageUrl();
        String categoryId = garment.getCategoryId();

        Log.d("AiActivity", "Processing item " + (currentChainIndex + 1) + "/" + chainQueue.size() + ": " + categoryId);
        processWithModelUrl(modelUrl, garmentUrl, categoryId);
    }

    /**
     * Process the Try-On/Edit request once the Model URL is secured (either from DB or Cloudinary).
     * This method implements the "Strict Rulebook" for different Fal.ai endpoints.
     */
    private void processWithModelUrl(String modelUrl, String garmentUrl, String categoryId) {
        String endpoint;
        JSONObject json = new JSONObject();
        
        try {
            if (isEditApiCategory(categoryId)) {
                // RULEBOOK FOR ACCESSORIES & FOOTWEAR (Generalist AI: banana-pro/edit)
                endpoint = "https://queue.fal.run/fal-ai/nano-banana-pro/edit";
                
                // 1. ANATOMICAL PLACEMENT & PIXEL FIDELITY PROMPT
                json.put("prompt", getEditPrompt(categoryId));
                
                // 2. REFERENCE FUSION
                JSONArray images = new JSONArray();
                images.put(modelUrl);   // Image 1: The Person (Avatar)
                images.put(garmentUrl); // Image 2: The Item (Clothes/Accessory)
                json.put("image_urls", images);
                
                // 3. QUALITY & DIMENSION LOCKS
                json.put("resolution", "1K");      // Match standard mobile photo pixel density
                json.put("aspect_ratio", "1:1");   // STOP widening; preserve original framing
                json.put("output_format", "png");  // High-fidelity lossless output
                
            } else {
                // RULEBOOK FOR APPAREL (Specialist AI: fashn/tryon)
                endpoint = "https://queue.fal.run/fal-ai/fashn/tryon/v1.6";
                
                String falCategory = mapCategory(categoryId);
                json.put("model_image", modelUrl);
                json.put("garment_image", garmentUrl);
                json.put("category", falCategory);

                // Quality & Layering Improvements
                json.put("mode", "quality");
                json.put("garment_photo_type", "auto");
                json.put("nsfw_filter", true);

                // Styling Hint for Outerwear (to see the shirt inside)
                if (isOuterwear(categoryId)) {
                    json.put("prompt", "open jacket over the shirt");
                }

                // Support for long items (Long Coats, Dresses)
                if (isLongGarment(categoryId)) {
                    json.put("long_top", true);
                }
            }
            
            submitFalAiJob(endpoint, json);
            
        } catch (JSONException e) {
            Log.e("AiActivity", "Rulebook JSON Error", e);
            handleAiFailure("JSON Error: " + e.getMessage());
        }
    }

    private boolean isOuterwear(String categoryId) {
        return "Outerwear".equalsIgnoreCase(categoryId);
    }

    private boolean isLongGarment(String categoryId) {
        return "Outerwear".equalsIgnoreCase(categoryId) || "Dresses".equalsIgnoreCase(categoryId);
    }

    /**
     * Category Identifier: Routes items to the correct AI engine.
     */
    private boolean isEditApiCategory(String categoryId) {
        if (categoryId == null) return false;
        switch (categoryId) {
            case "Socks & Tights":
            case "Footwear":
            case "Headwear":
            case "Eyewear":
            case "Handwear":
            case "Jewelry":
            case "Watches":
            case "Bags":
            case "Neckwear":
                return true;
            default:
                return false;
        }
    }

    /**
     * The AI Instruction Set: Simple instructions for natural blending 
     * while protecting the face, pose, and frame.
     */
    private String getEditPrompt(String categoryId) {
        String item = categoryId.toLowerCase();
        if (item.equals("socks & tights")) item = "socks";
        if (item.equals("bags")) item = "bag";
        if (item.contains("headwear")) item = "headwear";

        return "Add the " + item + " from the second image onto the person in the first image naturally. " +
               "DO NOT change the face, DO NOT change the pose, and DO NOT widen the image.";
    }

    private String mapCategory(String categoryId) {
        if (categoryId == null) return "auto";
        switch (categoryId) {
            case "Tops":
            case "Outerwear":
                return "tops";
            case "Bottoms":
                return "bottoms";
            case "Dresses":
            case "Swimwear":
                return "one-pieces";
            case "Belts":
                return "waistwear";
            default:
                return "auto";
        }
    }

    private void submitFalAiJob(String endpoint, JSONObject json) {
        String requestJson = json.toString();
        Log.d("AiActivity", "Fal.ai Queue Submit JSON to " + endpoint + ": " + requestJson);

        RequestBody body = RequestBody.create(requestJson, MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(endpoint)
                .addHeader("Authorization", "Key " + FAL_KEY)
                .post(body)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                handleAiFailure(e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (Response r = response) {
                    String responseBody = r.body() != null ? r.body().string() : "";
                    Log.d("AiActivity", "Fal.ai Submit Response: " + responseBody);

                    if (!r.isSuccessful()) {
                        handleAiFailure("Submit Error: " + r.code() + " " + r.message() + " - " + responseBody);
                        return;
                    }
                    
                    JSONObject result = new JSONObject(responseBody);
                    String requestId = result.getString("request_id");
                    
                    // Prioritize URLs provided by the API, fallback to manual construction
                    String statusUrl = result.optString("status_url", endpoint + "/requests/" + requestId + "/status");
                    String responseUrl = result.optString("response_url", endpoint + "/requests/" + requestId);
                    
                    Log.d("AiActivity", "Job submitted! Request ID: " + requestId);
                    Log.d("AiActivity", "Using statusUrl: " + statusUrl);
                    Log.d("AiActivity", "Using responseUrl: " + responseUrl);

                    pollFalAiStatus(statusUrl, responseUrl);
                } catch (Exception e) {
                    handleAiFailure("Submit parsing error: " + e.getMessage());
                }
            }
        });
    }


    private void pollFalAiStatus(String statusUrl, String responseUrl) {
        Log.d("AiActivity", "Polling status: " + statusUrl);
        Request request = new Request.Builder()
                .url(statusUrl)
                .addHeader("Authorization", "Key " + FAL_KEY)
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                handleAiFailure(e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (Response r = response) {
                    if (!r.isSuccessful()) {
                        handleAiFailure("Status Error: " + r.code());
                        return;
                    }
                    String responseBody = r.body().string();
                    JSONObject status = new JSONObject(responseBody);
                    String statusStr = status.getString("status");

                    if ("COMPLETED".equals(statusStr)) {
                        fetchFalAiResult(responseUrl);
                    } else if ("IN_QUEUE".equals(statusStr) || "IN_PROGRESS".equals(statusStr)) {
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> pollFalAiStatus(statusUrl, responseUrl), 2000);
                    } else {
                        handleAiFailure("Unknown status: " + statusStr);
                    }
                } catch (Exception e) {
                    handleAiFailure("Status parsing error");
                }
            }
        });
    }

    private void fetchFalAiResult(String responseUrl) {
        Log.d("AiActivity", "Fetching result: " + responseUrl);
        Request request = new Request.Builder()
                .url(responseUrl)
                .addHeader("Authorization", "Key " + FAL_KEY)
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                handleAiFailure(e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (Response r = response) {
                    String responseBody = r.body() != null ? r.body().string() : "";
                    Log.d("AiActivity", "Fal.ai Result Response: " + responseBody);

                    if (!r.isSuccessful()) {
                        handleAiFailure("Result Error: " + r.code() + " - " + responseBody);
                        return;
                    }
                    
                    JSONObject result = new JSONObject(responseBody);
                    JSONArray images = result.getJSONArray("images");
                    String resultUrl = images.getJSONObject(0).getString("url");
                    Log.d("AiActivity", "Success! Result Image URL: " + resultUrl);

                    runOnUiThread(() -> {
                        Glide.with(AiActivity.this).load(resultUrl).into(ivMainModel);
                        setNoModelVisible(false);
                        
                        // Increment and proceed to next item in chain
                        currentChainIndex++;
                        startNextInChain(resultUrl);
                    });
                } catch (Exception e) {
                    handleAiFailure("Result parsing error: " + e.getMessage());
                }
            }
        });
    }

    private void handleAiFailure(String message) {
        Log.e("AiActivity", "AI Error: " + message);
        runOnUiThread(() -> {
            pbLoading.setVisibility(View.GONE);
            findViewById(R.id.btn_generate_collapsed).setEnabled(true);
            findViewById(R.id.btn_generate_expanded).setEnabled(true);
            Toast.makeText(AiActivity.this, "AI failed: " + message, Toast.LENGTH_SHORT).show();
        });
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

package com.example.project;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class H6_RecommendationActivity extends AppCompatActivity {

    private FrameLayout canvasContainer;

    // Replaces btnShuffle
    private Button btnGenerate;

    // --- NEW BUBBLE MENU VARIABLES ---
    private FloatingActionButton fabMenu, fabHistory, fabAddBox, fabPremade;
    private boolean isFabMenuOpen = false;
    // ----------------------------------

    // Results variables
    private FrameLayout resultsContainer;
    private ImageButton btnCloseResults;
    private RecyclerView rvResults;
    private ResultsAdapter resultsAdapter;
    private List<HistorySnapshot> resultsList = new ArrayList<>();


    private CardView currentlySelectedCard;
    // ------------------------------------

    private List<String> clothingTypesList = new ArrayList<>();
    private List<ClothingItem> allItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.h6_recommendation);

        // Initialize Views
        canvasContainer = findViewById(R.id.canvasContainer);

        // 1. Initialize Generate Button (Renamed from Shuffle)
        btnGenerate = findViewById(R.id.btnGenerate);

        // 2. Initialize FAB Menu Views
        fabMenu = findViewById(R.id.fabMenu);
        fabHistory = findViewById(R.id.fabHistory);
        fabAddBox = findViewById(R.id.fabAddBox);
        fabPremade = findViewById(R.id.fabPremade);

        // Results Views
        resultsContainer = findViewById(R.id.resultsContainer);
        btnCloseResults = findViewById(R.id.btnCloseResults); // This ID is now correct
        rvResults = findViewById(R.id.rvResults);


        // --- SETUP LISTENERS ---

        // Generate Button Listener
        btnGenerate.setOnClickListener(v -> shuffleAllBoxes());

        // Back Button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Setup FAB Menu Logic (Bubble animation)
        setupFabMenu();


        // Setup Results RecyclerView
        resultsContainer.setVisibility(View.GONE);
        rvResults.setLayoutManager(new LinearLayoutManager(this));
        resultsAdapter = new ResultsAdapter(resultsList, snapshot -> {
            applySnapshotToCanvas(snapshot);
            resultsContainer.setVisibility(View.GONE);
            Toast.makeText(H6_RecommendationActivity.this, "Outfit Restored!", Toast.LENGTH_SHORT).show();
        }, canvasContainer);
        rvResults.setAdapter(resultsAdapter);

        // Results Panel Close Button
        btnCloseResults.setOnClickListener(v -> resultsContainer.setVisibility(View.GONE));

        // Load Data
        fetchCategoriesFromFirebase();
        loadClothesData();
    }

    // --- FAB Menu Animation Logic ---

    private void setupFabMenu() {
        // Main menu button toggles the sub-buttons
        fabMenu.setOnClickListener(v -> toggleFabMenu());

        // 1. History Bubble
        fabHistory.setOnClickListener(v -> {
            resultsContainer.setVisibility(View.VISIBLE);
            toggleFabMenu(); // Close menu after selection
        });

        // 2. Add Box Bubble
        fabAddBox.setOnClickListener(v -> {
            showAddBoxMenu(); // Call popup menu anchored to this FAB
            toggleFabMenu(); // Close menu after selection
        });

        // 3. Premade Box Bubble (Placeholder)
        fabPremade.setOnClickListener(v -> {
            Toast.makeText(H6_RecommendationActivity.this, "Premade layouts coming soon!", Toast.LENGTH_SHORT).show();
            toggleFabMenu(); // Close menu after selection
        });
    }

    private void toggleFabMenu() {
        isFabMenuOpen = !isFabMenuOpen;

        // Rotate the main FAB (+ becomes x)
        fabMenu.animate().rotation(isFabMenuOpen ? 45f : 0f).setDuration(300).start();

        if (isFabMenuOpen) {
            // Show Bubbles with cascading delay
            showFab(fabHistory, 50);
            showFab(fabAddBox, 100);
            showFab(fabPremade, 150);
        } else {
            // Hide Bubbles
            hideFab(fabHistory, 150);
            hideFab(fabAddBox, 100);
            hideFab(fabPremade, 50);
        }
    }

    private void showFab(View fab, long startDelay) {
        fab.setVisibility(View.VISIBLE);
        fab.setAlpha(0f);
        fab.setScaleX(0f);
        fab.setScaleY(0f);
        fab.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(200)
                .setStartDelay(startDelay)
                .setListener(null)
                .start();
    }

    private void hideFab(View fab, long startDelay) {
        fab.animate()
                .scaleX(0f)
                .scaleY(0f)
                .alpha(0f)
                .setDuration(200)
                .setStartDelay(startDelay)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        fab.setVisibility(View.GONE);
                    }
                })
                .start();
    }


    // --- Firebase Loading ---

    private void fetchCategoriesFromFirebase() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("categories");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                clothingTypesList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    // FIX: Retrieve as Object first to prevent "ArrayList to String" crash
                    Object nameObj = data.child("name").getValue();
                    String name = "";

                    if (nameObj instanceof String) {
                        name = (String) nameObj;
                    } else if (nameObj instanceof List) {
                        // If it's a list, grab the first item
                        List<?> list = (List<?>) nameObj;
                        if (!list.isEmpty() && list.get(0) instanceof String) {
                            name = (String) list.get(0);
                        }
                    }

                    if (name != null && !name.isEmpty() && !name.equalsIgnoreCase("Preoutfit") && !name.equalsIgnoreCase("Preoufit")) {
                        clothingTypesList.add(name);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(H6_RecommendationActivity.this, "Failed to load categories.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void loadClothesData() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("categories");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allItems.clear();
                for (DataSnapshot categorySnap : snapshot.getChildren()) {
                    DataSnapshot photosSnap = categorySnap.child("photos");
                    if (photosSnap.exists()) {
                        for (DataSnapshot itemSnap : photosSnap.getChildren()) {
                            String url = itemSnap.child("imageUrl").getValue(String.class);
                            String cat = itemSnap.child("category").getValue(String.class);

                            // 1. Extract SubTag
                            String sub = "";
                            DataSnapshot subTags = itemSnap.child("subTags");
                            if (subTags.exists()) {
                                for (DataSnapshot tag : subTags.getChildren()) {
                                    Object tagObj = tag.getValue();
                                    if (tagObj instanceof String) {
                                        sub = (String) tagObj;
                                        break;
                                    }
                                }
                            }

                            // 2. Extract Colors (Handle String or List)
                            List<String> detectedColors = new ArrayList<>();
                            Object colorObj = itemSnap.child("colors").getValue();

                            if (colorObj instanceof String) {
                                // Case: "Black" or "Black, White" -> Split into separate colors
                                String cStr = (String) colorObj;
                                for (String part : cStr.split(",")) {
                                    if (!part.trim().isEmpty()) {
                                        detectedColors.add(part.trim());
                                    }
                                }
                            } else if (colorObj instanceof List) {
                                // Case: ["Black", "White"] -> Add each as separate color
                                List<?> list = (List<?>) colorObj;
                                for (Object o : list) {
                                    if (o instanceof String) {
                                        String s = ((String) o).trim();
                                        if (!s.isEmpty()) {
                                            detectedColors.add(s);
                                        }
                                    }
                                }
                            }

                            // If no colors found, treat as empty so item still loads
                            if (detectedColors.isEmpty()) {
                                detectedColors.add("");
                            }

                            // 3. Add an item for EACH color found
                            // This ensures "Black" and "White" appear as separate filter chips
                            if (url != null) {
                                for (String color : detectedColors) {
                                    allItems.add(new ClothingItem(
                                            itemSnap.getKey(),
                                            url,
                                            cat != null ? cat.toLowerCase() : "",
                                            sub != null ? sub.toLowerCase() : "",
                                            color.toLowerCase()
                                    ));
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(H6_RecommendationActivity.this, "Failed to load items.", Toast.LENGTH_SHORT).show();
            }
        });
    }





    // --- UI Logic ---

    private void showAddBoxMenu() {
        if (clothingTypesList.isEmpty()) {
            Toast.makeText(this, "Categories are loading or empty...", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Create a RecyclerView to hold the grid
        RecyclerView recyclerView = new RecyclerView(this);
        int padding = 32; // Padding around the grid
        recyclerView.setPadding(padding, padding, padding, padding);

        // Set Grid Layout with 2 columns
        // FIX: Use the standard androidx.recyclerview.widget.GridLayoutManager
        recyclerView.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 2));

        // 2. Create the Dialog
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Select Category")
                .setView(recyclerView)
                .setNegativeButton("Cancel", null)
                .create();

        // 3. Set the Adapter to populate the grid
        recyclerView.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {

            class CategoryHolder extends RecyclerView.ViewHolder {
                TextView text;

                public CategoryHolder(View itemView) {
                    super(itemView);
                    text = (TextView) ((CardView) itemView).getChildAt(0);
                }
            }

            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                // Programmatically create a nice looking Card for each item
                CardView card = new CardView(parent.getContext());

                // Layout Params for the Card (Width, Height, Margins)
                // FIX: Use standard ViewGroup.LayoutParams constants and RecyclerView LayoutParams
                androidx.recyclerview.widget.GridLayoutManager.LayoutParams params =
                        new androidx.recyclerview.widget.GridLayoutManager.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                        );

                params.setMargins(16, 16, 16, 16); // Spacing between grid items
                card.setLayoutParams(params);

                card.setCardElevation(8f);
                card.setRadius(16f);
                card.setUseCompatPadding(true);

                // Create TextView inside the Card
                TextView tv = new TextView(parent.getContext());
                tv.setLayoutParams(new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                ));
                tv.setPadding(20, 60, 20, 60); // Padding inside the button
                tv.setGravity(Gravity.CENTER);
                tv.setTextSize(16f);
                tv.setTextColor(Color.BLACK);

                card.addView(tv);
                return new CategoryHolder(card);
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                CategoryHolder catHolder = (CategoryHolder) holder;
                String category = clothingTypesList.get(position);

                catHolder.text.setText(category);

                // Click Listener
                catHolder.itemView.setOnClickListener(v -> {
                    addMovableBox(category); // Call your existing logic
                    dialog.dismiss();        // Close the dialog
                });
            }

            @Override
            public int getItemCount() {
                return clothingTypesList.size();
            }
        });

        dialog.show();
    }

    private void addMovableBox(String category) {
        // Create CardView
        CardView card = new CardView(this);
        card.setId(View.generateViewId());

        BoxConfig config = BoxConfig.getSize(category);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(config.widthPx, config.heightPx);

        // Center initially
        params.gravity = Gravity.CENTER;

        card.setLayoutParams(params);
        card.setRadius(20f);
        card.setCardElevation(10f);
        card.setUseCompatPadding(true);

        // Container Layout inside Card
        FrameLayout innerLayout = new FrameLayout(this);
        card.addView(innerLayout);

        // 1. Main ImageView
        ImageView img = new ImageView(this);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        img.setImageResource(android.R.drawable.ic_menu_gallery);
        img.setAlpha(0.9f);
        innerLayout.addView(img, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));


        // 3. Lock Icon Indicator (Hidden by default, shows when locked)
        ImageView lockIndicator = new ImageView(this);
        lockIndicator.setImageResource(android.R.drawable.ic_secure); // Use a lock icon
        lockIndicator.setColorFilter(Color.RED);
        lockIndicator.setVisibility(View.GONE); // Hidden initially
        FrameLayout.LayoutParams lpLockInd = new FrameLayout.LayoutParams(40, 40);
        lpLockInd.gravity = Gravity.TOP | Gravity.END;
        lpLockInd.setMargins(0, 10, 10, 0);
        lockIndicator.setLayoutParams(lpLockInd);
        innerLayout.addView(lockIndicator);

        // --- LOGIC ---
        List<String> initialFilters = new ArrayList<>();
        initialFilters.add(category);
        // Tags: [0]=Filters, [1]=isLocked, [2]=url
        card.setTag(new Object[]{initialFilters, false, null});

        canvasContainer.addView(card);
        refreshBoxImage(card);

        // --- HANDLERS ---

        // Card Interaction (Move + Long Press)
// Inside the addMovableBox() method, find card.setOnTouchListener...

        card.setOnTouchListener(new View.OnTouchListener() {
            float dX, dY;
            float startX, startY;
            boolean isMoving = false;
            long startTime;
            boolean isLongPress = false;

            // This runnable for long-press is correct, no changes here.
            final Runnable longPressRunnable = () -> {
                isLongPress = true;
                currentlySelectedCard = card;
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (v != null) {
                    v.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                }
                showFloatingToolbar(card, lockIndicator);
            };

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                boolean isLocked = (boolean) ((Object[]) card.getTag())[1];

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = v.getX() - event.getRawX();
                        dY = v.getY() - event.getRawY();
                        startX = event.getRawX();
                        startY = event.getRawY();
                        startTime = System.currentTimeMillis();
                        isMoving = false;
                        isLongPress = false;
                        v.postDelayed(longPressRunnable, 500); // Set delay for long press
                        v.bringToFront();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        if (Math.abs(event.getRawX() - startX) > 20 || Math.abs(event.getRawY() - startY) > 20) {
                            v.removeCallbacks(longPressRunnable); // Cancel long press if dragging
                            if (!isLongPress && !isLocked) {
                                isMoving = true;
                                hideFloatingToolbar();
                                float parentWidth = canvasContainer.getWidth();
                                float parentHeight = canvasContainer.getHeight();
                                float newX = Math.max(0, Math.min(event.getRawX() + dX, parentWidth - v.getWidth()));
                                float newY = Math.max(0, Math.min(event.getRawY() + dY, parentHeight - v.getHeight()));
                                v.animate().x(newX).y(newY).setDuration(0).start();
                            }
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        v.removeCallbacks(longPressRunnable); // Always cancel runnable on up
                        long duration = System.currentTimeMillis() - startTime;

                        // *** THIS IS THE CORRECTED LOGIC ***
                        // If it was a short tap (not a drag or a long press), show the filter menu.
                        if (!isMoving && !isLongPress && duration < 200) {
                            // Find the label to pass to the menu
                            TextView filterLabel = v.findViewWithTag("filterLabelTag");
                            if (filterLabel != null) {
                                showFilterMenu((CardView) v, filterLabel);
                            }
                            hideFloatingToolbar(); // Also hide the toolbar just in case
                        }
                        // The single click to refresh the image is now replaced by the filter menu.
                        // refreshBoxImage(card) will be called from within the filter menu's listener.

                        return true;
                }
                return false;
            }
        });

    }

    // Declare this at class level
    private LinearLayout floatingToolbar;

    private void hideFloatingToolbar() {
        if (floatingToolbar == null) {
            floatingToolbar = findViewById(R.id.floating_toolbar);
        }
        if (floatingToolbar != null) {
            floatingToolbar.setVisibility(View.GONE);
        }
    }


    private void showFloatingToolbar(CardView selectedCard, ImageView lockIndicator) {
        if (floatingToolbar == null) {
            floatingToolbar = findViewById(R.id.floating_toolbar);
        }

        if (floatingToolbar != null) {
            floatingToolbar.setVisibility(View.VISIBLE);
            floatingToolbar.bringToFront();

            // Get current state
            Object[] tags = (Object[]) selectedCard.getTag();
            List<String> filters = (List<String>) tags[0];
            boolean currentLockState = (boolean) tags[1];

            // Ensure filter list has enough slots for [Category, Subtag, Color]
            while (filters.size() < 3) filters.add("All");

            String currentCategory = filters.get(0);
            String currentSubtag = filters.get(1);

            // --- 1. Delete ---
            ImageButton btnDelete = findViewById(R.id.btn_toolbar_delete);
            btnDelete.setOnClickListener(v -> {
                canvasContainer.removeView(selectedCard);
                hideFloatingToolbar();
                currentlySelectedCard = null;
            });

            // --- 2. Plus (Scale Up) ---
            ImageButton btnPlus = findViewById(R.id.btn_toolbar_plus);
            btnPlus.setOnClickListener(v -> {
                float currentScale = selectedCard.getScaleX();
                if (currentScale < 2.0f) {
                    selectedCard.setScaleX(currentScale + 0.1f);
                    selectedCard.setScaleY(currentScale + 0.1f);
                }
            });

            // --- 3. Minus (Scale Down) ---
            ImageButton btnMinus = findViewById(R.id.btn_toolbar_minus);
            btnMinus.setOnClickListener(v -> {
                float currentScale = selectedCard.getScaleX();
                if (currentScale > 0.5f) {
                    selectedCard.setScaleX(currentScale - 0.1f);
                    selectedCard.setScaleY(currentScale - 0.1f);
                }
            });

            // --- 4. Lock ---
            ImageButton btnLock = findViewById(R.id.btn_toolbar_lock);
            btnLock.setImageResource(currentLockState ? android.R.drawable.ic_lock_lock : android.R.drawable.ic_lock_idle_lock);
            btnLock.setOnClickListener(v -> {
                boolean isLocked = (boolean) tags[1];
                tags[1] = !isLocked;
                selectedCard.setTag(tags);
                lockIndicator.setVisibility(!isLocked ? View.VISIBLE : View.GONE);
                btnLock.setImageResource(!isLocked ? android.R.drawable.ic_lock_lock : android.R.drawable.ic_lock_idle_lock);
                if (!isLocked) hideFloatingToolbar();
            });

            // --- 5. Subtag Filter Button ---
            ImageButton btnFilterSub = findViewById(R.id.btn_toolbar_filter_subtag);
            btnFilterSub.setVisibility(View.VISIBLE);
            btnFilterSub.setOnClickListener(v -> {
                showSubtagFilterDialog(selectedCard, currentCategory);
                hideFloatingToolbar();
            });

            // --- 6. Color Filter Button ---
            ImageButton btnFilterColor = findViewById(R.id.btn_toolbar_filter_color);

            // Logic: If subtag is "All", show this. If a specific subtag is selected,
            // we usually rely on the automatic chain, but let's allow manual access if "All" is selected.
            if (currentSubtag.equals("All")) {
                btnFilterColor.setVisibility(View.VISIBLE);
            } else {
                // Per requirement: "if not all hide"
                btnFilterColor.setVisibility(View.GONE);
            }

            btnFilterColor.setOnClickListener(v -> {
                showColorFilterDialog(selectedCard, currentCategory);
                hideFloatingToolbar();
            });
        }
    }


    // Add this method to your H6_RecommendationActivity class
    private void showFilterMenu(CardView card, TextView filterLabel) {
        // 1. Find the hidden filter menu container included in your layout
        // (Assuming the XML snippet you provided is inside a view with ID 'filter_menu_container')
        View filterMenuContainer = findViewById(R.id.filter_options_container);

        if (filterMenuContainer == null) {
            Toast.makeText(this, "Filter menu layout not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Show the menu
        filterMenuContainer.setVisibility(View.VISIBLE);
        filterMenuContainer.bringToFront(); // Ensure it sits on top of the canvas

        // 3. Get references to internal views
        TextView title = filterMenuContainer.findViewById(R.id.filter_options_container);
        ChipGroup chipGroup = filterMenuContainer.findViewById(R.id.chip_group_filters);
        TextView btnFinish = filterMenuContainer.findViewById(R.id.btn_filter_finish);

        // 4. Get current state from the card tag
        // Tag structure: [0]=List<String> filters, [1]=boolean isLocked, [2]=String url
        Object[] tags = (Object[]) card.getTag();
        List<String> currentFilters = (List<String>) tags[0];
        String category = currentFilters.get(0); // The main category (e.g., "Tops")

        // 5. Set Title
        title.setText("Filter " + category);

        // 6. Populate Chips
        chipGroup.removeAllViews();

        // Get sub-tags for this specific category from your data source
        // (Assuming 'clothingItems' contains the raw data loaded from Firebase)
        Set<String> availableSubTags = new HashSet<>();
        for (ClothingItem item : allItems) {
            if (item.category.equalsIgnoreCase(category) && !item.subCategory.isEmpty()) {
                availableSubTags.add(item.subCategory); // Collect unique subtags
            }
        }

        // Add a "Clear / All" chip
        com.google.android.material.chip.Chip allChip = new com.google.android.material.chip.Chip(this);
        allChip.setText("All");
        allChip.setCheckable(true);
        // Check if "All" or nothing specific is currently selected
        boolean isAllSelected = currentFilters.size() < 2 || currentFilters.get(1).equals("All");
        allChip.setChecked(isAllSelected);
        chipGroup.addView(allChip);

        // Add chips for specific sub-tags
        for (String subTag : availableSubTags) {
            com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
            chip.setText(subTag);
            chip.setCheckable(true);

            // Check if this specific tag is currently active on the box
            if (!isAllSelected && currentFilters.size() >= 2 && currentFilters.get(1).equalsIgnoreCase(subTag)) {
                chip.setChecked(true);
            }

            chipGroup.addView(chip);
        }

        // Ensure single selection behavior (optional, based on your logic)
        chipGroup.setSingleSelection(true);

        // 7. Handle "Finish" Button Click
        btnFinish.setOnClickListener(v -> {
            // Determine which chip is selected
            String selectedSubTag = "All";
            int checkedId = chipGroup.getCheckedChipId();
            if (checkedId != View.NO_ID) {
                com.google.android.material.chip.Chip selectedChip = chipGroup.findViewById(checkedId);
                selectedSubTag = selectedChip.getText().toString();
            }

            // Update the Card's filter data
            // We preserve the category (index 0) and update the subtag (index 1)
            if (currentFilters.size() < 2) {
                currentFilters.add(selectedSubTag);
            } else {
                currentFilters.set(1, selectedSubTag);
            }

            // Update the UI Label on the box
            if (!selectedSubTag.equals("All")) {
                filterLabel.setText(category + "\n(" + selectedSubTag + ")");
            } else {
                filterLabel.setText(category);
            }

            // Hide the menu
            filterMenuContainer.setVisibility(View.GONE);

            // Trigger image refresh based on new filter
            refreshBoxImage(card);
        });
    }

    // --- 1. Subtag (Type) Dialog ---
    // Flow: User clicks a chip -> Updates data -> dismisses -> Opens Color Dialog
    private void showSubtagFilterDialog(CardView card, String category) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Type");

        // Create Layouts
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        com.google.android.material.chip.ChipGroup chipGroup = new com.google.android.material.chip.ChipGroup(this);
        chipGroup.setPadding(40, 20, 40, 20);
        chipGroup.setSingleSelection(true); // Only pick one type
        scrollView.addView(chipGroup);

        // Create the dialog object so we can dismiss it inside listeners
        final AlertDialog dialog = builder.create();

        // Prepare List of Options (Start with "All")
        List<String> options = new ArrayList<>();
        options.add("All");

        // Gather unique subtags from allItems
        for (ClothingItem item : allItems) {
            if (item.category.equalsIgnoreCase(category) && !item.subCategory.isEmpty()) {
                String sub = capitalize(item.subCategory);
                if (!options.contains(sub) && !sub.equalsIgnoreCase(category)) {
                    options.add(sub);
                }
            }
        }

        // Get current state
        Object[] tags = (Object[]) card.getTag();
        List<String> currentFilters = (List<String>) tags[0];
        String currentSub = currentFilters.size() > 1 ? currentFilters.get(1) : "All";

        // Create Chips
        for (String label : options) {
            com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
            chip.setText(label);
            chip.setCheckable(true);
            chip.setClickable(true);

            // Pre-check if matches current selection
            if (label.equalsIgnoreCase(currentSub)) {
                chip.setChecked(true);
            }

            // --- ACTION: Click -> Save -> Close -> Open Color Dialog ---
            chip.setOnClickListener(v -> {
                // 1. Update Data directly
                if (currentFilters.size() < 2) {
                    currentFilters.add(label);
                } else {
                    currentFilters.set(1, label);
                }

                // Update the UI label on the box immediately (optional, but good for feedback)
                applyFilterUpdate(card, label, 1);

                // 2. Dismiss this dialog
                dialog.dismiss();

                // 3. Automatically open the "Color" dialog
                showColorFilterDialog(card, category);
            });

            chipGroup.addView(chip);
        }

        dialog.setView(scrollView);
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel", (d, w) -> d.dismiss());
        dialog.show();
    }

    // --- 2. Color Dialog ---
    // Flow: Multiple Select -> Finish -> Refresh Image
    private void showColorFilterDialog(CardView card, String category) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Colors (Multiple)");

        // Create Layouts
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        com.google.android.material.chip.ChipGroup chipGroup = new com.google.android.material.chip.ChipGroup(this);
        chipGroup.setPadding(40, 20, 40, 20);
        chipGroup.setSingleSelection(false); // Allow multiple colors
        scrollView.addView(chipGroup);

        // Get Current Filters
        Object[] tags = (Object[]) card.getTag();
        List<String> filters = (List<String>) tags[0];
        String currentColors = filters.size() > 2 ? filters.get(2) : "All";

        // Parse current selection
        List<String> selectedColorsList = new ArrayList<>();
        if (!currentColors.equals("All")) {
            String[] split = currentColors.split(",");
            for (String s : split) selectedColorsList.add(s.trim());
        }

        // Fetch Unique Colors dynamically from 'allItems' based on category
        // Fetch Unique Colors dynamically from 'allItems' based on category AND sub-tag
        Set<String> availableColors = new HashSet<>();

        // Get the currently selected sub-tag (if any)
        String currentSubTag = filters.size() > 1 ? filters.get(1) : "All";

        for (ClothingItem item : allItems) {
            // 1. Must match Category
            if (!item.category.equalsIgnoreCase(category)) {
                continue;
            }

            // 2. Must match Sub-Tag (if not "All")
            // If currentSubTag is "Scarf", we only want colors from Scarves.
            if (!currentSubTag.equalsIgnoreCase("All") && !currentSubTag.equalsIgnoreCase("none")) {
                // Assuming item.subCategory holds the tag like "Scarf"
                if (item.subCategory == null || !item.subCategory.toLowerCase().contains(currentSubTag.toLowerCase())) {
                    continue; // Skip this item if it's not a Scarf
                }
            }

            // Add the color
            if (item.color != null && !item.color.isEmpty()) {
                availableColors.add(capitalize(item.color));
            }
        }


        List<String> sortedColors = new ArrayList<>(availableColors);
        java.util.Collections.sort(sortedColors);

        // "All" Chip Logic
        com.google.android.material.chip.Chip allChip = new com.google.android.material.chip.Chip(this);
        allChip.setText("All");
        allChip.setCheckable(true);
        allChip.setClickable(true);
        allChip.setChecked(currentColors.equals("All"));
        allChip.setOnClickListener(v -> {
            chipGroup.clearCheck();
            allChip.setChecked(true);
        });
        chipGroup.addView(allChip);

        // Dynamic Color Chips
        for (String color : sortedColors) {
            com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
            chip.setText(color);
            chip.setCheckable(true);
            chip.setClickable(true);

            // Check if previously selected (ignoring case)
            boolean isSelected = false;
            for(String s : selectedColorsList) {
                if(s.equalsIgnoreCase(color)) isSelected = true;
            }

            if (isSelected) {
                chip.setChecked(true);
                allChip.setChecked(false);
            }

            // If specific color clicked, uncheck "All"
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    allChip.setChecked(false);
                }
            });

            chipGroup.addView(chip);
        }

        builder.setView(scrollView);

        // Finish Button Logic
        builder.setPositiveButton("Finish", (dialog, which) -> {
            List<String> newSelection = new ArrayList<>();

            for (int i = 0; i < chipGroup.getChildCount(); i++) {
                View child = chipGroup.getChildAt(i);
                if (child instanceof com.google.android.material.chip.Chip) {
                    com.google.android.material.chip.Chip c = (com.google.android.material.chip.Chip) child;
                    if (c.isChecked() && !c.getText().toString().equals("All")) {
                        newSelection.add(c.getText().toString());
                    }
                }
            }

            String finalColorString;
            if (newSelection.isEmpty()) {
                finalColorString = "All";
            } else {
                // Join with commas
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    finalColorString = String.join(",", newSelection);
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (int k = 0; k < newSelection.size(); k++) {
                        sb.append(newSelection.get(k));
                        if (k < newSelection.size() - 1) sb.append(",");
                    }
                    finalColorString = sb.toString();
                }
            }

            // Update Data and Refresh
            applyFilterUpdate(card, finalColorString, 2);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }


    private void addChipToGroup(com.google.android.material.chip.ChipGroup group, String text, CardView card, int filterIndex, boolean triggerNextDialog) {
        com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
        chip.setText(text);
        chip.setCheckable(true);
        chip.setClickable(true);

        chip.setOnClickListener(v -> {
            applyFilterUpdate(card, text, filterIndex);

            // Close the current dialog (handled by AlertDialog behavior usually,
            // but since we want to chain immediately, we rely on the visual update)

            // Logic: "Show dialog box select chips... then next is color?"
            if (triggerNextDialog && filterIndex == 1 && !text.equals("All")) {
                // Small delay to allow the first dialog to close visually if needed,
                // or simply open the next one on top.
                Object[] tags = (Object[]) card.getTag();
                List<String> filters = (List<String>) tags[0];
                showColorFilterDialog(card, filters.get(0));
            }
        });
        group.addView(chip);
    }

    private void applyFilterUpdate(CardView card, String newValue, int index) {
        Object[] tags = (Object[]) card.getTag();
        List<String> filters = (List<String>) tags[0];

        // Ensure list is big enough
        while (filters.size() <= index) {
            filters.add("All");
        }
        filters.set(index, newValue);

        // Refresh the image based on new filters
        refreshBoxImage(card);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private List<ClothingItem> getFilteredItems(String category, String subTag, String color) {
        List<ClothingItem> filteredList = new ArrayList<>();
        for (ClothingItem item : allItems) {
            // 1. Match Category (e.g., "Accessories")
            // 1. Match Category (e.g., "Accessories")
            if (item.category == null || !item.category.equalsIgnoreCase(category)) {
                continue;
            }

            // 2. Match SubCategory / SubTag (e.g., "Scarf")
            // Logic: If a specific sub-tag is selected (and is not "none" or "All"), the item must match it.
            if (subTag != null && !subTag.isEmpty() && !subTag.equalsIgnoreCase("none") && !subTag.equalsIgnoreCase("All")) {
                // Check if item has sub-category data and if it contains the selected tag (case-insensitive)
                if (item.subCategory == null || !item.subCategory.toLowerCase().contains(subTag.toLowerCase())) {
                    continue;
                }
            }

            // 3. Match Color
            // Logic: "the color who has scarf will show if not none"
            // If a specific color is selected (and is not "none" or "All"), the item must match it.
            if (color != null && !color.isEmpty() && !color.equalsIgnoreCase("none") && !color.equalsIgnoreCase("All")) {
                if (item.color == null || !item.color.equalsIgnoreCase(color)) {
                    continue;
                }
            }

            filteredList.add(item);

        }
        return filteredList;
    }



    private void refreshBoxImage(CardView card) {
        Object[] tags = (Object[]) card.getTag();
        if ((boolean) tags[1]) return; // Locked

        List<String> filters = (List<String>) tags[0];
        if (filters == null || filters.isEmpty()) return;

        // Ensure safe access
        String fCategory = filters.get(0);
        String fSubtag = filters.size() > 1 ? filters.get(1) : "All";
        String fColor = filters.size() > 2 ? filters.get(2) : "All";

        List<ClothingItem> candidates = new ArrayList<>();

        for (ClothingItem item : allItems) {
            // 1. Check Category
            boolean matchCategory = item.category.equalsIgnoreCase(fCategory);

            // 2. Check Subtag (SubCategory)
            boolean matchSub = fSubtag.equals("All") || item.subCategory.equalsIgnoreCase(fSubtag);

            // 3. Check Color (Supports multi-select like "Red,Blue")
            boolean matchColor;
            if (fColor.equals("All")) {
                matchColor = true;
            } else {
                // We wrap with commas to ensure exact match (e.g., ",red,blue," contains ",red,")
                // ensuring "Light Blue" doesn't accidentally match "Blue" unless intended.
                String searchList = "," + fColor.toLowerCase() + ",";
                String itemColor = item.color != null ? item.color.toLowerCase() : "";

                // If item has no color, it fails match unless filter is All
                matchColor = !itemColor.isEmpty() && searchList.contains("," + itemColor + ",");
            }

            if (matchCategory && matchSub && matchColor) {
                candidates.add(item);
            }
        }

        ImageView imgView = (ImageView) ((FrameLayout) card.getChildAt(0)).getChildAt(0);

        if (!candidates.isEmpty()) {
            Random random = new Random();
            ClothingItem selected = candidates.get(random.nextInt(candidates.size()));
            tags[2] = selected.imageUrl; // Update current URL in tag
            imgView.setAlpha(1.0f);
            Glide.with(this).load(selected.imageUrl).centerCrop().into(imgView);
        } else {
            // No items match filters
            imgView.setImageResource(android.R.drawable.ic_menu_help);
            imgView.setAlpha(0.5f);
            tags[2] = null;
        }
    }


private void shuffleAllBoxes() {
    OutfitGenerator.generate(
            this,
            canvasContainer,
            allItems,
            new OutfitGenerator.GeneratorCallback() {
                @Override
                public void onCombinationsGenerated(List<HistorySnapshot> generatedSnapshots) {
                    resultsList.clear();
                    resultsList.addAll(generatedSnapshots);
                    resultsAdapter.notifyDataSetChanged();
                    resultsContainer.setVisibility(View.VISIBLE);
                    Toast.makeText(H6_RecommendationActivity.this, "Generated " + generatedSnapshots.size() + " results!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFirstCombinationApplied(List<String> firstComboUrls) {
                }
            }
    );
}

private void applySnapshotToCanvas(HistorySnapshot snapshot) {
    if (snapshot == null || snapshot.boxStates == null) return;
    for (BoxState state : snapshot.boxStates) {
        CardView card = canvasContainer.findViewById(state.viewId);
        if (card != null) {
            Object[] tags = (Object[]) card.getTag();
            boolean isLocked = false;
            if (tags != null && tags.length > 1 && tags[1] instanceof Boolean) {
                isLocked = (boolean) tags[1];
            }
            if (isLocked) continue;

            card.setX(state.x);
            card.setY(state.y);
            ViewGroup.LayoutParams params = card.getLayoutParams();
            if (params.width != state.width || params.height != state.height) {
                params.width = state.width;
                params.height = state.height;
                card.setLayoutParams(params);
            }
            if (tags != null && tags.length > 2) {
                tags[2] = state.url;
            }
            if (card.getChildCount() > 0 && card.getChildAt(0) instanceof FrameLayout) {
                FrameLayout inner = (FrameLayout) card.getChildAt(0);
                if (inner.getChildCount() > 0 && inner.getChildAt(0) instanceof ImageView) {
                    ImageView imgView = (ImageView) inner.getChildAt(0);
                    Glide.with(this).load(state.url).centerCrop().into(imgView);
                }
            }
        }
    }
}

// --- Data Models ---

public static class ClothingItem {
    public String id;
    public String imageUrl;
    public String category;
    public String subCategory; // previously subTags
    public String color;       // <--- ADD THIS FIELD

    public ClothingItem(String id, String url, String category, String subCategory, String color) {
        this.id = id;
        this.imageUrl = url;
        this.category = category;
        this.subCategory = subCategory;
        this.color = color;
    }
}


public static class BoxState {
    int viewId;
    String url;
    float x, y;
    int width, height;

    public BoxState(int viewId, String url, float x, float y, int width, int height) {
        this.viewId = viewId;
        this.url = url;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
}

public static class HistorySnapshot {
    List<BoxState> boxStates;
    String timestamp;

    public HistorySnapshot(List<BoxState> states) {
        this.boxStates = states;
        this.timestamp = String.valueOf(System.currentTimeMillis());
    }
}

public static class BoxConfig {
    public int widthPx;
    public int heightPx;

    public BoxConfig(int w, int h) {
        widthPx = w;
        heightPx = h;
    }

    public static BoxConfig getSize(String category) {
        DisplayMetrics dm = Resources.getSystem().getDisplayMetrics();
        float screenW = dm.widthPixels;
        float screenH = dm.heightPixels;
        float density = dm.density;

        float baseWidth = screenW * 0.35f;
        float baseHeight = screenH * 0.20f;
        float w, h;

        switch (category.toLowerCase()) {
            case "hat":
                w = baseWidth * 0.7f;
                h = baseHeight * 0.48f;
                break;
            case "accessories":
                w = baseWidth * 0.60f;
                h = baseHeight * 0.60f;
                break;
            case "shoes":
                w = baseWidth * 1.0f;
                h = baseHeight * 0.56f;
                break;
            case "bag":
                w = baseWidth * 0.85f;
                h = baseHeight * 0.75f;
                break;
            case "outer":
                w = baseWidth * 1.375f;
                h = baseHeight * 1.52f;
                break;
            case "top":
                w = baseWidth * 1.25f;
                h = baseHeight * 1.28f;
                break;
            case "bottom":
                w = baseWidth * 1.25f;
                h = baseHeight * 1.44f;
                break;
            case "dress":
                w = baseWidth * 1.8f;
                h = baseHeight * 2.2f;
                break;

            default:
                w = baseWidth;
                h = baseHeight;
                break;
        }

        float maxPx = 560f * density;
        if (w > maxPx) w = maxPx;
        if (h > maxPx) h = maxPx;

        float minPx = 90f * density;
        if (w < minPx) w = minPx;
        if (h < minPx) h = minPx;

        return new BoxConfig((int) w, (int) h);
    }
}

// --- Results Adapter ---
public static class ResultsAdapter extends RecyclerView.Adapter<ResultsAdapter.Holder> {
    private List<HistorySnapshot> list;
    private OnItemClickListener listener;
    private View canvas;

    public interface OnItemClickListener {
        void onItemClick(HistorySnapshot snapshot);
    }

    public ResultsAdapter(List<HistorySnapshot> list, OnItemClickListener listener, View canvas) {
        this.list = list;
        this.listener = listener;
        this.canvas = canvas;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history_outfit, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        HistorySnapshot snapshot = list.get(position);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(snapshot);
            }
        });

        holder.miniCanvas.removeAllViews();
        float originalCanvasWidth = canvas.getWidth();
        float originalCanvasHeight = canvas.getHeight();
        if (originalCanvasWidth == 0) {
            originalCanvasWidth = holder.itemView.getResources().getDisplayMetrics().widthPixels;
            originalCanvasHeight = holder.itemView.getResources().getDisplayMetrics().heightPixels;
        }

        int targetWidth = holder.itemView.getResources().getDisplayMetrics().widthPixels - 100;
        float scaleRatio = targetWidth / originalCanvasWidth;
        int targetHeight = (int) (originalCanvasHeight * scaleRatio);

        ViewGroup.LayoutParams canvasParams = holder.miniCanvas.getLayoutParams();
        canvasParams.width = targetWidth;
        canvasParams.height = targetHeight;
        holder.miniCanvas.setLayoutParams(canvasParams);

        for (BoxState state : snapshot.boxStates) {
            ImageView iv = new ImageView(holder.itemView.getContext());
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);

            int w = (int) (state.width * scaleRatio);
            int h = (int) (state.height * scaleRatio);
            int x = (int) (state.x * scaleRatio);
            int y = (int) (state.y * scaleRatio);

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(w, h);
            lp.leftMargin = x;
            lp.topMargin = y;
            lp.gravity = Gravity.TOP | Gravity.START;

            iv.setLayoutParams(lp);
            iv.setBackgroundColor(Color.WHITE);
            iv.setPadding(2, 2, 2, 2);

            Glide.with(holder.itemView.getContext()).load(state.url).override(w, h).into(iv);
            holder.miniCanvas.addView(iv);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class Holder extends RecyclerView.ViewHolder {
        FrameLayout miniCanvas;

        public Holder(@NonNull View itemView) {
            super(itemView);
            miniCanvas = itemView.findViewById(R.id.miniCanvas);
        }
    }
}
}





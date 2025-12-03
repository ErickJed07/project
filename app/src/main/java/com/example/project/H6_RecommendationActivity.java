package com.example.project;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button; // Changed from FloatingActionButton
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class H6_RecommendationActivity extends AppCompatActivity {

    private FrameLayout canvasContainer;
    private Button btnAddBox; // FIXED: Changed to Button to match XML
    private Button btnShuffle; // FIXED: Changed ImageButton to Button to match XML
    private FloatingActionButton btnHistory; // FIXED: Added for history button
    private FrameLayout historyContainer; // Added for history overlay
    private ImageButton btnCloseHistory; // Added for closing history

    private RecyclerView rvHistory;
    private HistoryAdapter historyAdapter;
    private List<HistorySnapshot> historyList = new ArrayList<>();

    // List to hold categories fetched from Firebase
    private List<String> clothingTypesList = new ArrayList<>();

    // Holds all clothes fetched from Firebase
    private List<ClothingItem> allItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.h6_recommendation); // Ensure this matches your XML filename

        // Initialize Views
        canvasContainer = findViewById(R.id.canvasContainer);
        btnAddBox = findViewById(R.id.btnAddBox);       // XML: <Button android:id="@+id/btnAddBox" ... />
        btnShuffle = findViewById(R.id.btnShuffleAll); // XML: <Button android:id="@+id/btnShuffleAll" ... />
        btnHistory = findViewById(R.id.btnHistory);    // XML: <FloatingActionButton android:id="@+id/btnHistory" ... />

        historyContainer = findViewById(R.id.historyContainer);
        btnCloseHistory = findViewById(R.id.btnCloseHistory);
        rvHistory = findViewById(R.id.rvHistory);

        // Setup History RecyclerView
        rvHistory.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        historyAdapter = new HistoryAdapter(historyList);
        rvHistory.setAdapter(historyAdapter);

        // Load Categories and Items from Firebase
        fetchCategoriesFromFirebase();
        loadClothesData();

        // Click Listeners
        btnAddBox.setOnClickListener(v -> showAddBoxMenu());
        btnShuffle.setOnClickListener(v -> shuffleAllBoxes());

        // Toggle History View
        btnHistory.setOnClickListener(v -> {
            historyContainer.setVisibility(View.VISIBLE);
            rvHistory.scrollToPosition(0);
        });

        btnCloseHistory.setOnClickListener(v -> historyContainer.setVisibility(View.GONE));

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
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
                    String name = data.child("name").getValue(String.class);
                    // Exclude "Preoutfit" from the menu
                    if (name != null && !name.equalsIgnoreCase("Preoutfit") && !name.equalsIgnoreCase("Preoufit")) {
                        clothingTypesList.add(name);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    // [IMPORTANT] Update this method to match your Firebase structure: Users -> categories -> [Category] -> photos
    private void loadClothesData() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Correct path: Users -> uid -> categories
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("categories");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allItems.clear();
                // 1. Loop through each Category (e.g., Bag, Top)
                for (DataSnapshot categorySnap : snapshot.getChildren()) {
                    // 2. Access the 'photos' node inside the category
                    DataSnapshot photosSnap = categorySnap.child("photos");
                    if (photosSnap.exists()) {
                        // 3. Loop through the photos
                        for (DataSnapshot itemSnap : photosSnap.getChildren()) {
                            String url = itemSnap.child("imageUrl").getValue(String.class);
                            String cat = itemSnap.child("category").getValue(String.class);

                            String sub = "";
                            // Handle subTags array (e.g., subTags: ["Handbag"])
                            DataSnapshot subTags = itemSnap.child("subTags");
                            if (subTags.exists()) {
                                for (DataSnapshot tag : subTags.getChildren()) {
                                    sub = tag.getValue(String.class);
                                    break; // Use the first tag as the subCategory
                                }
                            }

                            if (url != null) {
                                allItems.add(new ClothingItem(
                                        itemSnap.getKey(),
                                        url,
                                        cat != null ? cat.toLowerCase() : "",
                                        sub != null ? sub.toLowerCase() : ""
                                ));
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(H6_RecommendationActivity.this, "Failed to load items", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- UI Logic ---

    private void showAddBoxMenu() {
        PopupMenu popup = new PopupMenu(this, btnAddBox);
        if (clothingTypesList.isEmpty()) {
            popup.getMenu().add("Loading...");
        } else {
            for (String type : clothingTypesList) {
                popup.getMenu().add(type);
            }
        }

        popup.setOnMenuItemClickListener(item -> {
            if (!item.getTitle().toString().equals("Loading...")) {
                addMovableBox(item.getTitle().toString());
            }
            return true;
        });
        popup.show();
    }

    private void addMovableBox(String category) {
        // Create CardView
        CardView card = new CardView(this);
        int startSize = 350; // Default size
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(startSize, startSize);
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
        img.setImageResource(android.R.drawable.ic_menu_gallery); // Placeholder
        img.setAlpha(0.9f);
        innerLayout.addView(img, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 2. Filter Label
        TextView tvFilter = new TextView(this);
        tvFilter.setText("Filter: " + category);
        tvFilter.setTextSize(10f);
        tvFilter.setTextColor(Color.WHITE);
        tvFilter.setBackgroundColor(Color.parseColor("#80000000"));
        tvFilter.setPadding(8, 4, 8, 4);
        FrameLayout.LayoutParams lpText = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lpText.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        lpText.setMargins(0, 0, 0, 10); // Lift slightly above resize handle
        tvFilter.setLayoutParams(lpText);
        innerLayout.addView(tvFilter);

        // 3. Lock Button (Top Right)
        ImageButton btnLock = new ImageButton(this);
        btnLock.setImageResource(android.R.drawable.ic_lock_lock);
        btnLock.setBackgroundColor(Color.TRANSPARENT);
        btnLock.setColorFilter(Color.BLACK); // Make icon dark for visibility
        FrameLayout.LayoutParams lpLock = new FrameLayout.LayoutParams(60, 60);
        lpLock.gravity = Gravity.TOP | Gravity.END;
        lpLock.setMargins(0, 10, 10, 0);
        btnLock.setLayoutParams(lpLock);
        innerLayout.addView(btnLock);

        // --- EDIT MODE CONTROLS (Hidden by default) ---

        // 4. Delete Button (Top Left - Red X)
        ImageButton btnDelete = new ImageButton(this);
        btnDelete.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        btnDelete.setBackgroundResource(android.R.drawable.dialog_holo_light_frame); // simple background
        btnDelete.setColorFilter(Color.RED);
        btnDelete.setVisibility(View.GONE); // Hidden initially
        FrameLayout.LayoutParams lpDelete = new FrameLayout.LayoutParams(70, 70);
        lpDelete.gravity = Gravity.TOP | Gravity.START;
        btnDelete.setLayoutParams(lpDelete);
        innerLayout.addView(btnDelete);

        // 5. Resize Handle (Bottom Right)
        ImageView btnResize = new ImageView(this);
        btnResize.setImageResource(android.R.drawable.ic_menu_crop); // Use crop icon as resize handle
        btnResize.setBackgroundColor(Color.parseColor("#80FFFFFF")); // Semi-transparent white
        btnResize.setVisibility(View.GONE); // Hidden initially
        FrameLayout.LayoutParams lpResize = new FrameLayout.LayoutParams(70, 70);
        lpResize.gravity = Gravity.BOTTOM | Gravity.END;
        btnResize.setLayoutParams(lpResize);
        innerLayout.addView(btnResize);

        // --- LOGIC ---

        // Tags: [0] = Filters List, [1] = isLocked, [2] = currentImageUrl
        List<String> initialFilters = new ArrayList<>();
        initialFilters.add(category);
        card.setTag(new Object[]{initialFilters, false, null});

        // Add to layout
        canvasContainer.addView(card);

        // Center initially
        card.post(() -> {
            float centerX = (canvasContainer.getWidth() - card.getWidth()) / 2f;
            float centerY = (canvasContainer.getHeight() - card.getHeight()) / 2f;
            card.setX(centerX);
            card.setY(centerY);
            card.setVisibility(View.VISIBLE);
            refreshBoxImage(card);
        });

        // --- HANDLERS ---

        // A. Delete Logic
        btnDelete.setOnClickListener(v -> {
            canvasContainer.removeView(card);
            Toast.makeText(this, "Box Deleted", Toast.LENGTH_SHORT).show();
        });

        // B. Resize Logic (Touch Listener on the Handle)
        btnResize.setOnTouchListener(new View.OnTouchListener() {
            float lastX, lastY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastX = event.getRawX();
                        lastY = event.getRawY();
                        // Disallow parent (Card) from stealing this touch event
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - lastX;
                        float dy = event.getRawY() - lastY;

                        ViewGroup.LayoutParams params = card.getLayoutParams();
                        params.width = Math.max(200, params.width + (int) dx); // Min width 200
                        params.height = Math.max(200, params.height + (int) dy); // Min height 200
                        card.setLayoutParams(params);

                        lastX = event.getRawX();
                        lastY = event.getRawY();
                        return true;
                }
                return false;
            }
        });

        // C. Card Interaction (Move + Long Press to Show Controls)
        card.setOnTouchListener(new View.OnTouchListener() {
            float dX, dY;
            float startX, startY;
            boolean isMoving = false;
            long startTime;
            boolean isLongPress = false;

            // Long Press activates Edit Mode
            Runnable longPressRunnable = () -> {
                isLongPress = true;
                // Toggle visibility of controls
                boolean makeVisible = (btnDelete.getVisibility() == View.GONE);
                btnDelete.setVisibility(makeVisible ? View.VISIBLE : View.GONE);
                btnResize.setVisibility(makeVisible ? View.VISIBLE : View.GONE);

                // Visual feedback
                card.setCardElevation(makeVisible ? 20f : 10f);
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (v != null) v.vibrate(50);
            };

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // If locked, only allow Long Press or Click (disable move)
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

                        v.postDelayed(longPressRunnable, 600); // 600ms for hold
                        v.bringToFront();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        if (Math.abs(event.getRawX() - startX) > 10 || Math.abs(event.getRawY() - startY) > 10) {
                            v.removeCallbacks(longPressRunnable); // Cancel hold if moved
                            if (!isLocked) {
                                isMoving = true;
                                v.animate()
                                        .x(event.getRawX() + dX)
                                        .y(event.getRawY() + dY)
                                        .setDuration(0)
                                        .start();
                            }
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        v.removeCallbacks(longPressRunnable);
                        long duration = System.currentTimeMillis() - startTime;

                        if (!isMoving && !isLongPress && duration < 600) {
                            // If controls are open, clicking closes them
                            if (btnDelete.getVisibility() == View.VISIBLE) {
                                btnDelete.setVisibility(View.GONE);
                                btnResize.setVisibility(View.GONE);
                            } else {
                                // Otherwise, normal click: Filter Menu
                                showFilterMenu(card, tvFilter);
                            }
                        }
                        return true;
                }
                return false;
            }
        });

        // Lock Button Logic
        btnLock.setOnClickListener(v -> {
            Object[] tags = (Object[]) card.getTag();
            boolean currentLock = (boolean) tags[1];
            tags[1] = !currentLock; // toggle
            btnLock.setImageResource(currentLock ? android.R.drawable.ic_menu_edit : android.R.drawable.ic_lock_lock);
            Toast.makeText(this, !currentLock ? "Locked (Movement Disabled)" : "Unlocked", Toast.LENGTH_SHORT).show();
        });
    }


    private void showFilterMenu(CardView card, TextView tvLabel) {
        PopupMenu p = new PopupMenu(this, card);
        Object[] tags = (Object[]) card.getTag();
        List<String> filters = (List<String>) tags[0];

        // 1. Identify the Main Category
        // We look at the current filter. If it's a sub-category (like "Handbag"),
        // we find which main category it belongs to (like "Bag").
        String currentFilter = filters.get(0);
        String mainCategory = "";

        // First, check if the current filter is already a main category
        for (String type : clothingTypesList) {
            if (type.equalsIgnoreCase(currentFilter)) {
                mainCategory = type;
                break;
            }
        }

        // If not found, it might be a sub-category. Let's find its parent from allItems.
        if (mainCategory.isEmpty()) {
            for (ClothingItem item : allItems) {
                // Check if this item matches the current sub-filter
                if (item.subCategory.equalsIgnoreCase(currentFilter)) {
                    // Found the item! The parent is its category.
                    mainCategory = item.category;
                    // Capitalize first letter for display (e.g., "bag" -> "Bag")
                    if (mainCategory != null && !mainCategory.isEmpty()) {
                        mainCategory = mainCategory.substring(0, 1).toUpperCase() + mainCategory.substring(1);
                    }
                    break;
                }
            }
        }

        // 2. Add "All [Category]" option at the top (Bold)
        if (!mainCategory.isEmpty()) {
            String label = "All " + mainCategory;
            android.text.SpannableString s = new android.text.SpannableString(label);
            s.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, s.length(), 0);
            p.getMenu().add(0, 0, 0, s);
        }

        // 3. Add Sub-Category Options
        // We find all unique sub-categories available for this main category
        List<String> subOptions = new ArrayList<>();
        for (ClothingItem item : allItems) {
            if (item.category.equalsIgnoreCase(mainCategory) && !item.subCategory.isEmpty()) {
                // Avoid duplicates
                String sub = item.subCategory;
                // Capitalize for display
                sub = sub.substring(0, 1).toUpperCase() + sub.substring(1);

                if (!subOptions.contains(sub) && !sub.equalsIgnoreCase(mainCategory)) {
                    subOptions.add(sub);
                }
            }
        }

        int index = 1;
        for (String sub : subOptions) {
            // Don't show the currently selected one if you want, or just list them all
            p.getMenu().add(0, index++, index, sub);
        }

        p.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();

            if (title.startsWith("All ")) {
                String cat = title.substring(4);
                filters.clear(); // Clear old filters
                filters.add(cat); // Set main category
                tvLabel.setText("Filter: " + cat);
                refreshBoxImage(card);
                return true;
            }

            // Sub-filter selected
            filters.clear();
            filters.add(title); // Set new sub-filter
            tvLabel.setText("Filter: " + title);
            refreshBoxImage(card);
            return true;
        });
        p.show();
    }


    // --- Image Refresh Logic ---
    private void refreshBoxImage(CardView card) {
        Object[] tags = (Object[]) card.getTag();
        // tags[1] is boolean isLocked
        if ((boolean) tags[1]) return;

        // tags[0] is List<String> filters. Index 0 is the active category/subcategory
        List<String> filters = (List<String>) tags[0];
        if (filters == null || filters.isEmpty()) return;

        String selectedCategory = filters.get(0).trim();

        // 1. Filter existing Firebase items (allItems) based on the selected category
        List<ClothingItem> candidates = new ArrayList<>();

        for (ClothingItem item : allItems) {
            String itemCat = item.category != null ? item.category : "";
            String itemSub = item.subCategory != null ? item.subCategory : "";

            // Case-insensitive check
            if (itemCat.equalsIgnoreCase(selectedCategory) || itemSub.equalsIgnoreCase(selectedCategory)) {
                candidates.add(item);
            }
        }

        // 2. Pick Random and Load
        ImageView imgView = (ImageView) ((FrameLayout) card.getChildAt(0)).getChildAt(0);

        if (!candidates.isEmpty()) {
            Random random = new Random();
            ClothingItem selected = candidates.get(random.nextInt(candidates.size()));

            // Save URL in tag[2] for history later
            tags[2] = selected.url;

            imgView.setAlpha(1.0f);
            Glide.with(this)
                    .load(selected.url)
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(imgView);
        } else {
            // No items found
            imgView.setImageResource(android.R.drawable.ic_menu_help);
            imgView.setAlpha(0.5f);
            tags[2] = null;
        }
    }

    // --- New Shuffle Logic using Helper Class ---
    private void shuffleAllBoxes() {
        OutfitGenerator.generate(
                this,
                canvasContainer,
                allItems,
                new OutfitGenerator.GeneratorCallback() {
                    @Override
                    public void onCombinationsGenerated(List<HistorySnapshot> generatedSnapshots) {
                        // Add new snapshots to top of history
                        for (int i = generatedSnapshots.size() - 1; i >= 0; i--) {
                            historyList.add(0, generatedSnapshots.get(i));
                        }
                        historyAdapter.notifyDataSetChanged();
                        // If history is open, scroll to top
                        if (historyContainer.getVisibility() == View.VISIBLE) {
                            rvHistory.scrollToPosition(0);
                        }
                    }

                    @Override
                    public void onFirstCombinationApplied(List<String> firstComboUrls) {
                        // UI updated automatically by generator
                    }
                }
        );
    }

    // --- Data Models ---

    public static class ClothingItem {
        String id, url, category, subCategory;

        ClothingItem(String id, String url, String category, String subCategory) {
            this.id = id;
            this.url = url;
            this.category = category;
            this.subCategory = subCategory;
        }
    }

    public static class BoxState {
        String url;
        float x, y;
        int width, height;

        BoxState(String url, float x, float y, int width, int height) {
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

        HistorySnapshot(List<BoxState> states) {
            this.boxStates = states;
            this.timestamp = String.valueOf(System.currentTimeMillis());
        }
    }

    // --- Adapter Class ---
    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.Holder> {
        List<HistorySnapshot> list;

        // 1.0f means history item is 100% size (same size as real canvas)
        private final float SCALE_FACTOR = 1.0f;

        HistoryAdapter(List<HistorySnapshot> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Context context = parent.getContext();

            // 1. Wrapper (Space between items)
            FrameLayout wrapper = new FrameLayout(context);
            // Calculate height based on screen height (Full height since scale is 1.0)
            int h = (int) (context.getResources().getDisplayMetrics().heightPixels * SCALE_FACTOR);
            wrapper.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h));
            wrapper.setPadding(16, 16, 16, 16);

            // 2. CardView (The visual container)
            CardView card = new CardView(context);
            card.setRadius(16f);
            card.setCardElevation(8f);
            card.setUseCompatPadding(true);

            // 3. Inner Canvas (Where images go)
            FrameLayout miniCanvas = new FrameLayout(context);
            miniCanvas.setBackgroundColor(Color.parseColor("#F0F0F0")); // Softer gray

            // Assemble
            card.addView(miniCanvas, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            wrapper.addView(card, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            return new Holder(wrapper, miniCanvas);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            HistorySnapshot snapshot = list.get(position);

            // Clear previous views
            holder.miniCanvas.removeAllViews();

            // Recreate the snapshot visual
            for (BoxState state : snapshot.boxStates) {
                ImageView iv = new ImageView(holder.itemView.getContext());
                iv.setScaleType(ImageView.ScaleType.FIT_XY); // Warp image to fill box

                // Scale dimensions (1.0f = original size)
                int w = (int) (state.width * SCALE_FACTOR);
                int h = (int) (state.height * SCALE_FACTOR);

                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(w, h);

                // Scale positions
                lp.leftMargin = (int) (state.x * SCALE_FACTOR);
                lp.topMargin = (int) (state.y * SCALE_FACTOR);
                lp.gravity = Gravity.TOP | Gravity.START;

                iv.setLayoutParams(lp);

                // Optional: Add a white border
                iv.setPadding(1, 1, 1, 1);
                iv.setBackgroundColor(Color.WHITE);

                Glide.with(holder.itemView.getContext())
                        .load(state.url)
                        .override(w, h)
                        .into(iv);

                holder.miniCanvas.addView(iv);
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            FrameLayout miniCanvas;

            Holder(View itemView, FrameLayout miniCanvas) {
                super(itemView);
                this.miniCanvas = miniCanvas;
            }
        }
    }
}

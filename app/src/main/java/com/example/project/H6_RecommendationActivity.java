package com.example.project;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
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
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class H6_RecommendationActivity extends AppCompatActivity {

    // --- Data Model (Updated to match Firebase 'items' structure) ---
    private static class ClothingItem {
        String id, url, category, subCategory;

        ClothingItem(String id, String url, String category, String subCategory) {
            this.id = id;
            this.url = url;
            this.category = category;
            this.subCategory = subCategory;
        }
    }

    // History Model
    private static class HistorySnapshot {
        List<String> imageUrls;
        String timestamp;
        HistorySnapshot(List<String> urls) {
            this.imageUrls = urls;
            this.timestamp = String.valueOf(System.currentTimeMillis());
        }
    }

    // --- UI Views ---
    private FrameLayout canvasContainer;
    private Button btnAddBox, btnShuffleAll;
    private ImageButton btnBack, btnCloseHistory;
    private FloatingActionButton btnHistory;
    private FrameLayout historyContainer;
    private RecyclerView rvHistory;

    // --- Data Lists ---
    private List<ClothingItem> allItems = new ArrayList<>();
    private List<HistorySnapshot> historyList = new ArrayList<>();
    private HistoryAdapter historyAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.h6_recommendation);

        // 1. Link Views
        canvasContainer = findViewById(R.id.canvasContainer);
        btnAddBox = findViewById(R.id.btnAddBox);
        btnShuffleAll = findViewById(R.id.btnShuffleAll);
        btnBack = findViewById(R.id.btnBack);

        btnHistory = findViewById(R.id.btnHistory);
        historyContainer = findViewById(R.id.historyContainer);
        btnCloseHistory = findViewById(R.id.btnCloseHistory);
        rvHistory = findViewById(R.id.rvHistory);

        // 2. Setup History Recycler
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        historyAdapter = new HistoryAdapter(historyList);
        rvHistory.setAdapter(historyAdapter);

        // 3. Listeners
        btnBack.setOnClickListener(v -> finish());
        btnAddBox.setOnClickListener(v -> showAddBoxMenu());
        btnShuffleAll.setOnClickListener(v -> shuffleAllBoxes());
        btnHistory.setOnClickListener(v -> historyContainer.setVisibility(View.VISIBLE));
        btnCloseHistory.setOnClickListener(v -> historyContainer.setVisibility(View.GONE));

        // 4. Load Data
        loadClothesData();
    }

    // --- DATA LOADING (Updated for "items" node) ---
    private void loadClothesData() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Pointing to "items" based on your Firebase JSON
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("items");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allItems.clear();
                for (DataSnapshot itemSnap : snapshot.getChildren()) {
                    String url = itemSnap.child("imageUrl").getValue(String.class);
                    String cat = itemSnap.child("category").getValue(String.class);
                    String sub = itemSnap.child("subCategory").getValue(String.class);

                    if (url != null) {
                        // Store category/subCategory in lowercase for easier matching
                        allItems.add(new ClothingItem(
                                itemSnap.getKey(),
                                url,
                                cat != null ? cat.toLowerCase() : "",
                                sub != null ? sub.toLowerCase() : ""
                        ));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(H6_RecommendationActivity.this, "Failed to load items", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- CORE LOGIC: ADD MOVABLE BOX ---

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (value * density + 0.5f);
    }

    private void showAddBoxMenu() {
        PopupMenu popup = new PopupMenu(H6_RecommendationActivity.this, btnAddBox);

        popup.getMenu().add("Hat");
        popup.getMenu().add("Accessories");
        popup.getMenu().add("Outer");
        popup.getMenu().add("Top");
        popup.getMenu().add("Bag");
        popup.getMenu().add("Bottom");
        popup.getMenu().add("Shoes");
        popup.getMenu().add("Dress");

        popup.setOnMenuItemClickListener(item -> {
            String category = item.getTitle().toString();
            addMovableBox(category);
            return true;
        });

        popup.show();
    }

    private void resizeBox(CardView card, String category) {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;

        class BoxConfig {
            float widthRatio, heightRatio, aspect;
            BoxConfig(float w, float h, float a) {
                this.widthRatio = w; this.heightRatio = h; this.aspect = a;
            }
        }

        BoxConfig cfg;
        switch (category.toLowerCase()) {
            case "hat":         cfg = new BoxConfig(0.25f, 0.15f, 1.5f); break;
            case "accessories": cfg = new BoxConfig(0.25f, 0.25f, 1.0f); break;
            case "outer":       cfg = new BoxConfig(0.40f, 0.50f, 0.85f); break;
            case "top":         cfg = new BoxConfig(0.40f, 0.45f, 0.85f); break;
            case "bag":         cfg = new BoxConfig(0.30f, 0.30f, 0.9f); break;
            case "bottom":      cfg = new BoxConfig(0.40f, 0.55f, 0.75f); break;
            case "shoes":       cfg = new BoxConfig(0.35f, 0.20f, 1.6f); break;
            default:            cfg = new BoxConfig(0.40f, 0.45f, 0.85f); break;
        }

        int w = (int) (screenWidth * cfg.widthRatio);
        int h = (cfg.aspect > 0) ? (int) (w / cfg.aspect) : (int) (screenHeight * cfg.heightRatio);

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) card.getLayoutParams();
        if (params == null) {
            params = new FrameLayout.LayoutParams(w, h);
            params.leftMargin = 50;
            params.topMargin = 50;
        } else {
            params.width = w;
            params.height = h;
        }
        card.setLayoutParams(params);
    }

    private List<String> getSubTags(String category) {
        switch (category) {
            case "Hat": return Arrays.asList("hat", "cap", "beanie", "bucket hat", "beret");
            case "Accessories": return Arrays.asList("accessory", "belt", "scarf", "glasses", "watch");
            case "Outer": return Arrays.asList("jacket", "coat", "hoodie", "blazer", "cardigan");
            case "Top": return Arrays.asList("shirt", "tshirt", "longsleeve", "blouse", "hoodie", "tanktop");
            case "Bag": return Arrays.asList("bag", "handbag", "backpack", "tote", "purse");
            case "Bottom": return Arrays.asList("pants", "jeans", "shorts", "skirt", "trousers");
            case "Shoes": return Arrays.asList("shoes", "sneakers", "boots", "heels", "sandals");
            case "Dress": return Arrays.asList("dress", "gown", "casual dress");
            default: return new ArrayList<>();
        }
    }

    // --- NEW: Image Refresh Logic ---
    private void refreshBoxImage(CardView card) {
        Object[] tags = (Object[]) card.getTag();
        // tags[1] is boolean isLocked
        if ((boolean) tags[1]) return;

        List<String> filters = (List<String>) tags[0];
        String currentFilter = filters.get(0).toLowerCase(); // e.g., "top" or "shirt"

        // 1. Filter list based on main category OR subcategory
        List<ClothingItem> candidates = new ArrayList<>();
        for (ClothingItem item : allItems) {
            // If filter is "top", it matches item.category
            // If filter is "shirt", it matches item.subCategory
            if (item.category.equals(currentFilter) || item.subCategory.equals(currentFilter)) {
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
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void addMovableBox(String startCategory) {
        CardView card = new CardView(this);
        card.setVisibility(View.INVISIBLE);

        card.setLayoutParams(new FrameLayout.LayoutParams(0, 0));
        card.setRadius(24f);
        card.setCardElevation(10f);
        card.setCardBackgroundColor(Color.WHITE);

        FrameLayout innerLayout = new FrameLayout(this);

        // Image View
        ImageView img = new ImageView(this);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP); // Changed to CENTER_CROP
        img.setImageResource(android.R.drawable.ic_menu_gallery);
        img.setAlpha(0.3f);
        innerLayout.addView(img);

        // Filter Label
        TextView tvFilter = new TextView(this);
        String displayTitle = startCategory.substring(0, 1).toUpperCase() + startCategory.substring(1);
        tvFilter.setText("Filter: " + displayTitle);
        tvFilter.setBackgroundColor(Color.parseColor("#80000000"));
        tvFilter.setTextColor(Color.WHITE);
        tvFilter.setTextSize(10f);
        tvFilter.setPadding(8, 4, 8, 4);
        FrameLayout.LayoutParams tvParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tvParams.gravity = Gravity.TOP | Gravity.START;
        tvFilter.setLayoutParams(tvParams);
        innerLayout.addView(tvFilter);

        // Lock Icon
        ImageButton btnLock = new ImageButton(this);
        btnLock.setImageResource(android.R.drawable.ic_menu_edit);
        btnLock.setBackgroundColor(Color.TRANSPARENT);
        FrameLayout.LayoutParams lockParams = new FrameLayout.LayoutParams(60, 60);
        lockParams.gravity = Gravity.TOP | Gravity.END;
        btnLock.setLayoutParams(lockParams);
        innerLayout.addView(btnLock);

        card.addView(innerLayout);
        resizeBox(card, startCategory);

        // Initialize Data Tag
        List<String> initialFilters = new ArrayList<>();
        initialFilters.add(startCategory.toLowerCase());
        card.setTag(new Object[]{initialFilters, false, null});

        // Popup Menu for Filters
        tvFilter.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(H6_RecommendationActivity.this, v);
            List<String> subTags = getSubTags(startCategory);

            popup.getMenu().add("All " + startCategory);
            for (String tag : subTags) {
                String displayTag = tag.substring(0, 1).toUpperCase() + tag.substring(1);
                popup.getMenu().add(displayTag);
            }

            popup.setOnMenuItemClickListener(item -> {
                String selected = item.getTitle().toString();
                if (selected.startsWith("All ")) selected = startCategory;

                tvFilter.setText("Filter: " + selected);

                Object[] currentData = (Object[]) card.getTag();
                List<String> f = (List<String>) currentData[0];
                f.clear();
                f.add(selected.toLowerCase());

                // Refresh immediately upon changing filter
                refreshBoxImage(card);
                return true;
            });
            popup.show();
        });

        // Lock Logic
        btnLock.setOnClickListener(v -> {
            Object[] tags = (Object[]) card.getTag();
            boolean currentLock = (boolean) tags[1];
            tags[1] = !currentLock;

            if ((boolean) tags[1]) {
                btnLock.setImageResource(android.R.drawable.ic_lock_lock);
                btnLock.setColorFilter(Color.RED);
            } else {
                btnLock.setImageResource(android.R.drawable.ic_menu_edit);
                btnLock.setColorFilter(null);
            }
        });

        // Drag Logic
        card.setOnTouchListener(new View.OnTouchListener() {
            float dX, dY;
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                Object[] tags = (Object[]) view.getTag();
                if ((boolean) tags[1]) return false; // Locked

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = view.getX() - event.getRawX();
                        dY = view.getY() - event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        view.animate()
                                .x(event.getRawX() + dX)
                                .y(event.getRawY() + dY)
                                .setDuration(0)
                                .start();
                        break;
                    default:
                        return false;
                }
                return true;
            }
        });

        canvasContainer.addView(card);
        card.setVisibility(View.VISIBLE);

        // Load Initial Image
        refreshBoxImage(card);
    }

    private void shuffleAllBoxes() {
        List<String> currentUrls = new ArrayList<>();

        for (int i = 0; i < canvasContainer.getChildCount(); i++) {
            View child = canvasContainer.getChildAt(i);
            if (child instanceof CardView) {
                CardView card = (CardView) child;
                refreshBoxImage(card);

                Object[] tags = (Object[]) card.getTag();
                String url = (String) tags[2];
                if (url != null) currentUrls.add(url);
            }
        }

        if (!currentUrls.isEmpty()) {
            historyList.add(0, new HistorySnapshot(new ArrayList<>(currentUrls)));
            historyAdapter.notifyDataSetChanged();
        }
    }

    // --- ADD THIS CLASS INSIDE H6_RecommendationActivity ---

    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private final List<HistorySnapshot> snapshots;

        public HistoryAdapter(List<HistorySnapshot> snapshots) {
            this.snapshots = snapshots;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // NOTE: You need an XML layout file named 'item_history.xml' in res/layout.
            // If you name it something else, change R.layout.item_history below.
            View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.h6_recommendation, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            HistorySnapshot snapshot = snapshots.get(position);
            holder.bind(snapshot);
        }

        @Override
        public int getItemCount() {
            return snapshots.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            // Define your views here (e.g., TextView for timestamp, ImageView for preview)
            TextView tvTimestamp;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                // Ensure these IDs exist in your item_history.xml
                tvTimestamp = itemView.findViewById(R.id.btnHistory);
            }

            @SuppressLint("SetTextI18n")
            public void bind(HistorySnapshot snapshot) {
                // Convert timestamp to readable date if needed
                tvTimestamp.setText("Saved Outfit: " + snapshot.timestamp);

                // Logic to display the images in the snapshot goes here.
                // For example, loading the first image into an ImageView:
                // if (!snapshot.imageUrls.isEmpty()) {
                //     Glide.with(itemView.getContext()).load(snapshot.imageUrls.get(0)).into(imageView);
                // }
            }
        }
    }

}

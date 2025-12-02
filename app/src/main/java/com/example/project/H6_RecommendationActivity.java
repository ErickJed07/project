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

    // --- Data not Model ---
    private static class ClothingItem {
        String id, url, category;
        ClothingItem(String id, String url, String category) {
            this.id = id; this.url = url; this.category = category;
        }
    }

    // History Model: Stores a snapshot of URLs generated
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

        btnShuffleAll.setOnClickListener(v -> shuffleAllBoxes());

        btnHistory.setOnClickListener(v -> historyContainer.setVisibility(View.VISIBLE));
        btnCloseHistory.setOnClickListener(v -> historyContainer.setVisibility(View.GONE));

        // 4. Load Data
        loadClothesData();
    }

    private void loadClothesData() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("categories");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allItems.clear();
                for (DataSnapshot cat : snapshot.getChildren()) {
                    if (cat.hasChild("photos")) {
                        for (DataSnapshot photo : cat.child("photos").getChildren()) {
                            String url = photo.child("url").getValue(String.class);
                            String tag = photo.child("tagCloth").getValue(String.class);
                            if (url != null && tag != null) {
                                allItems.add(new ClothingItem(photo.getKey(), url, tag.toLowerCase()));
                            }
                        }
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // --- CORE LOGIC: ADD MOVABLE BOX ---

    private void resizeBox(CardView card, String category) {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;

        float widthRatio, heightRatio, aspect;

        switch (category.toLowerCase()) {
            case "hatsd":         widthRatio = 0.25f; heightRatio = 0.15f; aspect = 1.5f; break;
            case "accessories": widthRatio = 0.25f; heightRatio = 0.25f; aspect = 1.0f; break;
            case "outer":       widthRatio = 0.40f; heightRatio = 0.50f; aspect = 0.85f; break;
            case "top":         widthRatio = 0.40f; heightRatio = 0.45f; aspect = 0.85f; break;
            case "bag":         widthRatio = 0.30f; heightRatio = 0.30f; aspect = 0.9f; break;
            case "bottom":      widthRatio = 0.40f; heightRatio = 0.55f; aspect = 0.75f; break;
            case "shoes":       widthRatio = 0.35f; heightRatio = 0.20f; aspect = 1.6f; break;
            default:            widthRatio = 0.40f; heightRatio = 0.45f; aspect = 0.85f; break;
        }

        int w = (int) (screenWidth * widthRatio);
        int h = (aspect > 0) ? (int) (w / aspect) : (int) (screenHeight * heightRatio);

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

    @SuppressLint("ClickableViewAccessibility")
    private void addMovableBox(String startCategory) {
        // Create CardView Container
        CardView card = new CardView(this);
        card.setVisibility(View.INVISIBLE);

        // Initialize layout params
        card.setLayoutParams(new FrameLayout.LayoutParams(0, 0));
        card.setRadius(24f);
        card.setCardElevation(10f);
        card.setCardBackgroundColor(Color.WHITE);

        // Internal Layout
        FrameLayout innerLayout = new FrameLayout(this);

        // Image View
        ImageView img = new ImageView(this);
        img.setScaleType(ImageView.ScaleType.FIT_CENTER);
        img.setPadding(16, 16, 16, 16);
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

        // Apply correct size immediately
        resizeBox(card, startCategory);

        List<String> initialFilters = new ArrayList<>();
        initialFilters.add(startCategory.toLowerCase());
        card.setTag(new Object[]{initialFilters, false, null});

        // Logic: Dragging (Boundary Clamped)
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
                        float newX = event.getRawX() + dX;
                        float newY = event.getRawY() + dY;

                        View parent = (View) view.getParent();
                        int pW = parent.getWidth();
                        int pH = parent.getHeight();

                        newX = Math.max(0, Math.min(newX, pW - view.getWidth()));
                        newY = Math.max(0, Math.min(newY, pH - view.getHeight()));

                        // More efficient than animate() for dragging
                        view.setX(newX);
                        view.setY(newY);
                        break;
                    default: return false;
                }
                return true;
            }
        });

        // Logic: Lock/Unlock
        btnLock.setOnClickListener(v -> {
            Object[] tags = (Object[]) card.getTag();
            boolean currentLock = (boolean) tags[1];
            tags[1] = !currentLock;
            card.setTag(tags);

            if (!currentLock) {
                btnLock.setImageResource(android.R.drawable.ic_lock_lock);
                btnLock.setColorFilter(Color.RED);
                Toast.makeText(this, "Locked", Toast.LENGTH_SHORT).show();
            } else {
                btnLock.setImageResource(android.R.drawable.ic_menu_edit);
                btnLock.setColorFilter(Color.BLACK);
                Toast.makeText(this, "Unlocked", Toast.LENGTH_SHORT).show();
            }
        });

        // Add and Show
        canvasContainer.addView(card);
        card.setVisibility(View.VISIBLE);
    }

    private void shuffleAllBoxes() {
        List<String> snapshotUrls = new ArrayList<>();
        boolean anyBoxUpdated = false;

        for (int i = 0; i < canvasContainer.getChildCount(); i++) {
            View child = canvasContainer.getChildAt(i);
            if (child instanceof CardView) {
                CardView card = (CardView) child;
                Object[] tags = (Object[]) card.getTag(); // [0]=List<String>, [1]=locked, [2]=url

                // Check if it's locked
                boolean isLocked = (boolean) tags[1];
                if (isLocked) {
                    // If locked, keep existing image in snapshot history
                    if (tags[2] != null) snapshotUrls.add((String) tags[2]);
                    continue;
                }

                @SuppressWarnings("unchecked")
                List<String> filterKeywords = (List<String>) tags[0];

                // Find matching items
                List<ClothingItem> candidates = new ArrayList<>();

                // Check against all items in database
                for (ClothingItem item : allItems) {
                    if (filterKeywords.contains("all")) {
                        candidates.add(item);
                    } else {
                        for (String keyword : filterKeywords) {
                            if (item.category.toLowerCase().contains(keyword.toLowerCase())) {
                                candidates.add(item);
                                break;
                            }
                        }
                    }
                }

                if (!candidates.isEmpty()) {
                    // Pick Random
                    ClothingItem picked = candidates.get(new Random().nextInt(candidates.size()));

                    // Update UI
                    FrameLayout inner = (FrameLayout) card.getChildAt(0);
                    ImageView img = (ImageView) inner.getChildAt(0); // Index 0 is image

                    Glide.with(this).load(picked.url).into(img);
                    img.setAlpha(1.0f); // Remove dimming

                    // Update Tag with URL for history
                    tags[2] = picked.url;
                    snapshotUrls.add(picked.url);
                    anyBoxUpdated = true;
                }
            }
        }

        if (anyBoxUpdated) {
            // Add to History
            historyList.add(0, new HistorySnapshot(snapshotUrls)); // Add to top
            historyAdapter.notifyItemInserted(0);
            rvHistory.scrollToPosition(0);
        } else {
            Toast.makeText(this, "No items found for these filters", Toast.LENGTH_SHORT).show();
        }
    }

    // --- HISTORY ADAPTER ---
    private static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        List<HistorySnapshot> history;

        HistoryAdapter(List<HistorySnapshot> history) { this.history = history; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            RecyclerView rv = new RecyclerView(parent.getContext());
            rv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 250));
            rv.setLayoutManager(new LinearLayoutManager(parent.getContext(), LinearLayoutManager.HORIZONTAL, false));
            return new ViewHolder(rv);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            HistorySnapshot snapshot = history.get(position);
            holder.rvMini.setAdapter(new MiniImageAdapter(snapshot.imageUrls));
        }

        @Override
        public int getItemCount() { return history.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            RecyclerView rvMini;
            ViewHolder(View itemView) {
                super(itemView);
                rvMini = (RecyclerView) itemView;
            }
        }
    }

    private static class MiniImageAdapter extends RecyclerView.Adapter<MiniImageAdapter.MiniHolder> {
        List<String> urls;
        MiniImageAdapter(List<String> urls) { this.urls = urls; }

        @NonNull
        @Override
        public MiniHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView iv = new ImageView(parent.getContext());
            iv.setLayoutParams(new ViewGroup.LayoutParams(150, 150));
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            iv.setPadding(8,8,8,8);
            return new MiniHolder(iv);
        }

        @Override
        public void onBindViewHolder(@NonNull MiniHolder holder, int position) {
            Glide.with(holder.itemView).load(urls.get(position)).into((ImageView)holder.itemView);
        }

        @Override
        public int getItemCount() { return urls.size(); }

        static class MiniHolder extends RecyclerView.ViewHolder {
            MiniHolder(View v) { super(v); }
        }
    }
}

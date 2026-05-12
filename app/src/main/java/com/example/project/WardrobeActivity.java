package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class WardrobeActivity extends AppCompatActivity {

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView rvCategories, rvScheduledEvents, rvLaundryItems;
    private WardrobeCategoryHorizontalAdapter adapter;
    private E_Calendar_EventAdapter scheduledEventsAdapter;
    private LaundryAdapter laundryAdapter;
    private List<ViewCategoriesActivity.CategoryModel> categoryList = new ArrayList<>();
    private final List<E_Calendar_Event> scheduledEventsList = new ArrayList<>();
    private final List<LaundryItem> laundryItemsList = new ArrayList<>();
    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;
    private TextView tvTotalItems, tvUsedItems, tvScheduledLabel, tvLaundryLabel;
    private TextView tvLaundryVal;
    private View cvNoOutfits;
    private TextView tvNoOutfitsTitle, tvNoOutfitsDesc;
    private ImageView ivGenderIcon;
    private boolean isWoman = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wardrobe);

        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference("Users");

        tvTotalItems = findViewById(R.id.tv_total_items_val);
        tvUsedItems = findViewById(R.id.tv_used_items_val);
        tvLaundryVal = findViewById(R.id.tv_laundry_val);
        tvScheduledLabel = findViewById(R.id.tv_scheduled_label);
        tvLaundryLabel = findViewById(R.id.tv_laundry_label);
        cvNoOutfits = findViewById(R.id.cv_no_outfits);
        tvNoOutfitsTitle = findViewById(R.id.tv_no_outfits_title);
        tvNoOutfitsDesc = findViewById(R.id.tv_no_outfits_desc);
        ivGenderIcon = findViewById(R.id.iv_gender_icon);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        swipeRefreshLayout.setOnRefreshListener(this::refreshData);

        rvCategories = findViewById(R.id.rv_categories);
        rvCategories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        rvScheduledEvents = findViewById(R.id.rv_scheduled_outfits);
        rvScheduledEvents.setLayoutManager(new LinearLayoutManager(this));
        scheduledEventsAdapter = new E_Calendar_EventAdapter(scheduledEventsList, (event, position) -> {
            // No delete action needed here by default, or we can implement it
        });
        rvScheduledEvents.setAdapter(scheduledEventsAdapter);

        rvLaundryItems = findViewById(R.id.rv_laundry_items);
        rvLaundryItems.setLayoutManager(new LinearLayoutManager(this));
        laundryAdapter = new LaundryAdapter(laundryItemsList, this::markAsCleaned);
        rvLaundryItems.setAdapter(laundryAdapter);

        adapter = new WardrobeCategoryHorizontalAdapter(this, categoryList);
        rvCategories.setAdapter(adapter);

        findViewById(R.id.tv_view_categories).setOnClickListener(v -> {
            Intent intent = new Intent(WardrobeActivity.this, ViewCategoriesActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.tv_view_calendar).setOnClickListener(v -> {
            Intent intent = new Intent(WardrobeActivity.this, E_CalendarActivity.class);
            startActivity(intent);
        });

        fetchGenderAndLoadCategories();
        loadScheduledOutfits();
        loadLaundryStatus();
        loadLaundryItems();
        deduplicateUsedClothes();
        checkAndReturnUsedItems();
    }

    private void refreshData() {
        fetchGenderAndLoadCategories();
        loadScheduledOutfits();
        loadLaundryStatus();
        loadLaundryItems();
        deduplicateUsedClothes();
        checkAndReturnUsedItems();
        
        // Hide refresh animation after a short delay or after data is loaded
        // For simplicity, we'll hide it after 1.5 seconds or when categories are loaded
        swipeRefreshLayout.postDelayed(() -> {
            if (swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
        }, 1500);
    }

    private void loadLaundryStatus() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        dbRef.child(uid).child("lastLaundryAt").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Long lastLaundry = snapshot.getValue(Long.class);
                if (lastLaundry == null || tvLaundryVal == null) {
                    if (tvLaundryVal != null) tvLaundryVal.setText("None yet");
                    return;
                }

                long now = System.currentTimeMillis();
                long diff = now - lastLaundry;
                
                long days = diff / (24 * 60 * 60 * 1000);
                
                if (days == 0) {
                    tvLaundryVal.setText("Today");
                } else if (days == 1) {
                    tvLaundryVal.setText("Yesterday");
                } else {
                    tvLaundryVal.setText(days + " days ago");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void deduplicateUsedClothes() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        DatabaseReference usedCatRef = FirebaseDatabase.getInstance().getReference("Users")
                .child(uid).child("categories").child("used_clothes").child("photos");

        usedCatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                java.util.Map<String, String> seenUrls = new java.util.HashMap<>();
                java.util.List<DatabaseReference> toDelete = new java.util.ArrayList<>();

                for (DataSnapshot photoSnap : snapshot.getChildren()) {
                    String url = photoSnap.child("imageUrl").getValue(String.class);
                    if (url == null) url = photoSnap.child("url").getValue(String.class);

                    if (url != null) {
                        if (seenUrls.containsKey(url)) {
                            toDelete.add(photoSnap.getRef());
                        } else {
                            seenUrls.put(url, photoSnap.getKey());
                        }
                    }
                }

                for (DatabaseReference ref : toDelete) {
                    ref.removeValue();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void checkAndReturnUsedItems() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        DatabaseReference usedCatRef = FirebaseDatabase.getInstance().getReference("Users")
                .child(uid).child("categories").child("used_clothes").child("photos");

        usedCatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long now = System.currentTimeMillis();
                long sevenDaysMs = 7L * 24 * 60 * 60 * 1000;

                for (DataSnapshot photoSnap : snapshot.getChildren()) {
                    Long movedAt = photoSnap.child("movedToUsedAt").getValue(Long.class);
                    String originalCat = photoSnap.child("originalCategory").getValue(String.class);
                    
                    if (movedAt != null && originalCat != null) {
                        if (now - movedAt >= sevenDaysMs) {
                            String itemKey = photoSnap.getKey();

                            // Move back to original category using original key
                            DatabaseReference targetRef = FirebaseDatabase.getInstance().getReference("Users")
                                    .child(uid).child("categories").child(originalCat).child("photos");
                            
                            java.util.Map<String, Object> data = (java.util.Map<String, Object>) photoSnap.getValue();
                            if (data != null && itemKey != null) {
                                java.util.Map<String, Object> cleanData = new java.util.HashMap<>(data);
                                cleanData.remove("movedToUsedAt");
                                cleanData.remove("originalCategory");
                                
                                targetRef.child(itemKey).setValue(cleanData).addOnSuccessListener(aVoid -> {
                                    photoSnap.getRef().removeValue();
                                    FirebaseDatabase.getInstance().getReference("Users").child(uid).child("lastLaundryAt").setValue(now);
                                });
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadLaundryItems() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        DatabaseReference usedCatRef = dbRef.child(uid).child("categories").child("used_clothes").child("photos");
        DatabaseReference eventsRef = dbRef.child(uid).child("Events");

        // Use a ValueEventListener to listen to both sources for a complete "Used" list
        usedCatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot usedSnapshot) {
                final List<LaundryItem> combinedList = new ArrayList<>();
                final java.util.Set<String> seenIds = new java.util.HashSet<>();

                // 1. Load manually archived items from 'used_clothes'
                for (DataSnapshot photoSnap : usedSnapshot.getChildren()) {
                    try {
                        LaundryItem item = photoSnap.getValue(LaundryItem.class);
                        if (item == null) {
                            java.util.Map<String, Object> map = (java.util.Map<String, Object>) photoSnap.getValue();
                            if (map != null) item = parseLaundryItemManually(photoSnap.getKey(), map);
                        } else {
                            item.setId(photoSnap.getKey());
                        }

                        if (item != null && item.getId() != null) {
                            if (item.getImageUrl() == null) item.setImageUrl(photoSnap.child("url").getValue(String.class));
                            combinedList.add(item);
                            seenIds.add(item.getId());
                        }
                    } catch (Exception e) {
                        android.util.Log.e("WardrobeActivity", "Error parsing used folder item", e);
                    }
                }

                // 2. Load items from Today's and Past events (Virtual Used Clothes)
                eventsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot eventSnapshot) {
                        java.util.Calendar cal = java.util.Calendar.getInstance();
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                        String todayStr = sdf.format(cal.getTime());

                        for (DataSnapshot eventSnap : eventSnapshot.getChildren()) {
                            E_Calendar_Event event = eventSnap.getValue(E_Calendar_Event.class);
                            // Include Today's outfits AND Past outfits
                            if (event != null && event.getDate() != null && event.getDate().compareTo(todayStr) <= 0) {
                                if (event.getItems() != null) {
                                    for (ClothingItem cItem : event.getItems()) {
                                        if (cItem != null && cItem.getId() != null && !seenIds.contains(cItem.getId())) {
                                            LaundryItem lItem = convertToLaundryItem(cItem);
                                            if (lItem.getMovedToUsedAt() == 0) lItem.setMovedToUsedAt(event.getTimestamp());
                                            combinedList.add(lItem);
                                            seenIds.add(lItem.getId());
                                        }
                                    }
                                }
                            }
                        }
                        
                        laundryItemsList.clear();
                        laundryItemsList.addAll(combinedList);
                        laundryAdapter.notifyDataSetChanged();

                        if (tvUsedItems != null) {
                            tvUsedItems.setText(String.valueOf(laundryItemsList.size()));
                        }

                        if (laundryItemsList.isEmpty()) {
                            rvLaundryItems.setVisibility(View.GONE);
                        } else {
                            rvLaundryItems.setVisibility(View.VISIBLE);
                        }
                        updateScheduleEmptyState();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private LaundryItem convertToLaundryItem(ClothingItem cItem) {
        LaundryItem lItem = new LaundryItem();
        lItem.setId(cItem.getId());
        lItem.setImageUrl(cItem.getImageUrl());
        lItem.setCategoryId(cItem.getCategoryId());
        lItem.setSize(cItem.getSize());
        lItem.setSeason(cItem.getSeason());
        lItem.setColor(cItem.getColor());
        lItem.setOccasions(cItem.getOccasions());
        lItem.setFavorite(cItem.isFavorite());
        lItem.setTimestamp(cItem.getTimestamp());
        lItem.setOriginalCategory(cItem.getCategoryId());
        return lItem;
    }

    private void updateScheduleEmptyState() {
        if (scheduledEventsList.isEmpty() && laundryItemsList.isEmpty()) {
            cvNoOutfits.setVisibility(View.VISIBLE);
            tvNoOutfitsTitle.setText(R.string.no_outfits_scheduled);
            tvNoOutfitsDesc.setText(R.string.plan_your_looks);
        } else {
            cvNoOutfits.setVisibility(View.GONE);
        }
    }

    private LaundryItem parseLaundryItemManually(String id, java.util.Map<String, Object> map) {
        LaundryItem item = new LaundryItem();
        item.setId(id);
        
        // Very lenient parsing to ensure all 6 items show up
        String url = (String) map.get("imageUrl");
        if (url == null) url = (String) map.get("url");
        item.setImageUrl(url);
        
        item.setCategoryId((String) map.get("categoryId"));
        item.setSize((String) map.get("size"));
        item.setSeason((String) map.get("season"));
        item.setColor((String) map.get("color"));
        
        String orig = (String) map.get("originalCategory");
        if (orig == null) orig = (String) map.get("category"); // Check alternative key
        item.setOriginalCategory(orig);
        
        Object movedAt = map.get("movedToUsedAt");
        if (movedAt instanceof Long) {
            item.setMovedToUsedAt((Long) movedAt);
        } else if (movedAt instanceof Double) {
            item.setMovedToUsedAt(((Double) movedAt).longValue());
        } else if (movedAt instanceof String) {
            try { item.setMovedToUsedAt(Long.parseLong((String) movedAt)); } catch (Exception ignored) {}
        }
        
        Object occ = map.get("occasions");
        if (occ instanceof java.util.List) {
            item.setOccasions((java.util.List<String>) occ);
        }
        return item;
    }

    private void markAsCleaned(LaundryItem item) {
        if (mAuth.getCurrentUser() == null || item.getId() == null || item.getOriginalCategory() == null) {
            Toast.makeText(this, "Cannot mark as cleaned: missing info", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();
        DatabaseReference usedRef = dbRef.child(uid).child("categories").child("used_clothes").child("photos").child(item.getId());
        DatabaseReference targetRef = dbRef.child(uid).child("categories").child(item.getOriginalCategory()).child("photos").child(item.getId());

        // Copy back to original category
        java.util.Map<String, Object> cleanData = new java.util.HashMap<>();
        cleanData.put("id", item.getId());
        cleanData.put("imageUrl", item.getImageUrl());
        cleanData.put("categoryId", item.getOriginalCategory());
        cleanData.put("size", item.getSize());
        cleanData.put("season", item.getSeason());
        cleanData.put("color", item.getColor());
        cleanData.put("occasions", item.getOccasions());
        cleanData.put("favorite", item.isFavorite());
        cleanData.put("timestamp", System.currentTimeMillis());

        targetRef.setValue(cleanData).addOnSuccessListener(aVoid -> {
            usedRef.removeValue();
            Toast.makeText(this, "Item marked as cleaned", Toast.LENGTH_SHORT).show();
            dbRef.child(uid).child("lastLaundryAt").setValue(System.currentTimeMillis());
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to clean item", Toast.LENGTH_SHORT).show();
        });
    }

    private void fetchGenderAndLoadCategories() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        dbRef.child(uid).child("gender").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String gender = snapshot.getValue(String.class);
                isWoman = !"man".equalsIgnoreCase(gender);
                
                if (ivGenderIcon != null) {
                    ivGenderIcon.setImageResource(isWoman ? R.drawable.ic_female : R.drawable.ic_male);
                }
                
                loadCategories();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                loadCategories();
            }
        });
    }

    private void loadScheduledOutfits() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        DatabaseReference eventsRef = dbRef.child(uid).child("Events");
        eventsRef.addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                java.util.Calendar cal = java.util.Calendar.getInstance();
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                
                String todayStr = sdf.format(cal.getTime());
                cal.add(java.util.Calendar.DAY_OF_YEAR, 1);
                String tomorrowStr = sdf.format(cal.getTime());
                cal.add(java.util.Calendar.DAY_OF_YEAR, -2);
                String yesterdayStr = sdf.format(cal.getTime());

                List<E_Calendar_Event> todayEvents = new ArrayList<>();
                List<E_Calendar_Event> tomorrowEvents = new ArrayList<>();
                List<E_Calendar_Event> yesterdayEvents = new ArrayList<>();

                for (DataSnapshot eventSnap : snapshot.getChildren()) {
                    try {
                        E_Calendar_Event event = eventSnap.getValue(E_Calendar_Event.class);
                        if (event != null) {
                            event.setId(eventSnap.getKey());
                            if (todayStr.equals(event.getDate())) todayEvents.add(event);
                            else if (tomorrowStr.equals(event.getDate())) tomorrowEvents.add(event);
                            else if (yesterdayStr.equals(event.getDate())) yesterdayEvents.add(event);
                        }
                    } catch (Exception e) {
                        android.util.Log.e("WardrobeActivity", "Error parsing event: " + eventSnap.getKey(), e);
                    }
                }

                scheduledEventsList.clear();
                if (!todayEvents.isEmpty()) {
                    tvScheduledLabel.setText(R.string.scheduled_today);
                    scheduledEventsList.addAll(todayEvents);
                } else if (!tomorrowEvents.isEmpty()) {
                    tvScheduledLabel.setText(R.string.scheduled_tomorrow);
                    scheduledEventsList.addAll(tomorrowEvents);
                } else if (!yesterdayEvents.isEmpty()) {
                    tvScheduledLabel.setText(R.string.scheduled_yesterday);
                    scheduledEventsList.addAll(yesterdayEvents);
                } else {
                    tvScheduledLabel.setText(R.string.scheduled_outfits);
                }

                if (scheduledEventsList.isEmpty()) {
                    rvScheduledEvents.setVisibility(View.GONE);
                } else {
                    rvScheduledEvents.setVisibility(View.VISIBLE);
                    scheduledEventsAdapter.notifyDataSetChanged();
                }
                updateScheduleEmptyState();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
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
                
                // Calculate total items and load categories
                for (CategoryManager.CategoryItem fixedItem : CategoryManager.getCategories(isWoman)) {
                    // Skip 'used_clothes' as it has its own dedicated section now
                    if ("used_clothes".equals(fixedItem.id)) continue;

                    long itemCount = 0;
                    DataSnapshot categorySnapshot = snapshot.child(fixedItem.id);
                    if (categorySnapshot.exists() && categorySnapshot.hasChild("photos")) {
                        itemCount = categorySnapshot.child("photos").getChildrenCount();
                        totalItems += itemCount;
                    }
                    categoryList.add(new ViewCategoriesActivity.CategoryModel(fixedItem.id, fixedItem.name, fixedItem.iconRes, (int) itemCount));
                }

                if (tvTotalItems != null) {
                    tvTotalItems.setText(String.valueOf(totalItems));
                }
                adapter.updateList(new ArrayList<>(categoryList));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(WardrobeActivity.this, "Failed to load categories", Toast.LENGTH_SHORT).show();
            }
        });
    }


    public void onButtonClicked(View view) {
        Intent intent = null;
        int viewId = view.getId();
        if (viewId == R.id.home_menu) {
            intent = new Intent(this, D_FeedActivity.class);
        } else if (viewId == R.id.wardrobe_menu) {
            // Already here
            return;
        } else if (viewId == R.id.calendar_menu) {
            intent = new Intent(this, E_CalendarActivity.class);
        } else if (viewId == R.id.ai_menu) {
            intent = new Intent(this, AiActivity.class);
        } else if (viewId == R.id.profile_menu) {
            intent = new Intent(this, I_ProfileActivity.class);
        }
        
        if (intent != null) {
            startActivity(intent);
            finish();
        }
    }
}

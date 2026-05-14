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
    private TextView tvTotalItems, tvUsedItems, tvScheduledLabel, tvLaundryLabel, tvWashAll;
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
        tvWashAll = findViewById(R.id.tv_wash_all);
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
        laundryAdapter = new LaundryAdapter(laundryItemsList, this::unarchiveItem);
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

        if (tvWashAll != null) {
            tvWashAll.setOnClickListener(v -> washAllItems());
        }

        fetchGenderAndLoadCategories();
        loadScheduledOutfits();
        loadLaundryStatus();
        loadLaundryItems();
        deduplicateUsedClothes();
        checkAndReturnUsedItems();
        checkAndMovePastOutfitItems();
    }

    private void checkAndMovePastOutfitItems() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        DatabaseReference eventsRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("Events");

        java.util.Calendar cal = java.util.Calendar.getInstance();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        String todayStr = sdf.format(cal.getTime());

        eventsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot eventSnap : snapshot.getChildren()) {
                    E_Calendar_Event event = eventSnap.getValue(E_Calendar_Event.class);
                    if (event != null && event.getDate() != null && event.getDate().compareTo(todayStr) < 0) {
                        DataSnapshot itemsSnap = eventSnap.child("items");
                        if (itemsSnap.exists()) {
                            for (DataSnapshot itemSnap : itemsSnap.getChildren()) {
                                ClothingItem item = itemSnap.getValue(ClothingItem.class);
                                if (item != null && item.getCategoryId() != null && !"used_clothes".equals(item.getCategoryId())) {
                                    moveItemToUsed(uid, item, itemSnap.getRef());
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void moveItemToUsed(String uid, ClothingItem item, DatabaseReference eventItemRef) {
        DatabaseReference usedRootRef = FirebaseDatabase.getInstance().getReference("Users")
                .child(uid).child("categories").child("used_clothes");

        usedRootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot usedSnapshot) {
                // 1. Check if already in used_clothes (anywhere under it)
                boolean alreadyInUsed = false;
                DataSnapshot photosNode = usedSnapshot.hasChild("photos") ? usedSnapshot.child("photos") : usedSnapshot;
                for (DataSnapshot photoSnap : photosNode.getChildren()) {
                    if ("photos".equals(photoSnap.getKey())) continue;
                    String url = photoSnap.child("imageUrl").getValue(String.class);
                    if (url == null) url = photoSnap.child("url").getValue(String.class);
                    if (url != null && url.equals(item.getImageUrl())) {
                        alreadyInUsed = true;
                        break;
                    }
                }

                if (alreadyInUsed) {
                    eventItemRef.child("categoryId").setValue("used_clothes");
                    eventItemRef.child("originalCategory").setValue(item.getCategoryId());
                    return;
                }

                // 2. Not in used_clothes, find it in its old category
                DatabaseReference oldCatRef = FirebaseDatabase.getInstance().getReference("Users")
                        .child(uid).child("categories").child(item.getCategoryId()).child("photos");

                oldCatRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot oldSnap) {
                        for (DataSnapshot photoSnap : oldSnap.getChildren()) {
                            String url = photoSnap.child("imageUrl").getValue(String.class);
                            if (url == null) url = photoSnap.child("url").getValue(String.class);

                            if (url != null && url.equals(item.getImageUrl())) {
                                String itemKey = photoSnap.getKey();
                                Object data = photoSnap.getValue();

                                if (data instanceof java.util.Map && itemKey != null) {
                                    @SuppressWarnings("unchecked")
                                    java.util.Map<String, Object> dataMap = (java.util.Map<String, Object>) data;
                                    dataMap.put("originalCategory", item.getCategoryId());
                                    dataMap.put("movedToUsedAt", System.currentTimeMillis());
                                    dataMap.put("categoryId", "used_clothes");

                                    // Save to used_clothes/photos sub-node for consistency
                                    usedRootRef.child("photos").child(itemKey).setValue(dataMap).addOnSuccessListener(aVoid -> {
                                        photoSnap.getRef().removeValue();
                                        eventItemRef.child("categoryId").setValue("used_clothes");
                                        eventItemRef.child("originalCategory").setValue(item.getCategoryId());
                                    });
                                }
                                return;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void refreshData() {
        fetchGenderAndLoadCategories();
        loadScheduledOutfits();
        loadLaundryStatus();
        loadLaundryItems();
        deduplicateUsedClothes();
        checkAndReturnUsedItems();
        checkAndMovePastOutfitItems();

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
        DatabaseReference usedCatRef = dbRef.child(uid).child("categories").child("used_clothes");

        // Source strictly from the used_clothes category in Firebase
        usedCatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot usedSnapshot) {
                final List<LaundryItem> combinedList = new ArrayList<>();
                
                DataSnapshot photosNode = usedSnapshot.hasChild("photos") ? usedSnapshot.child("photos") : usedSnapshot;
                for (DataSnapshot photoSnap : photosNode.getChildren()) {
                    String key = photoSnap.getKey();
                    if ("photos".equals(key)) continue;
                    
                    try {
                        LaundryItem item = photoSnap.getValue(LaundryItem.class);
                        if (item != null) {
                            item.setId(key);
                            if (item.getImageUrl() == null) item.setImageUrl(photoSnap.child("url").getValue(String.class));
                            combinedList.add(item);
                        } else {
                            // Manual fallback if item is null
                            java.util.Map<String, Object> map = (java.util.Map<String, Object>) photoSnap.getValue();
                            if (map != null) combinedList.add(parseLaundryItemManually(key, map));
                        }
                    } catch (Exception e) {
                        android.util.Log.e("WardrobeActivity", "Error parsing laundry item: " + key, e);
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
                    if (tvWashAll != null) tvWashAll.setVisibility(View.GONE);
                } else {
                    rvLaundryItems.setVisibility(View.VISIBLE);
                    if (tvWashAll != null) tvWashAll.setVisibility(View.VISIBLE);
                }
                updateScheduleEmptyState();
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
        // This is now handled inside loadScheduledOutfits and loadLaundryItems
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

    private void washAllItems() {
        if (laundryItemsList.isEmpty()) return;

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Wash All Clothes")
                .setMessage("Are you sure you want to return all " + laundryItemsList.size() + " items to your wardrobe?")
                .setPositiveButton("Wash All", (dialog, which) -> {
                    List<LaundryItem> itemsToWash = new ArrayList<>(laundryItemsList);
                    for (LaundryItem item : itemsToWash) {
                        unarchiveItem(item);
                    }
                    Toast.makeText(this, "Washing process started", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void unarchiveItem(LaundryItem item) {
        if (mAuth.getCurrentUser() == null || item.getId() == null) {
            Toast.makeText(this, "Cannot wash item: missing ID", Toast.LENGTH_SHORT).show();
            return;
        }

        String targetCategory = item.getOriginalCategory();
        if (targetCategory == null || targetCategory.isEmpty() || "used_clothes".equals(targetCategory)) {
            // Fallback for older items without metadata
            targetCategory = "Tops"; 
        }

        String uid = mAuth.getCurrentUser().getUid();
        DatabaseReference usedRef = dbRef.child(uid).child("categories").child("used_clothes").child("photos").child(item.getId());
        DatabaseReference targetRef = dbRef.child(uid).child("categories").child(targetCategory).child("photos").child(item.getId());

        // Copy back to category
        java.util.Map<String, Object> cleanData = new java.util.HashMap<>();
        cleanData.put("id", item.getId());
        cleanData.put("imageUrl", item.getImageUrl());
        cleanData.put("categoryId", targetCategory);
        cleanData.put("size", item.getSize());
        cleanData.put("season", item.getSeason());
        cleanData.put("color", item.getColor());
        cleanData.put("occasions", item.getOccasions());
        cleanData.put("favorite", item.isFavorite());
        cleanData.put("timestamp", System.currentTimeMillis());

        final String finalCategory = targetCategory;
        targetRef.setValue(cleanData).addOnSuccessListener(aVoid -> {
            usedRef.removeValue();
            // Also try to remove from parent if it was stored directly there
            dbRef.child(uid).child("categories").child("used_clothes").child(item.getId()).removeValue();
            
            Toast.makeText(this, "Item returned to " + finalCategory, Toast.LENGTH_SHORT).show();
            dbRef.child(uid).child("lastLaundryAt").setValue(System.currentTimeMillis());
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to wash item", Toast.LENGTH_SHORT).show();
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
                cal.add(java.util.Calendar.DAY_OF_YEAR, -1);
                String yesterdayStr = sdf.format(cal.getTime());
                // For future, we just look at anything > todayStr

                List<E_Calendar_Event> todayEvents = new ArrayList<>();
                List<E_Calendar_Event> otherEvents = new ArrayList<>();

                for (DataSnapshot eventSnap : snapshot.getChildren()) {
                    try {
                        E_Calendar_Event event = eventSnap.getValue(E_Calendar_Event.class);
                        if (event != null) {
                            event.setId(eventSnap.getKey());
                            String date = event.getDate();
                            if (todayStr.equals(date)) {
                                todayEvents.add(event);
                            } else if (date != null && (date.equals(yesterdayStr) || date.compareTo(todayStr) > 0)) {
                                otherEvents.add(event);
                            }
                        }
                    } catch (Exception e) {
                        android.util.Log.e("WardrobeActivity", "Error parsing event: " + eventSnap.getKey(), e);
                    }
                }

                // Sort other events by date
                java.util.Collections.sort(otherEvents, (e1, e2) -> e1.getDate().compareTo(e2.getDate()));

                scheduledEventsList.clear();
                scheduledEventsList.addAll(todayEvents);
                scheduledEventsList.addAll(otherEvents);

                tvScheduledLabel.setText(R.string.scheduled_outfits);

                if (scheduledEventsList.isEmpty()) {
                    rvScheduledEvents.setVisibility(View.GONE);
                } else {
                    rvScheduledEvents.setVisibility(View.VISIBLE);
                    scheduledEventsAdapter.notifyDataSetChanged();
                }
                
                // Show "No Outfits Scheduled" if TODAY is empty
                if (todayEvents.isEmpty()) {
                    cvNoOutfits.setVisibility(View.VISIBLE);
                    tvNoOutfitsTitle.setText("No Outfits Scheduled Today");
                    tvNoOutfitsDesc.setText("Plan your look for today or check upcoming outfits below.");
                } else {
                    cvNoOutfits.setVisibility(View.GONE);
                }
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
                long usedItemsCount = 0;

                for (CategoryManager.CategoryItem fixedItem : CategoryManager.getCategories(isWoman)) {
                    long itemCount = 0;
                    DataSnapshot categorySnapshot = snapshot.child(fixedItem.id);
                    
                    if (categorySnapshot.exists()) {
                        // More robust counting: only count valid item objects
                        DataSnapshot photosNode = categorySnapshot.hasChild("photos") ? 
                                categorySnapshot.child("photos") : categorySnapshot;
                        
                        for (DataSnapshot child : photosNode.getChildren()) {
                            if ("photos".equals(child.getKey())) continue;
                            
                            // Check if this node is an object containing item data
                            if (child.getValue() instanceof java.util.Map) {
                                java.util.Map<?, ?> map = (java.util.Map<?, ?>) child.getValue();
                                if (map.containsKey("imageUrl") || map.containsKey("url")) {
                                    itemCount++;
                                }
                            }
                        }
                    }

                    if ("used_clothes".equals(fixedItem.id)) {
                        usedItemsCount = itemCount;
                    } else {
                        totalItems += itemCount;
                    }

                    categoryList.add(new ViewCategoriesActivity.CategoryModel(fixedItem.id, fixedItem.name, fixedItem.iconRes, (int) itemCount));
                }

                if (tvTotalItems != null) {
                    tvTotalItems.setText(String.valueOf(totalItems));
                }
                if (tvUsedItems != null) {
                    tvUsedItems.setText(String.valueOf(usedItemsCount));
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

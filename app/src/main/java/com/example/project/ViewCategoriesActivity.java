package com.example.project;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ViewCategoriesActivity extends AppCompatActivity {

    private RecyclerView rvCategories;
    private SwipeRefreshLayout swipeRefreshLayout;
    private WardrobeCategoryAdapter adapter;
    private List<CategoryModel> categoryList = new ArrayList<>();
    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;
    private boolean isWomanSelected = true;
    private ImageView ivGenderIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wardrobe_categories);

        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference("Users");

        rvCategories = findViewById(R.id.rv_categories);
        rvCategories.setLayoutManager(new GridLayoutManager(this, 2));

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::loadCategories);
        }

        adapter = new WardrobeCategoryAdapter(this, categoryList);
        rvCategories.setAdapter(adapter);

        ivGenderIcon = findViewById(R.id.iv_gender_icon);

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        
        findViewById(R.id.camera_menu).setOnClickListener(v -> {
            startActivity(new Intent(this, AddItemActivity.class));
        });

        EditText etSearch = findViewById(R.id.et_search);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        loadUserGenderAndCategories();
    }

    private void loadUserGenderAndCategories() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        dbRef.child(uid).child("gender").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String gender = snapshot.getValue(String.class);
                isWomanSelected = !"man".equalsIgnoreCase(gender);
                
                if (ivGenderIcon != null) {
                    ivGenderIcon.setImageResource(isWomanSelected ? R.drawable.ic_female : R.drawable.ic_male);
                }

                loadCategories();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                loadCategories();
            }
        });
    }

    private void updateToggleUI() {
        // Method removed as toggle is now permanent
    }

    private void loadCategories() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        dbRef.child(uid).child("categories").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                categoryList.clear();
                int totalItems = 0;
                
                // Add fixed categories from CategoryManager
                for (CategoryManager.CategoryItem fixedItem : CategoryManager.getCategories(isWomanSelected)) {
                    long itemCount = 0;
                    DataSnapshot categorySnapshot = snapshot.child(fixedItem.id);
                    if (categorySnapshot.exists()) {
                        // Source of truth: only count valid item objects
                        DataSnapshot photosNode = categorySnapshot.hasChild("photos") ? 
                                categorySnapshot.child("photos") : categorySnapshot;
                        
                        for (DataSnapshot child : photosNode.getChildren()) {
                            if ("photos".equals(child.getKey())) continue;
                            
                            if (child.getValue() instanceof java.util.Map) {
                                java.util.Map<?, ?> map = (java.util.Map<?, ?>) child.getValue();
                                if (map.containsKey("imageUrl") || map.containsKey("url")) {
                                    itemCount++;
                                }
                            }
                        }

                        // Exclude "Used" category from "All Clothes" total count
                        if (!"used_clothes".equals(fixedItem.id)) {
                            totalItems += itemCount;
                        }
                    }
                    categoryList.add(new CategoryModel(fixedItem.id, fixedItem.name, fixedItem.iconRes, (int) itemCount));
                }
                
                // Inject "All Clothes" at the beginning
                categoryList.add(0, new CategoryModel("all_clothes", "All Clothes", R.drawable.hanger, totalItems));
                
                adapter.updateList(new ArrayList<>(categoryList));

                // Move past outfit items to the archive automatically
                checkAndMovePastOutfitItems();
                // Remove existing duplicates in Used Clothes category
                deduplicateUsedClothes();
                // Check if any archived items should return to their category (7-day timer)
                checkAndReturnUsedItems();
                // Dynamically update the count for "Used" category based on Calendar events
                loadUsedItemCount();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ViewCategoriesActivity.this, "Failed to load categories", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUsedItemCount() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        DatabaseReference usedCatRef = dbRef.child(uid).child("categories").child("used_clothes");

        // Source count strictly from the used_clothes category node
        usedCatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long count = 0;
                DataSnapshot photosNode = snapshot.hasChild("photos") ? 
                        snapshot.child("photos") : snapshot;
                
                for (DataSnapshot child : photosNode.getChildren()) {
                    if ("photos".equals(child.getKey())) continue;
                    
                    if (child.getValue() instanceof java.util.Map) {
                        java.util.Map<?, ?> map = (java.util.Map<?, ?>) child.getValue();
                        if (map.containsKey("imageUrl") || map.containsKey("url")) {
                            count++;
                        }
                    }
                }

                // Update the count for the "used_clothes" item in the existing list
                boolean updated = false;
                for (int i = 0; i < categoryList.size(); i++) {
                    if ("used_clothes".equals(categoryList.get(i).id)) {
                        categoryList.get(i).itemCount = (int) count;
                        updated = true;
                        break;
                    }
                }
                if (updated) {
                    adapter.updateList(new ArrayList<>(categoryList));
                }
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
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
                        // This is a past event, check its items
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

                                if (data instanceof Map && itemKey != null) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> dataMap = (Map<String, Object>) data;
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

    private void deduplicateUsedClothes() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        DatabaseReference usedCatRef = FirebaseDatabase.getInstance().getReference("Users")
                .child(uid).child("categories").child("used_clothes").child("photos");

        usedCatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, String> seenUrls = new HashMap<>(); // URL -> First Key found
                List<DatabaseReference> toDelete = new ArrayList<>();

                for (DataSnapshot photoSnap : snapshot.getChildren()) {
                    String url = photoSnap.child("imageUrl").getValue(String.class);
                    if (url == null) url = photoSnap.child("url").getValue(String.class);

                    if (url != null) {
                        if (seenUrls.containsKey(url)) {
                            // This is a duplicate URL, mark for deletion
                            toDelete.add(photoSnap.getRef());
                        } else {
                            seenUrls.put(url, photoSnap.getKey());
                        }
                    }
                }

                // Delete all duplicates
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
                            
                            Map<String, Object> data = (Map<String, Object>) photoSnap.getValue();
                            if (data != null) {
                                Map<String, Object> cleanData = new HashMap<>(data);
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

    // Simple model class
    public static class CategoryModel {
        public String id;
        public String name;
        public int iconRes;
        public int itemCount;

        public CategoryModel(String id, String name, int itemCount) {
            this(id, name, 0, itemCount);
        }

        public CategoryModel(String id, String name, int iconRes, int itemCount) {
            this.id = id;
            this.name = name;
            this.iconRes = iconRes;
            this.itemCount = itemCount;
        }
    }
}

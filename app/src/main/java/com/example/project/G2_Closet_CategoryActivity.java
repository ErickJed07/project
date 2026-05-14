package com.example.project;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class G2_Closet_CategoryActivity extends AppCompatActivity {

    private RecyclerView galleryRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private G3_Closet_CategoryAdapter adapter;

    private final List<String> imageUrlList = new ArrayList<>();
    private final Map<String, String> urlToKeyMap = new HashMap<>();
    private final Map<String, String> urlToCategoryMap = new HashMap<>();
    private final Set<String> selectedUrls = new HashSet<>();

    private boolean isMultiSelectMode = false;
    private FloatingActionButton deleteFab;
    private FloatingActionButton washFab;
    private String categoryName;
    private String categoryId;

    private FirebaseAuth mAuth;
    private DatabaseReference categoryRef;
    private String uid;

    private boolean isLatestFirst = true;
    private boolean isFavoriteFilterActive = false;
    private final Map<String, Boolean> urlToFavoriteMap = new HashMap<>();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.g2_closet_gallery);

        View sortBtn = findViewById(R.id.sortButton);
        if (sortBtn != null) {
            sortBtn.setOnClickListener(v -> {
                isLatestFirst = !isLatestFirst;
                sortImages();
            });
        }

        findViewById(R.id.btnbackcloset).setOnClickListener(v -> {
            getOnBackPressedDispatcher().onBackPressed();
        });

        View favoriteFilterBtn = findViewById(R.id.btnLatest);
        if (favoriteFilterBtn != null) {
            favoriteFilterBtn.setOnClickListener(v -> {
                isFavoriteFilterActive = !isFavoriteFilterActive;
                favoriteFilterBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        isFavoriteFilterActive ? android.graphics.Color.parseColor("#FFE0E0") : android.graphics.Color.parseColor("#F0F0F0")
                ));
                sortImages();
            });
        }

        categoryName = getIntent().getStringExtra("CATEGORY_NAME");
        categoryId = getIntent().getStringExtra("CATEGORY_ID");

        TextView titleView = findViewById(R.id.categoryTitle);
        if (categoryName != null) titleView.setText(categoryName);

        mAuth = FirebaseAuth.getInstance();
        uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        if (uid == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        checkAndMovePastOutfitItems(); // Ensure past events are moved to Used category

        deleteFab = findViewById(R.id.fabDelete);
        if (deleteFab != null) deleteFab.hide();

        washFab = findViewById(R.id.fabWash);
        if (washFab != null) washFab.hide();

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::refreshData);
        }

        View checkUpdateBtn = findViewById(R.id.btnCheckUpdate);
        if (checkUpdateBtn != null) {
            checkUpdateBtn.setOnClickListener(v -> checkForUpdates());
        }

        galleryRecyclerView = findViewById(R.id.galleryRecyclerView);
        galleryRecyclerView.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 2));

        adapter = new G3_Closet_CategoryAdapter(
                this,
                imageUrlList,
                this::onImageClicked,
                this::onImageLongClicked,
                this::isUrlSelected,
                this::isMultiSelectMode,
                this::onFavoriteClicked,
                this::isUrlFavorite
        );
        galleryRecyclerView.setAdapter(adapter);

        if (categoryId != null) {
            if ("all_clothes".equals(categoryId)) {
                categoryRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("categories");
                loadAllImagesFromFirebase();
            } else if ("used_clothes".equals(categoryId)) {
                loadUsedImagesFromFirebase();
            } else {
                categoryRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("categories").child(categoryId);
                loadImagesFromFirebase();
            }
        }

        if (deleteFab != null) {
            deleteFab.setOnClickListener(v -> deleteSelectedImages());
        }

        if (washFab != null) {
            washFab.setOnClickListener(v -> washSelectedImages());
        }
    }

    private void washSelectedImages() {
        if (selectedUrls.isEmpty()) return;
        
        Set<String> urlsToWash = new HashSet<>(selectedUrls);
        for (String url : urlsToWash) {
            String key = urlToKeyMap.get(url);
            // In the "Used" view, we force unarchive if it came from the used_clothes node
            unarchiveFromFirebase(key);
        }
        
        exitMultiSelectMode();
        Toast.makeText(this, "Items washed and returned to wardrobe", Toast.LENGTH_SHORT).show();
        FirebaseDatabase.getInstance().getReference("Users").child(uid).child("lastLaundryAt").setValue(System.currentTimeMillis());
    }

    private void unarchiveFromFirebase(String key) {
        if (key == null) return;
        DatabaseReference usedRootRef = FirebaseDatabase.getInstance().getReference("Users")
                .child(uid).child("categories").child("used_clothes");
                
        // Check both common patterns
        usedRootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                DataSnapshot itemSnap = snapshot.child("photos").child(key);
                if (!itemSnap.exists()) itemSnap = snapshot.child(key);
                
                if (itemSnap.exists()) {
                    String originalCat = itemSnap.child("originalCategory").getValue(String.class);
                    if (originalCat == null) originalCat = "Tops"; // Fallback
                    
                    Map<String, Object> data = (Map<String, Object>) itemSnap.getValue();
                    if (data != null) {
                        Map<String, Object> cleanData = new HashMap<>(data);
                        cleanData.remove("movedToUsedAt");
                        cleanData.remove("originalCategory");
                        cleanData.put("categoryId", originalCat);
                        cleanData.put("timestamp", System.currentTimeMillis());
                        
                        final DatabaseReference finalItemRef = itemSnap.getRef();
                        
                        FirebaseDatabase.getInstance().getReference("Users")
                                .child(uid).child("categories").child(originalCat).child("photos")
                                .child(key).setValue(cleanData).addOnSuccessListener(aVoid -> {
                                    finalItemRef.removeValue();
                                });
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateItemsFoundCount() {
        TextView itemsFoundText = findViewById(R.id.itemsFoundText);
        if (itemsFoundText != null) {
            itemsFoundText.setText(getString(R.string.items_found_format, imageUrlList.size()));
        }
    }

    private void loadImagesFromFirebase() {
        categoryRef.child("photos").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                imageUrlList.clear();
                urlToKeyMap.clear();
                urlToCategoryMap.clear();
                urlToFavoriteMap.clear();

                for (DataSnapshot photoSnap : snapshot.getChildren()) {
                    String key = photoSnap.getKey();
                    String photoUrlString = photoSnap.child("imageUrl").getValue(String.class);
                    if (photoUrlString == null) photoUrlString = photoSnap.child("url").getValue(String.class);

                    if (photoUrlString != null && !photoUrlString.isEmpty()) {
                        imageUrlList.add(photoUrlString);
                        urlToKeyMap.put(photoUrlString, key);
                        urlToCategoryMap.put(photoUrlString, categoryId);
                        Boolean isFav = photoSnap.child("favorite").getValue(Boolean.class);
                        urlToFavoriteMap.put(photoUrlString, isFav != null && isFav);
                    }
                }
                sortImages();
                updateItemsFoundCount();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadAllImagesFromFirebase() {
        categoryRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                imageUrlList.clear();
                urlToKeyMap.clear();
                urlToCategoryMap.clear();
                urlToFavoriteMap.clear();

                for (DataSnapshot categorySnap : snapshot.getChildren()) {
                    String catId = categorySnap.getKey();
                    // Exclude "Used" clothes from the "All Clothes" view
                    if ("used_clothes".equals(catId)) continue;

                    for (DataSnapshot photoSnap : categorySnap.child("photos").getChildren()) {
                        String key = photoSnap.getKey();
                        String url = photoSnap.child("imageUrl").getValue(String.class);
                        if (url == null) url = photoSnap.child("url").getValue(String.class);

                        if (url != null && !url.isEmpty()) {
                            imageUrlList.add(url);
                            urlToKeyMap.put(url, key);
                            urlToCategoryMap.put(url, catId);
                            Boolean isFav = photoSnap.child("favorite").getValue(Boolean.class);
                            urlToFavoriteMap.put(url, isFav != null && isFav);
                        }
                    }
                }
                sortImages();
                updateItemsFoundCount();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadUsedImagesFromFirebase() {
        if (uid == null) return;
        DatabaseReference usedRootRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("categories").child("used_clothes");
        
        usedRootRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                imageUrlList.clear();
                urlToKeyMap.clear();
                urlToCategoryMap.clear();
                urlToFavoriteMap.clear();

                // 1. Check "used_clothes/photos" pattern
                if (snapshot.hasChild("photos")) {
                    for (DataSnapshot photoSnap : snapshot.child("photos").getChildren()) {
                        addUsedPhoto(photoSnap);
                    }
                }
                
                // 2. Check direct children pattern (excluding "photos" node itself)
                for (DataSnapshot photoSnap : snapshot.getChildren()) {
                    if ("photos".equals(photoSnap.getKey())) continue;
                    addUsedPhoto(photoSnap);
                }

                sortImages();
                updateItemsFoundCount();
            }

            private void addUsedPhoto(DataSnapshot photoSnap) {
                String key = photoSnap.getKey();
                String url = photoSnap.child("imageUrl").getValue(String.class);
                if (url == null) url = photoSnap.child("url").getValue(String.class);

                if (url != null && !url.isEmpty() && !urlToKeyMap.containsKey(url)) {
                    imageUrlList.add(url);
                    urlToKeyMap.put(url, key);
                    urlToCategoryMap.put(url, "used_clothes");
                    Boolean isFav = photoSnap.child("favorite").getValue(Boolean.class);
                    urlToFavoriteMap.put(url, isFav != null && isFav);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void onImageClicked(String url) {
        if (isMultiSelectMode) {
            toggleSelection(url);
        } else {
            Intent intent = new Intent(this, G4_Closet_Category_PhotoViewerActivity.class);
            intent.putStringArrayListExtra("IMAGES", new ArrayList<>(imageUrlList));
            intent.putExtra("CATEGORY_ID", categoryId);
            intent.putExtra("UID", uid);
            intent.putExtra("START_INDEX", imageUrlList.indexOf(url));
            startActivity(intent);
        }
    }

    private void onImageLongClicked(String url) {
        if (!isMultiSelectMode) {
            isMultiSelectMode = true;
            selectedUrls.add(url);
            adapter.notifyDataSetChanged();
            if (deleteFab != null) deleteFab.show();
            if ("used_clothes".equals(categoryId) && washFab != null) {
                washFab.show();
            }
        } else {
            toggleSelection(url);
        }
    }

    private void toggleSelection(String url) {
        if (selectedUrls.contains(url)) selectedUrls.remove(url);
        else selectedUrls.add(url);

        if (selectedUrls.isEmpty()) {
            exitMultiSelectMode();
        } else {
            if (deleteFab != null) deleteFab.show();
            if ("used_clothes".equals(categoryId) && washFab != null) {
                washFab.show();
            }
            adapter.notifyDataSetChanged();
        }
    }

    private void exitMultiSelectMode() {
        isMultiSelectMode = false;
        selectedUrls.clear();
        adapter.notifyDataSetChanged();
        if (deleteFab != null) deleteFab.hide();
        if (washFab != null) washFab.hide();
    }

    private boolean isUrlSelected(String url) { return selectedUrls.contains(url); }
    private boolean isMultiSelectMode() { return isMultiSelectMode; }
    private boolean isUrlFavorite(String url) {
        Boolean isFav = urlToFavoriteMap.get(url);
        return isFav != null && isFav;
    }

    private void onFavoriteClicked(String url, boolean isFavorite) {
        String key = urlToKeyMap.get(url);
        String catId = urlToCategoryMap.get(url);
        if (key != null && catId != null && uid != null) {
            FirebaseDatabase.getInstance().getReference("Users").child(uid).child("categories").child(catId).child("photos").child(key).child("favorite").setValue(isFavorite)
                    .addOnSuccessListener(aVoid -> {
                        urlToFavoriteMap.put(url, isFavorite);
                        sortImages();
                    });
        }
    }

    private void deleteSelectedImages() {
        if (selectedUrls.isEmpty()) return;
        Set<String> urlsToDelete = new HashSet<>(selectedUrls);
        for (String url : urlsToDelete) {
            String key = urlToKeyMap.get(url);
            String catId = urlToCategoryMap.get(url);
            if (key != null && catId != null) {
                FirebaseDatabase.getInstance().getReference("Users").child(uid).child("categories").child(catId).child("photos").child(key).removeValue();
            }
            new Thread(() -> {
                try {
                    String publicId = extractPublicId(url);
                    if (publicId != null) {
                        com.cloudinary.android.MediaManager.get().getCloudinary().uploader().destroy(publicId, com.cloudinary.utils.ObjectUtils.emptyMap());
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }).start();
        }
        selectedUrls.clear();
        exitMultiSelectMode();
        Toast.makeText(this, "Images deleted", Toast.LENGTH_SHORT).show();
    }

    private String extractPublicId(String url) {
        try {
            if (url.contains("/upload/")) {
                String[] parts = url.split("/upload/");
                if (parts.length > 1) {
                    String temp = parts[1];
                    if (temp.matches("^v\\d+/.*")) temp = temp.substring(temp.indexOf("/") + 1);
                    if (temp.contains(".")) temp = temp.substring(0, temp.lastIndexOf("."));
                    return temp;
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    private void sortImages() {
        if (imageUrlList.isEmpty()) return;
        java.util.Collections.sort(imageUrlList, (url1, url2) -> {
            if (isFavoriteFilterActive) {
                boolean fav1 = isUrlFavorite(url1);
                boolean fav2 = isUrlFavorite(url2);
                if (fav1 != fav2) return fav1 ? -1 : 1;
            }
            String key1 = urlToKeyMap.get(url1);
            String key2 = urlToKeyMap.get(url2);
            if (key1 == null || key2 == null) return 0;
            return isLatestFirst ? key2.compareTo(key1) : key1.compareTo(key2);
        });
        adapter.notifyDataSetChanged();
        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
    }

    private void refreshData() {
        checkAndMovePastOutfitItems(); // Ensure items from past events are moved to Used node
        if (categoryId != null) {
            if ("all_clothes".equals(categoryId)) {
                loadAllImagesFromFirebase();
            } else if ("used_clothes".equals(categoryId)) {
                loadUsedImagesFromFirebase();
            } else {
                loadImagesFromFirebase();
            }
        } else {
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void checkForUpdates() {
        Toast.makeText(this, "Checking for updates...", Toast.LENGTH_SHORT).show();
        // Copying logic from D_FeedActivity if needed, but keeping it simple for now.
        refreshData();
    }

    private void checkAndMovePastOutfitItems() {
        if (uid == null) return;
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
                                    Map<String, Object> dataMap = (Map<String, Object>) data;
                                    dataMap.put("originalCategory", item.getCategoryId());
                                    dataMap.put("movedToUsedAt", System.currentTimeMillis());
                                    dataMap.put("categoryId", "used_clothes");

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
}

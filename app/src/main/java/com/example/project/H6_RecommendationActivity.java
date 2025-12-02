package com.example.project;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
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
import java.util.Random;

public class H6_RecommendationActivity extends AppCompatActivity {

    // --- Helper Class ---
    private static class ClothingItem {
        String id;
        String url;
        String category; // e.g. "Top", "Pants"
        String color;    // <--- NEW: Add this field

        ClothingItem(String id, String url, String category, String color) {
            this.id = id;
            this.url = url;
            this.category = category;
            this.color = color; // <--- NEW: Save it
        }
    }


    // --- UI Components ---
    private ImageView imgTop, imgBottom;
    private Button btnShuffle, btnBack;

    // --- Data Lists ---
    private List<ClothingItem> allTops = new ArrayList<>();
    private List<ClothingItem> allBottoms = new ArrayList<>();

    // --- Currently Displayed ---
    private ClothingItem currentTop;
    private ClothingItem currentBottom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.h6_recommendation);

        // 1. Link Views
        imgTop = findViewById(R.id.imgTop);
        imgBottom = findViewById(R.id.imgBottom);
        btnShuffle = findViewById(R.id.btnShuffle);
        btnBack = findViewById(R.id.btnBack);

        // 2. Set Listeners

        // FIX: Check if btnShuffle exists before setting listener to prevent crash
        if (btnShuffle != null) {
            btnShuffle.setText("Generate All");
            btnShuffle.setOnClickListener(v -> generateAllCombinationsAndSave());
        } else {
            // Log a warning for debugging purposes
            System.out.println("WARNING: R.id.btnShuffle was not found in layout h6_recommendation.xml");
        }


        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // 3. Load Data
        loadClothesData();
    }

    private void loadClothesData() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return; // Safety check

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("categories");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                allTops.clear();
                allBottoms.clear();

                if (!snapshot.exists()) {
                    Toast.makeText(H6_RecommendationActivity.this, "No categories found in database", Toast.LENGTH_SHORT).show();
                    return;
                }

                for (DataSnapshot categorySnap : snapshot.getChildren()) {
                    // Check if "photos" exists
                    if (categorySnap.hasChild("photos")) {
                        for (DataSnapshot photoSnap : categorySnap.child("photos").getChildren()) {

                            String url = photoSnap.child("url").getValue(String.class);
                            String tagCloth = photoSnap.child("tagCloth").getValue(String.class);
                            String tagColor = photoSnap.child("tagColor").getValue(String.class);

                            // Fallback for missing data
                            if (tagColor == null) tagColor = "Unknown";

                            // DEBUG: Print what we found to Logcat
                            if (tagCloth != null) {
                                System.out.println("Found item: " + tagCloth + " (" + tagColor + ")");
                            }

                            if (url != null && tagCloth != null) {
                                ClothingItem item = new ClothingItem(photoSnap.getKey(), url, tagCloth, tagColor);
                                categorizeItem(item);
                            }
                        }
                    }
                }

                // DEBUG: Check list sizes
                System.out.println("Total Tops: " + allTops.size());
                System.out.println("Total Bottoms: " + allBottoms.size());

                suggestOutfit();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(H6_RecommendationActivity.this, "Failed to load clothes", Toast.LENGTH_SHORT).show();
            }
        });
    }



    private void categorizeItem(ClothingItem item) {
        String tag = item.category.toLowerCase().trim();

        // Filter Logic: Check if it's a Top or Bottom based on the tag
        // Expanded list based on common clothing names
        if (tag.contains("shirt") || tag.contains("top") || tag.contains("hoodie") ||
                tag.contains("jacket") || tag.contains("coat") || tag.contains("blouse") ||
                tag.contains("sweater") || tag.contains("vest")) {

            allTops.add(item);

        } else if (tag.contains("pant") || tag.contains("jean") || tag.contains("skirt") ||
                tag.contains("short") || tag.contains("leg") || tag.contains("trouser")) {

            allBottoms.add(item);
        }
    }


    private void suggestOutfit() {
        Random random = new Random();

        // Handle missing Tops
        if (!allTops.isEmpty()) {
            currentTop = allTops.get(random.nextInt(allTops.size()));
            Glide.with(this).load(currentTop.url).into(imgTop);
        } else {
            // Show a placeholder or clear the image if no tops found
            imgTop.setImageResource(R.drawable.box_background); // Ensure you have this drawable or use android.R.color.darker_gray
            Toast.makeText(this, "No Tops found. Check your tags!", Toast.LENGTH_SHORT).show();
        }

        // Handle missing Bottoms
        if (!allBottoms.isEmpty()) {
            // Basic logic for now, skipping color matching to ensure image shows first
            currentBottom = allBottoms.get(random.nextInt(allBottoms.size()));
            Glide.with(this).load(currentBottom.url).into(imgBottom);
        } else {
            imgBottom.setImageResource(R.drawable.box_background);
            Toast.makeText(this, "No Bottoms found. Check your tags!", Toast.LENGTH_SHORT).show();
        }

        // Only run color logic if BOTH exist
        if (!allTops.isEmpty() && !allBottoms.isEmpty()) {
            int attempts = 0;
            // Re-roll currentBottom if colors match, just to be safe
            while (currentTop.color.equalsIgnoreCase(currentBottom.color) && attempts < 5) {
                currentBottom = allBottoms.get(random.nextInt(allBottoms.size()));
                Glide.with(this).load(currentBottom.url).into(imgBottom);
                attempts++;
            }
        }
    }



    private void saveOutfitToFirebase() {
        if (currentTop == null || currentBottom == null) return;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference outfitRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("saved_outfits").push();

        Map<String, Object> outfitData = new HashMap<>();
        outfitData.put("topUrl", currentTop.url);
        outfitData.put("bottomUrl", currentBottom.url);
        outfitData.put("topId", currentTop.id);
        outfitData.put("bottomId", currentBottom.id);
        outfitData.put("dateCreated", System.currentTimeMillis());

        outfitRef.setValue(outfitData)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Outfit Saved Successfully!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save.", Toast.LENGTH_SHORT).show());
    }

    // ---------------------------------------------------------
    // NEW: Generate ALL combinations and return to H1
    // ---------------------------------------------------------
    private void generateAllCombinationsAndSave() {
        if (allTops.isEmpty() || allBottoms.isEmpty()) {
            Toast.makeText(this, "Need both Tops and Bottoms to generate combinations!", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Generating all outfits...", Toast.LENGTH_SHORT).show();

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference tempRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("temp_suggestions");

        // 1. Clear previous suggestions immediately
        tempRef.removeValue();

        // ---------------------------------------------------------
        // NEW: Tell Firebase to delete this node when the app disconnects (closes/quits)
        // ---------------------------------------------------------
        tempRef.onDisconnect().removeValue();

        // 2. Loop through EVERY Top
        for (ClothingItem top : allTops) {
            for (ClothingItem bottom : allBottoms) {

                String key = tempRef.push().getKey();
                Map<String, Object> comboData = new HashMap<>();

                comboData.put("topUrl", top.url);
                comboData.put("bottomUrl", bottom.url);
                comboData.put("topId", top.id);
                comboData.put("bottomId", bottom.id);
                comboData.put("topTag", top.category);
                comboData.put("bottomTag", bottom.category);

                if (key != null) {
                    tempRef.child(key).setValue(comboData);
                }
            }
        }

        finish();
    }


}

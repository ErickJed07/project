package com.example.project;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class OutfitGenerator {

    // Define the maximum number of combinations to prevent memory crashes
    private static final int MAX_COMBINATIONS = 50;

    public interface GeneratorCallback {
        void onCombinationsGenerated(List<H6_RecommendationActivity.HistorySnapshot> generatedSnapshots);
        void onFirstCombinationApplied(List<String> firstComboUrls);
    }

    /**
     * Main method to generate outfits based on current boxes on the canvas.
     */
    public static void generate(H6_RecommendationActivity activity,
                                FrameLayout canvasContainer,
                                List<H6_RecommendationActivity.ClothingItem> allItems,
                                GeneratorCallback callback) {

        if (canvasContainer.getChildCount() == 0) {
            Toast.makeText(activity, "Add boxes first", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Identify all active boxes and their potential items
        List<List<String>> allSlots = new ArrayList<>();
        List<CardView> boxReferences = new ArrayList<>();

        for (int i = 0; i < canvasContainer.getChildCount(); i++) {
            View child = canvasContainer.getChildAt(i);
            if (child instanceof CardView) {
                CardView card = (CardView) child;
                Object[] tags = (Object[]) card.getTag();

                // Check lock state
                boolean isLocked = (boolean) tags[1];

                List<String> matchingUrls = new ArrayList<>();

                if (isLocked) {
                    // If locked, only use the current URL (1 option)
                    String currentUrl = (String) tags[2];
                    if (currentUrl != null) matchingUrls.add(currentUrl);
                } else {
                    // Find potential items from Firebase
                    List<String> filters = (List<String>) tags[0];
                    if (filters == null || filters.isEmpty()) continue;
                    String category = filters.get(0).trim();

                    for (H6_RecommendationActivity.ClothingItem item : allItems) {
                        boolean matchCat = item.category != null && item.category.equalsIgnoreCase(category);
                        boolean matchSub = item.subCategory != null && item.subCategory.equalsIgnoreCase(category);
                        if (matchCat || matchSub) {
                            matchingUrls.add(item.url);
                        }
                    }
                }

                if (matchingUrls.isEmpty()) continue;

                allSlots.add(matchingUrls);
                boxReferences.add(card);
            }
        }

        if (allSlots.isEmpty()) {
            Toast.makeText(activity, "No matching items found to shuffle.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Generate Combinations (Recursive Cartesian Product)
        List<List<String>> allCombinations = new ArrayList<>();
        generateCombinationsRecursive(allSlots, 0, new ArrayList<>(), allCombinations);

        // 3. Process Results
        if (!allCombinations.isEmpty()) {
            // A. Prepare History Snapshots (with position data)
            int limit = Math.min(allCombinations.size(), MAX_COMBINATIONS);
            List<H6_RecommendationActivity.HistorySnapshot> snapshots = new ArrayList<>();

            for (int i = 0; i < limit; i++) {
                List<String> comboUrls = allCombinations.get(i);
                List<H6_RecommendationActivity.BoxState> snapshotStates = new ArrayList<>();

                for (int j = 0; j < boxReferences.size(); j++) {
                    CardView box = boxReferences.get(j);
                    String url = comboUrls.get(j);

                    // Capture current position relative to the canvas
                    snapshotStates.add(new H6_RecommendationActivity.BoxState(
                            url,
                            box.getX(),
                            box.getY(),
                            box.getWidth(),
                            box.getHeight()
                    ));
                }
                snapshots.add(new H6_RecommendationActivity.HistorySnapshot(snapshotStates));
            }

            // B. Apply the FIRST combination to the UI immediately
            List<String> firstCombo = allCombinations.get(0);
            for (int i = 0; i < boxReferences.size(); i++) {
                CardView card = boxReferences.get(i);
                String url = firstCombo.get(i);

                Object[] tags = (Object[]) card.getTag();
                tags[2] = url; // Update current URL tag

                ImageView imgView = (ImageView) ((FrameLayout) card.getChildAt(0)).getChildAt(0);
                Glide.with(activity).load(url).into(imgView);
            }

            // C. Notify Activity
            callback.onCombinationsGenerated(snapshots);
            callback.onFirstCombinationApplied(firstCombo);

            Toast.makeText(activity, "Generated " + limit + " layouts!", Toast.LENGTH_SHORT).show();
        }
    }

    // Recursive helper
    private static void generateCombinationsRecursive(List<List<String>> lists, int depth, List<String> current, List<List<String>> result) {
        if (result.size() >= MAX_COMBINATIONS) return;

        if (depth == lists.size()) {
            result.add(new ArrayList<>(current));
            return;
        }

        for (String url : lists.get(depth)) {
            current.add(url);
            generateCombinationsRecursive(lists, depth + 1, current, result);
            current.remove(current.size() - 1); // Backtrack
        }
    }
}

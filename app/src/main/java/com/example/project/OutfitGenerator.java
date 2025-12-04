package com.example.project;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class OutfitGenerator {

    // Define the maximum number of combinations to prevent memory crashes
    private static final int MAX_COMBINATIONS = 50;

    public interface GeneratorCallback {
        // We still pass the list in case you want to use it for undo/redo later,
        // but the UI for it is gone.
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
                    // Find potential items from Firebase with Strict Filtering
                    // Structure: [Category, SubCategory, Color]
                    List<String> filters = (List<String>) tags[0];
                    if (filters == null || filters.isEmpty()) continue;

                    // Parse filters safely
                    String reqCategory = filters.size() > 0 ? filters.get(0).trim() : "";
                    String reqSubCat   = filters.size() > 1 ? filters.get(1).trim() : "";
                    String reqColor    = filters.size() > 2 ? filters.get(2).trim() : "";

                    for (H6_RecommendationActivity.ClothingItem item : allItems) {
                        boolean isMatch = true;

                        // 1. Check Category (Must match)
                        if (!reqCategory.isEmpty()) {
                            if (item.category == null || !item.category.equalsIgnoreCase(reqCategory)) {
                                isMatch = false;
                            }
                        }

                        // 2. Check SubCategory (Must match if filter is present)
                        if (isMatch && !reqSubCat.isEmpty()) {
                            if (item.subCategory == null || !item.subCategory.equalsIgnoreCase(reqSubCat)) {
                                isMatch = false;
                            }
                        }

                        // 3. Check Color (Must match if filter is present)
                        // 3. Check Color (Must match if filter is present)
                        if (isMatch && !reqColor.isEmpty()) {
                            boolean colorFound = false;

                            // Check the list of colors
                            if (item.colors != null) {
                                for (String c : item.colors) {
                                    if (c.equalsIgnoreCase(reqColor)) {
                                        colorFound = true;
                                        break;
                                    }
                                }
                            }

                            if (!colorFound) {
                                isMatch = false;
                            }
                        }


                        // If it passed all active filters, add it
                        if (isMatch) {
                            matchingUrls.add(item.imageUrl);
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

        // 2. Generate Combinations
        // OPTIMIZATION: If we only removed the history UI, we might just want ONE random combination
        // instead of generating 50 and picking the first.
        // However, to keep behavior identical to before (finding all combos), we keep this.
        List<List<String>> allCombinations = new ArrayList<>();
        generateCombinationsRecursive(allSlots, 0, new ArrayList<>(), allCombinations);

        // 3. Process Results
        if (!allCombinations.isEmpty()) {
            // Shuffle the combinations so "First" isn't always the same predictable sequence
            // if the user clicks shuffle multiple times on the same locked items.
            long seed = System.nanoTime();
            java.util.Collections.shuffle(allCombinations, new Random(seed));

            // A. Prepare History Snapshots (even if unused by UI now, logic remains for safety)
            int limit = Math.min(allCombinations.size(), MAX_COMBINATIONS);
            List<H6_RecommendationActivity.HistorySnapshot> snapshots = new ArrayList<>();


            for (int i = 0; i < limit; i++) {
                List<String> comboUrls = allCombinations.get(i);
                List<H6_RecommendationActivity.BoxState> snapshotStates = new ArrayList<>();

                for (int j = 0; j < boxReferences.size(); j++) {
                    CardView box = boxReferences.get(j);
                    String url = comboUrls.get(j);

                    // Capture current position AND THE ID
                    snapshotStates.add(new H6_RecommendationActivity.BoxState(
                            box.getId(), // <--- PASS THE ID HERE
                            url,
                            box.getX(),
                            box.getY(),
                            box.getWidth(),
                            box.getHeight()
                    ));
                }
                snapshots.add(new H6_RecommendationActivity.HistorySnapshot(snapshotStates));
            }



            // B. Apply the FIRST combination (after shuffle) to the UI immediately
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

            Toast.makeText(activity, "Outfit Shuffled!", Toast.LENGTH_SHORT).show();
        }
    }

    // Recursive helper
    private static void generateCombinationsRecursive(List<List<String>> lists, int depth, List<String> current, List<List<String>> result) {
        if (result.size() >= MAX_COMBINATIONS) return;

        if (depth == lists.size()) {
            result.add(new ArrayList<>(current));
            return;
        }

        // Randomize the order we pick items from each slot so we get variety early
        List<String> currentSlotItems = new ArrayList<>(lists.get(depth));
        long seed = System.nanoTime();
        java.util.Collections.shuffle(currentSlotItems, new Random(seed));

        for (String url : currentSlotItems) {
            current.add(url);
            generateCombinationsRecursive(lists, depth + 1, current, result);
            current.remove(current.size() - 1); // Backtrack
            if (result.size() >= MAX_COMBINATIONS) break; // Optimization break
        }
    }
}

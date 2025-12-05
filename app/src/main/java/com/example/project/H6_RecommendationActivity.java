package com.example.project;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import android.Manifest;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


public class H6_RecommendationActivity extends AppCompatActivity {
    private static final int REQ_CODE_POST_NOTIFICATIONS = 101;
    private FrameLayout canvasContainer, resultsContainer, manualPickerContainer;
    private RecyclerView rvResults, rvManualPhotoGrid;
    private ImageButton btnCloseResults, btnClosePicker;
    private CardView boxBeingEdited, currentlySelectedCard;
    private FloatingActionButton fabMenu, fabHistory, fabAddBox, fabPremade;
    private Button btnGenerate;
    private MaterialSwitch switchMode;
    private ResultsAdapter resultsAdapter;
    private boolean isFabMenuOpen = false;
    private List<String> selectedColorsList = new ArrayList<>(), clothingTypesList = new ArrayList<>();
    private List<HistorySnapshot> resultsList = new ArrayList<>();
    private List<ClothingItem> allItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.h6_recommendation);

        initCloudinary();

        // Initialize Views
        canvasContainer = findViewById(R.id.canvasContainer);

        // 1. Initialize Generate Button (Renamed from Shuffle)
        btnGenerate = findViewById(R.id.btnGenerate);


        // 2. Initialize FAB Menu Views
        fabMenu = findViewById(R.id.fabMenu);
        fabHistory = findViewById(R.id.fabHistory);
        fabAddBox = findViewById(R.id.fabAddBox);
        fabPremade = findViewById(R.id.fabPremade);

        // Results Views
        resultsContainer = findViewById(R.id.resultsContainer);
        btnCloseResults = findViewById(R.id.btnCloseResults); // This ID is now correct
        rvResults = findViewById(R.id.rvResults);


        // --- SETUP LISTENERS ---

        // Generate Button Listener
        btnGenerate.setOnClickListener(v -> shuffleAllBoxes());

        // Back Button
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            Intent intent = new Intent(H6_RecommendationActivity.this, G1_ClosetActivity.class); // Verify your class name is A1_ClosetActivity
            startActivity(intent);
            finish();
        });

        // Setup FAB Menu Logic (Bubble animation)
        setupFabMenu();
// --- Initialize Manual Picker UI ---
        switchMode = findViewById(R.id.switchMode);
        manualPickerContainer = findViewById(R.id.manualPickerContainer);
        rvManualPhotoGrid = findViewById(R.id.rvManualPhotoGrid);
        btnClosePicker = findViewById(R.id.btnClosePicker);

// Setup Grid Layout for the picker
        rvManualPhotoGrid.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 3));

// Close button logic
        if (btnClosePicker != null) {
            btnClosePicker.setOnClickListener(v -> manualPickerContainer.setVisibility(View.GONE));
        }

// Ensure default state
        if (switchMode != null) {
            switchMode.setChecked(true); // Default to "Generate/Old Function"
        }


        // Setup Results RecyclerView
        resultsContainer.setVisibility(View.GONE);
        rvResults.setLayoutManager(new LinearLayoutManager(this));
        resultsAdapter = new ResultsAdapter(resultsList, snapshot -> {
            applySnapshotToCanvas(snapshot);
            resultsContainer.setVisibility(View.GONE);
            Toast.makeText(H6_RecommendationActivity.this, "Outfit Restored!", Toast.LENGTH_SHORT).show();
        }, canvasContainer);
        rvResults.setAdapter(resultsAdapter);

        // Results Panel Close Button
        btnCloseResults.setOnClickListener(v -> resultsContainer.setVisibility(View.GONE));

            View btnSave = findViewById(R.id.btnSave);

            checkAndRequestPermissions();

            if (btnSave != null) {
                btnSave.setOnClickListener(v -> {
                    // 1. Check if canvas has content
                    if (canvasContainer.getWidth() > 0 && canvasContainer.getHeight() > 0) {
                        // 2. Capture Bitmap
                        Bitmap outfitBitmap = getBitmapFromView(canvasContainer);

                        // 3. Save Locally
                        String localPath = saveBitmapToFile(outfitBitmap);

                        // 4. Show Popup
                        if (localPath != null) {
                            showSavePopup(localPath);
                        } else {
                            Toast.makeText(this, "Error saving image locally", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Canvas is not ready", Toast.LENGTH_SHORT).show();
                    }
                });
            }

        // Load Data
        fetchCategoriesFromFirebase();
        loadClothesData();

    }
    private void initCloudinary() {
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("cloud_name", BuildConfig.CLOUDINARY_CLOUD_NAME);
            config.put("api_key", BuildConfig.CLOUDINARY_API_KEY);
            config.put("api_secret", BuildConfig.CLOUDINARY_API_SECRET);
            MediaManager.init(this, config);
        } catch (Exception e) {
            // Already initialized
        }
    }



















    private Bitmap getBitmapFromView(View view) {
        // 1. Store visibility states to restore them later
        Map<View, Integer> originalVisibility = new HashMap<>();

        if (view instanceof ViewGroup) {
            ViewGroup container = (ViewGroup) view;

            // --- NEW: Hide the main Hint Text specifically ---
            View hintView = container.findViewById(R.id.hint);
            if (hintView != null && hintView.getVisibility() == View.VISIBLE) {
                originalVisibility.put(hintView, hintView.getVisibility());
                hintView.setVisibility(View.GONE);
            }
            // ------------------------------------------------

            for (int i = 0; i < container.getChildCount(); i++) {
                View childBox = container.getChildAt(i);

                // Skip the hint view since we already handled it above
                if (childBox.getId() == R.id.hint) continue;

                // LOGIC FOR BOXES: Check if this box has an image
                boolean hasImage = false;

                if (childBox instanceof ViewGroup) {
                    ViewGroup boxLayout = (ViewGroup) childBox;
                    // Iterate through the box's children to find an ImageView
                    for (int j = 0; j < boxLayout.getChildCount(); j++) {
                        View innerView = boxLayout.getChildAt(j);
                        if (innerView instanceof ImageView) {
                            if (((ImageView) innerView).getDrawable() != null) {
                                hasImage = true;
                                break;
                            }
                        }
                    }
                }
            }
        }

        // 2. Create Bitmap (Empty boxes and hint are now GONE)
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Drawable bgDrawable = view.getBackground();
        if (bgDrawable != null) {
            bgDrawable.draw(canvas);
        } else {
            canvas.drawColor(Color.WHITE);
        }

        view.draw(canvas);

        // 3. Restore original visibility (Show empty boxes and hint again)
        for (Map.Entry<View, Integer> entry : originalVisibility.entrySet()) {
            entry.getKey().setVisibility(entry.getValue());
        }

        return bitmap;
    }
    private String saveBitmapToFile(Bitmap bitmap) {
        try {
            File dir = new File(getFilesDir(), "outfits");
            if (!dir.exists()) dir.mkdirs();

            String fileName = "outfit_" + System.currentTimeMillis() + ".png";
            File file = new File(dir, fileName);

            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();

            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    private void showSavePopup(String localImagePath) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View popupView = getLayoutInflater().inflate(R.layout.h5_dresser_save_popup, null);
        builder.setView(popupView);
        AlertDialog dialog = builder.create();

        // Bind Popup Views
        EditText etTitle = popupView.findViewById(R.id.etTitle);
        EditText etDate = popupView.findViewById(R.id.etDate);
        EditText etTime = popupView.findViewById(R.id.etTime);
        CheckBox cbEnableAlarm = popupView.findViewById(R.id.cbEnableAlarm);
        TextInputLayout layoutReminder = popupView.findViewById(R.id.layoutReminder);
        AutoCompleteTextView spinnerReminder = popupView.findViewById(R.id.spinnerReminder);
        Button btnCancel = popupView.findViewById(R.id.btnCancel);
        Button btnPopupSave = popupView.findViewById(R.id.btnSave); // The Save button INSIDE the popup

        final Calendar selectedDate = Calendar.getInstance();

        // 1. Date Picker
        etDate.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            DatePickerDialog dateDialog = new DatePickerDialog(this, (view, year, month, day) -> {
                selectedDate.set(Calendar.YEAR, year);
                selectedDate.set(Calendar.MONTH, month);
                selectedDate.set(Calendar.DAY_OF_MONTH, day);
                etDate.setText(String.format(Locale.getDefault(), "%02d/%02d/%d", day, month + 1, year));
            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
            dateDialog.show();
        });

        // 2. Time Picker
        etTime.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            TimePickerDialog timeDialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                selectedDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selectedDate.set(Calendar.MINUTE, minute);
                etTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), false);
            timeDialog.show();
        });

        // 3. Reminder Dropdown
        String[] reminders = {"None", "1 hour before", "45 min before", "30 min before", "15 min before"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, reminders);
        spinnerReminder.setAdapter(adapter);
        spinnerReminder.setText(reminders[0], false);

        // 4. Checkbox Toggle
        cbEnableAlarm.setOnCheckedChangeListener((buttonView, isChecked) ->
                layoutReminder.setVisibility(isChecked ? View.VISIBLE : View.GONE));

        // 5. Cancel Button
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // 6. Save (Upload) Button
        btnPopupSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String reminder = spinnerReminder.getText().toString();
            String timeStr = etTime.getText().toString();
            String dateString = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.getTime());

            if (title.isEmpty()) {
                Toast.makeText(this, "Title required", Toast.LENGTH_SHORT).show();
                return;
            }

            btnPopupSave.setEnabled(false);
            btnPopupSave.setText("Uploading...");

            // Get User ID
            String uid = FirebaseAuth.getInstance().getCurrentUser() != null ?
                    FirebaseAuth.getInstance().getCurrentUser().getUid() : "anon";

            // Upload to Cloudinary
            MediaManager.get().upload(localImagePath)
                    .option("folder", "ClosetImages/" + uid + "/Events")
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {}

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {}

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            String cloudUrl = (String) resultData.get("secure_url");
                            runOnUiThread(() -> {
                                // 1. Save data to Firebase
                                saveEventToFirebase(title, dateString, timeStr, reminder, cloudUrl);

                                // 2. Dismiss the input form dialog
                                dialog.dismiss();

                                // 3. Show "Stay or Go" Confirmation Dialog
                                new AlertDialog.Builder(H6_RecommendationActivity.this)
                                        .setTitle("Outfit Saved!")
                                        .setMessage("Do you want to stay here or view your Calendar?")
                                        .setPositiveButton("Go to Calendar", (d, w) -> {
                                            // Navigate to Calendar Activity
                                            Intent intent = new Intent(H6_RecommendationActivity.this, E1_CalendarActivity.class);
                                            startActivity(intent);
                                            finish(); // Optional: Close current activity
                                        })
                                        .setNegativeButton("Stay Here", (d, w) -> {
                                            // Just dismiss and let user continue
                                            d.dismiss();
                                            // Reset button state in case they open the save menu again
                                            btnPopupSave.setEnabled(true);
                                            btnPopupSave.setText("Save");
                                        })
                                        .setCancelable(false) // Force a choice
                                        .show();
                            });
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            runOnUiThread(() -> {
                                btnPopupSave.setEnabled(true);
                                btnPopupSave.setText("Save");
                                Toast.makeText(H6_RecommendationActivity.this, "Upload failed: " + error.getDescription(), Toast.LENGTH_LONG).show();
                            });
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {}
                    })
                    .dispatch();
        });

        dialog.show();
    }
    private void saveEventToFirebase(String title, String date, String time, String reminder, String imageUrl) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("Events").push();

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("title", title);
        eventData.put("date", date);
        eventData.put("time", time);
        eventData.put("reminder", reminder);
        eventData.put("imageUrl", imageUrl);
        eventData.put("timestamp", System.currentTimeMillis());

        ref.setValue(eventData)
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save to database", Toast.LENGTH_SHORT).show());
    }
    private void checkAndRequestPermissions() {
        // Android 13+ Notification Permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_CODE_POST_NOTIFICATIONS);
            }
        }

        // Android 12+ Alarm Permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (am != null && !am.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CODE_POST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("H6_Recommendation", "POST_NOTIFICATIONS granted by user");
            } else {
                Toast.makeText(this, "Notifications are disabled. Reminders won't work.", Toast.LENGTH_LONG).show();
            }
        }
    }
    private void loadManualImagesForCategory(String category) {
        List<ClothingItem> filteredList = new ArrayList<>();

        // Filter the global 'allItems' list for this category
        for (ClothingItem item : allItems) {
            // Check against category or sub-category only (ignoring color)
            boolean matchesCategory = item.category != null && item.category.equalsIgnoreCase(category);
            boolean matchesSubCategory = item.subCategory != null && item.subCategory.equalsIgnoreCase(category);

            if (matchesCategory || matchesSubCategory) {
                filteredList.add(item);
            }
        }
        // Set the adapter
        rvManualPhotoGrid.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                ImageView iv = new ImageView(parent.getContext());
                iv.setLayoutParams(new ViewGroup.LayoutParams(300, 300)); // Fixed size for grid
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                iv.setPadding(4, 4, 4, 4);
                return new RecyclerView.ViewHolder(iv) {};
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                ClothingItem item = filteredList.get(position);
                ImageView iv = (ImageView) holder.itemView;

                // Load image (assuming you use Glide or similar, modify as needed)
                com.bumptech.glide.Glide.with(H6_RecommendationActivity.this)
                        .load(item.imageUrl)
                        .into(iv);

                // CLICK LISTENER: When user picks an image
                iv.setOnClickListener(v -> {
                    if (boxBeingEdited != null) {
                        // 1. Update the image inside the movable box
                        FrameLayout inner = (FrameLayout) boxBeingEdited.getChildAt(0);
                        ImageView boxImage = (ImageView) inner.getChildAt(0);

                        com.bumptech.glide.Glide.with(H6_RecommendationActivity.this)
                                .load(item.imageUrl)
                                .into(boxImage);

                        // 2. Update the box's data tag so it remembers this URL
                        Object[] currentTag = (Object[]) boxBeingEdited.getTag();
                        // Update the URL index (assuming index 2 is url based on your code: [filters, locked, url])
                        currentTag[2] = item.imageUrl;
                        boxBeingEdited.setTag(currentTag);
                    }
                    // Close the picker
                    manualPickerContainer.setVisibility(View.GONE);
                });
            }

            @Override
            public int getItemCount() {
                return filteredList.size();
            }
        });
    }
    private void setupFabMenu() {
        // Main menu button toggles the sub-buttons
        fabMenu.setOnClickListener(v -> toggleFabMenu());

        // 1. History Bubble
        fabHistory.setOnClickListener(v -> {
            resultsContainer.setVisibility(View.VISIBLE);
            toggleFabMenu(); // Close menu after selection
        });

        // 2. Add Box Bubble
        fabAddBox.setOnClickListener(v -> {
            showAddBoxMenu(); // Call popup menu anchored to this FAB
            toggleFabMenu(); // Close menu after selection
        });

        // 3. Premade Box Bubble (Placeholder)
        fabPremade.setOnClickListener(v -> {
            Toast.makeText(H6_RecommendationActivity.this, "Premade layouts coming soon!", Toast.LENGTH_SHORT).show();
            toggleFabMenu(); // Close menu after selection
        });
    }
    private void toggleFabMenu() {
        isFabMenuOpen = !isFabMenuOpen;

        // Rotate the main FAB (+ becomes x)
        fabMenu.animate().rotation(isFabMenuOpen ? 45f : 0f).setDuration(300).start();

        if (isFabMenuOpen) {
            // Show Bubbles with cascading delay
            showFab(fabHistory, 50);
            showFab(fabAddBox, 100);
            showFab(fabPremade, 150);
        } else {
            // Hide Bubbles
            hideFab(fabHistory, 150);
            hideFab(fabAddBox, 100);
            hideFab(fabPremade, 50);
        }
    }
    private void showFab(View fab, long startDelay) {
        fab.setVisibility(View.VISIBLE);
        fab.setAlpha(0f);
        fab.setScaleX(0f);
        fab.setScaleY(0f);
        fab.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(200)
                .setStartDelay(startDelay)
                .setListener(null)
                .start();
    }
    private void hideFab(View fab, long startDelay) {
        fab.animate()
                .scaleX(0f)
                .scaleY(0f)
                .alpha(0f)
                .setDuration(200)
                .setStartDelay(startDelay)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        fab.setVisibility(View.GONE);
                    }
                })
                .start();
    }
    private void fetchCategoriesFromFirebase() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("categories");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                clothingTypesList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    // FIX: Retrieve as Object first to prevent "ArrayList to String" crash
                    Object nameObj = data.child("name").getValue();
                    String name = "";

                    if (nameObj instanceof String) {
                        name = (String) nameObj;
                    } else if (nameObj instanceof List) {
                        // If it's a list, grab the first item
                        List<?> list = (List<?>) nameObj;
                        if (!list.isEmpty() && list.get(0) instanceof String) {
                            name = (String) list.get(0);
                        }
                    }

                    if (name != null && !name.isEmpty() && !name.equalsIgnoreCase("Preoutfit") && !name.equalsIgnoreCase("Preoufit")) {
                        clothingTypesList.add(name);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(H6_RecommendationActivity.this, "Failed to load categories.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void loadClothesData() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("categories");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allItems.clear();
                for (DataSnapshot categorySnap : snapshot.getChildren()) {
                    DataSnapshot photosSnap = categorySnap.child("photos");
                    if (photosSnap.exists()) {
                        for (DataSnapshot itemSnap : photosSnap.getChildren()) {
                            String url = itemSnap.child("imageUrl").getValue(String.class);
                            String cat = itemSnap.child("category").getValue(String.class);

                            // 1. Extract SubTag
                            String sub = "";
                            DataSnapshot subTags = itemSnap.child("subTags");
                            if (subTags.exists()) {
                                for (DataSnapshot tag : subTags.getChildren()) {
                                    Object tagObj = tag.getValue();
                                    if (tagObj instanceof String) {
                                        sub = (String) tagObj;
                                        break;
                                    }
                                }
                            }

                            // 2. Extract Colors (Handle String or List)
                            List<String> detectedColors = new ArrayList<>();
                            Object colorObj = itemSnap.child("colors").getValue();

                            if (colorObj instanceof String) {
                                // Case: "Black" or "Black, White" -> Split into separate colors
                                String cStr = (String) colorObj;
                                for (String part : cStr.split(",")) {
                                    if (!part.trim().isEmpty()) {
                                        detectedColors.add(part.trim());
                                    }
                                }
                            } else if (colorObj instanceof List) {
                                // Case: ["Black", "White"] -> Add each as separate color
                                List<?> list = (List<?>) colorObj;
                                for (Object o : list) {
                                    if (o instanceof String) {
                                        String s = ((String) o).trim();
                                        if (!s.isEmpty()) {
                                            detectedColors.add(s);
                                        }
                                    }
                                }
                            }

                            if (detectedColors.isEmpty()) {
                                detectedColors.add("");
                            }


                            if (url != null) {
                                // Convert all colors in the list to lowercase for consistency
                                List<String> finalColors = new ArrayList<>();
                                for (String c : detectedColors) {
                                    finalColors.add(c.toLowerCase());
                                }

                                // Pass the LIST (finalColors), not a single string
                                allItems.add(new ClothingItem(
                                        itemSnap.getKey(),
                                        url,
                                        cat != null ? cat.toLowerCase() : "",
                                        sub != null ? sub.toLowerCase() : "",
                                        finalColors // <--- Pass the whole list here
                                ));

// --- FIX ENDS HERE ---

                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(H6_RecommendationActivity.this, "Failed to load items.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void showAddBoxMenu() {
        if (clothingTypesList.isEmpty()) {
            Toast.makeText(this, "Categories are loading or empty...", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Create a RecyclerView to hold the grid
        RecyclerView recyclerView = new RecyclerView(this);
        int padding = 32; // Padding around the grid
        recyclerView.setPadding(padding, padding, padding, padding);

        // Set Grid Layout with 2 columns
        // FIX: Use the standard androidx.recyclerview.widget.GridLayoutManager
        recyclerView.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 2));

        // 2. Create the Dialog
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Select Category")
                .setView(recyclerView)
                .setNegativeButton("Cancel", null)
                .create();

        // 3. Set the Adapter to populate the grid
        recyclerView.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {

            class CategoryHolder extends RecyclerView.ViewHolder {
                TextView text;

                public CategoryHolder(View itemView) {
                    super(itemView);
                    text = (TextView) ((CardView) itemView).getChildAt(0);
                }
            }

            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                CardView card = new CardView(parent.getContext());

                // Layout Params for the Card (Width, Height, Margins)
                // FIX: Use standard ViewGroup.LayoutParams constants and RecyclerView LayoutParams
                androidx.recyclerview.widget.GridLayoutManager.LayoutParams params =
                        new androidx.recyclerview.widget.GridLayoutManager.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                        );

                params.setMargins(16, 16, 16, 16); // Spacing between grid items
                card.setLayoutParams(params);

                card.setCardElevation(8f);
                card.setRadius(16f);
                card.setUseCompatPadding(true);

                // Create TextView inside the Card
                TextView tv = new TextView(parent.getContext());
                tv.setLayoutParams(new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                ));
                tv.setPadding(20, 60, 20, 60); // Padding inside the button
                tv.setGravity(Gravity.CENTER);
                tv.setTextSize(16f);
                tv.setTextColor(Color.BLACK);

                card.addView(tv);
                return new CategoryHolder(card);
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                CategoryHolder catHolder = (CategoryHolder) holder;
                String category = clothingTypesList.get(position);

                catHolder.text.setText(category);

                // Click Listener
                catHolder.itemView.setOnClickListener(v -> {
                    addMovableBox(category); // Call your existing logic
                    dialog.dismiss();        // Close the dialog
                });
            }

            @Override
            public int getItemCount() {
                return clothingTypesList.size();
            }
        });

        dialog.show();
    }
    private void addMovableBox(String category) {
        // ... [Keep your existing CardView creation code same as before] ...
        CardView card = new CardView(this);
        card.setId(View.generateViewId());

        BoxConfig config = BoxConfig.getSize(category);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(config.widthPx, config.heightPx);
        params.gravity = Gravity.CENTER;
        card.setLayoutParams(params);
        card.setRadius(20f);
        card.setCardElevation(0);
        card.setUseCompatPadding(false);
        card.setCardBackgroundColor(Color.TRANSPARENT);


        FrameLayout innerLayout = new FrameLayout(this);
        card.addView(innerLayout);

        ImageView img = new ImageView(this);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        img.setImageResource(android.R.drawable.ic_menu_gallery);
        img.setAlpha(0.9f);
        innerLayout.addView(img, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        ImageView lockIndicator = new ImageView(this);
        lockIndicator.setImageResource(android.R.drawable.ic_secure);
        lockIndicator.setColorFilter(Color.RED);
        lockIndicator.setVisibility(View.GONE);
        FrameLayout.LayoutParams lpLockInd = new FrameLayout.LayoutParams(40, 40);
        lpLockInd.gravity = Gravity.TOP | Gravity.END;
        lpLockInd.setMargins(0, 10, 10, 0);
        lockIndicator.setLayoutParams(lpLockInd);
        innerLayout.addView(lockIndicator);

        List<String> initialFilters = new ArrayList<>();
        initialFilters.add(category);
        // Tags: [0]=Filters, [1]=isLocked, [2]=url
        card.setTag(new Object[]{initialFilters, false, null});

        canvasContainer.addView(card);
        refreshBoxImage(card);

        // --- HANDLERS (Restored Drag & Click) ---
        card.setOnTouchListener(new View.OnTouchListener() {
            float dX, dY;
            float startX, startY;
            boolean isMoving = false;
            long startTime;
            boolean isLongPress = false;
            final Handler handler = new Handler();

            // Runnable for Long Press (Optional: remove if you only want Drag + Click)
            final Runnable longPressRunnable = () -> {
                isLongPress = true;
                currentlySelectedCard = card;
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (v != null) v.vibrate(50);
                showFloatingToolbar(card, lockIndicator);
            };

            @Override
            public boolean onTouch(View v, MotionEvent event) {
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
                        v.bringToFront();

                        // Start Long Press Timer
                        handler.postDelayed(longPressRunnable, 500);
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        // If moved significantly, it is a DRAG
                        if (Math.abs(event.getRawX() - startX) > 10 || Math.abs(event.getRawY() - startY) > 10) {
                            handler.removeCallbacks(longPressRunnable); // Cancel long press

                            if (!isLocked && !isLongPress) {
                                isMoving = true;
                                hideFloatingToolbar();
                                float parentWidth = canvasContainer.getWidth();
                                float parentHeight = canvasContainer.getHeight();
                                float newX = Math.max(0, Math.min(event.getRawX() + dX, parentWidth - v.getWidth()));
                                float newY = Math.max(0, Math.min(event.getRawY() + dY, parentHeight - v.getHeight()));
                                v.animate().x(newX).y(newY).setDuration(0).start();
                            }
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        handler.removeCallbacks(longPressRunnable); // Stop checking for long press
                        long duration = System.currentTimeMillis() - startTime;

                        // CLICK LOGIC: If not moving and not a long press
                        if (!isMoving && !isLongPress && duration < 500) {

                            // 1. CHECK THE MODE SWITCH
                            if (switchMode != null && switchMode.isChecked()) {
                                // --- SWITCH ON: SHOW FLOATING TOOLBAR ---
                                currentlySelectedCard = card;
                                showFloatingToolbar(card, lockIndicator);
                            } else {
                                // --- SWITCH OFF: MANUAL PICKER ---
                                boxBeingEdited = (CardView) v;
                                manualPickerContainer.setVisibility(View.VISIBLE);
                                loadManualImagesForCategory(category);
                                hideFloatingToolbar();
                            }
                        }
                        return true;
                }
                return false;
            }
        });
    }
    private LinearLayout floatingToolbar;
    private void hideFloatingToolbar() {
        if (floatingToolbar == null) {
            floatingToolbar = findViewById(R.id.floating_toolbar);
        }
        if (floatingToolbar != null) {
            floatingToolbar.setVisibility(View.GONE);
        }
    }
    private void showFloatingToolbar(CardView selectedCard, ImageView lockIndicator) {
        if (floatingToolbar == null) {
            floatingToolbar = findViewById(R.id.floating_toolbar);
        }

        if (floatingToolbar != null) {
            floatingToolbar.setVisibility(View.VISIBLE);
            floatingToolbar.bringToFront();

            // Get current state
            Object[] tags = (Object[]) selectedCard.getTag();
            List<String> filters = (List<String>) tags[0];
            boolean currentLockState = (boolean) tags[1];

            // Ensure filter list has enough slots for [Category, Subtag, Color]
            while (filters.size() < 3) filters.add("All");

            String currentCategory = filters.get(0);
            String currentSubtag = filters.get(1);

            // --- 1. Delete ---
            ImageButton btnDelete = findViewById(R.id.btn_toolbar_delete);
            btnDelete.setOnClickListener(v -> {
                canvasContainer.removeView(selectedCard);
                hideFloatingToolbar();
                currentlySelectedCard = null;
            });

            // --- 2. Plus (Scale Up) ---
            ImageButton btnPlus = findViewById(R.id.btn_toolbar_plus);
            btnPlus.setOnClickListener(v -> {
                float currentScale = selectedCard.getScaleX();
                if (currentScale < 2.0f) {
                    selectedCard.setScaleX(currentScale + 0.1f);
                    selectedCard.setScaleY(currentScale + 0.1f);
                }
            });

            // --- 3. Minus (Scale Down) ---
            ImageButton btnMinus = findViewById(R.id.btn_toolbar_minus);
            btnMinus.setOnClickListener(v -> {
                float currentScale = selectedCard.getScaleX();
                if (currentScale > 0.5f) {
                    selectedCard.setScaleX(currentScale - 0.1f);
                    selectedCard.setScaleY(currentScale - 0.1f);
                }
            });

            // --- 4. Lock ---
            ImageButton btnLock = findViewById(R.id.btn_toolbar_lock);
            btnLock.setImageResource(currentLockState ? android.R.drawable.ic_lock_lock : android.R.drawable.ic_lock_idle_lock);
            btnLock.setOnClickListener(v -> {
                boolean isLocked = (boolean) tags[1];
                tags[1] = !isLocked;
                selectedCard.setTag(tags);
                lockIndicator.setVisibility(!isLocked ? View.VISIBLE : View.GONE);
                btnLock.setImageResource(!isLocked ? android.R.drawable.ic_lock_lock : android.R.drawable.ic_lock_idle_lock);
                if (!isLocked) hideFloatingToolbar();
            });

            // --- 5. Subtag Filter Button ---
            ImageButton btnFilterSub = findViewById(R.id.btn_toolbar_filter_subtag);
            btnFilterSub.setVisibility(View.VISIBLE);
            btnFilterSub.setOnClickListener(v -> {
                showSubtagFilterDialog(selectedCard, currentCategory);
                hideFloatingToolbar();
            });
        }
    }
    private void showSubtagFilterDialog(CardView card, String category) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Type");

        // Create Layouts
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        com.google.android.material.chip.ChipGroup chipGroup = new com.google.android.material.chip.ChipGroup(this);
        chipGroup.setPadding(40, 20, 40, 20);
        chipGroup.setSingleSelection(true); // Only pick one type
        scrollView.addView(chipGroup);

        // Create the dialog object so we can dismiss it inside listeners
        final AlertDialog dialog = builder.create();

        // Prepare List of Options (Start with "All")
        List<String> options = new ArrayList<>();
        options.add("All");

        // Gather unique subtags from allItems
        for (ClothingItem item : allItems) {
            if (item.category.equalsIgnoreCase(category) && !item.subCategory.isEmpty()) {
                String sub = capitalize(item.subCategory);
                if (!options.contains(sub) && !sub.equalsIgnoreCase(category)) {
                    options.add(sub);
                }
            }
        }

        // Get current state
        Object[] tags = (Object[]) card.getTag();
        List<String> currentFilters = (List<String>) tags[0];
        String currentSub = currentFilters.size() > 1 ? currentFilters.get(1) : "All";

        // Create Chips
        for (String label : options) {
            com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
            chip.setText(label);
            chip.setCheckable(true);
            chip.setClickable(true);

            // Pre-check if matches current selection
            if (label.equalsIgnoreCase(currentSub)) {
                chip.setChecked(true);
            }

            // --- ACTION: Click -> Save -> Close -> Open Color Dialog ---
            chip.setOnClickListener(v -> {
                // 1. Update Data directly
                if (currentFilters.size() < 2) {
                    currentFilters.add(label);
                } else {
                    currentFilters.set(1, label);
                }

                // Update the UI label on the box immediately (optional, but good for feedback)
                applyFilterUpdate(card, label, 1);

                // 2. Dismiss this dialog
                dialog.dismiss();

                // 3. Automatically open the "Color" dialog
                showColorFilterDialog(card, category);
            });

            chipGroup.addView(chip);
        }

        dialog.setView(scrollView);
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel", (d, w) -> d.dismiss());
        dialog.show();
    }
    private void showColorFilterDialog(CardView card, String category) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Colors (Multiple)");

        // Create Layouts
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        com.google.android.material.chip.ChipGroup chipGroup = new com.google.android.material.chip.ChipGroup(this);
        chipGroup.setPadding(40, 20, 40, 20);
        chipGroup.setSingleSelection(false); // Allow multiple colors
        scrollView.addView(chipGroup);

            // 1. Get the filters from the selected box
            Object[] tags = (Object[]) card.getTag();
            List<String> filters = (List<String>) tags[0];

            // 2. Define the variables that were causing the error
            String currentCategory = filters.size() > 0 ? filters.get(0) : "All";
            // This defines the variable 'currentSubTag' that was missing:
            String currentSubTag = filters.size() > 1 ? filters.get(1) : "All";
        String currentColors = filters.size() > 2 ? filters.get(2) : "All";

            Set<String> availableColors = new HashSet<>();

            // 3. Now your loop will work because currentSubTag is defined
            for (ClothingItem item : allItems) {
                if (!item.category.equalsIgnoreCase(currentCategory)) {
                    continue;
                }

                // This line caused the error before, now it works:
                if (!currentSubTag.equalsIgnoreCase("All") && !currentSubTag.equalsIgnoreCase("none")) {
                    // Check if item.subCategory matches
                    if (item.subCategory == null || !item.subCategory.toLowerCase().contains(currentSubTag.toLowerCase())) {
                        continue;
                    }
                }

                // Add all colors from this item
                if (item.colors != null) {
                    for(String c : item.colors) {
                        availableColors.add(capitalize(c));
                    }
                }
            }






        List<String> sortedColors = new ArrayList<>(availableColors);
        java.util.Collections.sort(sortedColors);

        // "All" Chip Logic
        com.google.android.material.chip.Chip allChip = new com.google.android.material.chip.Chip(this);
        allChip.setText("All");
        allChip.setCheckable(true);
        allChip.setClickable(true);
        allChip.setChecked(currentColors.equals("All"));
        allChip.setOnClickListener(v -> {
            chipGroup.clearCheck();
            allChip.setChecked(true);
        });
        chipGroup.addView(allChip);

        // Dynamic Color Chips
        for (String color : sortedColors) {
            com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
            chip.setText(color);
            chip.setCheckable(true);
            chip.setClickable(true);

            // Check if previously selected (ignoring case)
            boolean isSelected = false;
            for(String s : selectedColorsList) {
                if(s.equalsIgnoreCase(color)) isSelected = true;
            }

            if (isSelected) {
                chip.setChecked(true);
                allChip.setChecked(false);
            }

            // If specific color clicked, uncheck "All"
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    allChip.setChecked(false);
                }
            });

            chipGroup.addView(chip);
        }

        builder.setView(scrollView);

        // Finish Button Logic
        builder.setPositiveButton("Finish", (dialog, which) -> {
            List<String> newSelection = new ArrayList<>();

            for (int i = 0; i < chipGroup.getChildCount(); i++) {
                View child = chipGroup.getChildAt(i);
                if (child instanceof com.google.android.material.chip.Chip) {
                    com.google.android.material.chip.Chip c = (com.google.android.material.chip.Chip) child;
                    if (c.isChecked() && !c.getText().toString().equals("All")) {
                        newSelection.add(c.getText().toString());
                    }
                }
            }

            String finalColorString;
            if (newSelection.isEmpty()) {
                finalColorString = "All";
            } else {
                // Join with commas
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    finalColorString = String.join(",", newSelection);
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (int k = 0; k < newSelection.size(); k++) {
                        sb.append(newSelection.get(k));
                        if (k < newSelection.size() - 1) sb.append(",");
                    }
                    finalColorString = sb.toString();
                }
            }

            // Update Data and Refresh
            applyFilterUpdate(card, finalColorString, 2);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    private void applyFilterUpdate(CardView card, String newValue, int index) {
        Object[] tags = (Object[]) card.getTag();
        List<String> filters = (List<String>) tags[0];

        // Ensure list is big enough
        while (filters.size() <= index) {
            filters.add("All");
        }
        filters.set(index, newValue);

        // Refresh the image based on new filters
        refreshBoxImage(card);
    }
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    private void refreshBoxImage(CardView card) {
        Object[] tags = (Object[]) card.getTag();
        if ((boolean) tags[1]) return; // Locked

        List<String> filters = (List<String>) tags[0];
        if (filters == null || filters.isEmpty()) return;

        // Ensure safe access
        String fCategory = filters.get(0);
        String fSubtag = filters.size() > 1 ? filters.get(1) : "All";
        String fColor = filters.size() > 2 ? filters.get(2) : "All";

        List<ClothingItem> candidates = new ArrayList<>();

        for (ClothingItem item : allItems) {
            // 1. Check Category
            boolean matchCategory = item.category.equalsIgnoreCase(fCategory);

            // 2. Check Subtag (SubCategory)
            boolean matchSub = fSubtag.equals("All") || item.subCategory.equalsIgnoreCase(fSubtag);

            // 3. Check Color (Supports multi-select like "Red,Blue")
            // 3. Check Color (Supports multi-select like "Red,Blue")
            boolean matchColor;
            if (fColor.equals("All")) {
                matchColor = true;
            } else {
                // This part needs to change.
                matchColor = false; // Assume no match until one is found.
                String[] selectedColors = fColor.toLowerCase().split(",");

                // Check if any of the item's colors are in the list of selected filter colors.
                if (item.colors != null && !item.colors.isEmpty()) {
                    for (String selectedColor : selectedColors) {
                        // Use .contains() for case-insensitive check by converting item's colors to lowercase
                        List<String> itemColorsLowerCase = new ArrayList<>();
                        for (String c : item.colors) {
                            itemColorsLowerCase.add(c.toLowerCase());
                        }

                        if (itemColorsLowerCase.contains(selectedColor.trim())) {
                            matchColor = true;
                            break; // Found a match, no need to check other colors.
                        }
                    }
                }
            }

            if (matchCategory && matchSub && matchColor) {
                candidates.add(item);
            }

        }

        ImageView imgView = (ImageView) ((FrameLayout) card.getChildAt(0)).getChildAt(0);

        if (!candidates.isEmpty()) {
            Random random = new Random();
            ClothingItem selected = candidates.get(random.nextInt(candidates.size()));
            tags[2] = selected.imageUrl; // Update current URL in tag
            imgView.setAlpha(1.0f);
            Glide.with(this).load(selected.imageUrl).centerCrop().into(imgView);
        } else {
            // No items match filters
            imgView.setImageResource(android.R.drawable.ic_menu_help);
            imgView.setAlpha(0.5f);
            tags[2] = null;
        }
    }
    private void shuffleAllBoxes() {
        OutfitGenerator.generate(
                this,
                canvasContainer,
                allItems,
                new OutfitGenerator.GeneratorCallback() {
                    @Override
                    public void onCombinationsGenerated(List<HistorySnapshot> generatedSnapshots) {
                        resultsList.clear();
                        resultsList.addAll(generatedSnapshots);
                        resultsAdapter.notifyDataSetChanged();
                        resultsContainer.setVisibility(View.VISIBLE);
                        Toast.makeText(H6_RecommendationActivity.this, "Generated " + generatedSnapshots.size() + " results!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFirstCombinationApplied(List<String> firstComboUrls) {
                    }
                }
        );
    }
    private void applySnapshotToCanvas(HistorySnapshot snapshot) {
        if (snapshot == null || snapshot.boxStates == null) return;
        for (BoxState state : snapshot.boxStates) {
            CardView card = canvasContainer.findViewById(state.viewId);
            if (card != null) {
                Object[] tags = (Object[]) card.getTag();
                boolean isLocked = false;
                if (tags != null && tags.length > 1 && tags[1] instanceof Boolean) {
                    isLocked = (boolean) tags[1];
                }
                if (isLocked) continue;

                card.setX(state.x);
                card.setY(state.y);
                ViewGroup.LayoutParams params = card.getLayoutParams();
                if (params.width != state.width || params.height != state.height) {
                    params.width = state.width;
                    params.height = state.height;
                    card.setLayoutParams(params);
                }
                if (tags != null && tags.length > 2) {
                    tags[2] = state.url;
                }
                if (card.getChildCount() > 0 && card.getChildAt(0) instanceof FrameLayout) {
                    FrameLayout inner = (FrameLayout) card.getChildAt(0);
                    if (inner.getChildCount() > 0 && inner.getChildAt(0) instanceof ImageView) {
                        ImageView imgView = (ImageView) inner.getChildAt(0);
                        Glide.with(this).load(state.url).centerCrop().into(imgView);
                    }
                }
            }
        }
    }
    public static class ClothingItem {
        public String id;
        public String imageUrl;
        public String category;
        public String subCategory;
        public List<String> colors; // <--- Must be List<String>

        // Update Constructor
        public ClothingItem(String id, String url, String category, String subCategory, List<String> colors) {
            this.id = id;
            this.imageUrl = url;
            this.category = category;
            this.subCategory = subCategory;
            this.colors = colors;
        }
    }
    public static class BoxState {
        int viewId;
        String url;
        float x, y;
        int width, height;

        public BoxState(int viewId, String url, float x, float y, int width, int height) {
            this.viewId = viewId;
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

        public HistorySnapshot(List<BoxState> states) {
            this.boxStates = states;
            this.timestamp = String.valueOf(System.currentTimeMillis());
        }
    }
    public static class BoxConfig {
        public int widthPx;
        public int heightPx;

        public BoxConfig(int w, int h) {
            widthPx = w;
            heightPx = h;
        }

        public static BoxConfig getSize(String category) {
            DisplayMetrics dm = Resources.getSystem().getDisplayMetrics();
            float screenW = dm.widthPixels;
            float screenH = dm.heightPixels;
            float density = dm.density;

            float baseWidth = screenW * 0.35f;
            float baseHeight = screenH * 0.20f;
            float w, h;

            switch (category.toLowerCase()) {
                case "hat":
                    w = baseWidth * 0.7f;
                    h = baseHeight * 0.48f;
                    break;
                case "accessories":
                    w = baseWidth * 0.60f;
                    h = baseHeight * 0.60f;
                    break;
                case "shoes":
                    w = baseWidth * 1.0f;
                    h = baseHeight * 0.56f;
                    break;
                case "bag":
                    w = baseWidth * 0.85f;
                    h = baseHeight * 0.75f;
                    break;
                case "outer":
                    w = baseWidth * 1.375f;
                    h = baseHeight * 1.52f;
                    break;
                case "top":
                    w = baseWidth * 1.25f;
                    h = baseHeight * 1.28f;
                    break;
                case "bottom":
                    w = baseWidth * 1.25f;
                    h = baseHeight * 1.44f;
                    break;
                case "dress":
                    w = baseWidth * 1.8f;
                    h = baseHeight * 2.2f;
                    break;

                default:
                    w = baseWidth;
                    h = baseHeight;
                    break;
            }

            float maxPx = 560f * density;
            if (w > maxPx) w = maxPx;
            if (h > maxPx) h = maxPx;

            float minPx = 90f * density;
            if (w < minPx) w = minPx;
            if (h < minPx) h = minPx;

            return new BoxConfig((int) w, (int) h);
        }
    }
    public static class ResultsAdapter extends RecyclerView.Adapter<ResultsAdapter.Holder> {
        private List<HistorySnapshot> list;
        private OnItemClickListener listener;
        private View canvas;

        public interface OnItemClickListener {
            void onItemClick(HistorySnapshot snapshot);
        }

        public ResultsAdapter(List<HistorySnapshot> list, OnItemClickListener listener, View canvas) {
            this.list = list;
            this.listener = listener;
            this.canvas = canvas;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_history_outfit, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            HistorySnapshot snapshot = list.get(position);
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(snapshot);
                }
            });

            holder.miniCanvas.removeAllViews();
            float originalCanvasWidth = canvas.getWidth();
            float originalCanvasHeight = canvas.getHeight();
            if (originalCanvasWidth == 0) {
                originalCanvasWidth = holder.itemView.getResources().getDisplayMetrics().widthPixels;
                originalCanvasHeight = holder.itemView.getResources().getDisplayMetrics().heightPixels;
            }

            int targetWidth = holder.itemView.getResources().getDisplayMetrics().widthPixels - 100;
            float scaleRatio = targetWidth / originalCanvasWidth;
            int targetHeight = (int) (originalCanvasHeight * scaleRatio);

            ViewGroup.LayoutParams canvasParams = holder.miniCanvas.getLayoutParams();
            canvasParams.width = targetWidth;
            canvasParams.height = targetHeight;
            holder.miniCanvas.setLayoutParams(canvasParams);

            for (BoxState state : snapshot.boxStates) {
                ImageView iv = new ImageView(holder.itemView.getContext());
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);

                int w = (int) (state.width * scaleRatio);
                int h = (int) (state.height * scaleRatio);
                int x = (int) (state.x * scaleRatio);
                int y = (int) (state.y * scaleRatio);

                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(w, h);
                lp.leftMargin = x;
                lp.topMargin = y;
                lp.gravity = Gravity.TOP | Gravity.START;

                iv.setLayoutParams(lp);
                iv.setBackgroundColor(Color.WHITE);
                iv.setPadding(2, 2, 2, 2);

                Glide.with(holder.itemView.getContext()).load(state.url).override(w, h).into(iv);
                holder.miniCanvas.addView(iv);
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        public class Holder extends RecyclerView.ViewHolder {
            FrameLayout miniCanvas;

            public Holder(@NonNull View itemView) {
                super(itemView);
                miniCanvas = itemView.findViewById(R.id.miniCanvas);
            }
        }
    }

}





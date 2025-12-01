package com.example.project;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
// Color Palette Import
import androidx.palette.graphics.Palette;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

// ML KIT IMPORTS
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.chip.Chip;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class F1_CameraActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int GALLERY_REQUEST_CODE = 101;

    // API KEYS
    private static final String REMOVE_BG_API_KEY = "NWtuUZKef3YtvncY3uhme1mX";

    // UI Components
    PreviewView previewView;
    ImageView imagePreview, backButton;
    Button btnTakePhoto, btnSave;
    ProgressBar progressBar;
    TextView tagsTextView;
    ChipGroup tagsChipGroup;

    // Logic Variables
    Bitmap originalBitmap;
    Bitmap processedBitmap;
    List<String> detectedTags = new ArrayList<>();
    // We'll use a local list inside showSaveDialog, or keep one here if needed globally
    // but for the dialog logic, local is usually cleaner. Keeping class member just in case.
    List<CategoryItem> categoryListItems = new ArrayList<>();

    private ImageCapture imageCapture;
    private Camera camera; // Needed for Zoom control

    // Store selections
    private String selectedClothingItem = "Unknown";
    private String selectedColor = "Unknown";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.f1_camera_act);

        initCloudinary();

        // Initialize Views
        previewView = findViewById(R.id.previewView);
        imagePreview = findViewById(R.id.imagePreview);
        backButton = findViewById(R.id.backButton);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);
        tagsTextView = findViewById(R.id.statusTextView);

        tagsChipGroup = findViewById(R.id.tagsChipGroup);

        // 1. SINGLE SELECTION: Enforce only one tag at a time
        tagsChipGroup.setSingleSelection(true);

        // Hide Save button initially
        btnSave.setVisibility(View.GONE);

        // Setup Buttons
        btnTakePhoto.setOnClickListener(view -> takePhoto());

        // ADVANCED: Long click "Take Photo" to open Gallery for testing
        btnTakePhoto.setOnLongClickListener(view -> {
            openGallery();
            return true;
        });

        btnSave.setOnClickListener(view -> showSaveDialog());
        backButton.setOnClickListener(v -> finish());

        // Start Camera if permissions granted
        if (checkSelfPermission()) {
            startCamera();
        }
    }

    // -------------------------- 1. ADVANCED CAMERAX SETUP (WITH ZOOM) --------------------------

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Use High Quality Capture
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();

                // Bind camera and store instance to control zoom
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

                // Enable Pinch-to-Zoom
                setupZoom();

            } catch (ExecutionException | InterruptedException e) {
                Log.e("CameraX", "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupZoom() {
        ScaleGestureDetector scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (camera != null) {
                    CameraControl control = camera.getCameraControl();
                    float currentZoom = camera.getCameraInfo().getZoomState().getValue().getZoomRatio();
                    float scale = detector.getScaleFactor();
                    control.setZoomRatio(currentZoom * scale);
                }
                return true;
            }
        });

        previewView.setOnTouchListener((view, event) -> {
            scaleGestureDetector.onTouchEvent(event);
            return true;
        });
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        progressBar.setVisibility(View.VISIBLE);
        tagsTextView.setText("Capturing High-Res Image...");
        btnSave.setVisibility(View.GONE); // Hide save button until done

        imageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                Bitmap bitmap = imageProxyToBitmap(image);
                image.close();
                handleImageResult(bitmap);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e("CameraX", "Capture failed", exception);
                progressBar.setVisibility(View.GONE);
                Toast.makeText(F1_CameraActivity.this, "Capture Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // -------------------------- 2. GALLERY HANDLING --------------------------

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, GALLERY_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(data.getData());
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                handleImageResult(bitmap);
            } catch (Exception e) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void handleImageResult(Bitmap bitmap) {
        if (bitmap != null) {
            // 1. Save original for ML
            originalBitmap = bitmap;

            // 2. Update UI
            previewView.setVisibility(View.GONE);
            imagePreview.setVisibility(View.VISIBLE);
            imagePreview.setImageBitmap(bitmap);

            // 3. Start Parallel Processing (Background Removal)
            tagsTextView.setText("Processing Image...");
            new RemoveBgTask().execute(bitmap);
        }
    }

    // Helper: Convert ImageProxy to Bitmap safely (Avoids Memory Crash)
    private Bitmap imageProxyToBitmap(ImageProxy image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);

        // Scale down to avoid massive memory usage
        int targetWidth = 1200;
        if (options.outWidth > targetWidth) {
            options.inSampleSize = options.outWidth / targetWidth;
        }

        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);

        if (bitmap != null) {
            Matrix matrix = new Matrix();
            matrix.postRotate(image.getImageInfo().getRotationDegrees());
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }
        return null;
    }

    // -------------------------- 3. SMART ML KIT LABELING --------------------------

    private void detectClothingLabels(Bitmap bitmap) {
        if (bitmap == null) return;

        // Reset state
        selectedClothingItem = "Unknown";
        selectedColor = "Unknown";
        btnSave.setVisibility(View.GONE); // Ensure hidden

        InputImage image = InputImage.fromBitmap(bitmap, 0);
        ImageLabeler labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);

        labeler.process(image)
                .addOnSuccessListener(labels -> {
                    detectedTags.clear();
                    tagsChipGroup.removeAllViews(); // Clear previous buttons

                    StringBuilder debugRawData = new StringBuilder();

                    for (ImageLabel label : labels) {
                        String text = label.getText();
                        float confidence = label.getConfidence();
                        debugRawData.append(text).append(" (").append((int)(confidence * 100)).append("%)\n");

                        String correctedTag = correctLabelMistakes(text);

                        if (confidence > 0.3 && isClothingItem(correctedTag)) {
                            if (!detectedTags.contains(correctedTag)) {
                                detectedTags.add(correctedTag);
                                // CREATE CLICKABLE CHIP for Clothing (isColorTag = false)
                                addTagChip(correctedTag, false);
                            }
                        }
                    }

                    if (detectedTags.isEmpty()) {
                        Toast.makeText(this, "No specific clothing detected.", Toast.LENGTH_SHORT).show();
                    } else {
                        tagsTextView.setText("Select the item:");
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "AI Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // Updated Helper method to create the button dynamically
    private void addTagChip(String text, boolean isColorTag) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setCheckable(true);
        chip.setClickable(true);

        // STYLE: Default state (Unselected)
        chip.setChipBackgroundColorResource(android.R.color.white);
        chip.setChipStrokeWidth(2);
        chip.setChipStrokeColorResource(android.R.color.holo_blue_dark);
        chip.setTextColor(getResources().getColor(android.R.color.black));

        if (isColorTag) {
            // Optional: Visual distinction for color tags
            chip.setChipStrokeColorResource(android.R.color.holo_green_dark);
        }

        // LISTENER: Handle Selection with `setOnCheckedChangeListener` for Single Selection support
        chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // HIGHLIGHT STATE
                if (isColorTag) {
                    // --- COLOR LOGIC ---
                    chip.setChipBackgroundColorResource(android.R.color.holo_green_light);
                    chip.setTextColor(getResources().getColor(android.R.color.black));

                    selectedColor = text;

                    // 1. Update Text to show final result
                    tagsTextView.setText("Selected: " + selectedClothingItem + ", " + selectedColor);

                    // 2. Show Save Button now that flow is complete
                    btnSave.setVisibility(View.VISIBLE);

                } else {
                    // --- CLOTHING ITEM LOGIC ---
                    // Temporarily highlight before removing views
                    chip.setChipBackgroundColorResource(android.R.color.holo_blue_dark);
                    chip.setTextColor(getResources().getColor(android.R.color.white));

                    selectedClothingItem = text;

                    // 1. Clear existing chips (remove clothing options)
                    tagsChipGroup.removeAllViews();

                    // 2. Show a small header or toast
                    tagsTextView.setText("Item: " + selectedClothingItem + ". Detecting Color...");
                    Toast.makeText(this, "Finding color for " + text + "...", Toast.LENGTH_SHORT).show();

                    // 3. Trigger color detection to populate new chips
                    detectDominantColor(originalBitmap);
                }
            } else {
                // UNHIGHLIGHT STATE (When deselected by SingleSelection)
                chip.setChipBackgroundColorResource(android.R.color.white);
                chip.setTextColor(getResources().getColor(android.R.color.black));

                if (isColorTag) {
                    btnSave.setVisibility(View.GONE); // Hide save if deselecting color
                }
            }
        });

        tagsChipGroup.addView(chip);
    }

    // POWERFUL FEATURE: Map wrong AI labels to correct clothing terms
    private String correctLabelMistakes(String label) {
        if (label.equalsIgnoreCase("Jersey")) return "Shirt";
        if (label.equalsIgnoreCase("Top")) return "Shirt";
        if (label.equalsIgnoreCase("Blouse")) return "Shirt";
        if (label.equalsIgnoreCase("Short")) return "Shorts"; // Fix "Short" vs "Shorts"
        if (label.equalsIgnoreCase("Miniskirt")) return "Skirt";
        return label;
    }

    private boolean isClothingItem(String label) {
        String[] clothingItems = {
                "Shirt", "Top", "T-shirt", "Pants", "Jeans", "Trousers",
                "Dress", "Skirt", "Coat", "Jacket", "Shoe", "Footwear",
                "Clothing", "Outerwear", "Fashion", "Apparel", "Blazer",
                "Sweater", "Cardigan", "Vest", "Suit", "Formal wear",
                "Shorts", "Sneakers", "Hoodie", "Active shirt", "Sportswear"
        };
        for (String item : clothingItems) {
            if (label.equalsIgnoreCase(item)) return true;
        }
        return false;
    }

    // -------------------------- 4. COLOR RECOGNITION HELPER --------------------------

    private void detectDominantColor(Bitmap bitmap) {
        if (bitmap == null) return;

        // Generate palette asynchronously
        Palette.from(bitmap).generate(palette -> {
            if (palette == null) return;

            tagsTextView.setText("Select color for: " + selectedClothingItem);

            List<String> colorOptions = new ArrayList<>();

            // 1. Vibrant (Often the main clothing color)
            if (palette.getVibrantSwatch() != null) {
                String name = getSimpleColorName(palette.getVibrantSwatch().getRgb());
                if (!colorOptions.contains(name)) colorOptions.add(name);
            }

            // 2. Dark Vibrant
            if (palette.getDarkVibrantSwatch() != null) {
                String name = getSimpleColorName(palette.getDarkVibrantSwatch().getRgb());
                if (!colorOptions.contains(name)) colorOptions.add(name);
            }

            // 3. Dominant (Overall average)
            if (palette.getDominantSwatch() != null) {
                String name = getSimpleColorName(palette.getDominantSwatch().getRgb());
                if (!colorOptions.contains(name)) colorOptions.add(name);
            }

            // Add standard fallbacks if palette is weird
            if (colorOptions.isEmpty()) {
                colorOptions.add("Black");
                colorOptions.add("White");
                colorOptions.add("Grey");
            }

            // Display the color options as chips
            for (String colorName : colorOptions) {
                addTagChip(colorName, true); // true = isColorTag
            }
        });
    }

    // Simplified Color Naming (Adjusted for better White vs Grey detection)
    private String getSimpleColorName(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = (color) & 0xFF;

        // 1. WHITE DETECTION
        if (r > 180 && g > 180 && b > 180) {
            if (Math.abs(r - g) < 30 && Math.abs(r - b) < 30 && Math.abs(g - b) < 30) {
                return "White";
            }
        }

        // 2. BLACK DETECTION
        if (r < 50 && g < 50 && b < 50) return "Black";

        // 3. GREY DETECTION
        if (Math.abs(r - g) < 20 && Math.abs(r - b) < 20 && Math.abs(g - b) < 20) {
            if (r > 80) return "Grey";
            return "Dark Grey";
        }

        // 4. COLOR DETECTION
        if (r > g + 50 && r > b + 50) {
            if (g > 150) return "Orange";
            return "Red";
        }

        if (g > r + 30 && g > b + 30) return "Green";

        if (b > r + 40 && b > g + 40) {
            if (r > 120 && g > 120) return "Light Blue";
            if (r < 60 && g < 60) return "Navy"; // Dark blue
            return "Blue";
        }

        if (r > 200 && g > 200 && b < 100) return "Yellow";
        if (r > 140 && g < 100 && b > 140) return "Purple";
        if (r > 200 && g < 160 && b > 160) return "Pink";

        if (r > 100 && g > 50 && b < 50 && r > b + 40) return "Brown";
        if (r > 180 && g > 160 && b > 130) return "Beige";

        return "Multi-color";
    }

    // -------------------------- 5. BACKGROUND REMOVAL --------------------------

    private class RemoveBgTask extends AsyncTask<Bitmap, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(Bitmap... bitmaps) {
            return removeBackground(bitmaps[0]);
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            progressBar.setVisibility(View.GONE);
            if (result != null) {
                processedBitmap = result;
                imagePreview.setImageBitmap(result); // Show processed image

                tagsTextView.setText("Analyzing details...");
                detectClothingLabels(originalBitmap);
            } else {
                tagsTextView.setText("Bg removal failed. Analyzing original...");
                processedBitmap = originalBitmap;
                detectClothingLabels(originalBitmap);
            }
        }
    }

    public Bitmap removeBackground(Bitmap bitmap) {
        try {
            OkHttpClient client = new OkHttpClient();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            // Use JPEG 80% quality for speed and reliability
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos);
            byte[] imageBytes = bos.toByteArray();

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image_file", "photo.jpg",
                            RequestBody.create(MediaType.parse("image/jpeg"), imageBytes))
                    .addFormDataPart("size", "auto")
                    .build();

            Request request = new Request.Builder()
                    .url("https://api.remove.bg/v1.0/removebg")
                    .addHeader("X-Api-Key", REMOVE_BG_API_KEY)
                    .post(requestBody)
                    .build();

            Response response = client.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                InputStream inputStream = response.body().byteStream();
                return BitmapFactory.decodeStream(inputStream);
            } else {
                Log.e("RemoveBG", "API Error: " + (response.body() != null ? response.body().string() : response.message()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // -------------------------- 6. SAVING & CLOUDINARY --------------------------

    private void showSaveDialog() {
        if (processedBitmap == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Ensure this layout exists in your resources
        View dialogView = LayoutInflater.from(this).inflate(R.layout.f3_camera_edit_dialog_save_image, null);
        builder.setView(dialogView);

        // 1. Find the EditText for Item Name and Auto-fill
        TextView etItemName = dialogView.findViewById(R.id.etItemName);
        String autoName = selectedClothingItem;
        if (!selectedColor.equals("Unknown")) {
            autoName += ", " + selectedColor;
        }
        etItemName.setText(autoName);

        Spinner spinner = dialogView.findViewById(R.id.spinnerCategories);
        ImageView btnAddCategory = dialogView.findViewById(R.id.btnAddCategory);
        View btnSaveCategory = dialogView.findViewById(R.id.btnSaveCategory);

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        AlertDialog dialog = builder.create();
        dialog.show();

        // ---------------------------------------------------------
        // UPDATE: Path excludes "closetData"
        // Path: Users -> uid -> categories
        // ---------------------------------------------------------
        DatabaseReference categoriesRef = FirebaseDatabase.getInstance()
                .getReference("Users").child(uid).child("categories");

        List<String> categoryNames = new ArrayList<>();
        categoryListItems.clear();

        // Load Categories
        categoriesRef.get().addOnSuccessListener(snapshot -> {
            categoryListItems.clear();
            categoryNames.clear();
            for (DataSnapshot catSnap : snapshot.getChildren()) {
                String name = catSnap.child("name").getValue(String.class);
                String id = catSnap.getKey();

                // Fallback if name is missing (use ID)
                if (name == null) name = id;

                if (name != null) {
                    categoryListItems.add(new CategoryItem(id, name));
                    categoryNames.add(name);
                }
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, categoryNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
        });

        // Add New Category Logic
        btnAddCategory.setOnClickListener(v -> {
            AlertDialog.Builder inputDialog = new AlertDialog.Builder(this);
            EditText input = new EditText(this);
            input.setHint("New category name");
            inputDialog.setView(input);

            inputDialog.setPositiveButton("Add", (d, which) -> {
                String newCatName = input.getText().toString().trim();
                if (!newCatName.isEmpty()) {

                    // ---------------------------------------------------------
                    // UPDATE: ID Generation (Name + "_id")
                    // ---------------------------------------------------------
                    String sanitized = newCatName.replaceAll("[.#$\\[\\]]", "");
                    String newCatId = sanitized + "_id";

                    Map<String, Object> catData = new HashMap<>();
                    catData.put("id", newCatId);
                    catData.put("name", newCatName);

                    // Save directly to .../categories/ID
                    categoriesRef.child(newCatId).setValue(catData);

                    // Update local list and spinner immediately
                    categoryListItems.add(new CategoryItem(newCatId, newCatName));
                    categoryNames.add(newCatName);
                    ((ArrayAdapter) spinner.getAdapter()).notifyDataSetChanged();
                    spinner.setSelection(categoryNames.size() - 1);
                }
            });
            inputDialog.setNegativeButton("Cancel", (d, w) -> d.dismiss());
            inputDialog.show();
        });

        // Save Button Logic
        btnSaveCategory.setOnClickListener(v -> {
            int selectedPos = spinner.getSelectedItemPosition();

            if (categoryListItems.isEmpty()) {
                Toast.makeText(this, "Categories are still loading...", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedPos < 0 || selectedPos >= categoryListItems.size()) {
                Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get the ID from our helper list
            String selectedCategoryId = categoryListItems.get(selectedPos).id;

            String finalItemName = etItemName.getText().toString().trim();
            if (finalItemName.isEmpty()) finalItemName = selectedClothingItem;
            selectedClothingItem = finalItemName;

            progressBar.setVisibility(View.VISIBLE);
            dialog.dismiss();

            File fileToUpload = bitmapToFile(processedBitmap, "closet_item.png");
            if (fileToUpload != null) {
                uploadToCloudinary(fileToUpload, uid, selectedCategoryId);
            }
        });
    }


    private void uploadToCloudinary(File file, String uid, String categoryId) {
        MediaManager.get().upload(file.getAbsolutePath())
                .option("folder", "ClosetImages/" + uid)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}
                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String secureUrl = (String) resultData.get("secure_url");
                        saveItemToFirebase(uid, categoryId, secureUrl);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(F1_CameraActivity.this, "Upload Failed: " + error.getDescription(), Toast.LENGTH_LONG).show();
                        });
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                })
                .dispatch();
    }

    private void saveItemToFirebase(String uid, String categoryId, String imageUrl) {
        // ---------------------------------------------------------
        // UPDATE: Save to categories -> catID -> photos
        // ---------------------------------------------------------
        DatabaseReference itemRef = FirebaseDatabase.getInstance()
                .getReference("Users").child(uid)
                .child("categories").child(categoryId).child("photos").push();

        Map<String, Object> itemData = new HashMap<>();

        // 1. Save URL
        itemData.put("url", imageUrl);

        String clothVal = selectedClothingItem;
        String colorVal = selectedColor;


        if (selectedClothingItem.contains(",")) {
            String[] parts = selectedClothingItem.split(",");
            if (parts.length > 0) clothVal = parts[0].trim();
            if (parts.length > 1) colorVal = parts[1].trim();
        }

        itemData.put("tagCloth", clothVal);
        itemData.put("tagColor", colorVal);

        itemRef.setValue(itemData).addOnSuccessListener(aVoid -> {
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(F1_CameraActivity.this, "Item Saved!", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }



    private File bitmapToFile(Bitmap bitmap, String fileName) {
        try {
            File f = new File(getCacheDir(), fileName);
            f.createNewFile();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos);
            byte[] bitmapdata = bos.toByteArray();
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(bitmapdata);
            fos.flush();
            fos.close();
            return f;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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

    private boolean checkSelfPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            }
        }
    }

    private static class CategoryItem {
        String id;
        String name;
        CategoryItem(String id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name; // Necessary for ArrayAdapter to display name
        }
    }
}

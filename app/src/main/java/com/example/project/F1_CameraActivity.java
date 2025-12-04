package com.example.project;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Space;

import androidx.annotation.NonNull;
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

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class F1_CameraActivity extends AppCompatActivity {

    // --- UI COMPONENTS ---
    private PreviewView previewView;
    private ImageView imagePreview;
    private ImageView backButton;
    private Button btnTakePhoto;
    private Button btnSave;
    private ProgressBar progressBar;
    private TextView statusTextView;

    // --- CAMERA VARS ---
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageCapture imageCapture;
    private Camera camera;
    private static final int PERMISSION_REQUEST_CODE = 100;

    // --- IMAGE VARS ---
    private Bitmap originalBitmap;
    private Bitmap processedBitmap;

    private List<String> clothingTypesList = new ArrayList<>();


    private final String[] CLOTHING_COLORS = {
            "Black", "White", "Grey", "Beige", "Red", "Blue",
            "Green", "Yellow", "Orange", "Purple", "Pink", "Brown", "Multi"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.f1_camera_act); // Ensure XML matches this name

        // --- NEW CODE: Handle Back Button to go to Feed ---
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(F1_CameraActivity.this, D1_FeedActivity.class);
                // Clear stack so they can't go "back" to calendar from feed easily
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });

        // 1. Initialize UI
        previewView = findViewById(R.id.previewView);
        imagePreview = findViewById(R.id.imagePreview);
        backButton = findViewById(R.id.backButton);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);
        statusTextView = findViewById(R.id.statusTextView);

        btnSave.setVisibility(View.GONE);

        // 2. Check Permissions & Start Camera
        if (checkSelfPermission()) {
            startCamera();
        }

        // 3. Set Listeners
        btnTakePhoto.setOnClickListener(v -> takePhoto());

        backButton.setOnClickListener(v -> {
            if (imagePreview.getVisibility() == View.VISIBLE) {
                resetToCamera();
            } else {
                finish();
            }
        });

        // If user cancels but wants to start over
        btnSave.setOnClickListener(v -> showCategorySelectionDialog());

        try {
            initCloudinary();
        } catch (Exception e) {
            // Already initialized
        }
            // 1. Add Default items (Safety net)

            fetchCategoriesFromFirebase();



    }

    private boolean checkSelfPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void fetchCategoriesFromFirebase() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) return;

        String uid = currentUser.getUid();
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Users")
                .child(uid)
                .child("categories");

        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    List<String> firebaseCategories = new ArrayList<>();

                    for (DataSnapshot data : snapshot.getChildren()) {
                        // 1. Try getting "name" field (based on your addNewCategoryToFirebase logic)
                        String name = data.child("name").getValue(String.class);

                        // 2. Fallback: If "name" is null, use the Key (e.g., "Hat")
                        if (name == null) {
                            name = data.getKey();
                        }

                        // 3. Only add if name exists and isn't empty
                        if (name != null && !name.trim().isEmpty()) {
                            firebaseCategories.add(name);
                        }
                    }

                    if (!firebaseCategories.isEmpty()) {
                        clothingTypesList.clear();
                        clothingTypesList.addAll(firebaseCategories);
                        Collections.sort(clothingTypesList);
                        Log.d("F1_Camera", "Loaded categories: " + clothingTypesList.toString());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("F1_Camera", "Db Error", error.toException());
            }
        });
    }





    private void initCloudinary() {
        Map<String, Object> config = new HashMap<>();
        config.put("cloud_name", BuildConfig.CLOUDINARY_CLOUD_NAME);
        config.put("api_key", BuildConfig.CLOUDINARY_API_KEY);
        config.put("api_secret", BuildConfig.CLOUDINARY_API_SECRET);
        MediaManager.init(this, config);
    }

    private void resetToCamera() {
        imagePreview.setVisibility(View.GONE);
        previewView.setVisibility(View.VISIBLE);
        statusTextView.setText("Ready");
        btnSave.setVisibility(View.GONE);
        btnTakePhoto.setVisibility(View.VISIBLE);
    }

    // -------------------------- 1. CAMERA SETUP --------------------------

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

                setupZoom();

            } catch (ExecutionException | InterruptedException e) {
                Log.e("CameraX", "Binding failed", e);
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
                    control.setZoomRatio(currentZoom * detector.getScaleFactor());
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
        statusTextView.setText("Capturing...");

        imageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                Bitmap bitmap = imageProxyToBitmap(image);
                image.close();
                handleImageResult(bitmap);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(F1_CameraActivity.this, "Capture Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // -------------------------- 2. IMAGE HANDLING --------------------------

    private void handleImageResult(Bitmap bitmap) {
        if (bitmap != null) {
            originalBitmap = bitmap;
            previewView.setVisibility(View.GONE);
            imagePreview.setVisibility(View.VISIBLE);
            imagePreview.setImageBitmap(bitmap);

            btnTakePhoto.setVisibility(View.GONE);
            statusTextView.setText("Processing Image...");

            new RemoveBgTask().execute(bitmap);
        }
    }

    private Bitmap imageProxyToBitmap(ImageProxy image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        BitmapFactory.Options options = new BitmapFactory.Options();

        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);

        int targetWidth = 1000;
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

    // -------------------------- 3. BACKGROUND REMOVAL TASK --------------------------

    private class RemoveBgTask extends AsyncTask<Bitmap, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(Bitmap... bitmaps) {
            // Placeholder for BG removal logic
            return bitmaps[0];
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            progressBar.setVisibility(View.GONE);
            processedBitmap = (result != null) ? result : originalBitmap;
            imagePreview.setImageBitmap(processedBitmap);

            // Start the Step-by-Step Dialog Flow immediately
            showCategorySelectionDialog();

            btnSave.setText("Restart Tagging");
            btnSave.setVisibility(View.VISIBLE);
            statusTextView.setText("Select Tags");
        }
    }

    // -------------------------- 4. SUB-TAG DATA LOGIC --------------------------

    private List<String> getSubTags(String category) {
        switch (category) {
            case "All": return Arrays.asList("all", "any", "everything");
            case "Hat": return Arrays.asList("cap", "beanie", "bucket hat", "beret", "snapback", "visor");
            case "Accessories": return Arrays.asList("belt", "scarf", "glasses", "sunglasses", "watch", "bracelet", "earrings");
            case "Outer": return Arrays.asList("jacket", "coat", "hoodie", "blazer", "cardigan", "sweater", "windbreaker");
            case "Top": return Arrays.asList("shirt", "tshirt", "longsleeve", "blouse", "hoodie", "tanktop", "crop top");
            case "Bag": return Arrays.asList("handbag", "crossbody", "backpack", "tote", "purse");
            case "Bottom": return Arrays.asList("pants", "jeans", "shorts", "skirt", "trousers", "cargo", "leggings");
            case "Shoes": return Arrays.asList("sneakers", "boots", "heels", "sandals", "slippers", "loafers");
            case "Dress": return Arrays.asList("dress", "gown", "casual dress", "long dress", "mini dress");

            default: return new ArrayList<>(); // Returns empty list for unknown categories
        }
    }

    // -------------------------- 5. MULTI-STEP POPUP DIALOGS --------------------------

    // STEP 1: Main Category
    private void showNewCategoryInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 60, 60, 60);
        layout.setBackgroundResource(R.drawable.round_image_clip); // Assuming you have this drawable

        TextView title = new TextView(this);
        title.setText("New Category Name");
        title.setTextSize(20f);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        // Input field for the new category
        EditText input = new EditText(this);
        input.setHint("e.g. Scarf, Hat");
        layout.addView(input);

        Space space = new Space(this);
        space.setMinimumHeight(40);
        layout.addView(space);

        Button btnCreate = new Button(this);
        btnCreate.setText("Create & Continue");
        btnCreate.setBackgroundColor(Color.BLACK);
        btnCreate.setTextColor(Color.WHITE);
        layout.addView(btnCreate);

        builder.setView(layout);
        AlertDialog dialog = builder.create();

        btnCreate.setOnClickListener(v -> {
            String newCategoryName = input.getText().toString().trim();
            if (!newCategoryName.isEmpty()) {
                // Capitalize first letter for consistency
                String formattedName = newCategoryName.substring(0, 1).toUpperCase() + newCategoryName.substring(1);

                dialog.dismiss();

                // 1. Save this new category to Firebase
                addNewCategoryToFirebase(formattedName);

                // 2. Continue the flow (Go to Step 2: Sub-Category)
                // Since it's a new category, showSubCategorySelectionDialog will handle it as "unknown" and ask for details
                showSubCategorySelectionDialog(formattedName);
            } else {
                input.setError("Please enter a name");
            }
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        dialog.show();
    }

    private void showCategorySelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(60, 60, 60, 60);
        mainLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        mainLayout.setBackgroundResource(R.drawable.round_image_clip);

        TextView title = new TextView(this);
        title.setText("1. What is this item?");
        title.setTextSize(22f);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 30);
        mainLayout.addView(title);

        ScrollView scrollView = new ScrollView(this);
        ChipGroup chipGroup = new ChipGroup(this);
        chipGroup.setPadding(10, 10, 10, 10);
        chipGroup.setSingleSelection(true);

        builder.setView(mainLayout);
        AlertDialog dialog = builder.create();

        // Loop through the dynamic list fetched from Firebase
        for (String type : clothingTypesList) {
            // Filter out "Pre - Outfit"
            if (type.equals("PreOutfit")) {
                continue;
            }

            addChip(chipGroup, type, v -> {
                dialog.dismiss();
                showSubCategorySelectionDialog(type);
            });
        }

        // Logic for "Others": Open a dialog to CREATE a new category
        addChip(chipGroup, "Other", v -> {
            dialog.dismiss();
            showNewCategoryInputDialog();
        });

        scrollView.addView(chipGroup);
        mainLayout.addView(scrollView);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        dialog.show();
    }
    private void addNewCategoryToFirebase(String categoryName) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();
        String sanitizedName = categoryName.replaceAll("[.#$\\[\\]]", "");
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Users")
                .child(uid)
                .child("categories");

        // Check if it already exists locally to avoid duplicate logic, though Firebase handles overwrites
        if (!clothingTypesList.contains(categoryName)) {
            clothingTypesList.add(categoryName);

            String safeId = sanitizedName;
            Map<String, Object> categoryData = new HashMap<>();
            categoryData.put("id", safeId);
            categoryData.put("name", categoryName);


            dbRef.child(categoryName).setValue(categoryData)
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Category Added!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to add category", Toast.LENGTH_SHORT).show());
        }
    }

    // STEP 2: Sub-Category (Handles both Chips and Manual Entry)
    private void showSubCategorySelectionDialog(String mainCategory) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(60, 60, 60, 60);
        mainLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        mainLayout.setBackgroundResource(R.drawable.round_image_clip);

        TextView title = new TextView(this);
        title.setText("2. Type of " + mainCategory + "?");
        title.setTextSize(22f);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 30);
        mainLayout.addView(title);

        List<String> subTags = getSubTags(mainCategory);

        builder.setView(mainLayout);
        AlertDialog dialog = builder.create();

        // LOGIC: IF subTags exist, show Chips. IF NOT, show EditText.
        if (!subTags.isEmpty()) {
            // --- SCENARIO A: KNOWN CATEGORY (Show Chips) ---
            ScrollView scrollView = new ScrollView(this);
            ChipGroup chipGroup = new ChipGroup(this);
            chipGroup.setPadding(10, 10, 10, 10);
            chipGroup.setSingleSelection(true);

            for (String subTag : subTags) {
                String displayTag = subTag.substring(0, 1).toUpperCase() + subTag.substring(1);
                addChip(chipGroup, displayTag, v -> {
                    dialog.dismiss();
                    List<String> selection = new ArrayList<>();
                    selection.add(displayTag);
                    showColorSelectionDialog(mainCategory, selection);
                });
            }

            // Option for manual entry even if chips exist
            addChip(chipGroup, "Other ", v -> {
                dialog.dismiss();
                List<String> selection = new ArrayList<>();
                selection.add("Other");
                showColorSelectionDialog(mainCategory, selection);
            });

            scrollView.addView(chipGroup);
            mainLayout.addView(scrollView);

        } else {
            // --- SCENARIO B: NEW/UNKNOWN CATEGORY (Show EditText) ---
            EditText editText = new EditText(this);
            editText.setHint("what do you want to add");
            editText.setBackgroundResource(android.R.drawable.edit_text);
            editText.setPadding(30, 30, 30, 30);
            mainLayout.addView(editText);

            Space space = new Space(this);
            space.setMinimumHeight(40);
            mainLayout.addView(space);

            Button btnNext = new Button(this);
            btnNext.setText("Next >");
            btnNext.setBackgroundColor(Color.BLACK);
            btnNext.setTextColor(Color.WHITE);

            btnNext.setOnClickListener(v -> {
                String typedText = editText.getText().toString().trim();
                if (!typedText.isEmpty()) {
                    dialog.dismiss();
                    List<String> selection = new ArrayList<>();
                    selection.add(typedText);
                    showColorSelectionDialog(mainCategory, selection);
                } else {
                    editText.setError("Please describe the item");
                }
            });
            mainLayout.addView(btnNext);
        }

        Button btnBack = new Button(this);
        btnBack.setText("<< Back");
        btnBack.setBackgroundColor(Color.TRANSPARENT);
        btnBack.setTextColor(Color.GRAY);
        btnBack.setOnClickListener(v -> {
            dialog.dismiss();
            showCategorySelectionDialog();
        });
        mainLayout.addView(btnBack);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        dialog.show();
    }

    // STEP 3: Color Selection
    private void showColorSelectionDialog(String category, List<String> subTags) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(60, 60, 60, 60);
        mainLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        mainLayout.setBackgroundResource(R.drawable.round_image_clip);

        TextView title = new TextView(this);
        title.setText("3. What color is it?");
        title.setTextSize(22f);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 30);
        mainLayout.addView(title);

        ScrollView scrollView = new ScrollView(this);
        ChipGroup chipGroup = new ChipGroup(this);
        chipGroup.setPadding(10, 10, 10, 10);
        chipGroup.setSingleSelection(false); // Allow multiple colors

        builder.setView(mainLayout);
        AlertDialog dialog = builder.create();

        final List<String> selectedColors = new ArrayList<>();

        for (String color : CLOTHING_COLORS) {
            Chip chip = new Chip(this);
            chip.setText(color);
            chip.setCheckable(true);
            chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));

            // Color circles
            int colorValue;
            switch(color) {
                case "Black": colorValue = Color.BLACK; break;
                case "White": colorValue = Color.WHITE; break;
                case "Red": colorValue = Color.RED; break;
                case "Blue": colorValue = Color.BLUE; break;
                case "Green": colorValue = Color.GREEN; break;
                case "Yellow": colorValue = Color.YELLOW; break;
                case "Orange": colorValue = Color.parseColor("#FFA500"); break;
                case "Purple": colorValue = Color.parseColor("#800080"); break;
                case "Pink": colorValue = Color.parseColor("#FFC0CB"); break;
                case "Brown": colorValue = Color.parseColor("#A52A2A"); break;
                default: colorValue = Color.GRAY; break;
            }

            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(colorValue);
            drawable.setStroke(2, Color.GRAY); // Border for visibility (esp white)
            drawable.setSize(40, 40);
            drawable.setBounds(0, 0, 40, 40);
            chip.setChipIcon(drawable);
            chip.setChipIconVisible(true);

            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedColors.add(color);
                    chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#D1C4E9")));
                } else {
                    selectedColors.remove(color);
                    chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
                }
            });
            chipGroup.addView(chip);
        }

        addChip(chipGroup, "Others", v -> {
            dialog.dismiss();
            showSubCategorySelectionDialog("");
        });

        scrollView.addView(chipGroup);
        mainLayout.addView(scrollView);

        // Finish / Upload Button
        Button btnFinish = new Button(this);
        btnFinish.setText("Save & Upload");
        btnFinish.setBackgroundColor(Color.BLACK);
        btnFinish.setTextColor(Color.WHITE);
        btnFinish.setOnClickListener(v -> {
            if (!selectedColors.isEmpty()) {
                dialog.dismiss();
                uploadToCloudinary(processedBitmap, category, subTags, selectedColors);
            } else {
                Toast.makeText(this, "Select at least one color", Toast.LENGTH_SHORT).show();
            }
        });
        mainLayout.addView(btnFinish);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        dialog.show();
    }

    // -------------------------- 6. UPLOAD & SAVE LOGIC --------------------------

    private void uploadToCloudinary(Bitmap bitmap, String category, List<String> subTags, List<String> colors) {
        progressBar.setVisibility(View.VISIBLE);
        statusTextView.setText("Uploading...");

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        MediaManager.get().upload(byteArray)
                // 1. This sets the fixed folder path "Categories/Photos"
                // This removes the specific category name (like "Top") from the folder structure
                .option("folder", "CategoriesPhotos")

                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("secure_url");
                        // 2. We still pass the 'category' to Firebase so the database knows what it is
                        saveToFirebase(imageUrl, category, subTags, colors);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        progressBar.setVisibility(View.GONE);
                        statusTextView.setText("Upload Failed");
                        Toast.makeText(F1_CameraActivity.this, "Upload Error: " + error.getDescription(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                })
                .dispatch();
    }


    private void saveToFirebase(String imageUrl, String category, List<String> subTags, List<String> colors) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("Users")
                .child(uid)
                .child("categories")
                .child(category)
                .child("photos");
        // Or whatever your database node is named

        String itemId = databaseRef.push().getKey();

        if (itemId != null) {
            Map<String, Object> itemData = new HashMap<>();
            itemData.put("imageUrl", imageUrl);
            itemData.put("category", category);
            itemData.put("subTags", subTags);
            itemData.put("colors", colors);
            itemData.put("timestamp", ServerValue.TIMESTAMP);

            databaseRef.child(itemId).setValue(itemData)
                    .addOnSuccessListener(aVoid -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(F1_CameraActivity.this, "Item Saved Successfully!", Toast.LENGTH_SHORT).show();
                        navigateToCloset();
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(F1_CameraActivity.this, "Failed to save metadata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }


    // Helper to add Chips
    private void addChip(ChipGroup group, String text, View.OnClickListener listener) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setClickable(true);
        chip.setCheckable(true);
        chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
        chip.setOnClickListener(listener);
        group.addView(chip);
    }

    private void navigateToCloset() {
        Intent intent = new Intent(F1_CameraActivity.this, G1_ClosetActivity.class);
        // Clear the back stack so the user can't press "Back" to return to the camera
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // Close the Camera Activity
    }

}

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
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

// --- OKHTTP IMPORTS FOR BG REMOVAL ---
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

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
        setContentView(R.layout.f1_camera_act);

        // --- Handle Back Button to go to Feed ---
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(F1_CameraActivity.this, D_FeedActivity.class);
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

        // If user cancels but wants to start over (Fallback)
        btnSave.setOnClickListener(v -> showCategorySelectionDialog());

        try {
            initCloudinary();
        } catch (Exception e) {
            // Already initialized
        }

        // 4. Load Categories
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
        clothingTypesList.clear();
        for (CategoryManager.CategoryItem item : CategoryManager.getCategories()) {
            clothingTypesList.add(item.name);
        }
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

            // --- AUTOMATICALLY REMOVE BACKGROUND HERE ---
            removeBackground(bitmap);
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

    // -------------------------- 3. AUTOMATIC BACKGROUND REMOVAL --------------------------

    private void removeBackground(Bitmap originalBitmap) {
        // 1. Show loading
        runOnUiThread(() -> {
            progressBar.setVisibility(View.VISIBLE);
            statusTextView.setText("Removing Background...");
        });

        // 2. Convert Bitmap to Byte Array
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        originalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        // 3. Build API Request
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image_file", "image.jpg",
                        RequestBody.create(byteArray, MediaType.parse("image/jpeg")))
                .addFormDataPart("size", "auto")
                .build();

        Request request = new Request.Builder()
                .url("https://api.remove.bg/v1.0/removebg")
                // --- YOUR API KEY IS HERE ---
                .addHeader("X-Api-Key", "qqik5T9DJ5XyZBGXzA52Xe6B")
                .post(requestBody)
                .build();

        // 4. Execute Request
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(F1_CameraActivity.this, "Bg Removal Failed (Net)", Toast.LENGTH_SHORT).show();

                    // Fallback: Use original image
                    processedBitmap = originalBitmap;
                    onBgRemovalFinished();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    byte[] responseBytes = response.body().bytes();
                    Bitmap noBgBitmap = BitmapFactory.decodeByteArray(responseBytes, 0, responseBytes.length);

                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        // SUCCESS: Update processedBitmap
                        processedBitmap = noBgBitmap;
                        imagePreview.setImageBitmap(processedBitmap); // Update preview to show transparent image
                        Toast.makeText(F1_CameraActivity.this, "Background Removed!", Toast.LENGTH_SHORT).show();

                        onBgRemovalFinished();
                    });
                } else {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(F1_CameraActivity.this, "Bg Error: " + response.code(), Toast.LENGTH_SHORT).show();

                        // Fallback: Use original image
                        processedBitmap = originalBitmap;
                        onBgRemovalFinished();
                    });
                }
            }
        });
    }

    // Helper to trigger the next step after BG removal
    private void onBgRemovalFinished() {
        btnSave.setText("Restart Tagging");
        btnSave.setVisibility(View.VISIBLE);
        statusTextView.setText("Select Tags");
        // Automatically open the dialog
        showCategorySelectionDialog();
    }

    // -------------------------- 4. SUB-TAG DATA LOGIC --------------------------

    private List<String> getSubTags(String category) {
        switch (category) {
            case "Intimates": return Arrays.asList("Bra", "Underwear", "Base layer");
            case "Tops": return Arrays.asList("T-shirt", "Blouse", "Sweater", "Shirt", "Tank top");
            case "Bottoms": return Arrays.asList("Pants", "Skirt", "Shorts", "Jeans", "Leggings");
            case "One-piece": return Arrays.asList("Dress", "Jumpsuit");
            case "Outerwear": return Arrays.asList("Jacket", "Coat", "Blazer", "Hoodie");
            case "Swimwear": return Arrays.asList("Bikini", "Trunks");
            case "Footwear": return Arrays.asList("Shoes", "Boots", "Sandals", "Sneakers", "Heels");
            case "Headwear": return Arrays.asList("Hat", "Headband", "Cap", "Beanie");
            default: return new ArrayList<>();
        }
    }

    // -------------------------- 5. MULTI-STEP POPUP DIALOGS --------------------------

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

        for (CategoryManager.CategoryItem item : CategoryManager.getCategories()) {
            addChip(chipGroup, item.name, v -> {
                dialog.dismiss();
                showSubCategorySelectionDialog(item.id, item.name);
            });
        }

        scrollView.addView(chipGroup);
        mainLayout.addView(scrollView);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        dialog.show();
    }

    // STEP 2: Sub-Category
    private void showSubCategorySelectionDialog(String categoryId, String mainCategory) {
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

        if (!subTags.isEmpty()) {
            ScrollView scrollView = new ScrollView(this);
            ChipGroup chipGroup = new ChipGroup(this);
            chipGroup.setPadding(10, 10, 10, 10);
            chipGroup.setSingleSelection(true);

            for (String subTag : subTags) {
                addChip(chipGroup, subTag, v -> {
                    dialog.dismiss();
                    List<String> selection = new ArrayList<>();
                    selection.add(subTag);
                    showColorSelectionDialog(categoryId, mainCategory, selection);
                });
            }

            scrollView.addView(chipGroup);
            mainLayout.addView(scrollView);

        } else {
            TextView noSub = new TextView(this);
            noSub.setText("Skip to color selection.");
            noSub.setPadding(0, 20, 0, 20);
            mainLayout.addView(noSub);

            addChip(mainLayout, "Continue", v -> {
                dialog.dismiss();
                showColorSelectionDialog(categoryId, mainCategory, new ArrayList<>());
            });
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
    private void showColorSelectionDialog(String categoryId, String mainCategory, List<String> subTags) {
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
        chipGroup.setSingleSelection(false);

        builder.setView(mainLayout);
        AlertDialog dialog = builder.create();

        final List<String> selectedColors = new ArrayList<>();

        for (String color : CLOTHING_COLORS) {
            Chip chip = new Chip(this);
            chip.setText(color);
            chip.setCheckable(true);
            chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));

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
            drawable.setStroke(2, Color.GRAY);
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

        scrollView.addView(chipGroup);
        mainLayout.addView(scrollView);

        Button btnFinish = new Button(this);
        btnFinish.setText("Save & Upload");
        btnFinish.setBackgroundColor(Color.BLACK);
        btnFinish.setTextColor(Color.WHITE);
        btnFinish.setOnClickListener(v -> {
            if (!selectedColors.isEmpty()) {
                dialog.dismiss();
                uploadToCloudinary(processedBitmap, categoryId, mainCategory, subTags, selectedColors);
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

    private void uploadToCloudinary(Bitmap bitmap, String categoryId, String categoryName, List<String> subTags, List<String> colors) {
        progressBar.setVisibility(View.VISIBLE);
        statusTextView.setText("Uploading...");

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        MediaManager.get().upload(byteArray)
                .option("folder", "CategoriesPhotos")
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("secure_url");
                        saveToFirebase(imageUrl, categoryId, categoryName, subTags, colors);
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

    private void saveToFirebase(String imageUrl, String categoryId, String categoryName, List<String> subTags, List<String> colors) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("Users")
                .child(uid)
                .child("categories")
                .child(categoryId)
                .child("photos");

        String itemId = databaseRef.push().getKey();

        if (itemId != null) {
            Map<String, Object> itemData = new HashMap<>();
            itemData.put("imageUrl", imageUrl);
            itemData.put("category", categoryName);
            itemData.put("categoryId", categoryId);
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

    private void addChip(LinearLayout layout, String text, View.OnClickListener listener) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setOnClickListener(listener);
        layout.addView(btn);
    }

    private void navigateToCloset() {
        Intent intent = new Intent(F1_CameraActivity.this, G1_ClosetActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}

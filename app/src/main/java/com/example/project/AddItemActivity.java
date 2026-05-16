package com.example.project;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.card.MaterialCardView;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.widget.LinearLayout;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropActivity;
import com.yalantis.ucrop.model.AspectRatio;
import java.util.Objects;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.widget.Button;
import androidx.palette.graphics.Palette;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.HapticFeedbackConstants;
import androidx.core.graphics.ColorUtils;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.ObjectKey;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import ai.fal.client.AsyncFalClient;
import ai.fal.client.ClientConfig;
import ai.fal.client.SubscribeOptions;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import android.graphics.Canvas;
import java.io.FileOutputStream;
import android.util.Log;

public class AddItemActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_CUSTOM_OCCASIONS = "custom_occasions";
    private static final String KEY_HIDDEN_DEFAULT_OCCASIONS = "hidden_default_occasions";
    private boolean isPhotoSelected = false;

    private LinearLayout llPhotoOptions;
    private MaterialCardView cvPhotoPreview;
    private MaterialCardView cvCropPhoto;
    private ImageView ivPhotoPreview;
    private MaterialCardView cvRemovePhoto;
    private View clPreviewContainer;
    private EditText etName;
    private AsyncFalClient falClient;
    private Uri currentPhotoUri;
    private List<ColorOption> selectedColorOptions = new ArrayList<>();
    private String selectedCategoryId = "";

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    openCamera();
                } else {
                    Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> squareCameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        handleImageSelection(uri);
                    }
                }
            });

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    startCrop(uri);
                }
            });

    private final ActivityResultLauncher<Intent> cropLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    final Uri resultUri = UCrop.getOutput(result.getData());
                    if (resultUri != null) {
                        handleImageSelection(resultUri);
                    }
                } else if (result.getResultCode() == UCrop.RESULT_ERROR) {
                    final Throwable cropError = UCrop.getError(result.getData());
                    if (cropError != null) {
                        Toast.makeText(this, "Crop error: " + cropError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);

        llPhotoOptions = findViewById(R.id.ll_photo_options);
        cvPhotoPreview = findViewById(R.id.cv_photo_preview);
        cvCropPhoto = findViewById(R.id.cv_crop_photo);
        ivPhotoPreview = findViewById(R.id.iv_photo_preview);
        cvRemovePhoto = findViewById(R.id.cv_remove_photo);
        clPreviewContainer = findViewById(R.id.cl_preview_container);
        etName = findViewById(R.id.et_name);

        // Initialize FalClient
        try {
            falClient = AsyncFalClient.withConfig(
                    ClientConfig.builder()
                            .withCredentials(() -> BuildConfig.FAL_KEY)
                            .build()
            );
        } catch (Exception e) {
            Log.e("AddItemActivity", "Failed to initialize FalClient", e);
        }

        // Initialize Cloudinary
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("cloud_name", BuildConfig.CLOUDINARY_CLOUD_NAME);
            config.put("api_key", BuildConfig.CLOUDINARY_API_KEY);
            config.put("api_secret", BuildConfig.CLOUDINARY_API_SECRET);
            MediaManager.init(this, config);
        } catch (IllegalStateException e) {
            // Already initialized
        }

        TextView tvSize = findViewById(R.id.tv_size);
        TextView tvCategory = findViewById(R.id.tv_category);
        ChipGroup cgOccasions = findViewById(R.id.cg_occasions);
        
        setupInitialOccasions(cgOccasions);
        loadCustomOccasions(cgOccasions);

        findViewById(R.id.cv_close).setOnClickListener(v -> finish());

        findViewById(R.id.rl_size_dropdown).setOnClickListener(v -> {
            SizeSelectionBottomSheet bottomSheet = SizeSelectionBottomSheet.newInstance(tvSize.getText().toString(), size -> {
                tvSize.setText(size);
                tvSize.setTextColor(Color.parseColor("#1A1C1E"));
                findViewById(R.id.tv_size_asterisk).setVisibility(View.GONE);
                findViewById(R.id.rl_size_dropdown).setBackgroundResource(R.drawable.bg_input_field);
            });
            bottomSheet.show(getSupportFragmentManager(), "SizeSelection");
        });

        findViewById(R.id.rl_category_dropdown).setOnClickListener(v -> {
            CategorySelectionBottomSheet bottomSheet = CategorySelectionBottomSheet.newInstance(selectedCategoryId, categoryId -> {
                selectedCategoryId = categoryId;
                CategoryManager.CategoryItem item = CategoryManager.getCategoryById(categoryId);
                if (item != null) {
                    tvCategory.setText(item.name);
                } else {
                    tvCategory.setText(categoryId);
                }
                tvCategory.setTextColor(Color.parseColor("#1A1C1E"));
                findViewById(R.id.tv_category_asterisk).setVisibility(View.GONE);
                findViewById(R.id.rl_category_dropdown).setBackgroundResource(R.drawable.bg_input_field);
            });
            bottomSheet.show(getSupportFragmentManager(), "CategorySelection");
        });

        findViewById(R.id.chip_add_occasion).setOnClickListener(v -> showAddOccasionDialog(cgOccasions));

        findViewById(R.id.btn_save).setOnClickListener(v -> {
            if (validateFields()) {
                saveItemData();
            } else {
                Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
            }
        });

        // setupColorSelection();
        
        findViewById(R.id.ll_take_photo).setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA);
            }
        });

        findViewById(R.id.ll_from_gallery).setOnClickListener(v -> {
            pickImageLauncher.launch("image/*");
        });

        cvRemovePhoto.setOnClickListener(v -> {
            isPhotoSelected = false;
            clPreviewContainer.setVisibility(View.GONE);
            llPhotoOptions.setVisibility(View.VISIBLE);
            ivPhotoPreview.setImageDrawable(null);
            currentPhotoUri = null;
        });

        cvCropPhoto.setOnClickListener(v -> {
            if (currentPhotoUri != null) {
                startCrop(currentPhotoUri);
            }
        });

        // Eyedropper takes priority over full preview on the photo itself
        // Moved to showColorPickerPopup as requested
        // setupEyedropper();

        findViewById(R.id.rl_color_selection).setOnClickListener(v -> {
            if (currentPhotoUri != null) {
                showColorPickerPopup(currentPhotoUri);
            } else {
                Toast.makeText(this, "Please select a photo first", Toast.LENGTH_SHORT).show();
            }
        });

        // Handle category from intent
        String categoryIdFromIntent = getIntent().getStringExtra("CATEGORY_ID");
        if (categoryIdFromIntent != null && !categoryIdFromIntent.isEmpty()) {
            selectedCategoryId = categoryIdFromIntent;
            CategoryManager.CategoryItem item = CategoryManager.getCategoryById(categoryIdFromIntent);
            if (item != null) {
                tvCategory.setText(item.name);
            } else {
                tvCategory.setText(categoryIdFromIntent);
            }
            tvCategory.setTextColor(Color.parseColor("#1A1C1E"));
            findViewById(R.id.tv_category_asterisk).setVisibility(View.GONE);
            findViewById(R.id.rl_category_dropdown).setBackgroundResource(R.drawable.bg_input_field);
        }
    }

    private void showColorPickerPopup(Uri uri) {
        android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_color_picker_popup);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        ImageView ivPicker = dialog.findViewById(R.id.iv_color_picker_photo);
        View btnClose = dialog.findViewById(R.id.cv_close_picker);

        Glide.with(this)
                .load(uri)
                .into(ivPicker);

        ivPicker.setOnTouchListener((v, event) -> {
            Drawable drawable = ivPicker.getDrawable();
            if (!(drawable instanceof BitmapDrawable)) return false;

            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            float[] imageCoords = getBitmapCoordsForFitCenter(event.getX(), event.getY(), ivPicker, bitmap);
            int x = (int) imageCoords[0];
            int y = (int) imageCoords[1];

            if (x >= 0 && x < bitmap.getWidth() && y >= 0 && y < bitmap.getHeight()) {
                int pixel = bitmap.getPixel(x, y);
                ColorNameHelper.ColorEntry closest = ColorNameHelper.getClosestColor(pixel);

                if (closest != null) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        v.performClick();
                        handleEyedropperColor(closest, dialog);
                    }
                }
            }
            return true;
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private float[] getBitmapCoordsForFitCenter(float touchX, float touchY, ImageView imageView, Bitmap bitmap) {
        float[] coords = new float[2];

        int viewWidth = imageView.getWidth();
        int viewHeight = imageView.getHeight();
        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();

        float scale;
        float dx = 0, dy = 0;

        if (viewWidth * bitmapHeight > viewHeight * bitmapWidth) {
            // View is wider than bitmap (aspect-ratio-wise)
            scale = (float) viewHeight / (float) bitmapHeight;
            dx = (viewWidth - bitmapWidth * scale) / 2f;
        } else {
            // View is taller than bitmap (aspect-ratio-wise)
            scale = (float) viewWidth / (float) bitmapWidth;
            dy = (viewHeight - bitmapHeight * scale) / 2f;
        }

        coords[0] = (touchX - dx) / scale;
        coords[1] = (touchY - dy) / scale;

        return coords;
    }


    private void handleEyedropperColor(ColorNameHelper.ColorEntry colorEntry, android.app.Dialog pickerDialog) {
        if (selectedColorOptions.isEmpty()) {
            // No colors selected, just add it
            selectedColorOptions.add(new ColorOption(colorEntry.name, colorEntry.hex));
            updateColorDisplay();
            pickerDialog.dismiss();
        } else {
            // Colors already exist, show dialog to add or change
            showColorAddOrChangeDialog(colorEntry, pickerDialog);
        }
    }

    private void showColorAddOrChangeDialog(ColorNameHelper.ColorEntry colorEntry, android.app.Dialog pickerDialog) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_color_choice, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        TextView tvMessage = dialogView.findViewById(R.id.tv_dialog_message);
        LinearLayout llExistingColors = dialogView.findViewById(R.id.ll_existing_colors);
        TextView tvReplaceInstruction = dialogView.findViewById(R.id.tv_replace_instruction);
        MaterialCardView cvPreview = dialogView.findViewById(R.id.cv_color_preview);
        ImageView ivIcon = dialogView.findViewById(R.id.iv_dialog_icon);
        
        setCardAndIconColor(cvPreview, ivIcon, colorEntry.hex);

        View btnReplace = dialogView.findViewById(R.id.btn_replace);
        View btnAdd = dialogView.findViewById(R.id.btn_add);
        View btnCancel = dialogView.findViewById(R.id.btn_cancel);

        tvTitle.setText("Pick " + colorEntry.name);

        if (selectedColorOptions.isEmpty()) {
            tvMessage.setText("Do you want to add this color?");
        } else {
            tvMessage.setText("Do you want to add this color or manage existing ones?");
            llExistingColors.setVisibility(View.VISIBLE);
            tvReplaceInstruction.setVisibility(View.VISIBLE);
            
            for (int i = 0; i < selectedColorOptions.size(); i++) {
                final int index = i;
                ColorOption existingColor = selectedColorOptions.get(i);
                View colorView = getLayoutInflater().inflate(R.layout.item_color_preview_selectable, llExistingColors, false);
                View colorFill = colorView.findViewById(R.id.v_color_fill);
                ImageView removeIcon = colorView.findViewById(R.id.iv_remove_icon);
                
                colorFill.setBackgroundColor(Color.parseColor(existingColor.getHexCode()));
                
                if (ColorUtils.calculateLuminance(Color.parseColor(existingColor.getHexCode())) > 0.5) {
                    removeIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#6C28D9")));
                }

                colorView.setOnClickListener(v1 -> {
                    selectedColorOptions.remove(index);
                    updateColorDisplay();
                    // Refresh the dialog list
                    llExistingColors.removeView(colorView);
                    if (selectedColorOptions.isEmpty()) {
                        llExistingColors.setVisibility(View.GONE);
                        tvReplaceInstruction.setVisibility(View.GONE);
                        btnReplace.setVisibility(View.GONE);
                        tvMessage.setText("Do you want to add this color?");
                    }
                });
                
                llExistingColors.addView(colorView);
            }
        }

        btnReplace.setOnClickListener(v -> {
            // Replace everything with the new color
            selectedColorOptions.clear();
            selectedColorOptions.add(new ColorOption(colorEntry.name, colorEntry.hex));
            updateColorDisplay();
            pickerDialog.dismiss();
            dialog.dismiss();
        });

        btnAdd.setOnClickListener(v -> {
            boolean exists = selectedColorOptions.stream()
                    .anyMatch(co -> co.getName().equalsIgnoreCase(colorEntry.name));
            if (!exists) {
                selectedColorOptions.add(new ColorOption(colorEntry.name, colorEntry.hex));
                updateColorDisplay();
            }
            pickerDialog.dismiss();
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }



    private void openCamera() {
        Intent intent = new Intent(this, SquareCameraActivity.class);
        squareCameraLauncher.launch(intent);
    }

    private void startCrop(Uri uri) {
        Uri sourceUri = uri;
        try {
            // Load and pad the bitmap to square with black background
            Bitmap originalBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            if (originalBitmap != null) {
                Bitmap paddedBitmap = padBitmapToSquare(originalBitmap);
                
                // Save padded bitmap to a temporary file
                File cachePath = new File(getCacheDir(), "images");
                if (!cachePath.exists()) cachePath.mkdirs();
                File paddedFile = new File(cachePath, "padded_" + System.currentTimeMillis() + ".jpg");
                FileOutputStream out = new FileOutputStream(paddedFile);
                paddedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                out.close();
                
                sourceUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", paddedFile);
            }
        } catch (IOException e) {
            Log.e("AddItemActivity", "Error padding bitmap", e);
            // Fallback to original uri
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "Crop_" + timeStamp + ".jpg";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File destFile = new File(storageDir, imageFileName);
        Uri destUri = Uri.fromFile(destFile);

        UCrop.Options options = new UCrop.Options();
        options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
        options.setCompressionQuality(90);
        
        // UI Customization for better UX
        options.setToolbarColor(ContextCompat.getColor(this, R.color.white));
        options.setToolbarWidgetColor(Color.parseColor("#1A1C1E"));
        options.setToolbarTitle("Edit Photo");
        options.setActiveControlsWidgetColor(Color.parseColor("#6C28D9"));

        // Status bar and fitsSystemWindows are handled via Theme.UCrop.Project in themes.xml
        options.setStatusBarColor(ContextCompat.getColor(this, R.color.white));
        
        // --- CROP FLEXIBILITY OPTIMIZATION ---
        // Allow zooming out of image bounds to pad with background
        options.setFreeStyleCropEnabled(false); // Lock to square ratio strictly
        options.setHideBottomControls(false);  // Show controls
        options.setCircleDimmedLayer(false);   
        options.setShowCropFrame(true);
        options.setShowCropGrid(true);
        
        // Lock to 1:1 only and hide other ratio options
        options.setAspectRatioOptions(0, new AspectRatio("1:1", 1, 1));
        
        // This allows moving the image outside the square crop frame
        // and zooming out beyond the original image size.
        options.setAllowedGestures(UCropActivity.ALL, UCropActivity.ALL, UCropActivity.ALL);
        
        // Ensure the background is black to match the padding we added
        options.setRootViewBackgroundColor(Color.BLACK);
        
        // Visibility optimization
        options.setDimmedLayerColor(Color.parseColor("#80000000"));
        options.setCropFrameColor(Color.WHITE);
        options.setCropGridColor(Color.WHITE);
        options.setCropFrameStrokeWidth(3);
        options.setCropGridStrokeWidth(2);
        
        // Animation
        options.setImageToCropBoundsAnimDuration(666);

        Intent intent = UCrop.of(sourceUri, destUri)
                .withAspectRatio(1, 1) // Force square
                .withOptions(options)
                .getIntent(this);
        cropLauncher.launch(intent);
    }

    private Bitmap padBitmapToSquare(Bitmap srcBmp) {
        int width = srcBmp.getWidth();
        int height = srcBmp.getHeight();
        int size = Math.max(width, height);

        Bitmap dstBmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(dstBmp);
        
        // Fill background with black
        canvas.drawColor(Color.BLACK);
        
        // Center the image
        float left = (size - width) / 2f;
        float top = (size - height) / 2f;
        
        canvas.drawBitmap(srcBmp, left, top, null);
        
        return dstBmp;
    }

    private void handleImageSelection(Uri uri) {
        currentPhotoUri = uri;
        isPhotoSelected = true;
        findViewById(R.id.tv_photo_asterisk).setVisibility(View.GONE);
        resetPhotoError();

        llPhotoOptions.setVisibility(View.GONE);
        clPreviewContainer.setVisibility(View.VISIBLE);

        Glide.with(this)
                .load(uri)
                .fitCenter()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .signature(new ObjectKey(System.currentTimeMillis()))
                .into(ivPhotoPreview);

        analyzeImageWithAI(uri);
    }

    private void analyzeImageWithAI(Uri uri) {
        View aiInsight = findViewById(R.id.cv_ai_insight);
        TextView tvAiStatus = findViewById(R.id.tv_ai_status);
        TextView tvAiMessage = findViewById(R.id.tv_ai_message);
        View pbAiLoading = findViewById(R.id.pb_ai_loading);

        aiInsight.setVisibility(View.VISIBLE);
        tvAiStatus.setText("AI is analyzing your item...");
        tvAiMessage.setText("Hang tight! I'm identifying the name, colors, and best occasions for this piece.");
        pbAiLoading.setVisibility(View.VISIBLE);

        // We need a URL for Fal.ai Vision. Upload to Cloudinary first.
        MediaManager.get().upload(uri)
                .option("folder", "TempAIAnalysis")
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("secure_url");
                        callAiVisionAPI(imageUrl);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        runOnUiThread(() -> {
                            tvAiStatus.setText("AI Analysis failed");
                            tvAiMessage.setText("Could not upload image for analysis.");
                            pbAiLoading.setVisibility(View.GONE);
                        });
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                })
                .dispatch();
    }

    private void callAiVisionAPI(String imageUrl) {
        if (falClient == null) return;

        String prompt = "Analyze this clothing item image and provide details. " +
                "Respond ONLY with a JSON object in this format: " +
                "{\"name\": \"short descriptive name\", \"color\": \"one main color name\", " +
                "\"colorHex\": \"#HEXCODE\", \"season\": \"Spring/Summer/Fall/Winter\", " +
                "\"occasions\": [\"Casual\", \"Formal\", \"Work\", \"Party\", \"Sport\"] (select relevant)}";

        List<String> imageUrls = new ArrayList<>();
        imageUrls.add(imageUrl);

        Map<String, Object> input = new HashMap<>();
        input.put("image_urls", imageUrls);
        input.put("prompt", prompt);
        input.put("model", "google/gemini-2.0-flash-001");

        falClient.subscribe("openrouter/router/vision",
                SubscribeOptions.<JsonObject>builder()
                        .input(input)
                        .resultType(JsonObject.class)
                        .build()
        ).whenComplete((result, throwable) -> {
            runOnUiThread(() -> {
                View pbAiLoading = findViewById(R.id.pb_ai_loading);
                TextView tvAiStatus = findViewById(R.id.tv_ai_status);
                TextView tvAiMessage = findViewById(R.id.tv_ai_message);
                View ivCloseAi = findViewById(R.id.iv_close_ai);
                pbAiLoading.setVisibility(View.GONE);
                ivCloseAi.setVisibility(View.VISIBLE);
                
                ivCloseAi.setOnClickListener(v -> {
                    findViewById(R.id.cv_ai_insight).setVisibility(View.GONE);
                });

                if (throwable != null) {
                    tvAiStatus.setText("AI Analysis failed");
                    tvAiMessage.setText("Error: " + throwable.getMessage());
                    return;
                }

                if (result != null && result.getData().has("output")) {
                    try {
                        String aiResponse = result.getData().get("output").getAsString();
                        // Strip potential markdown code blocks
                        aiResponse = aiResponse.replaceAll("```json", "").replaceAll("```", "").trim();
                        
                        JsonObject json = JsonParser.parseString(aiResponse).getAsJsonObject();
                        applyAiResults(json);

                        tvAiStatus.setText("Analysis Complete!");
                        tvAiMessage.setText("I've filled in the details for you. Feel free to adjust them!");
                        
                        // Auto-hide after 5 seconds
                        new android.os.Handler().postDelayed(() -> {
                            findViewById(R.id.cv_ai_insight).animate().alpha(0f).setDuration(500).withEndAction(() -> {
                                findViewById(R.id.cv_ai_insight).setVisibility(View.GONE);
                                findViewById(R.id.cv_ai_insight).setAlpha(1f);
                            }).start();
                        }, 5000);

                    } catch (Exception e) {
                        tvAiStatus.setText("AI Analysis Error");
                        tvAiMessage.setText("Could not parse AI response.");
                        Log.e("AddItemActivity", "JSON Parse Error: " + e.getMessage());
                    }
                }
            });
        });
    }

    private void applyAiResults(JsonObject json) {
        // Clear previous AI or manual selections to avoid mixing
        ChipGroup cgSeasons = findViewById(R.id.cg_seasons);
        ChipGroup cgOccasions = findViewById(R.id.cg_occasions);
        cgSeasons.clearCheck();
        cgOccasions.clearCheck();

        // Name
        if (json.has("name")) {
            etName.setText(json.get("name").getAsString());
        }

        // Color
        if (json.has("color") && json.has("colorHex")) {
            String colorName = json.get("color").getAsString();
            String colorHex = json.get("colorHex").getAsString();
            
            // Check if color name is valid/standard or just add it
            selectedColorOptions.clear();
            selectedColorOptions.add(new ColorOption(colorName, colorHex));
            updateColorDisplay();
        }

        // Season
        if (json.has("season")) {
            String season = json.get("season").getAsString();
            for (int i = 0; i < cgSeasons.getChildCount(); i++) {
                View child = cgSeasons.getChildAt(i);
                if (child instanceof Chip) {
                    Chip chip = (Chip) child;
                    if (chip.getText().toString().equalsIgnoreCase(season)) {
                        chip.setChecked(true);
                    }
                }
            }
        }

        // Occasions
        if (json.has("occasions")) {
            JsonArray occasions = json.getAsJsonArray("occasions");
            for (int j = 0; j < occasions.size(); j++) {
                String occName = occasions.get(j).getAsString();
                for (int i = 0; i < cgOccasions.getChildCount(); i++) {
                    View child = cgOccasions.getChildAt(i);
                    if (child instanceof Chip) {
                        Chip chip = (Chip) child;
                        if (chip.getText().toString().equalsIgnoreCase(occName)) {
                            chip.setChecked(true);
                        }
                    }
                }
            }
        }
    }



    private void resetPhotoError() {
        MaterialCardView cv1 = findViewById(R.id.ll_take_photo);
        MaterialCardView cv2 = findViewById(R.id.ll_from_gallery);
        cv1.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#D1D5DB")));
        cv1.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#F3F4F9")));
        cv2.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#D1D5DB")));
        cv2.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#F3F4F9")));
    }

    private boolean validateFields() {
        boolean isValid = true;

        // Name check
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            findViewById(R.id.tv_name_asterisk).setVisibility(View.VISIBLE);
            etName.setBackgroundResource(R.drawable.bg_input_field_error);
            isValid = false;
        } else {
            findViewById(R.id.tv_name_asterisk).setVisibility(View.GONE);
            etName.setBackgroundResource(R.drawable.bg_input_field);
        }

        // Photo check
        if (!isPhotoSelected) {
            findViewById(R.id.tv_photo_asterisk).setVisibility(View.VISIBLE);
            MaterialCardView cv1 = findViewById(R.id.ll_take_photo);
            MaterialCardView cv2 = findViewById(R.id.ll_from_gallery);
            cv1.setStrokeColor(ColorStateList.valueOf(Color.RED));
            cv1.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#FFF5F5")));
            cv2.setStrokeColor(ColorStateList.valueOf(Color.RED));
            cv2.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#FFF5F5")));
            isValid = false;
        }
        
        // Category check
        TextView tvCategory = findViewById(R.id.tv_category);
        if (tvCategory.getText().toString().equals("Select category")) {
            findViewById(R.id.tv_category_asterisk).setVisibility(View.VISIBLE);
            findViewById(R.id.rl_category_dropdown).setBackgroundResource(R.drawable.bg_input_field_error);
            isValid = false;
        }
        
        // Size check
        TextView tvSize = findViewById(R.id.tv_size);
        if (tvSize.getText().toString().equals("Select size")) {
            findViewById(R.id.tv_size_asterisk).setVisibility(View.VISIBLE);
            findViewById(R.id.rl_size_dropdown).setBackgroundResource(R.drawable.bg_input_field_error);
            isValid = false;
        }
        
        // Color check
        if (selectedColorOptions.isEmpty()) {
            findViewById(R.id.tv_color_asterisk).setVisibility(View.VISIBLE);
            findViewById(R.id.rl_color_selection).setBackgroundResource(R.drawable.bg_input_field_error);
            isValid = false;
        } else {
            findViewById(R.id.tv_color_asterisk).setVisibility(View.GONE);
            findViewById(R.id.rl_color_selection).setBackgroundResource(R.drawable.bg_input_field);
        }
        
        return isValid;
    }

    private void showAddOccasionDialog(ChipGroup chipGroup) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_custom_occasion, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            // Show keyboard automatically
            dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        EditText etName = dialogView.findViewById(R.id.et_occasion_name);
        View btnAdd = dialogView.findViewById(R.id.btn_add_occasion);

        // Auto-focus the EditText
        etName.requestFocus();

        btnAdd.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (!name.isEmpty()) {
                addNewOccasionChip(chipGroup, name, true);
                dialog.dismiss();
            } else {
                etName.setError("Name required");
            }
        });

        dialog.show();
    }

    private void setupInitialOccasions(ChipGroup chipGroup) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Set<String> hiddenDefaults = prefs.getStringSet(KEY_HIDDEN_DEFAULT_OCCASIONS, new HashSet<>());

        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            View child = chipGroup.getChildAt(i);
            if (child instanceof Chip && child.getId() != R.id.chip_add_occasion) {
                Chip chip = (Chip) child;
                String name = chip.getText().toString();
                
                if (hiddenDefaults.contains(name)) {
                    chip.setVisibility(View.GONE);
                }
                setupChipLongClick(chipGroup, chip, name, false);
            }
        }
    }

    private void setupChipLongClick(ChipGroup chipGroup, Chip chip, String name, boolean isCustom) {
        chip.setOnLongClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            showDeleteConfirmationDialog(chipGroup, chip, name, isCustom);
            return true;
        });
    }

    private void showDeleteConfirmationDialog(ChipGroup chipGroup, Chip chip, String name, boolean isCustom) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_confirm_delete, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        TextView tvMessage = dialogView.findViewById(R.id.tv_dialog_message);
        View btnCancel = dialogView.findViewById(R.id.btn_cancel);
        View btnDelete = dialogView.findViewById(R.id.btn_delete);

        tvTitle.setText("Delete '" + name + "'?");
        tvMessage.setText("Are you sure you want to remove this occasion? You can always add it back later.");

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnDelete.setOnClickListener(v -> {
            if (isCustom) {
                chipGroup.removeView(chip);
                removeCustomOccasion(name);
            } else {
                chip.setVisibility(View.GONE);
                chip.setChecked(false); // Uncheck it if it was checked
                saveHiddenDefaultOccasion(name);
            }
            Toast.makeText(this, "Occasion deleted", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void addNewOccasionChip(ChipGroup chipGroup, String name, boolean saveToPrefs) {
        // Check for duplicates (including hidden ones)
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            View child = chipGroup.getChildAt(i);
            if (child instanceof Chip) {
                Chip existingChip = (Chip) child;
                if (existingChip.getText().toString().equalsIgnoreCase(name)) {
                    if (existingChip.getVisibility() == View.GONE) {
                        // Restore hidden default
                        existingChip.setVisibility(View.VISIBLE);
                        removeHiddenDefaultOccasion(name);
                        return;
                    }
                    if (saveToPrefs) {
                        Toast.makeText(this, "Occasion already exists", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
            }
        }

        Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_occasion_chip, chipGroup, false);
        chip.setText(name);
        setupChipLongClick(chipGroup, chip, name, true);
        
        int childCount = chipGroup.getChildCount();
        chipGroup.addView(chip, childCount - 1);

        if (saveToPrefs) {
            saveCustomOccasion(name);
        }
    }

    private void saveCustomOccasion(String name) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Set<String> customOccasions = prefs.getStringSet(KEY_CUSTOM_OCCASIONS, new HashSet<>());
        Set<String> newSet = new HashSet<>(customOccasions);
        newSet.add(name);
        prefs.edit().putStringSet(KEY_CUSTOM_OCCASIONS, newSet).apply();
        
        // If it was a hidden default, remove from hidden
        removeHiddenDefaultOccasion(name);
    }

    private void removeCustomOccasion(String name) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Set<String> customOccasions = prefs.getStringSet(KEY_CUSTOM_OCCASIONS, new HashSet<>());
        Set<String> newSet = new HashSet<>(customOccasions);
        newSet.remove(name);
        prefs.edit().putStringSet(KEY_CUSTOM_OCCASIONS, newSet).apply();
    }

    private void saveHiddenDefaultOccasion(String name) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Set<String> hiddenDefaults = prefs.getStringSet(KEY_HIDDEN_DEFAULT_OCCASIONS, new HashSet<>());
        Set<String> newSet = new HashSet<>(hiddenDefaults);
        newSet.add(name);
        prefs.edit().putStringSet(KEY_HIDDEN_DEFAULT_OCCASIONS, newSet).apply();
    }

    private void removeHiddenDefaultOccasion(String name) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Set<String> hiddenDefaults = prefs.getStringSet(KEY_HIDDEN_DEFAULT_OCCASIONS, new HashSet<>());
        if (hiddenDefaults.contains(name)) {
            Set<String> newSet = new HashSet<>(hiddenDefaults);
            newSet.remove(name);
            prefs.edit().putStringSet(KEY_HIDDEN_DEFAULT_OCCASIONS, newSet).apply();
        }
    }

    private void loadCustomOccasions(ChipGroup chipGroup) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Set<String> customOccasions = prefs.getStringSet(KEY_CUSTOM_OCCASIONS, new HashSet<>());
        for (String name : customOccasions) {
            addNewOccasionChip(chipGroup, name, false);
        }
    }


    private void updateColorDisplay() {
        TextView tvColorLabel = findViewById(R.id.tv_selected_color);
        LinearLayout llColorsContainer = findViewById(R.id.ll_selected_colors_container);
        MaterialCardView cvWand = findViewById(R.id.cv_color_wand);
        ImageView ivWand = findViewById(R.id.iv_color_wand);
        
        llColorsContainer.removeAllViews();

        if (selectedColorOptions.isEmpty()) {
            tvColorLabel.setText("Tap photo for color");
            tvColorLabel.setTextColor(Color.parseColor("#74777F"));
            cvWand.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#F3F4F9")));
            ivWand.setImageTintList(ColorStateList.valueOf(Color.parseColor("#6C28D9")));
        } else {
            tvColorLabel.setTextColor(Color.parseColor("#1A1C1E"));
            findViewById(R.id.tv_color_asterisk).setVisibility(View.GONE);
            findViewById(R.id.rl_color_selection).setBackgroundResource(R.drawable.bg_input_field);

            setCardAndIconColor(cvWand, ivWand, selectedColorOptions.get(0).getHexCode());

            String colorNames = selectedColorOptions.stream()
                    .map(ColorOption::getName)
                    .collect(Collectors.joining(", "));
            
            tvColorLabel.setText(colorNames);

            // Add previews with overlap
            for (int i = 0; i < Math.min(selectedColorOptions.size(), 4); i++) {
                int marginStart = (i == 0) ? 0 : -12;
                addColorPreview(llColorsContainer, selectedColorOptions.get(i).getHexCode(), marginStart);
            }
        }
    }



    private void setCardAndIconColor(MaterialCardView card, ImageView icon, String hexColor) {
        int color = Color.parseColor(hexColor);
        card.setCardBackgroundColor(ColorStateList.valueOf(color));
        
        if (ColorUtils.calculateLuminance(color) > 0.5) {
            icon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#6C28D9")));
        } else {
            icon.setImageTintList(ColorStateList.valueOf(Color.WHITE));
        }
    }

    private void addColorPreview(LinearLayout container, String hexColor, int marginStartDp) {
        View view = getLayoutInflater().inflate(R.layout.item_color_preview_small, container, false);
        View colorFill = view.findViewById(R.id.v_color_preview);
        colorFill.setBackgroundColor(Color.parseColor(hexColor));
        
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
        params.setMarginStart((int) (marginStartDp * getResources().getDisplayMetrics().density));
        view.setLayoutParams(params);
        
        container.addView(view);
    }

    private List<String> getSelectedChips(ChipGroup chipGroup) {
        List<String> selected = new ArrayList<>();
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            View child = chipGroup.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                if (chip.isChecked() && chip.getId() != R.id.chip_add_occasion) {
                    selected.add(chip.getText().toString());
                }
            }
        }
        return selected;
    }

    private void saveItemData() {
        if (currentPhotoUri == null) return;

        findViewById(R.id.btn_save).setEnabled(false);
        Toast.makeText(this, "Uploading item...", Toast.LENGTH_SHORT).show();

        String name = etName.getText().toString().trim();
        String category = ((TextView) findViewById(R.id.tv_category)).getText().toString();
        String size = ((TextView) findViewById(R.id.tv_size)).getText().toString();
        List<String> colors = selectedColorOptions.stream().map(ColorOption::getName).collect(Collectors.toList());
        List<String> seasons = getSelectedChips(findViewById(R.id.cg_seasons));
        List<String> occasions = getSelectedChips(findViewById(R.id.cg_occasions));

        MediaManager.get().upload(currentPhotoUri)
                .option("folder", "CategoriesPhotos")
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String secureUrl = (String) resultData.get("secure_url");
                        saveToFirebase(secureUrl, name, category, size, colors, seasons, occasions);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        runOnUiThread(() -> {
                            Toast.makeText(AddItemActivity.this, "Upload failed: " + error.getDescription(), Toast.LENGTH_LONG).show();
                            findViewById(R.id.btn_save).setEnabled(true);
                        });
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                })
                .dispatch();
    }

    private void saveToFirebase(String imageUrl, String name, String category, String size, List<String> colors, List<String> seasons, List<String> occasions) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            findViewById(R.id.btn_save).setEnabled(true);
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users")
                .child(uid)
                .child("categories")
                .child(selectedCategoryId)
                .child("photos")
                .push();

        Map<String, Object> data = new HashMap<>();
        data.put("imageUrl", imageUrl);
        data.put("name", name);
        data.put("category", category); // Store the display name
        data.put("categoryId", selectedCategoryId); // Store the ID too
        data.put("size", size);
        data.put("colors", colors);
        data.put("season", seasons);
        data.put("occasions", occasions);
        data.put("timestamp", System.currentTimeMillis());

        ref.setValue(data).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Item Saved Successfully", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to save to database", Toast.LENGTH_SHORT).show();
            findViewById(R.id.btn_save).setEnabled(true);
        });
    }
}

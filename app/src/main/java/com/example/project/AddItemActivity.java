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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;

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
    private Uri currentPhotoUri;
    private List<ColorOption> selectedColorOptions = new ArrayList<>();

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    openCamera();
                } else {
                    Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Uri> takePhotoLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success) {
                    handleImageSelection(currentPhotoUri);
                }
            });

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    handleImageSelection(uri);
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
            CategorySelectionBottomSheet bottomSheet = CategorySelectionBottomSheet.newInstance(tvCategory.getText().toString(), category -> {
                tvCategory.setText(category);
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

        setupColorSelection();
        
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
            cvPhotoPreview.setVisibility(View.GONE);
            llPhotoOptions.setVisibility(View.VISIBLE);
            ivPhotoPreview.setImageDrawable(null);
            currentPhotoUri = null;
        });

        cvCropPhoto.setOnClickListener(v -> {
            if (currentPhotoUri != null) {
                startCrop(currentPhotoUri);
            }
        });

        ivPhotoPreview.setOnClickListener(v -> {
            if (currentPhotoUri != null) {
                showImagePopup(currentPhotoUri);
            }
        });
    }


    private void openCamera() {
        try {
            File photoFile = createImageFile();
            currentPhotoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
            takePhotoLauncher.launch(currentPhotoUri);
        } catch (IOException e) {
            Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
        }
    }

    private void startCrop(Uri uri) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "Crop_" + timeStamp + ".jpg";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File destFile = new File(storageDir, imageFileName);
        Uri destUri = Uri.fromFile(destFile);

        UCrop.Options options = new UCrop.Options();
        options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
        options.setCompressionQuality(90);
        options.setToolbarColor(ContextCompat.getColor(this, R.color.white));
        options.setStatusBarColor(ContextCompat.getColor(this, R.color.white));
        options.setActiveControlsWidgetColor(Color.parseColor("#6C28D9"));

        Intent intent = UCrop.of(uri, destUri)
                .withOptions(options)
                .getIntent(this);
        cropLauncher.launch(intent);
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void handleImageSelection(Uri uri) {
        currentPhotoUri = uri;
        isPhotoSelected = true;
        findViewById(R.id.tv_photo_asterisk).setVisibility(View.GONE);
        resetPhotoError();

        llPhotoOptions.setVisibility(View.GONE);
        cvPhotoPreview.setVisibility(View.VISIBLE);

        Glide.with(this)
                .load(uri)
                .centerCrop()
                .into(ivPhotoPreview);
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
        TextView tvColorLabel = findViewById(R.id.tv_selected_color);
        if (tvColorLabel.getText().toString().equals("Select color")) {
            findViewById(R.id.tv_color_asterisk).setVisibility(View.VISIBLE);
            findViewById(R.id.rl_color_selection).setBackgroundResource(R.drawable.bg_input_field_error);
            isValid = false;
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

    private void setupColorSelection() {
        View rlColorSelection = findViewById(R.id.rl_color_selection);

        rlColorSelection.setOnClickListener(v -> {
            ColorSelectionBottomSheet bottomSheet = ColorSelectionBottomSheet.newInstance((selectedColors, isMultiple) -> {
                selectedColorOptions = selectedColors;
                updateColorDisplay();
            });
            bottomSheet.show(getSupportFragmentManager(), "ColorSelection");
        });
    }

    private void showImagePopup(Uri uri) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_image_preview);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setGravity(Gravity.CENTER);
        }

        ImageView ivPopup = dialog.findViewById(R.id.iv_popup_image);
        View btnClose = dialog.findViewById(R.id.btn_close_popup);

        Glide.with(this).load(uri).into(ivPopup);
        btnClose.setOnClickListener(v -> dialog.dismiss());
        ivPopup.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void updateColorDisplay() {
        TextView tvColor = findViewById(R.id.tv_selected_color);
        LinearLayout llColorsContainer = findViewById(R.id.ll_selected_colors_container);
        llColorsContainer.removeAllViews();

        if (selectedColorOptions.isEmpty()) {
            tvColor.setText("Select color");
            tvColor.setTextColor(Color.parseColor("#74777F"));
        } else {
            tvColor.setTextColor(Color.parseColor("#1A1C1E"));
            findViewById(R.id.tv_color_asterisk).setVisibility(View.GONE);
            findViewById(R.id.rl_color_selection).setBackgroundResource(R.drawable.bg_input_field);

            String colorNames = selectedColorOptions.stream()
                    .map(ColorOption::getName)
                    .collect(Collectors.joining(", "));
            
            tvColor.setText(colorNames);

            // Add previews with overlap
            for (int i = 0; i < Math.min(selectedColorOptions.size(), 4); i++) {
                int marginStart = (i == 0) ? 0 : -12;
                addColorPreview(llColorsContainer, selectedColorOptions.get(i).getHexCode(), marginStart);
            }
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
                        saveToFirebase(secureUrl, category, size, colors, seasons, occasions);
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

    private void saveToFirebase(String imageUrl, String category, String size, List<String> colors, List<String> seasons, List<String> occasions) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            findViewById(R.id.btn_save).setEnabled(true);
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String sanitizedCategory = category.replaceAll("[.#$\\[\\]]", "");
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users")
                .child(uid)
                .child("categories")
                .child(sanitizedCategory)
                .child("photos")
                .push();

        Map<String, Object> data = new HashMap<>();
        data.put("imageUrl", imageUrl);
        data.put("category", category);
        data.put("size", size);
        data.put("colors", colors);
        data.put("season", seasons);
        data.put("occasions", occasions);
        data.put("timestamp", System.currentTimeMillis());
        data.put("name", "");

        ref.setValue(data).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Item Saved Successfully", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to save to database", Toast.LENGTH_SHORT).show();
            findViewById(R.id.btn_save).setEnabled(true);
        });
    }
}

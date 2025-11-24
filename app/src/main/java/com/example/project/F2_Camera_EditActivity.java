package com.example.project;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class F2_Camera_EditActivity extends AppCompatActivity {

    private F5_Camera_Edit_ToolsView toolsView;
    private ImageView btnManualLasso, btnUndo, btnRedo, btnRestore, btnZoomReset, btnInvertDelete, btnBack, btnSave;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.f2_camera_edit);

        toolsView = findViewById(R.id.toolsView);

        btnManualLasso = findViewById(R.id.btnLasso);
        btnUndo = findViewById(R.id.btnUndo);
        btnRedo = findViewById(R.id.btnRedo);
        btnRestore = findViewById(R.id.btnRestore);
        btnZoomReset = findViewById(R.id.btnZoomReset);
        btnInvertDelete = findViewById(R.id.btnInvertDelete);
        btnBack = findViewById(R.id.btnBack);
        btnSave = findViewById(R.id.btnSave);

        String photoPath = getIntent().getStringExtra("photo_path");
        if (photoPath != null && !photoPath.isEmpty()) {
            Bitmap bitmap = F3_Camera_Util.decodeSampledBitmapFromFile(photoPath, 1024, 1024);
            if (bitmap != null) toolsView.setImageBitmap(bitmap);
            else Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No image path provided", Toast.LENGTH_SHORT).show();
        }

        setupListeners();
        resetInvertToggle();
        clearModeButtonHighlights();
    }

    private void setupListeners() {
        btnManualLasso.setOnClickListener(v -> toggleMode(F5_Camera_Edit_ToolsView.Mode.LASSO));
        btnUndo.setOnClickListener(v -> {
            if (toolsView.canUndo()) toolsView.undo();
            else Toast.makeText(this, "Nothing to undo", Toast.LENGTH_SHORT).show();
        });
        btnRedo.setOnClickListener(v -> {
            if (toolsView.canRedo()) toolsView.redo();
            else Toast.makeText(this, "Nothing to redo", Toast.LENGTH_SHORT).show();
        });
        btnRestore.setOnClickListener(v -> {
            toolsView.restoreOriginal();
            clearModeButtonHighlights();
            resetInvertToggle();
            toolsView.setMode(F5_Camera_Edit_ToolsView.Mode.NONE);
        });
        btnZoomReset.setOnClickListener(v -> toolsView.resetZoom());
        btnInvertDelete.setOnClickListener(v -> {
            boolean currentInvert = toolsView.isInvertDelete();
            toolsView.setInvertDelete(!currentInvert);
            updateInvertToggleVisual(!currentInvert);
        });
        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(F2_Camera_EditActivity.this, F1_CameraActivity.class));
            finish();
        });
        btnSave.setOnClickListener(v -> showSaveDialog());
    }

    private void toggleMode(F5_Camera_Edit_ToolsView.Mode mode) {
        F5_Camera_Edit_ToolsView.Mode current = toolsView.getMode();
        if (current == mode) {
            toolsView.setMode(F5_Camera_Edit_ToolsView.Mode.NONE);
            clearModeButtonHighlights();
        } else {
            toolsView.setMode(mode);
            highlightSelectedButton(mode);
        }
    }

    private void clearModeButtonHighlights() {
        btnManualLasso.setColorFilter(null);
    }

    private void highlightSelectedButton(F5_Camera_Edit_ToolsView.Mode mode) {
        clearModeButtonHighlights();
        int highlightColor = getResources().getColor(android.R.color.holo_blue_light);
        if (mode == F5_Camera_Edit_ToolsView.Mode.LASSO) btnManualLasso.setColorFilter(highlightColor);
    }

    private void resetInvertToggle() {
        toolsView.setInvertDelete(false);
        updateInvertToggleVisual(false);
    }

    private void updateInvertToggleVisual(boolean isInverted) {
        btnInvertDelete.setAlpha(isInverted ? 1f : 0.5f);
    }

    // ------------------------ SAVE TO CATEGORY DIALOG ------------------------
    private void showSaveDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.f3_camera_edit_dialog_save_image, null);
        builder.setView(dialogView);

        Spinner spinner = dialogView.findViewById(R.id.spinnerCategories);
        ImageView btnAddCategory = dialogView.findViewById(R.id.btnAddCategory);
        View btnSaveCategory = dialogView.findViewById(R.id.btnSaveCategory);

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        AlertDialog dialog = builder.create();
        dialog.show();

        // ---------------- Load categories from Firebase ----------------
        DatabaseReference categoriesRef = FirebaseDatabase.getInstance()
                .getReference("ClosetData")
                .child(uid)
                .child("categories");

        categoriesRef.get().addOnSuccessListener(snapshot -> {
            List<String> categories = new ArrayList<>();
            for (DataSnapshot catSnap : snapshot.getChildren()) {
                String name = catSnap.child("name").getValue(String.class);
                if (name != null) categories.add(name);
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, categories);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
        }).addOnFailureListener(e -> e.printStackTrace());

        // ---------------- Add new category ----------------
        btnAddCategory.setOnClickListener(v -> {
            AlertDialog.Builder inputDialog = new AlertDialog.Builder(this);
            EditText input = new EditText(this);
            input.setHint("New category name");
            inputDialog.setView(input);

            inputDialog.setPositiveButton("Add", (d, which) -> {
                String newCat = input.getText().toString().trim();
                if (!newCat.isEmpty()) {
                    // Save category to Firebase
                    String categoryId = categoriesRef.push().getKey();
                    if (categoryId != null) {
                        Map<String, Object> categoryData = new HashMap<>();
                        categoryData.put("id", categoryId);
                        categoryData.put("name", newCat);
                        categoriesRef.child(categoryId).setValue(categoryData)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Category added", Toast.LENGTH_SHORT).show();
                                    // Update spinner
                                    ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinner.getAdapter();
                                    adapter.add(newCat);
                                    adapter.notifyDataSetChanged();
                                    spinner.setSelection(adapter.getPosition(newCat));
                                })
                                .addOnFailureListener(Throwable::printStackTrace);
                    }
                } else {
                    Toast.makeText(this, "Category name empty", Toast.LENGTH_SHORT).show();
                }
            });

            inputDialog.setNegativeButton("Cancel", (d, which) -> d.dismiss());
            inputDialog.show();
        });

        // ---------------- Save image locally under selected category ----------------
        btnSaveCategory.setOnClickListener(v -> {
            String selectedCategory = (String) spinner.getSelectedItem();
            if (selectedCategory == null || selectedCategory.isEmpty()) {
                Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save locally
            File categoryDir = new File(getFilesDir(), "ClosetImages/" + uid + "/" + selectedCategory);
            if (!categoryDir.exists()) categoryDir.mkdirs();

            String photoPath = getIntent().getStringExtra("photo_path");
            if (photoPath != null && !photoPath.isEmpty()) {
                File sourceFile = new File(photoPath);
                File destFile = new File(categoryDir, sourceFile.getName());

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                    Toast.makeText(this, "Saved to category: " + selectedCategory, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
                }
            }

            dialog.dismiss();
        });
    }

    // ------------------------ HELPERS ------------------------
    private void saveCategoryLocally(Context context, String categoryName, String uid) {
        if (categoryName == null || categoryName.trim().isEmpty() || uid == null) return;
        File categoryDir = new File(context.getFilesDir(), "ClosetImages/" + uid + "/" + categoryName);
        if (!categoryDir.exists()) categoryDir.mkdirs();
    }

    private void saveCategoryToFirebaseWithID(String categoryName, String uid) {
        if (categoryName == null || categoryName.trim().isEmpty() || uid == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("ClosetData")
                .child(uid)
                .child("categories");

        String categoryId = ref.push().getKey();
        if (categoryId == null) return;

        Map<String, Object> categoryData = new HashMap<>();
        categoryData.put("id", categoryId);
        categoryData.put("name", categoryName);

        ref.child(categoryId).setValue(categoryData)
                .addOnSuccessListener(aVoid -> {})
                .addOnFailureListener(e -> e.printStackTrace());
    }
}

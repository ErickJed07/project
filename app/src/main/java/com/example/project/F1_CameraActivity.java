package com.example.project;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class F1_CameraActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST = 100;
    private static final String REMOVE_BG_API_KEY = "G5QoyX7YQovkgzvvqaMuYGKD";

    ImageView imagePreview, backButton;
    Button btnTakePhoto, btnSave;
    ProgressBar progressBar;

    Bitmap processedBitmap;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.f1_camera_act);

        imagePreview = findViewById(R.id.imagePreview);
        backButton = findViewById(R.id.backButton);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);

        btnTakePhoto.setOnClickListener(view -> openCamera());
        btnSave.setOnClickListener(view -> showSaveDialog());
    }

    private void openCamera() {
        if (checkSelfPermission()) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent, CAMERA_REQUEST);
        }
    }

    private boolean checkSelfPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        1);
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            progressBar.setVisibility(View.VISIBLE);
            new RemoveBgTask().execute(photo);
        }
    }

    // --------------------------
    // BACKGROUND REMOVAL TASK
    // --------------------------
    private class RemoveBgTask extends AsyncTask<Bitmap, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(Bitmap... bitmaps) {
            Bitmap input = bitmaps[0];
            return removeBackground(input);
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            progressBar.setVisibility(View.GONE);

            if (result != null) {
                processedBitmap = result;
                imagePreview.setImageBitmap(result);
                btnSave.setVisibility(View.VISIBLE);
            }
        }
    }


    // ------------------------------------
    //   REMOVE.BG API CALL
    // ------------------------------------
    public Bitmap removeBackground(Bitmap bitmap) {

        try {
            OkHttpClient client = new OkHttpClient();

            // Convert bitmap to PNG byte array
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
            byte[] imageBytes = bos.toByteArray();

            // FIXED LINE BELOW: Swapped arguments to (MediaType, byte[])
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image_file", "photo.png",
                            RequestBody.create(MediaType.parse("image/png"), imageBytes))
                    .addFormDataPart("size", "auto")
                    .build();

            Request request = new Request.Builder()
                    .url("https://api.remove.bg/v1.0/removebg")
                    .addHeader("X-Api-Key", REMOVE_BG_API_KEY)
                    .post(requestBody)
                    .build();

            Response response = client.newCall(request).execute();

            if (response.isSuccessful()) {
                InputStream inputStream = response.body().byteStream();
                return BitmapFactory.decodeStream(inputStream);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
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

package com.example.project;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
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
import android.util.Log;
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

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
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

    // Initialize Cloudinary Config (Ideally do this in Application class, but works here for quick setup)
    private void initCloudinary() {
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("cloud_name", "YOUR_CLOUD_NAME"); // TODO: Replace with your Cloud Name
            config.put("api_key", "YOUR_API_KEY");       // TODO: Replace with your API Key
            config.put("api_secret", "YOUR_API_SECRET"); // TODO: Replace with your API Secret
            MediaManager.init(this, config);
        } catch (Exception e) {
            // MediaManager might already be initialized
        }
    }

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.f1_camera_act);

        initCloudinary(); // Ensure Cloudinary is ready

        imagePreview = findViewById(R.id.imagePreview);
        backButton = findViewById(R.id.backButton);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);

        btnTakePhoto.setOnClickListener(view -> openCamera());
        btnSave.setOnClickListener(view -> showSaveDialog());

        backButton.setOnClickListener(v -> finish());
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
            return removeBackground(bitmaps[0]);
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            progressBar.setVisibility(View.GONE);
            if (result != null) {
                processedBitmap = result;
                imagePreview.setImageBitmap(result);
                btnSave.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(F1_CameraActivity.this, "Background removal failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public Bitmap removeBackground(Bitmap bitmap) {
        try {
            OkHttpClient client = new OkHttpClient();
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
            byte[] imageBytes = bos.toByteArray();

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
        if (processedBitmap == null) {
            Toast.makeText(this, "No image to save", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.f3_camera_edit_dialog_save_image, null);
        builder.setView(dialogView);

        Spinner spinner = dialogView.findViewById(R.id.spinnerCategories);
        ImageView btnAddCategory = dialogView.findViewById(R.id.btnAddCategory);
        View btnSaveCategory = dialogView.findViewById(R.id.btnSaveCategory);

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        AlertDialog dialog = builder.create();
        dialog.show();

        // 1. Load categories from Firebase: Users -> [uid] -> closetData -> categories
        DatabaseReference categoriesRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(uid)
                .child("closetData") // Corrected path based on your data structure
                .child("categories");

        // Wrapper to hold category IDs to map name -> ID later
        final List<CategoryItem> categoryList = new ArrayList<>();
        final List<String> categoryNames = new ArrayList<>();

        categoriesRef.get().addOnSuccessListener(snapshot -> {
            categoryList.clear();
            categoryNames.clear();

            for (DataSnapshot catSnap : snapshot.getChildren()) {
                String name = catSnap.child("name").getValue(String.class);
                String id = catSnap.getKey(); // Get the Firebase Key (e.g. -OfOaJ...)

                if (name != null) {
                    categoryList.add(new CategoryItem(id, name));
                    categoryNames.add(name);
                }
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, categoryNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
        }).addOnFailureListener(e -> e.printStackTrace());

        // 2. Add new category logic
        btnAddCategory.setOnClickListener(v -> {
            AlertDialog.Builder inputDialog = new AlertDialog.Builder(this);
            EditText input = new EditText(this);
            input.setHint("New category name");
            inputDialog.setView(input);

            inputDialog.setPositiveButton("Add", (d, which) -> {
                String newCatName = input.getText().toString().trim();
                if (!newCatName.isEmpty()) {
                    String newCatId = categoriesRef.push().getKey();
                    Map<String, Object> catData = new HashMap<>();
                    catData.put("id", newCatId);
                    catData.put("name", newCatName);

                    categoriesRef.child(newCatId).setValue(catData).addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Category Added", Toast.LENGTH_SHORT).show();
                        // Refresh list manually for UX
                        categoryList.add(new CategoryItem(newCatId, newCatName));
                        categoryNames.add(newCatName);
                        ((ArrayAdapter) spinner.getAdapter()).notifyDataSetChanged();
                        spinner.setSelection(categoryNames.size() - 1);
                    });
                }
            });
            inputDialog.setNegativeButton("Cancel", (d, w) -> d.dismiss());
            inputDialog.show();
        });

        // 3. Save to Cloudinary + Firebase
        btnSaveCategory.setOnClickListener(v -> {
            int selectedPos = spinner.getSelectedItemPosition();
            if (selectedPos < 0 || categoryList.isEmpty()) {
                Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get the specific Category ID from our list
            String selectedCategoryId = categoryList.get(selectedPos).id;
            String selectedCategoryName = categoryList.get(selectedPos).name;

            progressBar.setVisibility(View.VISIBLE);
            dialog.dismiss(); // Close dialog while uploading

            // Convert Bitmap to File for Cloudinary Upload
            File fileToUpload = bitmapToFile(processedBitmap, "temp_closet_image.png");

            if (fileToUpload != null) {
                uploadToCloudinary(fileToUpload, uid, selectedCategoryId);
            } else {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Error preparing image", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ------------------------ CLOUDINARY UPLOAD ------------------------
    private void uploadToCloudinary(File file, String uid, String categoryId) {
        String requestId = MediaManager.get().upload(file.getAbsolutePath())
                .option("folder", "ClosetImages/" + uid) // Organize in Cloudinary folders
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        Log.d("Cloudinary", "Upload started");
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        // Update progress if needed
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String secureUrl = (String) resultData.get("secure_url");
                        Log.d("Cloudinary", "Upload success: " + secureUrl);

                        // Now save URL to Firebase
                        saveImageUrlToFirebase(uid, categoryId, secureUrl);
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

    // ------------------------ FIREBASE SAVE ------------------------
    private void saveImageUrlToFirebase(String uid, String categoryId, String imageUrl) {
        DatabaseReference categoryRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(uid)
                .child("closetData")
                .child("categories")
                .child(categoryId);

        // Target the "photos" node inside the specific category
        // push() creates a unique ID, and we set the value directly to the URL string
        // This creates the structure: photos -> [uniqueID]: "https://..."
        categoryRef.child("photos").push().setValue(imageUrl)
                .addOnSuccessListener(aVoid -> {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(F1_CameraActivity.this, "Image Saved to Closet!", Toast.LENGTH_SHORT).show();

                        // Close the activity and return to previous screen
                        finish();
                    });
                })
                .addOnFailureListener(e -> {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(F1_CameraActivity.this, "Database Save Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                });
    }

    // Helper to convert Bitmap to File
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

    // Helper Class to store ID and Name for Spinner
    private static class CategoryItem {
        String id;
        String name;

        CategoryItem(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}

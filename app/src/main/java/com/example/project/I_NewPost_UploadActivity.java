package com.example.project;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// --- Cloudinary Imports ---
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class I_NewPost_UploadActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private I_NewPost_GalleryAdapter galleryAdapter;
    private ArrayList<String> imageUris;
    private ViewPager2 viewPager;
    private I_NewPost_ViewPageAdapter imageAdapter;
    private ArrayList<String> selectedImages = new ArrayList<>();  // Holds selected image URIs

    private boolean isMultipleSelection = false;  // Flag to track multiple selection mode

    private DatabaseReference database; // Firebase Realtime Database reference

    private EditText captionEditText;  // EditText for the caption input
    private TextView savePostButton;  // TextView for the save post button

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.i_newpostupload);

        // --- Initialize Cloudinary ---
        // TODO: Replace these placeholders with your actual Cloudinary Dashboard details
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("cloud_name", BuildConfig.CLOUDINARY_CLOUD_NAME);    // Use credentials from gradle.properties
            config.put("api_key", BuildConfig.CLOUDINARY_API_KEY);
            config.put("api_secret", BuildConfig.CLOUDINARY_API_SECRET);
            MediaManager.init(this, config);  // Initialize Cloudinary MediaManager
        } catch (IllegalStateException e) {
            // MediaManager is already initialized, this is fine.
            e.printStackTrace();
        }


        // Initialize Firebase Realtime Database
        database = FirebaseDatabase.getInstance().getReference("PostEvents");

        // Initialize views
        recyclerView = findViewById(R.id.recyclerViewGallery);
        viewPager = findViewById(R.id.viewPager);
        captionEditText = findViewById(R.id.captionEditText);
        savePostButton = findViewById(R.id.Savepost);

        // Initialize imageUris list and adapter
        imageUris = new ArrayList<>();
        galleryAdapter = new I_NewPost_GalleryAdapter(imageUris, selectedImages, imageUri -> {
            if (isMultipleSelection) {
                if (selectedImages.contains(imageUri)) {
                    selectedImages.remove(imageUri);
                } else {
                    selectedImages.add(imageUri);
                }
            } else {
                selectedImages.clear();
                selectedImages.add(imageUri);
            }

            // Update the ViewPager and RecyclerView after image selection
            imageAdapter.notifyDataSetChanged();
            galleryAdapter.notifyDataSetChanged();  // Update gallery to reflect selection

            // Show a Toast with the number of selected images
            Toast.makeText(this, selectedImages.size() + " image(s) selected.", Toast.LENGTH_SHORT).show();
        });

        // Set up RecyclerView
        recyclerView.setLayoutManager(new GridLayoutManager(this, 4));
        recyclerView.setAdapter(galleryAdapter);

        // Set up ViewPager2 with an adapter to display selected images
        imageAdapter = new I_NewPost_ViewPageAdapter(this, selectedImages);
        viewPager.setAdapter(imageAdapter);

        // Handle permissions for image access
        // CHANGE: We now check for TIRAMISU (Android 13, API 33)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // For Android 13+: Use READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                getImagesFromDevice();
            } else {
                requestPermissions();
            }
        } else {
            // For Android 12 (API 31/32) and below: Use READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                getImagesFromDevice();
            } else {
                requestPermissions();
            }
        }

        // Handle "Select multiple images" toggle
        ImageButton selectMultipleButton = findViewById(R.id.select_multiple);
        selectMultipleButton.setOnClickListener(v -> {
            isMultipleSelection = !isMultipleSelection;
            if (!isMultipleSelection) {
                selectedImages.clear();
                galleryAdapter.notifyDataSetChanged();
                Toast.makeText(this, "Single selection enabled", Toast.LENGTH_SHORT).show();
            } else {
                selectedImages.clear();
                galleryAdapter.notifyDataSetChanged();
                Toast.makeText(this, "Multiple selection enabled", Toast.LENGTH_SHORT).show();
            }
        });

        // Save post data when the save button is clicked
        savePostButton.setOnClickListener(v -> {
            savePostData();
        });
    }

    // Method to fetch images from the device storage
    private void getImagesFromDevice() {
        Uri imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Images.Media.DATA};
        String sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";

        Cursor cursor = getContentResolver().query(imageUri, projection, null, null, sortOrder);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int columnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                String filePath = cursor.getString(columnIndex);
                imageUris.add("file://" + filePath); // Add image file URI
            }
            cursor.close();
            galleryAdapter.notifyDataSetChanged();
        }
    }

    // Handle permission request result for image access
    private void requestPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 100);
        } else {
            // This line fixes the crash for Android 12 (API 31/32)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getImagesFromDevice();
            } else {
                Toast.makeText(this, "Permission denied! Cannot access images.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 1. Start the Upload Process
    // 1. Start the Upload Process
    private void savePostData() {
        if (!selectedImages.isEmpty()) {
            // Disable button to prevent multiple clicks
            savePostButton.setEnabled(false);
            Toast.makeText(this, "Uploading " + selectedImages.size() + " images...", Toast.LENGTH_SHORT).show();

            List<String> uploadedImageUrls = new ArrayList<>();
            AtomicInteger uploadCounter = new AtomicInteger(0);
            int totalImages = selectedImages.size();

            // Loop through selected images and upload each to Cloudinary
            for (String imageUri : selectedImages) {
                MediaManager.get().upload(Uri.parse(imageUri))
                        .option("folder", "Posts") // <--- ADDED: Uploads to 'Posts' folder in Cloudinary
                        .callback(new UploadCallback() {
                            @Override
                            public void onStart(String requestId) {
                                // Optional: Update UI for start
                            }

                            @Override
                            public void onProgress(String requestId, long bytes, long totalBytes) {
                                // Optional: Update Progress Bar
                            }

                            @Override
                            public void onSuccess(String requestId, Map resultData) {
                                // Retrieve the secure public URL from Cloudinary
                                String secureUrl = (String) resultData.get("secure_url");
                                uploadedImageUrls.add(secureUrl);

                                // Check if all images are done uploading
                                if (uploadCounter.incrementAndGet() == totalImages) {
                                    // All done! Save to Firebase
                                    saveToFirebase(uploadedImageUrls);
                                }
                            }

                            @Override
                            public void onError(String requestId, ErrorInfo error) {
                                runOnUiThread(() -> {
                                    Toast.makeText(I_NewPost_UploadActivity.this, "Upload failed: " + error.getDescription(), Toast.LENGTH_LONG).show();
                                    savePostButton.setEnabled(true); // Re-enable button so user can try again
                                });
                            }

                            @Override
                            public void onReschedule(String requestId, ErrorInfo error) {
                                // Handle rescheduling if needed
                            }
                        })
                        .dispatch();
            }

        } else {
            Toast.makeText(this, "Please select at least one image.", Toast.LENGTH_SHORT).show();
        }
    }


    // 2. Save Post Metadata to Firebase (Called only after images are uploaded)
    private void saveToFirebase(List<String> cloudImageUrls) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid(); // Get current user's ID
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Retrieve current post count, default to 0 if null
                    Integer currentPostCount = dataSnapshot.child("posts").getValue(Integer.class);
                    if (currentPostCount == null) {
                        currentPostCount = 0;
                    }
                    // Increment the post count
                    userRef.child("posts").setValue(currentPostCount + 1);

                    // Fetch the username from Firebase
                    String username = dataSnapshot.child("username").getValue(String.class);
                    if (username == null) {
                        username = "Anonymous"; // Fallback if username is not found
                    }

                    // Generate post ID
                    String postId = FirebaseDatabase.getInstance().getReference("PostEvents").push().getKey();

                    // Get the current date in the format required
                    String date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new Date());

                    // Get the caption from the EditText
                    String caption = captionEditText.getText().toString().trim();

                    // Create a post event object using the CLOUDINARY URLs
                    I_NewPost_Event postEvent = new I_NewPost_Event(username, caption, cloudImageUrls, postId, date, userId , 0, null);

                    // Save the post data to Firebase under "PostEvents"
                    FirebaseDatabase.getInstance().getReference("PostEvents")
                            .child(postId)  // Save directly under postId
                            .setValue(postEvent)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(I_NewPost_UploadActivity.this, "Post saved successfully!", Toast.LENGTH_SHORT).show();

                                // Navigate to FeedActivity after saving the post
                                Intent intent = new Intent(I_NewPost_UploadActivity.this, D_FeedActivity.class);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(I_NewPost_UploadActivity.this, "Failed to save post", Toast.LENGTH_SHORT).show();
                                savePostButton.setEnabled(true);
                            });

                } else {
                    Toast.makeText(I_NewPost_UploadActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                    savePostButton.setEnabled(true);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(I_NewPost_UploadActivity.this, "Failed to fetch user data", Toast.LENGTH_SHORT).show();
                savePostButton.setEnabled(true);
            }
        });
    }
}

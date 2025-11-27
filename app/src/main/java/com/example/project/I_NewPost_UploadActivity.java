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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                getImagesFromDevice();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
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
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 100);
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

    private void savePostData() {
        if (!selectedImages.isEmpty()) {
            // Save images to the app's local storage and get the paths
            List<String> savedImagePaths = new ArrayList<>();
            for (String imageUri : selectedImages) {
                String savedPath = saveImageToLocalStorage(imageUri);
                if (savedPath != null) {
                    savedImagePaths.add(savedPath);
                }
            }

            if (savedImagePaths.isEmpty()) {
                Toast.makeText(this, "Failed to save images", Toast.LENGTH_SHORT).show();
                return;
            }

            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid(); // Get current user's ID
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
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

                        // Create a post event object with the dynamic data
                        I_NewPost_Event postEvent = new I_NewPost_Event(username, caption, savedImagePaths, postId, date, userId , 0, null);

                        // Save the post data to Firebase under "PostEvents"
                        FirebaseDatabase.getInstance().getReference("PostEvents")
                                .child(postId)  // Save directly under postId
                                .setValue(postEvent)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(I_NewPost_UploadActivity.this, "Post saved successfully!", Toast.LENGTH_SHORT).show();

                                    // Navigate to FeedActivity after saving the post
                                    Intent intent = new Intent(I_NewPost_UploadActivity.this, D1_FeedActivity.class);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(I_NewPost_UploadActivity.this, "Failed to save post", Toast.LENGTH_SHORT).show();
                                });

                    } else {
                        Toast.makeText(I_NewPost_UploadActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(I_NewPost_UploadActivity.this, "Failed to fetch user data", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "Please select at least one image.", Toast.LENGTH_SHORT).show();
        }
    }





    // Save the image to local storage and return the saved file path
    private String saveImageToLocalStorage(String imageUri) {
        try {
            // Extract the file name from the URI
            Uri uri = Uri.parse(imageUri);
            String fileName = uri.getLastPathSegment();

            // Create a file in the app's local storage
            File dir = new File(getFilesDir(), "posts");
            if (!dir.exists()) {
                dir.mkdirs(); // Create directory if it doesn't exist
            }

            File file = new File(dir, fileName);
            FileOutputStream fos = new FileOutputStream(file);

            // Copy the image from the URI to the app's local storage
            InputStream inputStream = getContentResolver().openInputStream(uri);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
            fos.flush();
            fos.close();
            inputStream.close();

            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

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
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class I_PostUploadActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private I_PostGalleryAdapter galleryAdapter;
    private ArrayList<String> imageUris;
    private ViewPager2 viewPager;
    private I_PostViewPagerAdapter imageAdapter;
    private ArrayList<String> selectedImages = new ArrayList<>();
    private boolean isMultipleSelection = false;
    private DatabaseReference database;
    private EditText captionEditText;
    private TextView savePostButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.i_newpostupload);

        try {
            Map<String, Object> config = new HashMap<>();
            config.put("cloud_name", BuildConfig.CLOUDINARY_CLOUD_NAME);
            config.put("api_key", BuildConfig.CLOUDINARY_API_KEY);
            config.put("api_secret", BuildConfig.CLOUDINARY_API_SECRET);
            MediaManager.init(this, config);
        } catch (IllegalStateException e) {}

        database = FirebaseDatabase.getInstance().getReference("PostEvents");
        recyclerView = findViewById(R.id.recyclerViewGallery);
        viewPager = findViewById(R.id.viewPager);
        captionEditText = findViewById(R.id.captionEditText);
        savePostButton = findViewById(R.id.Savepost);

        imageUris = new ArrayList<>();
        galleryAdapter = new I_PostGalleryAdapter(imageUris, selectedImages, imageUri -> {
            if (isMultipleSelection) {
                if (selectedImages.contains(imageUri)) selectedImages.remove(imageUri);
                else selectedImages.add(imageUri);
            } else {
                selectedImages.clear();
                selectedImages.add(imageUri);
            }
            imageAdapter.notifyDataSetChanged();
            galleryAdapter.notifyDataSetChanged();
            Toast.makeText(this, selectedImages.size() + " image(s) selected.", Toast.LENGTH_SHORT).show();
        });

        recyclerView.setLayoutManager(new GridLayoutManager(this, 4));
        recyclerView.setAdapter(galleryAdapter);

        imageAdapter = new I_PostViewPagerAdapter(this, selectedImages);
        viewPager.setAdapter(imageAdapter);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) getImagesFromDevice();
            else requestPermissions();
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) getImagesFromDevice();
            else requestPermissions();
        }

        findViewById(R.id.select_multiple).setOnClickListener(v -> {
            isMultipleSelection = !isMultipleSelection;
            selectedImages.clear();
            galleryAdapter.notifyDataSetChanged();
            Toast.makeText(this, isMultipleSelection ? "Multiple selection enabled" : "Single selection enabled", Toast.LENGTH_SHORT).show();
        });

        savePostButton.setOnClickListener(v -> savePostData());
    }

    private void getImagesFromDevice() {
        Uri imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Images.Media.DATA};
        String sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";
        Cursor cursor = getContentResolver().query(imageUri, projection, null, null, sortOrder);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                imageUris.add("file://" + cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA)));
            }
            cursor.close();
            galleryAdapter.notifyDataSetChanged();
        }
    }

    private void requestPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 100);
        else ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) getImagesFromDevice();
    }

    private void savePostData() {
        if (!selectedImages.isEmpty()) {
            savePostButton.setEnabled(false);
            Toast.makeText(this, "Uploading...", Toast.LENGTH_SHORT).show();
            List<String> uploadedImageUrls = new ArrayList<>();
            AtomicInteger uploadCounter = new AtomicInteger(0);
            for (String uri : selectedImages) {
                MediaManager.get().upload(Uri.parse(uri)).option("folder", "Posts").callback(new UploadCallback() {
                    @Override public void onSuccess(String requestId, Map resultData) {
                        uploadedImageUrls.add((String) resultData.get("secure_url"));
                        if (uploadCounter.incrementAndGet() == selectedImages.size()) saveToFirebase(uploadedImageUrls);
                    }
                    @Override public void onError(String requestId, ErrorInfo error) {
                        runOnUiThread(() -> { Toast.makeText(I_PostUploadActivity.this, "Upload failed", Toast.LENGTH_LONG).show(); savePostButton.setEnabled(true); });
                    }
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long b, long t) {}
                    @Override public void onReschedule(String r, ErrorInfo e) {}
                }).dispatch();
            }
        }
    }

    private void saveToFirebase(List<String> cloudImageUrls) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Integer count = snapshot.child("posts").getValue(Integer.class);
                    userRef.child("posts").setValue((count == null ? 0 : count) + 1);
                    String username = snapshot.child("username").getValue(String.class);
                    String postId = FirebaseDatabase.getInstance().getReference("PostEvents").push().getKey();
                    String date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new Date());
                    I_PostEvent postEvent = new I_PostEvent(username != null ? username : "Anonymous", captionEditText.getText().toString().trim(), cloudImageUrls, postId, date, userId, 0, null);
                    FirebaseDatabase.getInstance().getReference("PostEvents").child(postId).setValue(postEvent).addOnSuccessListener(aVoid -> {
                        startActivity(new Intent(I_PostUploadActivity.this, D_FeedActivity.class));
                        finish();
                    });
                }
            }
            @Override public void onCancelled(DatabaseError error) {}
        });
    }
}

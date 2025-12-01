package com.example.project;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class C_SignInProfilePhoto extends AppCompatActivity {

    private static final int IMAGE_REQ_CODE = 101;
    private ImageView imgPreview;
    private Button btnSave, btnSkip, btnChoose;
    private Uri imageUri;

    // Variables to hold data passed from previous screen
    private String email;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.c_signinprofilephoto);

        // --- 1. Receive Data from Previous Screen ---
        email = getIntent().getStringExtra("USER_EMAIL");
        username = getIntent().getStringExtra("USER_USERNAME");

        initCloudinary();

        imgPreview = findViewById(R.id.img_profile_preview);
        btnChoose = findViewById(R.id.btn_choose_photo);
        btnSave = findViewById(R.id.btn_save_photo);
        btnSkip = findViewById(R.id.btn_skip);

        // Pick Image
        btnChoose.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, IMAGE_REQ_CODE);
        });

        // Save / Upload
        btnSave.setOnClickListener(v -> {
            if (imageUri != null) {
                uploadToCloudinary(imageUri);
            }
        });

        // Skip - Save with default image
        btnSkip.setOnClickListener(v -> {
            saveUserToDatabase("default");
        });
    }

    private void initCloudinary() {
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("cloud_name", BuildConfig.CLOUDINARY_CLOUD_NAME);
            config.put("api_key", BuildConfig.CLOUDINARY_API_KEY);
            config.put("api_secret", BuildConfig.CLOUDINARY_API_SECRET);
            MediaManager.init(this, config);
        } catch (IllegalStateException e) {
            // Already initialized
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_REQ_CODE && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            imgPreview.setImageURI(imageUri);
            btnSave.setVisibility(View.VISIBLE); // Show save button
        }
    }

    private void uploadToCloudinary(Uri uri) {
        Toast.makeText(this, "Uploading...", Toast.LENGTH_SHORT).show();

        MediaManager.get().upload(uri)
                .option("folder", "ProfilePhoto") // <--- Uploads to the "ProfilePhoto" folder
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        // Get URL and save the FULL user to Firebase
                        String downloadUrl = (String) resultData.get("secure_url");
                        saveUserToDatabase(downloadUrl);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Toast.makeText(C_SignInProfilePhoto.this, "Upload Error: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    // --- The Main Save Method (Renamed from updateFirebaseProfile) ---
    private void saveUserToDatabase(String photoUrl) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("Users");

            // Create the full user map here
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("email", email);
            userMap.put("username", username);
            userMap.put("profilePhoto", photoUrl);
            userMap.put("Fans", 0);
            userMap.put("Models", 0);
            userMap.put("FansList", new HashMap<>());
            userMap.put("ModelsList", new HashMap<>());
            userMap.put("posts", 0);

            // Save to DB
            databaseRef.child(user.getUid()).setValue(userMap)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(C_SignInProfilePhoto.this, "Profile Saved!", Toast.LENGTH_SHORT).show();
                            navigateToLogin();
                        } else {
                            Toast.makeText(C_SignInProfilePhoto.this, "Database Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(C_SignInProfilePhoto.this, B_LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

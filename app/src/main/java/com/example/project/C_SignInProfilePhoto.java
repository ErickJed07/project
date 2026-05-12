package com.example.project;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Activity for users to add a profile photo and select their gender during sign-in.
 * Upgraded with modern Activity Result API, Glide, and ML Kit Face Detection.
 */
public class C_SignInProfilePhoto extends AppCompatActivity {

    private static final String TAG = "C_SignInProfilePhoto";

    private ImageView imgPreview;
    private Button btnSave, btnSkip, btnChoose;
    private RadioGroup rgGender;
    private Uri imageUri;
    private FaceDetector faceDetector;

    // Data passed from the previous registration screen
    private String email;
    private String username;

    // Modern way to handle activity results (replaces startActivityForResult)
    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    validateAndSetImage(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply saved theme before onCreate
        ThemeHelper.applyTheme(ThemeHelper.getSavedTheme(this));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.c_signinprofilephoto);

        // 1. Receive Data
        email = getIntent().getStringExtra("USER_EMAIL");
        username = getIntent().getStringExtra("USER_USERNAME");

        // 2. Initialize Components
        initCloudinary();
        initFaceDetector();
        setupUI();
    }

    private void setupUI() {
        imgPreview = findViewById(R.id.iv_profile_preview);
        btnChoose = findViewById(R.id.btn_choose_photo);
        FloatingActionButton btnChooseFab = findViewById(R.id.btn_choose_photo_fab);
        btnSave = findViewById(R.id.btn_save_photo);
        btnSkip = findViewById(R.id.btn_skip);
        rgGender = findViewById(R.id.rg_gender);

        // Initial Button State
        updateSaveButtonState(false);

        // Pick Image Listener
        View.OnClickListener pickImageListener = v -> pickImageLauncher.launch("image/*");
        btnChoose.setOnClickListener(pickImageListener);
        btnChooseFab.setOnClickListener(pickImageListener);

        // Save Button Listener
        btnSave.setOnClickListener(v -> {
            if (imageUri != null) {
                uploadToCloudinary(imageUri);
            } else {
                // Should not happen if button is disabled correctly
                Toast.makeText(this, "Please select a photo first", Toast.LENGTH_SHORT).show();
            }
        });

        // Skip Listener
        btnSkip.setOnClickListener(v -> saveUserToDatabase("default"));
    }

    private void updateSaveButtonState(boolean enabled) {
        btnSave.setEnabled(enabled);
        btnSave.setAlpha(enabled ? 1.0f : 0.6f);
        if (enabled) {
            btnSave.setText("Save & Continue");
        }
    }

    private void initFaceDetector() {
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .build();
        faceDetector = FaceDetection.getClient(options);
    }

    /**
     * Validates that the selected image contains a face using ML Kit.
     * Then displays it using Glide.
     */
    private void validateAndSetImage(Uri uri) {
        try {
            InputImage image = InputImage.fromFilePath(this, uri);
            Toast.makeText(this, "Validating photo...", Toast.LENGTH_SHORT).show();
            
            faceDetector.process(image)
                    .addOnSuccessListener(faces -> {
                        if (!faces.isEmpty()) {
                            imageUri = uri;
                            // Use Glide for efficient image loading and caching
                            Glide.with(this)
                                    .load(uri)
                                    .centerCrop()
                                    .into(imgPreview);
                            updateSaveButtonState(true);
                        } else {
                            Toast.makeText(this, "We couldn't detect a face. Please use a clear profile photo.", Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Face detection error", e);
                        // Fallback: allow the photo even if face detection fails for technical reasons
                        imageUri = uri;
                        Glide.with(this).load(uri).centerCrop().into(imgPreview);
                        updateSaveButtonState(true);
                    });
        } catch (IOException e) {
            Log.e(TAG, "Error reading image file", e);
            Toast.makeText(this, "Error reading image", Toast.LENGTH_SHORT).show();
        }
    }

    private void initCloudinary() {
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("cloud_name", BuildConfig.CLOUDINARY_CLOUD_NAME);
            config.put("api_key", BuildConfig.CLOUDINARY_API_KEY);
            config.put("api_secret", BuildConfig.CLOUDINARY_API_SECRET);
            MediaManager.init(this, config);
        } catch (IllegalStateException e) {
            // Already initialized, no action needed
        }
    }

    private void uploadToCloudinary(Uri uri) {
        Toast.makeText(this, "Uploading photo...", Toast.LENGTH_SHORT).show();
        btnSave.setEnabled(false);
        btnSave.setText("Uploading...");

        MediaManager.get().upload(uri)
                .option("folder", "ProfilePhotos") // Organized folder in Cloudinary
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                    
                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String downloadUrl = (String) resultData.get("secure_url");
                        saveUserToDatabase(downloadUrl);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        runOnUiThread(() -> {
                            updateSaveButtonState(true);
                            Toast.makeText(C_SignInProfilePhoto.this, "Upload Failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                        });
                    }
                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    /**
     * Saves the final user profile to Firebase Realtime Database.
     */
    private void saveUserToDatabase(String photoUrl) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Authentication session expired.", Toast.LENGTH_SHORT).show();
            navigateToLogin();
            return;
        }

        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("Users");
        String gender = rgGender.getCheckedRadioButtonId() == R.id.rb_woman ? "woman" : "man";

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("email", email != null ? email : user.getEmail());
        userMap.put("username", username != null ? username : "NewUser");
        userMap.put("profilePhoto", photoUrl);
        userMap.put("uid", user.getUid());
        userMap.put("gender", gender);
        userMap.put("Fans", 0);
        userMap.put("Models", 0);
        userMap.put("posts", 0);

        databaseRef.child(user.getUid()).setValue(userMap)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(C_SignInProfilePhoto.this, "Profile Saved Successfully!", Toast.LENGTH_SHORT).show();
                        navigateToLogin();
                    } else {
                        updateSaveButtonState(true);
                        Toast.makeText(C_SignInProfilePhoto.this, "Database Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(C_SignInProfilePhoto.this, B_LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up ML Kit resources
        if (faceDetector != null) {
            faceDetector.close();
        }
    }
}

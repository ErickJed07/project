package com.example.project;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseLandmark;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class G7_NewOutfitActivity extends AppCompatActivity {

    private ImageView selectedModelImage;
    private FloatingActionButton selectModelFab, selectClosetFab;
    private LinearLayout modelFabContainer, closetFabContainer;
    private DatabaseReference dbRef;
    private FirebaseAuth mAuth;
    private List<String> modelUrls = new ArrayList<>();
    private PoseDetector poseDetector;
    
    private BottomSheetDialog bottomSheetDialog;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        validateAndUploadModel(imageUri);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.g7_new_outfit);

        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference();

        PoseDetectorOptions options = new PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.SINGLE_IMAGE_MODE)
                .build();
        poseDetector = PoseDetection.getClient(options);

        // Initialize Cloudinary using Config
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("cloud_name", G7_ApiConfig.CLOUDINARY_CLOUD_NAME);
            config.put("api_key", G7_ApiConfig.CLOUDINARY_API_KEY);
            config.put("api_secret", G7_ApiConfig.CLOUDINARY_API_SECRET);
            MediaManager.init(this, config);
        } catch (IllegalStateException e) {
            // Already initialized
        }

        ImageView backBtn = findViewById(R.id.back_btn);
        TextView saveBtn = findViewById(R.id.save_btn);
        selectedModelImage = findViewById(R.id.selected_model_image);
        selectModelFab = findViewById(R.id.select_model_fab);
        selectClosetFab = findViewById(R.id.select_closet_fab);
        modelFabContainer = findViewById(R.id.model_fab_container);
        closetFabContainer = findViewById(R.id.closet_fab_container);

        closetFabContainer.setVisibility(View.GONE);

        backBtn.setOnClickListener(v -> finish());

        saveBtn.setOnClickListener(v -> {
            Toast.makeText(this, "Outfit Saved", Toast.LENGTH_SHORT).show();
            finish();
        });

        selectModelFab.setOnClickListener(v -> showModelBottomSheet());
        
        selectClosetFab.setOnClickListener(v -> {
            Toast.makeText(this, "Closet selection coming soon", Toast.LENGTH_SHORT).show();
        });

        fetchModels();
    }

    private void fetchModels() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        dbRef.child("Users").child(uid).child("ModelsList").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                modelUrls.clear();
                modelUrls.add("ADD_MODEL");
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Object value = ds.getValue();
                    if (value instanceof String) {
                        modelUrls.add((String) value);
                    } else if (ds.getKey() != null) {
                        fetchModelProfilePhoto(ds.getKey());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void fetchModelProfilePhoto(String modelId) {
        dbRef.child("Users").child(modelId).child("profilePhoto").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String url = snapshot.getValue(String.class);
                if (url != null && !url.isEmpty() && !modelUrls.contains(url)) {
                    modelUrls.add(url);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void showModelBottomSheet() {
        bottomSheetDialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.g7_bottom_sheet_models, null);
        bottomSheetDialog.setContentView(view);

        RecyclerView recyclerView = view.findViewById(R.id.models_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        
        G7_ModelAdapter adapter = new G7_ModelAdapter(this, modelUrls, new G7_ModelAdapter.OnModelClickListener() {
            @Override
            public void onModelClick(String url) {
                Glide.with(G7_NewOutfitActivity.this).load(url).into(selectedModelImage);
                bottomSheetDialog.dismiss();
                modelFabContainer.setVisibility(View.GONE);
                closetFabContainer.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAddModelClick() {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                pickImageLauncher.launch(intent);
            }
        });
        
        recyclerView.setAdapter(adapter);
        bottomSheetDialog.show();
    }

    private void validateAndUploadModel(Uri imageUri) {
        try {
            InputImage image = InputImage.fromFilePath(this, imageUri);
            Toast.makeText(this, "Validating body pose...", Toast.LENGTH_SHORT).show();
            poseDetector.process(image)
                    .addOnSuccessListener(pose -> {
                        if (checkPoseForFullBody(pose)) {
                            uploadToCloudinary(imageUri);
                        } else {
                            Toast.makeText(this, getString(R.string.error_not_full_body), Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("G7_NewOutfitActivity", "Pose detection error", e);
                        Toast.makeText(this, getString(R.string.error_obscured_joints), Toast.LENGTH_LONG).show();
                    });
        } catch (IOException e) {
            Log.e("G7_NewOutfitActivity", "Error reading image", e);
            Toast.makeText(this, "Error reading image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkPoseForFullBody(Pose pose) {
        int[] requiredLandmarks = {
                PoseLandmark.NOSE,
                PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER,
                PoseLandmark.LEFT_ELBOW, PoseLandmark.RIGHT_ELBOW,
                PoseLandmark.LEFT_WRIST, PoseLandmark.RIGHT_WRIST,
                PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP,
                PoseLandmark.LEFT_KNEE, PoseLandmark.RIGHT_KNEE,
                PoseLandmark.LEFT_ANKLE, PoseLandmark.RIGHT_ANKLE
        };

        for (int landmarkType : requiredLandmarks) {
            PoseLandmark landmark = pose.getPoseLandmark(landmarkType);
            if (landmark == null || landmark.getInFrameLikelihood() < 0.7f) {
                return false;
            }
        }
        return true;
    }

    private void uploadToCloudinary(Uri imageUri) {
        Toast.makeText(this, "Uploading model...", Toast.LENGTH_SHORT).show();
        MediaManager.get().upload(imageUri)
                .option("folder", "Models")
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}
                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("secure_url");
                        saveModelToFirebase(imageUrl);
                    }
                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        runOnUiThread(() -> Toast.makeText(G7_NewOutfitActivity.this, "Upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show());
                    }
                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    private void saveModelToFirebase(String imageUrl) {
        String uid = mAuth.getCurrentUser().getUid();
        String modelId = dbRef.child("Users").child(uid).child("ModelsList").push().getKey();
        
        if (modelId != null) {
            dbRef.child("Users").child(uid).child("ModelsList").child(modelId).setValue(imageUrl)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(G7_NewOutfitActivity.this, "Model added successfully!", Toast.LENGTH_SHORT).show();
                    if (bottomSheetDialog != null && bottomSheetDialog.isShowing()) {
                        bottomSheetDialog.dismiss();
                    }
                });
            dbRef.child("Users").child(uid).child("customModels").child(modelId).setValue(imageUrl);
        }
    }
}

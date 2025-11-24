package com.example.project;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class F1_CameraActivity extends AppCompatActivity {

    private PreviewView previewView;
    private ImageView btnFlashToggle;
    private ProgressBar progressBar;
    private ImageCapture imageCapture;
    private ProcessCameraProvider cameraProvider;
    private boolean isFlashOn = false;
    private boolean cameraPaused = false;

    private void setUIEnabled(boolean enabled) {
        btnFlashToggle.setEnabled(enabled);
        findViewById(R.id.btnCapture).setEnabled(enabled);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.f1_camera);

        previewView = findViewById(R.id.previewView);
        btnFlashToggle = findViewById(R.id.btnFlashToggle);
        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        btnFlashToggle.setOnClickListener(v -> {
            isFlashOn = !isFlashOn;
            btnFlashToggle.setImageResource(isFlashOn ? R.drawable.flash_on : R.drawable.flash_off);
            if (!cameraPaused) {
                setUIEnabled(false);
                startCamera();
            }
        });

        findViewById(R.id.btnCapture).setOnClickListener(v -> {
            if (imageCapture != null && !cameraPaused) {
                capturePhoto();
            } else {
                Toast.makeText(this, "Camera not ready", Toast.LENGTH_SHORT).show();
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setFlashMode(isFlashOn ? ImageCapture.FLASH_MODE_ON : ImageCapture.FLASH_MODE_OFF)
                        .build();

                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture);

                cameraPaused = false;
                setUIEnabled(true);

            } catch (Exception e) {
                Log.e("CameraX", "Camera start failed", e);
                Toast.makeText(this, "Failed to start camera", Toast.LENGTH_SHORT).show();
                setUIEnabled(true);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void capturePhoto() {
        progressBar.setVisibility(android.view.View.VISIBLE);
        setUIEnabled(false);

        File photoFile = new File(getFilesDir(), "photo_" +
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".jpg");

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        progressBar.setVisibility(android.view.View.GONE);
                        setUIEnabled(true);
                        Log.d("F1_CameraActivity", "Photo saved at: " + photoFile.getAbsolutePath());

                        Intent intent = new Intent(F1_CameraActivity.this, F2_Camera_EditActivity.class);
                        intent.putExtra("photo_path", photoFile.getAbsolutePath());
                        startActivity(intent);

                        if (cameraProvider != null) {
                            cameraProvider.unbindAll();
                            cameraPaused = true;
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        progressBar.setVisibility(android.view.View.GONE);
                        setUIEnabled(true);
                        Toast.makeText(F1_CameraActivity.this, "Capture failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}

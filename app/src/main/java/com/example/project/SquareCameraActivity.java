package com.example.project;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.core.ViewPort;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;

public class SquareCameraActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private PreviewView previewView;
    private ImageView ivCapturedImage;
    private View btnShutter;
    private View layoutConfirmOptions;
    private View layoutGridOverlay;
    private ImageView btnFlashToggle;
    private ImageCapture imageCapture;
    private Bitmap capturedBitmap;
    private int flashMode = ImageCapture.FLASH_MODE_OFF;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_square_camera);

        previewView = findViewById(R.id.view_finder);
        ivCapturedImage = findViewById(R.id.iv_captured_image);
        btnShutter = findViewById(R.id.btn_shutter);
        layoutConfirmOptions = findViewById(R.id.layout_confirm_options);
        layoutGridOverlay = findViewById(R.id.layout_grid_overlay);
        btnFlashToggle = findViewById(R.id.btn_flash_toggle);

        findViewById(R.id.btn_close).setOnClickListener(v -> finish());

        btnShutter.setOnClickListener(v -> takePhoto());

        findViewById(R.id.btn_retake).setOnClickListener(v -> resetCamera());

        findViewById(R.id.btn_use_photo).setOnClickListener(v -> returnResult());

        btnFlashToggle.setOnClickListener(v -> toggleFlash());

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE);
        }
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setFlashMode(flashMode)
                        .build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();

                ViewPort viewPort = previewView.getViewPort();
                if (viewPort != null) {
                    UseCaseGroup useCaseGroup = new UseCaseGroup.Builder()
                            .addUseCase(preview)
                            .addUseCase(imageCapture)
                            .setViewPort(viewPort)
                            .build();
                    cameraProvider.bindToLifecycle(this, cameraSelector, useCaseGroup);
                } else {
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
                }

            } catch (ExecutionException | InterruptedException e) {
                Log.e("SquareCamera", "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void toggleFlash() {
        if (flashMode == ImageCapture.FLASH_MODE_OFF) {
            flashMode = ImageCapture.FLASH_MODE_ON;
            btnFlashToggle.setImageResource(R.drawable.flash_on);
            Toast.makeText(this, "Flash ON", Toast.LENGTH_SHORT).show();
        } else {
            flashMode = ImageCapture.FLASH_MODE_OFF;
            btnFlashToggle.setImageResource(R.drawable.flash_off);
            Toast.makeText(this, "Flash OFF", Toast.LENGTH_SHORT).show();
        }

        if (imageCapture != null) {
            imageCapture.setFlashMode(flashMode);
        }
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        imageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                capturedBitmap = processImageProxy(image);
                image.close();

                runOnUiThread(() -> {
                    ivCapturedImage.setImageBitmap(capturedBitmap);
                    ivCapturedImage.setVisibility(View.VISIBLE);
                    // Do not hide previewView immediately to avoid flicker, 
                    // ivCapturedImage will cover it due to elevation.
                    layoutGridOverlay.setVisibility(View.GONE);
                    btnShutter.setVisibility(View.GONE);
                    layoutConfirmOptions.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e("SquareCamera", "Photo capture failed: " + exception.getMessage(), exception);
            }
        });
    }

    private Bitmap processImageProxy(ImageProxy image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        
        // Use a more robust way to decode the byte array
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        if (bitmap != null) {
            Matrix matrix = new Matrix();
            matrix.postRotate(image.getImageInfo().getRotationDegrees());
            
            // Create a mutable copy if necessary and ensure it's in a compatible format
            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            // Force Square Crop
            int width = rotatedBitmap.getWidth();
            int height = rotatedBitmap.getHeight();
            int dimension = Math.min(width, height);
            int x = (width - dimension) / 2;
            int y = (height - dimension) / 2;

            return Bitmap.createBitmap(rotatedBitmap, x, y, dimension, dimension);
        }
        return null;
    }

    private void resetCamera() {
        capturedBitmap = null;
        ivCapturedImage.setVisibility(View.GONE);
        previewView.setVisibility(View.VISIBLE);
        layoutGridOverlay.setVisibility(View.VISIBLE);
        btnShutter.setVisibility(View.VISIBLE);
        layoutConfirmOptions.setVisibility(View.GONE);
    }

    private void returnResult() {
        if (capturedBitmap == null) return;

        try {
            File cachePath = new File(getCacheDir(), "images");
            if (!cachePath.exists()) {
                cachePath.mkdirs();
            }
            String timeStamp = String.valueOf(System.currentTimeMillis());
            File file = new File(cachePath, "captured_square_" + timeStamp + ".jpg");
            FileOutputStream stream = new FileOutputStream(file);
            capturedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            stream.close();

            Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent resultIntent = new Intent();
            resultIntent.setData(contentUri);
            setResult(RESULT_OK, resultIntent);
            finish();
        } catch (IOException e) {
            Log.e("SquareCamera", "Error saving captured image", e);
            setResult(RESULT_CANCELED);
            finish();
        }
    }
}

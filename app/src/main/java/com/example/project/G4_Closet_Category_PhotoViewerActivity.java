package com.example.project;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class G4_Closet_Category_PhotoViewerActivity extends AppCompatActivity {

    private static final String TAG = "ClosetPhotoViewer";
    private ViewPager2 viewPager;
    private List<String> imagePaths;
    private G5_Closet_Category_PhotoPagerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.g3_closet_category_photo_viewer);

        imagePaths = getIntent().getStringArrayListExtra("IMAGES");
        if (imagePaths == null) imagePaths = new ArrayList<>();

        // Defensive: if no images, close viewer
        if (imagePaths.isEmpty()) {
            Toast.makeText(this, "No images to display", Toast.LENGTH_SHORT).show();
            finishWithAnimation();
            return;
        }

        int startIndex = getIntent().getIntExtra("START_INDEX", 0);
        if (startIndex < 0 || startIndex >= imagePaths.size()) startIndex = 0;

        viewPager = findViewById(R.id.viewPager);
        adapter = new G5_Closet_Category_PhotoPagerAdapter(this, imagePaths);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(startIndex, false);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finishWithAnimation());

        Button btnDelete = findViewById(R.id.btnDelete);
        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> {
                try {
                    int position = viewPager.getCurrentItem();
                    if (position < 0 || position >= imagePaths.size()) return;

                    String path = imagePaths.get(position);
                    File f = new File(path);
                    if (f.exists()) {
                        boolean deleted = f.delete();
                        Log.d(TAG, "delete file " + path + " -> " + deleted);
                    }

                    imagePaths.remove(position);
                    adapter.notifyItemRemoved(position);

                    if (imagePaths.isEmpty()) {
                        // no more images -> close viewer
                        finishWithAnimation();
                    } else {
                        // ensure current item is valid
                        int newPos = Math.min(position, imagePaths.size() - 1);
                        viewPager.setCurrentItem(newPos, true);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error deleting image", e);
                    Toast.makeText(this, "Error deleting image", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Optional: Modern back handling
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishWithAnimation();
            }
        });
    }

    private void finishWithAnimation() {
        finish();
        overridePendingTransition(R.anim.no_animation, R.anim.slide_out_down);
    }
}

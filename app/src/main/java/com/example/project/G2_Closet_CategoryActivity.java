package com.example.project;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

public class G2_Closet_CategoryActivity extends AppCompatActivity {

    private RecyclerView galleryRecyclerView;
    private G3_Closet_CategoryAdapter adapter;
    private final List<File> imageFileList = new ArrayList<>();
    private final Set<File> selectedFiles = new HashSet<>();
    private boolean isMultiSelectMode = false;
    private FloatingActionButton deleteFab;
    private String categoryName;

    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;
    private String uid; // ✅ store current user's UID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.g2_closet_gallery);

        categoryName = getIntent().getStringExtra("CATEGORY_NAME");
        TextView titleView = findViewById(R.id.categoryTitle);
        if (categoryName != null) titleView.setText(categoryName);

        mAuth = FirebaseAuth.getInstance();
        uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        // ✅ Firebase reference per user
        if (uid != null) {
            dbRef = FirebaseDatabase.getInstance().getReference("ClosetData").child(uid);
        } else {
            dbRef = FirebaseDatabase.getInstance().getReference("ClosetData");
        }

        deleteFab = findViewById(R.id.fabDelete);
        deleteFab.hide();

        galleryRecyclerView = findViewById(R.id.galleryRecyclerView);
        galleryRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        adapter = new G3_Closet_CategoryAdapter(
                this,
                imageFileList,
                this::onImageClicked,
                this::onImageLongClicked,
                this::isFileSelected,
                this::isMultiSelectMode
        );

        galleryRecyclerView.setAdapter(adapter);
        loadImagesForCategory();

        deleteFab.setOnClickListener(v -> {
            for (File file : selectedFiles) {
                file.delete();
            }
            selectedFiles.clear();
            exitMultiSelectMode();
            loadImagesForCategory();
            Toast.makeText(this, "Photos deleted", Toast.LENGTH_SHORT).show();
        });
    }

    private void onImageClicked(File file) {
        if (isMultiSelectMode) {
            toggleSelection(file);
        } else {
            ArrayList<String> imagePaths = new ArrayList<>();
            for (File f : imageFileList) imagePaths.add(f.getAbsolutePath());
            int index = imageFileList.indexOf(file);

            Intent intent = new Intent(this, G4_Closet_Category_PhotoViewerActivity.class);
            intent.putStringArrayListExtra("IMAGES", imagePaths);
            intent.putExtra("START_INDEX", index);
            // DEBUG: show how many images are being passed
            Toast.makeText(this, "Opening viewer: " + imagePaths.size() + " images", Toast.LENGTH_SHORT).show();
            startActivity(intent);
        }
    }

    private void onImageLongClicked(File file) {
        if (!isMultiSelectMode) {
            isMultiSelectMode = true;
            selectedFiles.add(file);
            adapter.notifyDataSetChanged();
        // DEBUG: show how many images were loaded and the directory
        try {
            File categoryDir = new File(getFilesDir(), "ClosetImages/" + uid + "/" + categoryName);
            Toast.makeText(this, "Found " + imageFileList.size() + " images in " + categoryDir.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) { /* ignore debug toast failures */ }
        
            deleteFab.show();
        } else {
            toggleSelection(file);
        }
    }

    private void toggleSelection(File file) {
        if (selectedFiles.contains(file)) {
            selectedFiles.remove(file);
        } else {
            selectedFiles.add(file);
        }

        if (selectedFiles.isEmpty()) {
            exitMultiSelectMode();
        }

        adapter.notifyDataSetChanged();
        // DEBUG: show how many images were loaded and the directory
        try {
            File categoryDir = new File(getFilesDir(), "ClosetImages/" + uid + "/" + categoryName);
            Toast.makeText(this, "Found " + imageFileList.size() + " images in " + categoryDir.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) { /* ignore debug toast failures */ }
        
    }

    private void exitMultiSelectMode() {
        isMultiSelectMode = false;
        selectedFiles.clear();
        adapter.notifyDataSetChanged();
        // DEBUG: show how many images were loaded and the directory
        try {
            File categoryDir = new File(getFilesDir(), "ClosetImages/" + uid + "/" + categoryName);
            Toast.makeText(this, "Found " + imageFileList.size() + " images in " + categoryDir.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) { /* ignore debug toast failures */ }
        
        deleteFab.hide();
    }

    private boolean isFileSelected(File file) {
        return selectedFiles.contains(file);
    }

    private boolean isMultiSelectMode() {
        return isMultiSelectMode;
    }

    // ✅ Load images only for this user's category
    private void loadImagesForCategory() {
        imageFileList.clear();

        if (uid == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        File categoryDir = new File(getFilesDir(), "ClosetImages/" + uid + "/" + categoryName);
        if (!categoryDir.exists()) categoryDir.mkdirs();

        File[] files = categoryDir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.getName().endsWith(".jpg") || f.getName().endsWith(".png")) {
                    imageFileList.add(f);
                }
            }
        }

        adapter.notifyDataSetChanged();
// DEBUG: show how many images were loaded and the directory
        try {
            Toast.makeText(this, "Found " + imageFileList.size() + " images in " + categoryDir.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) { /* ignore debug toast failures */ }


    }

    public void onCloset_backClicked(View view) {
        finish();
    }

    // ✅ Now saves images per user
    public static void saveImageToCategory(Context context, Bitmap bitmap, String categoryName, String imageName, String uid) {
        if (uid == null) return;

        File categoryDir = new File(context.getFilesDir(), "ClosetImages/" + uid + "/" + categoryName);
        if (!categoryDir.exists()) categoryDir.mkdirs();

        File imageFile = new File(categoryDir, imageName + ".jpg");
        try (FileOutputStream fos = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onButtonClicked(View view) {
        Intent intent = null;
        int id = view.getId();

        if (id == R.id.home_menu) intent = new Intent(this, D1_FeedActivity.class);
        else if (id == R.id.calendar_menu) intent = new Intent(this, E1_CalendarActivity.class);
        else if (id == R.id.add_menu) intent = new Intent(this, F1_CameraActivity.class);
        else if (id == R.id.closet_menu) intent = new Intent(this, G1_ClosetActivity.class);
        else if (id == R.id.profile_menu) intent = new Intent(this, I1_ProfileActivity.class);

        if (intent != null) {
            startActivity(intent);
            finish();
        }
    }

    /*** NEW STATIC METHODS FOR CATEGORY MANAGEMENT ***/
    public static List<String> loadCategories(Context context, String uid) {
        List<String> categories = new ArrayList<>();
        if (uid == null) return categories;

        File closetRoot = new File(context.getFilesDir(), "ClosetImages/" + uid);
        if (!closetRoot.exists()) closetRoot.mkdirs();

        File[] dirs = closetRoot.listFiles();
        if (dirs != null) {
            for (File f : dirs) {
                if (f.isDirectory()) {
                    categories.add(f.getName());
                }
            }
        }
        return categories;
    }

    public static void saveCategoryLocally(Context context, String categoryName, String uid) {
        if (categoryName == null || categoryName.trim().isEmpty() || uid == null) return;

        File categoryDir = new File(context.getFilesDir(), "ClosetImages/" + uid + "/" + categoryName);
        if (!categoryDir.exists()) {
            categoryDir.mkdirs();
        }
    }

    public static void saveCategoryToFirebase(Context context, String categoryName, String uid) {
        if (categoryName == null || categoryName.trim().isEmpty() || uid == null) return;

        saveCategoryLocally(context, categoryName, uid);

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("ClosetData")
                .child(uid) // ✅ per-user categories
                .child("categories");
        ref.child(categoryName).setValue(true);
    }
}

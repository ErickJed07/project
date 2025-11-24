package com.example.project;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class H1_DressActivity extends AppCompatActivity {

    private H2_Dresser_OutfitView outfitView;
    private Button btnSave;
    private ImageButton btnToggleCarousel, btnBack, btnRotate;
    private String uid;

    private static final int REQ_CODE_POST_NOTIFICATIONS = 101;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.h1_dresser);

        outfitView = findViewById(R.id.outfitView);
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);
        btnToggleCarousel = findViewById(R.id.btnToggleCarousel);
        btnRotate = findViewById(R.id.btnrotate);

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        btnRotate.setVisibility(View.GONE);

        // ✅ Runtime permissions
        checkAndRequestPermissions();

        // Back → Closet
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(H1_DressActivity.this, G1_ClosetActivity.class);
            startActivity(intent);
            finish();
        });

        // Toggle categories + photos
        btnToggleCarousel.setOnClickListener(v -> showDresserBottomSheet());

        // Reset rotation
        btnRotate.setOnClickListener(v -> outfitView.resetSelectedRotation());

        // Sync reset button visibility with selection
        outfitView.setOnSelectionChangedListener((selected, scale) -> {
            btnRotate.setVisibility(selected != null ? View.VISIBLE : View.GONE);
        });

        // Save button
        btnSave.setOnClickListener(v -> {
            Bitmap bitmap = outfitView.exportToBitmap();
            if (bitmap != null) {
                String path = saveBitmapToFile(bitmap);
                if (path != null) {
                    saveToPermanentCategory(path);  // Save image in permanent category
                    showSavePopup(path);  // Show save popup for event scheduling
                } else {
                    Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Nothing to save", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * ✅ Handle runtime permissions for notifications + exact alarms
     */
    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.d("H1_DressActivity", "Requesting POST_NOTIFICATIONS permission");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_CODE_POST_NOTIFICATIONS);
            } else {
                Log.d("H1_DressActivity", "POST_NOTIFICATIONS already granted");
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (am != null && !am.canScheduleExactAlarms()) {
                Log.w("H1_DressActivity", "Exact alarm permission not granted. Opening settings...");
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_CODE_POST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("H1_DressActivity", "POST_NOTIFICATIONS granted by user");
            } else {
                Log.w("H1_DressActivity", "POST_NOTIFICATIONS denied by user");
                Toast.makeText(this, "Notifications are disabled. Reminders may not work.", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Save bitmap to local file
     */
    private String saveBitmapToFile(Bitmap bitmap) {
        try {
            File dir = new File(getFilesDir(), "outfits");
            if (!dir.exists()) dir.mkdirs();

            String fileName = "outfit_" + System.currentTimeMillis() + ".png";
            File file = new File(dir, fileName);

            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();

            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Save photo in the "PermanentCategory" folder
     */
    private void saveToPermanentCategory(String photoPath) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Define the "Permanent" category directory
        File permanentCategoryDir = new File(getFilesDir(), "ClosetImages/" + uid + "/PermanentCategory");

        // Create directory if it doesn't exist
        if (!permanentCategoryDir.exists()) {
            permanentCategoryDir.mkdirs();
        }

        if (photoPath != null && !photoPath.isEmpty()) {
            File sourceFile = new File(photoPath);  // Source file from the given photo path
            File destFile = new File(permanentCategoryDir, sourceFile.getName());  // Destination file in the "PermanentCategory" folder

            try {
                // Copy the file to the permanent category directory
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                // Show confirmation that the photo was saved to the permanent category
                Toast.makeText(this, "Saved to Permanent Category", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * BottomSheet with categories + photos
     */
    private void showDresserBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.h2_dresser_bottomsheet, null);
        bottomSheetDialog.setContentView(sheetView);

        RecyclerView sheetCategories = sheetView.findViewById(R.id.recyclerCategories);
        RecyclerView sheetPhotos = sheetView.findViewById(R.id.recyclerPhotos);

        sheetCategories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        sheetPhotos.setLayoutManager(new GridLayoutManager(this, 3));

        H5_Dress_PhotoAdapter sheetPhotoAdapter = new H5_Dress_PhotoAdapter(
                this,
                new ArrayList<>(),
                photoPath -> {
                    if (photoPath != null) {
                        Uri uri = Uri.parse(photoPath);
                        File f = new File(photoPath);
                        if (f.exists()) uri = Uri.fromFile(f);

                        ImageView iv = outfitView.addImage(uri);
                        outfitView.setSelectedView(iv);
                        bottomSheetDialog.dismiss();
                    }
                }
        );
        sheetPhotos.setAdapter(sheetPhotoAdapter);

        H4_DressAdapter sheetCategoryAdapter = new H4_DressAdapter(
                this,
                new ArrayList<>(),
                categoryName -> loadPhotosForCategory(categoryName, sheetPhotoAdapter)
        );
        sheetCategories.setAdapter(sheetCategoryAdapter);

        loadCategoriesFromFirebase(sheetCategoryAdapter, sheetPhotoAdapter);

        bottomSheetDialog.show();
    }

    private void loadCategoriesFromFirebase(final H4_DressAdapter categoryAdapter,
                                            final H5_Dress_PhotoAdapter photoAdapter) {
        if (uid == null) return;

        DatabaseReference ref = FirebaseDatabase
                .getInstance()
                .getReference("ClosetData")
                .child(uid)
                .child("categories");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> categoryNames = new ArrayList<>();
                List<String> categoryThumbs = new ArrayList<>();

                for (DataSnapshot child : snapshot.getChildren()) {
                    String name = child.child("name").getValue(String.class);
                    if (name != null) {
                        categoryNames.add(name);

                        File categoryDir = new File(getFilesDir(), "ClosetImages/" + uid + "/" + name);
                        File[] files = categoryDir.listFiles((dir, fileName) ->
                                fileName.toLowerCase().endsWith(".jpg") || fileName.toLowerCase().endsWith(".png")
                        );

                        String latestPath = null;
                        if (files != null && files.length > 0) {
                            Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
                            latestPath = files[0].getAbsolutePath();
                        }
                        categoryThumbs.add(latestPath);
                    }
                }

                categoryAdapter.updateData(categoryNames, categoryThumbs);

                if (!categoryNames.isEmpty()) {
                    loadPhotosForCategory(categoryNames.get(0), photoAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w("H1_DressActivity", "Failed to load categories", error.toException());
            }
        });
    }

    private void loadPhotosForCategory(String categoryName, H5_Dress_PhotoAdapter adapter) {
        if (uid == null) return;

        File categoryDir = new File(getFilesDir(), "ClosetImages/" + uid + "/" + categoryName);
        File[] files = categoryDir.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".png")
        );

        List<String> paths = new ArrayList<>();
        if (files != null && files.length > 0) {
            Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
            for (File f : files) paths.add(f.getAbsolutePath());
            adapter.setPhotos(paths);
        } else {
            adapter.setPhotos(new ArrayList<>());
            Toast.makeText(H1_DressActivity.this, "No images in " + categoryName, Toast.LENGTH_SHORT).show();
        }
    }

    private void showSavePopup(String imagePath) {
        View popupView = getLayoutInflater().inflate(R.layout.h5_dresser_save_popup, null);

        EditText etTitle = popupView.findViewById(R.id.etTitle);
        EditText etDate = popupView.findViewById(R.id.etDate);
        CheckBox cbEnableAlarm = popupView.findViewById(R.id.cbEnableAlarm);
        TextInputLayout layoutTime = popupView.findViewById(R.id.layoutTime);
        TextInputLayout layoutReminder = popupView.findViewById(R.id.layoutReminder);
        EditText etTime = popupView.findViewById(R.id.etTime);
        AutoCompleteTextView spinnerReminder = popupView.findViewById(R.id.spinnerReminder);
        Button btnCancel = popupView.findViewById(R.id.btnCancel);
        Button btnSave = popupView.findViewById(R.id.btnSave);

        final Calendar selectedDate = Calendar.getInstance();

        etDate.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            DatePickerDialog dialog = new DatePickerDialog(
                    this,
                    (view, year, month, day) -> {
                        selectedDate.set(Calendar.YEAR, year);
                        selectedDate.set(Calendar.MONTH, month);
                        selectedDate.set(Calendar.DAY_OF_MONTH, day);
                        etDate.setText(day + "/" + (month + 1) + "/" + year);
                    },
                    now.get(Calendar.YEAR),
                    now.get(Calendar.MONTH),
                    now.get(Calendar.DAY_OF_MONTH)
            );
            dialog.show();
        });

        etTime.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            TimePickerDialog dialog = new TimePickerDialog(
                    this,
                    (view, hourOfDay, minute) -> {
                        selectedDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        selectedDate.set(Calendar.MINUTE, minute);
                        etTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
                    },
                    now.get(Calendar.HOUR_OF_DAY),
                    now.get(Calendar.MINUTE),
                    false
            );
            dialog.show();
        });

        String[] reminders = {"None", "1 hour before", "45 min before", "30 min before", "15 min before"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, reminders);
        spinnerReminder.setAdapter(adapter);
        spinnerReminder.setText(reminders[0], false);

        cbEnableAlarm.setOnCheckedChangeListener((buttonView, isChecked) -> {
            layoutReminder.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(popupView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String reminder = spinnerReminder.getText().toString();
            String time = etTime.getText().toString();
            String dateString = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(selectedDate.getTime());

            if (title.isEmpty()) {
                Toast.makeText(this, "Title required", Toast.LENGTH_SHORT).show();
                return;
            }

            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("CalendarEvents")
                    .child(uid);

            String eventKey = userRef.push().getKey();
            if (eventKey == null) {
                eventKey = UUID.randomUUID().toString();
            }

            E2_Calendar_Event event = new E2_Calendar_Event(
                    eventKey,
                    title,
                    dateString,
                    time == null ? "" : time,
                    imagePath,
                    reminder == null ? E3_Calendar_ReminderUtils.NONE : reminder
            );

            userRef.child(eventKey).setValue(event);

            Toast.makeText(this, "Event saved!", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(H1_DressActivity.this, E1_CalendarActivity.class);
            startActivity(intent);
            dialog.dismiss();
        });

        dialog.show();
    }
}

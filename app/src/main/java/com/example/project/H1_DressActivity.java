package com.example.project;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
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
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class H1_DressActivity extends AppCompatActivity {
    private Button btnOpenReco;

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

        // Initialize Cloudinary if not already done
        initCloudinary();

        outfitView = findViewById(R.id.outfitView);
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);
        btnToggleCarousel = findViewById(R.id.btnToggleCarousel);
        btnRotate = findViewById(R.id.btnrotate);

        // Ensure user is logged in
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        // Reco Button Logic
        btnOpenReco = findViewById(R.id.btnOpenReco);
        btnOpenReco.setOnClickListener(v -> {
            Intent intent = new Intent(H1_DressActivity.this, H6_RecommendationActivity.class);
            startActivity(intent);
        });


        btnRotate.setVisibility(View.GONE);

        // Runtime permissions
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

        // Sync reset button visibility with selection
        outfitView.setOnSelectionChangedListener((selected, scale) -> {
            btnRotate.setVisibility(selected != null ? View.VISIBLE : View.GONE);
        });

        // Save button

    }

    private void initCloudinary() {
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("cloud_name", BuildConfig.CLOUDINARY_CLOUD_NAME);
            config.put("api_key", BuildConfig.CLOUDINARY_API_KEY);
            config.put("api_secret", BuildConfig.CLOUDINARY_API_SECRET);
            MediaManager.init(this, config);
        } catch (Exception e) {
            // Already initialized
        }
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_CODE_POST_NOTIFICATIONS);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (am != null && !am.canScheduleExactAlarms()) {
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
                Toast.makeText(this, "Notifications are disabled.", Toast.LENGTH_LONG).show();
            }
        }
    }

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



    private void showSavePopup(String localImagePath) {
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
            DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, day) -> {
                selectedDate.set(Calendar.YEAR, year);
                selectedDate.set(Calendar.MONTH, month);
                selectedDate.set(Calendar.DAY_OF_MONTH, day);
                etDate.setText(day + "/" + (month + 1) + "/" + year);
            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        });

        etTime.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            TimePickerDialog dialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                selectedDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selectedDate.set(Calendar.MINUTE, minute);
                etTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), false);
            dialog.show();
        });

        String[] reminders = {"None", "1 hour before", "45 min before", "30 min before", "15 min before"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, reminders);
        spinnerReminder.setAdapter(adapter);
        spinnerReminder.setText(reminders[0], false);

        cbEnableAlarm.setOnCheckedChangeListener((buttonView, isChecked) -> layoutReminder.setVisibility(isChecked ? View.VISIBLE : View.GONE));

        AlertDialog dialog = new AlertDialog.Builder(this).setView(popupView).create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String reminder = spinnerReminder.getText().toString();
            String time = etTime.getText().toString();
            String dateString = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.getTime());

            if (title.isEmpty()) {
                Toast.makeText(this, "Title required", Toast.LENGTH_SHORT).show();
                return;
            }
            btnSave.setEnabled(false);
            Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show();

            MediaManager.get().upload(localImagePath)
                    .option("folder", "ClosetImages/" + uid + "/Events")
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {}
                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {}
                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            String cloudUrl = (String) resultData.get("secure_url");
                            runOnUiThread(() -> {
                                saveEventToFirebase(title, dateString, time, reminder, cloudUrl);
                                dialog.dismiss();
                            });
                        }
                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            runOnUiThread(() -> {
                                btnSave.setEnabled(true);
                                Toast.makeText(H1_DressActivity.this, "Upload failed: " + error.getDescription(), Toast.LENGTH_LONG).show();
                            });
                        }
                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {}
                    }).dispatch();
        });
        dialog.show();
    }

    private void saveEventToFirebase(String title, String dateString, String time, String reminder, String imageUrl) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("CalendarEvents").child(uid);
        String eventKey = userRef.push().getKey();
        if (eventKey == null) eventKey = UUID.randomUUID().toString();

        E2_Calendar_Event event = new E2_Calendar_Event(
                eventKey, title, dateString,
                time == null ? "" : time,
                imageUrl,
                reminder == null ? E3_Calendar_ReminderUtils.NONE : reminder
        );
        userRef.child(eventKey).setValue(event);
        Toast.makeText(this, "Event saved!", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(H1_DressActivity.this, E1_CalendarActivity.class));
        finish();
    }

    private void showDresserBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.h2_dresser_bottomsheet, null);
        bottomSheetDialog.setContentView(sheetView);

        RecyclerView sheetCategories = sheetView.findViewById(R.id.recyclerCategories);
        RecyclerView sheetPhotos = sheetView.findViewById(R.id.recyclerPhotos);

        sheetCategories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        sheetPhotos.setLayoutManager(new GridLayoutManager(this, 3));

        H5_Dress_PhotoAdapter sheetPhotoAdapter = new H5_Dress_PhotoAdapter(this, new ArrayList<>(), photoPath -> {
            if (photoPath != null) {
                Glide.with(H1_DressActivity.this).asBitmap().load(photoPath).into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        ImageView iv = outfitView.addImage(resource);
                        outfitView.setSelectedView(iv);
                        bottomSheetDialog.dismiss();
                    }
                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {}
                });
            }
        });
        sheetPhotos.setAdapter(sheetPhotoAdapter);

        H4_DressAdapter sheetCategoryAdapter = new H4_DressAdapter(this, new ArrayList<>(), categoryName -> loadPhotosForCategory(categoryName, sheetPhotoAdapter));
        sheetCategories.setAdapter(sheetCategoryAdapter);

        loadCategoriesFromFirebase(sheetCategoryAdapter, sheetPhotoAdapter);
        bottomSheetDialog.show();
    }

    private void loadCategoriesFromFirebase(final H4_DressAdapter categoryAdapter, final H5_Dress_PhotoAdapter photoAdapter) {
        if (uid == null) return;
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("categories");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> categoryNames = new ArrayList<>();
                List<String> categoryThumbs = new ArrayList<>();

                for (DataSnapshot categorySnapshot : snapshot.getChildren()) {
                    String categoryId = categorySnapshot.getKey();
                    String name = categorySnapshot.child("name").getValue(String.class);

                    if (name == null && "permanent_category_id".equals(categoryId)) {
                        name = "PermanentCategory";
                    }

                    if (name != null) {
                        categoryNames.add(name);
                        String latestUrl = null;
                        if (categorySnapshot.hasChild("photos")) {
                            for (DataSnapshot photoChild : categorySnapshot.child("photos").getChildren()) {
                                if (photoChild.hasChild("url")) {
                                    latestUrl = photoChild.child("url").getValue(String.class);
                                } else {
                                    Object val = photoChild.getValue();
                                    if (val instanceof String) latestUrl = (String) val;
                                    else if (val instanceof Map) latestUrl = (String) ((Map) val).get("url");
                                }
                                if (latestUrl != null && !latestUrl.isEmpty()) break;
                            }
                        }
                        categoryThumbs.add(latestUrl);
                    }
                }
                categoryAdapter.updateData(categoryNames, categoryThumbs);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadPhotosForCategory(String categoryName, H5_Dress_PhotoAdapter adapter) {
        if (uid == null) return;
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("categories");

        ref.orderByChild("name").equalTo(categoryName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> photoPaths = new ArrayList<>();
                for (DataSnapshot catSnapshot : snapshot.getChildren()) {
                    DataSnapshot photosSnapshot = catSnapshot.child("photos");
                    for (DataSnapshot photoSnap : photosSnapshot.getChildren()) {
                        if (photoSnap.hasChild("url")) {
                            String url = photoSnap.child("url").getValue(String.class);
                            if (url != null) photoPaths.add(url);
                        } else {
                            Object val = photoSnap.getValue();
                            if (val instanceof String) photoPaths.add((String) val);
                            else if (val instanceof Map && ((Map) val).containsKey("url"))
                                photoPaths.add((String) ((Map) val).get("url"));
                        }
                    }
                }

                if (photoPaths.isEmpty() && "PermanentCategory".equals(categoryName)) {
                    ref.child("permanent_category_id").child("photos").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snap) {
                            List<String> permPhotos = new ArrayList<>();
                            for (DataSnapshot p : snap.getChildren()) {
                                if (p.hasChild("url")) {
                                    String url = p.child("url").getValue(String.class);
                                    if (url != null) permPhotos.add(url);
                                } else {
                                    Object val = p.getValue();
                                    if (val instanceof String) permPhotos.add((String) val);
                                    else if (val instanceof Map && ((Map) val).containsKey("url"))
                                        permPhotos.add((String) ((Map) val).get("url"));
                                }
                            }
                            adapter.updatePhotos(permPhotos);
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
                } else {
                    adapter.updatePhotos(photoPaths);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}

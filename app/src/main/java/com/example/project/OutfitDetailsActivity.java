package com.example.project;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OutfitDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "extra_event_id";
    public static final String EXTRA_EVENT_TITLE = "extra_event_title";
    public static final String EXTRA_EVENT_DATE = "extra_event_date";
    public static final String EXTRA_EVENT_TIME = "extra_event_time";
    public static final String EXTRA_EVENT_REMINDER = "extra_event_reminder";
    public static final String EXTRA_EVENT_IMAGE_URL = "extra_event_image_url";

    private String eventId, title, date, time, reminder, imageUrl;
    private TextView tvOutfitTitle, tvOutfitDateTime, tvReminderValue, tvEmptyItems, tvItemCount;
    private android.view.View layoutReminder;
    private android.widget.ImageView ivOutfitIcon;
    private RecyclerView rvSelectedItems;
    private OutfitItemAdapter adapter;
    private List<ClothingItem> itemsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.e4_calendar_outfit_details);

        // Get data from intent
        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        title = getIntent().getStringExtra(EXTRA_EVENT_TITLE);
        date = getIntent().getStringExtra(EXTRA_EVENT_DATE);
        time = getIntent().getStringExtra(EXTRA_EVENT_TIME);
        reminder = getIntent().getStringExtra(EXTRA_EVENT_REMINDER);
        imageUrl = getIntent().getStringExtra(EXTRA_EVENT_IMAGE_URL);

        // Views
        tvOutfitTitle = findViewById(R.id.tvOutfitTitle);
        tvOutfitDateTime = findViewById(R.id.tvOutfitDateTime);
        tvReminderValue = findViewById(R.id.tvReminderValue);
        tvEmptyItems = findViewById(R.id.tvEmptyItems);
        tvItemCount = findViewById(R.id.tvItemCount);
        layoutReminder = findViewById(R.id.layoutReminder);
        ivOutfitIcon = findViewById(R.id.ivOutfitIcon);
        ImageButton btnBack = findViewById(R.id.btnBack);
        ImageButton btnFullScreen = findViewById(R.id.btnFullScreen);
        rvSelectedItems = findViewById(R.id.rvSelectedItems);

        // Set data
        updateUI();

        // Setup RecyclerView
        rvSelectedItems.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new OutfitItemAdapter(itemsList);
        rvSelectedItems.setAdapter(adapter);

        // Load items from Firebase
        loadEventItems();

        // If this is a past event, ensure items are marked as used/archived
        if (isPastEvent()) {
            archiveItemsAsUsed();
        }

        // Listeners
        btnBack.setOnClickListener(v -> finish());
        ivOutfitIcon.setOnClickListener(v -> showFullScreenImage());
        btnFullScreen.setOnClickListener(v -> showFullScreenImage());

        findViewById(R.id.btnSortItems).setOnClickListener(this::showSortMenu);
        findViewById(R.id.btnAddItem).setOnClickListener(v -> showEditDialog());
    }

    private void showSortMenu(android.view.View v) {
        android.widget.PopupMenu popup = new android.widget.PopupMenu(this, v);
        popup.getMenu().add("Sort by Category");
        popup.getMenu().add("Sort by Color");
        popup.getMenu().add("Sort by Size");

        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if (title.equals("Sort by Category")) {
                java.util.Collections.sort(itemsList, (o1, o2) -> {
                    String c1 = o1.getCategoryId() != null ? o1.getCategoryId() : "";
                    String c2 = o2.getCategoryId() != null ? o2.getCategoryId() : "";
                    return c1.compareToIgnoreCase(c2);
                });
            } else if (title.equals("Sort by Color")) {
                java.util.Collections.sort(itemsList, (o1, o2) -> {
                    String c1 = o1.getColor() != null ? o1.getColor() : "";
                    String c2 = o2.getColor() != null ? o2.getColor() : "";
                    return c1.compareToIgnoreCase(c2);
                });
            } else if (title.equals("Sort by Size")) {
                java.util.Collections.sort(itemsList, (o1, o2) -> {
                    String s1 = o1.getSize() != null ? o1.getSize() : "";
                    String s2 = o2.getSize() != null ? o2.getSize() : "";
                    return s1.compareToIgnoreCase(s2);
                });
            }
            adapter.notifyDataSetChanged();
            return true;
        });
        popup.show();
    }

    private void confirmDelete() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Schedule")
                .setMessage("Are you sure you want to delete this outfit from your calendar?")
                .setPositiveButton("Delete", (dialog, which) -> deleteEvent())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteEvent() {
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (uid == null) return;

        com.google.firebase.database.FirebaseDatabase.getInstance()
                .getReference("Users").child(uid).child("Events").child(eventId)
                .removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Schedule deleted", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show());
    }

    private void showFullScreenImage() {
        if (imageUrl == null || imageUrl.isEmpty()) {
            Toast.makeText(this, "No image to show", Toast.LENGTH_SHORT).show();
            return;
        }

        android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.g4_closet_category_item_photo_fullscreen);
        
        android.widget.ImageView fullImage = dialog.findViewById(R.id.fullImage);
        com.bumptech.glide.Glide.with(this)
                .load(imageUrl)
                .into(fullImage);
        
        fullImage.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void updateUI() {
        tvOutfitTitle.setText(title != null ? title : "Outfit");
        String formattedDateTime = formatDateTime(date, time);
        tvOutfitDateTime.setText(formattedDateTime);

        if (reminder != null && !reminder.isEmpty() && !reminder.equals("None")) {
            layoutReminder.setVisibility(android.view.View.VISIBLE);
            tvReminderValue.setText(reminder);
        } else {
            layoutReminder.setVisibility(android.view.View.GONE);
        }

        if (imageUrl != null && !imageUrl.isEmpty()) {
            com.bumptech.glide.Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.bg_outfit_icon)
                    .error(R.drawable.bg_outfit_icon)
                    .centerCrop()
                    .into(ivOutfitIcon);
        } else {
            ivOutfitIcon.setImageResource(R.drawable.bg_outfit_icon);
        }
    }

    private String formatDateTime(String dateStr, String timeStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            Date d = inputFormat.parse(dateStr + " " + timeStr);
            if (d != null) {
                SimpleDateFormat outputFormat = new SimpleDateFormat("EEEE, MMMM d • h:mm a", Locale.getDefault());
                return outputFormat.format(d);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (dateStr != null ? dateStr : "") + " • " + (timeStr != null ? timeStr : "");
    }

    private void showEditDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_save_to_calendar);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = dialog.findViewById(R.id.tv_dialog_title);
        if (tvTitle != null) tvTitle.setText("Edit Schedule");

        android.widget.EditText etTitle = dialog.findViewById(R.id.et_event_title);
        android.widget.EditText etDate = dialog.findViewById(R.id.et_event_date);
        android.widget.EditText etTime = dialog.findViewById(R.id.et_event_time);
        android.widget.Spinner spinnerReminder = dialog.findViewById(R.id.spinner_reminder);
        android.widget.Button btnSave = dialog.findViewById(R.id.btn_save_calendar);
        android.widget.Button btnCancel = dialog.findViewById(R.id.btn_cancel_calendar);

        if (btnSave != null) btnSave.setText("Update Schedule");

        // Pre-fill data
        etTitle.setText(title);
        etDate.setText(date);
        etTime.setText(time);

        // Set spinner selection
        if (spinnerReminder != null) {
            android.widget.ArrayAdapter<CharSequence> reminderAdapter = (android.widget.ArrayAdapter<CharSequence>) spinnerReminder.getAdapter();
            if (reminderAdapter != null) {
                int spinnerPosition = reminderAdapter.getPosition(reminder);
                spinnerReminder.setSelection(spinnerPosition);
            }
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        etDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            try {
                Date d = dateFormat.parse(etDate.getText().toString());
                if (d != null) c.setTime(d);
            } catch (Exception ignored) {}

            new android.app.DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(year, month, dayOfMonth);
                etDate.setText(dateFormat.format(selected.getTime()));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        etTime.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new android.app.TimePickerDialog(this, (view, hourOfDay, minute) -> {
                String t = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                etTime.setText(t);
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String newTitle = etTitle.getText().toString().trim();
            String newDate = etDate.getText().toString().trim();
            String newTime = etTime.getText().toString().trim();
            String newReminder = spinnerReminder.getSelectedItem().toString();

            if (newTitle.isEmpty() || newDate.isEmpty() || newTime.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            updateEventInFirebase(newTitle, newDate, newTime, newReminder);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateEventInFirebase(String newTitle, String newDate, String newTime, String newReminder) {
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (uid == null) return;

        com.google.firebase.database.DatabaseReference ref = com.google.firebase.database.FirebaseDatabase.getInstance()
                .getReference("Users").child(uid).child("Events").child(eventId);

        ref.child("title").setValue(newTitle);
        ref.child("date").setValue(newDate);
        ref.child("time").setValue(newTime);
        ref.child("reminder").setValue(newReminder)
                .addOnSuccessListener(aVoid -> {
                    this.title = newTitle;
                    this.date = newDate;
                    this.time = newTime;
                    this.reminder = newReminder;
                    updateUI();
                    Toast.makeText(this, "Schedule Updated!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show());
    }

    private void loadEventItems() {
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (uid == null) return;

        com.google.firebase.database.DatabaseReference ref = com.google.firebase.database.FirebaseDatabase.getInstance()
                .getReference("Users").child(uid).child("Events").child(eventId).child("items");

        ref.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                itemsList.clear();
                if (snapshot.exists()) {
                    for (com.google.firebase.database.DataSnapshot itemSnap : snapshot.getChildren()) {
                        ClothingItem item = itemSnap.getValue(ClothingItem.class);
                        if (item != null) {
                            itemsList.add(item);
                        }
                    }
                } else {
                    // Fallback to mock data for demonstration if no items are saved
                    loadMockData();
                }
                adapter.notifyDataSetChanged();
                checkEmptyState();
            }

            @Override
            public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {
                Toast.makeText(OutfitDetailsActivity.this, "Failed to load items", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMockData() {
        itemsList.add(new ClothingItem("1", "https://i.pinimg.com/originals/e5/0b/4d/e50b4d00898864704b2b24e65696d5e6.jpg", "Tops", "S", "Summer", "Emerald Green", null, false, 0));
        itemsList.add(new ClothingItem("2", "https://i.pinimg.com/originals/82/34/00/82340068a184e9376662e08e8e6005c2.jpg", "Bottoms", "M", "All", "Classic Black", null, false, 0));
        itemsList.add(new ClothingItem("3", "https://i.pinimg.com/originals/16/e1/e4/16e1e48e898864704b2b24e65696d5e6.jpg", "Footwear", "7", "All", "Matte Black", null, false, 0));
        itemsList.add(new ClothingItem("4", "https://i.pinimg.com/originals/44/22/00/44220068a184e9376662e08e8e6005c2.jpg", "Bags", "N/A", "All", "Gold", null, false, 0));
    }

    private void checkEmptyState() {
        int count = itemsList.size();
        if (tvItemCount != null) {
            tvItemCount.setText(count + (count == 1 ? " Item" : " Items"));
        }

        if (itemsList.isEmpty()) {
            tvEmptyItems.setVisibility(android.view.View.VISIBLE);
            rvSelectedItems.setVisibility(android.view.View.GONE);
        } else {
            tvEmptyItems.setVisibility(android.view.View.GONE);
            rvSelectedItems.setVisibility(android.view.View.VISIBLE);
        }
    }

    private boolean isPastEvent() {
        if (date == null) return false;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date eventDate = sdf.parse(date);
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            return eventDate != null && eventDate.before(today.getTime());
        } catch (Exception e) {
            return false;
        }
    }

    private void archiveItemsAsUsed() {
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (uid == null || eventId == null) return;

        com.google.firebase.database.DatabaseReference eventItemsRef = com.google.firebase.database.FirebaseDatabase.getInstance()
                .getReference("Users").child(uid).child("Events").child(eventId).child("items");

        eventItemsRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                for (com.google.firebase.database.DataSnapshot itemSnap : snapshot.getChildren()) {
                    ClothingItem item = itemSnap.getValue(ClothingItem.class);
                    if (item != null && item.getCategoryId() != null && !item.getCategoryId().equals("used_clothes")) {
                        moveItemToUsed(uid, item, itemSnap.getRef());
                    }
                }
            }

            @Override
            public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {}
        });
    }

    private void moveItemToUsed(String uid, ClothingItem item, com.google.firebase.database.DatabaseReference eventItemRef) {
        com.google.firebase.database.DatabaseReference oldCatRef = com.google.firebase.database.FirebaseDatabase.getInstance()
                .getReference("Users").child(uid).child("categories").child(item.getCategoryId()).child("photos");

        com.google.firebase.database.DatabaseReference usedCatRef = com.google.firebase.database.FirebaseDatabase.getInstance()
                .getReference("Users").child(uid).child("categories").child("used_clothes").child("photos");

        oldCatRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                for (com.google.firebase.database.DataSnapshot photoSnap : snapshot.getChildren()) {
                    String url = photoSnap.child("imageUrl").getValue(String.class);
                    if (url == null) url = photoSnap.child("url").getValue(String.class);

                    if (url != null && url.equals(item.getImageUrl())) {
                        Object data = photoSnap.getValue();
                        
                        String itemKey = photoSnap.getKey();
                        if (itemKey == null) continue;

                        // Preserve original category when moving to used archive
                        if (data instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> dataMap = (Map<String, Object>) data;
                            dataMap.put("originalCategory", item.getCategoryId());
                            dataMap.put("movedToUsedAt", System.currentTimeMillis());
                            dataMap.put("categoryId", "used_clothes");
                        }

                        usedCatRef.child(itemKey).setValue(data).addOnSuccessListener(aVoid -> {
                            photoSnap.getRef().removeValue();
                            // Update item category in event to avoid repeated moves
                            eventItemRef.child("categoryId").setValue("used_clothes");
                        });
                        return;
                    }
                }
            }

            @Override
            public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {}
        });
    }

}

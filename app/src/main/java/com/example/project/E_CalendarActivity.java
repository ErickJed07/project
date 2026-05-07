package com.example.project;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class E_CalendarActivity extends AppCompatActivity {

    private GridLayout calendarGrid;
    private TextView selectedDayLabel;
    private TextView monthLabel, yearLabel;
    private Calendar calendar;
    private TextView selectedDayView = null;

    private RecyclerView eventRecyclerView;
    private E_Calendar_EventAdapter calendarEventAdapter;
    private List<E_Calendar_Event> calendarEventList = new ArrayList<>();

    private Map<String, List<E_Calendar_Event>> eventMap = new HashMap<>();
    private String selectedDateString = "";

    private DatabaseReference eventsRef;
    private String userId;

    private GestureDetector gestureDetector;
    private FloatingActionButton fabAddEvent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.e_calendar);

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(E_CalendarActivity.this, D_FeedActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            eventsRef = FirebaseDatabase.getInstance().getReference("Users").child(userId).child("Events");
        } else {
            finish();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        calendarGrid = findViewById(R.id.calendar_grid);
        monthLabel = findViewById(R.id.month_label);
        yearLabel = findViewById(R.id.year_label);
        selectedDayLabel = findViewById(R.id.selected_day_label);
        fabAddEvent = findViewById(R.id.fab_add_event);

        fabAddEvent.setOnClickListener(v -> {
            android.util.Log.d("CalendarActivity", "FAB clicked");
            showAddEventDialog();
        });

        eventRecyclerView = findViewById(R.id.eventRecyclerView);
        eventRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        calendarEventAdapter = new E_Calendar_EventAdapter(calendarEventList, (event, position) -> {
            if (event.getId() != null) {
                E_Calendar_ReminderUtils.cancelReminder(E_CalendarActivity.this, event);
                eventsRef.child(event.getId()).removeValue()
                        .addOnSuccessListener(aVoid ->
                                Toast.makeText(E_CalendarActivity.this, "Event Deleted", Toast.LENGTH_SHORT).show()
                        )
                        .addOnFailureListener(e ->
                                Toast.makeText(E_CalendarActivity.this, "Delete Failed", Toast.LENGTH_SHORT).show()
                        );
            }
        });
        eventRecyclerView.setAdapter(calendarEventAdapter);

        calendar = Calendar.getInstance();

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        selectedDateString = df.format(calendar.getTime());

        updateCalendar();

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1.getX() - e2.getX() > 50) {
                    calendar.add(Calendar.MONTH, 1);
                    updateCalendar();
                    return true;
                } else if (e2.getX() - e1.getX() > 50) {
                    calendar.add(Calendar.MONTH, -1);
                    updateCalendar();
                    return true;
                }
                return false;
            }
        });

        loadEventsFromFirebase();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    private String getFullDateString(Calendar selectedDate) {
        Calendar today = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMM d", Locale.getDefault());

        if (selectedDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                selectedDate.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                selectedDate.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)) {
            return dateFormat.format(selectedDate.getTime()) + ", Today";
        }

        Calendar todayDate = (Calendar) today.clone();
        todayDate.set(Calendar.HOUR_OF_DAY, 0);
        todayDate.set(Calendar.MINUTE, 0);
        todayDate.set(Calendar.SECOND, 0);
        todayDate.set(Calendar.MILLISECOND, 0);

        Calendar selectedDateOnly = (Calendar) selectedDate.clone();
        selectedDateOnly.set(Calendar.HOUR_OF_DAY, 0);
        selectedDateOnly.set(Calendar.MINUTE, 0);
        selectedDateOnly.set(Calendar.SECOND, 0);
        selectedDateOnly.set(Calendar.MILLISECOND, 0);

        long diffInMillis = selectedDateOnly.getTimeInMillis() - todayDate.getTimeInMillis();
        long diffDays = diffInMillis / (1000 * 60 * 60 * 24);

        if (diffDays == 1) {
            return dateFormat.format(selectedDate.getTime()) + ", Tomorrow";
        } else if (diffDays == -1) {
            return dateFormat.format(selectedDate.getTime()) + ", Yesterday";
        } else if (diffDays > 365) {
            return dateFormat.format(selectedDate.getTime()) + ", 365+ days later";
        } else if (diffDays < -365) {
            return dateFormat.format(selectedDate.getTime()) + ", 365+ days ago";
        } else if (diffDays > 1) {
            return dateFormat.format(selectedDate.getTime()) + ", " + diffDays + " days later";
        } else {
            return dateFormat.format(selectedDate.getTime()) + ", " + Math.abs(diffDays) + " days ago";
        }
    }

    private void updateCalendar() {
        calendarGrid.removeAllViews();

        SimpleDateFormat sdf = new SimpleDateFormat("MMMM", Locale.getDefault());
        monthLabel.setText(sdf.format(calendar.getTime()));

        SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
        yearLabel.setText(yearFormat.format(calendar.getTime()));

        Calendar todayCalendar = Calendar.getInstance();
        int todayDay = todayCalendar.get(Calendar.DAY_OF_MONTH);
        boolean isCurrentMonth = todayCalendar.get(Calendar.MONTH) == calendar.get(Calendar.MONTH)
                && todayCalendar.get(Calendar.YEAR) == calendar.get(Calendar.YEAR);

        Calendar tempCalendar = (Calendar) calendar.clone();
        tempCalendar.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = tempCalendar.get(Calendar.DAY_OF_WEEK) - 1;
        int daysInMonth = tempCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        int totalCells = 42;
        selectedDayView = null;

        for (int i = 0; i < totalCells; i++) {
            View dayCell = createDayView();
            TextView dayNumber = dayCell.findViewById(R.id.day_number);
            View eventBg = dayCell.findViewById(R.id.event_background);
            View selectionBg = dayCell.findViewById(R.id.selection_background);

            if (i >= firstDayOfWeek && i < firstDayOfWeek + daysInMonth) {
                int day = i - firstDayOfWeek + 1;
                dayNumber.setText(String.valueOf(day));

                Calendar tempDate = (Calendar) calendar.clone();
                tempDate.set(Calendar.DAY_OF_MONTH, day);
                String dateKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(tempDate.getTime());
                dayCell.setTag(dateKey);

                boolean hasEvents = eventMap.containsKey(dateKey) && !eventMap.get(dateKey).isEmpty();
                eventBg.setVisibility(hasEvents ? View.VISIBLE : View.GONE);

                if (dateKey.equals(selectedDateString) || (isCurrentMonth && day == todayDay && selectedDayView == null)) {
                    selectionBg.setVisibility(View.VISIBLE);
                    selectedDayView = dayNumber;
                    selectedDateString = dateKey;
                    if (selectedDayLabel != null) {
                        selectedDayLabel.setText(getFullDateString(tempDate));
                    }
                    dayNumber.setTextColor(Color.WHITE);
                } else {
                    selectionBg.setVisibility(View.GONE);
                    dayNumber.setTextColor(Color.BLACK);
                }

                dayCell.setOnClickListener(v -> {
                    if (selectedDayView != null) {
                        selectedDayView.setTextColor(Color.BLACK);
                        try {
                            // Find the FrameLayout (root of e2_calendar_day)
                            View parent = (View) selectedDayView.getParent();
                            if (parent != null) {
                                View grandParent = (View) parent.getParent();
                                if (grandParent != null) {
                                    View oldSelectionBg = grandParent.findViewById(R.id.selection_background);
                                    if (oldSelectionBg != null) oldSelectionBg.setVisibility(View.GONE);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    selectionBg.setVisibility(View.VISIBLE);
                    selectedDayView = dayNumber;
                    dayNumber.setTextColor(Color.WHITE);
                    selectedDateString = dateKey;
                    loadEventsForSelectedDate();
                    if (selectedDayLabel != null) {
                        selectedDayLabel.setText(getFullDateString(tempDate));
                    }
                });

            } else {
                dayNumber.setText(getAdjacentDay(i, firstDayOfWeek, daysInMonth));
                dayNumber.setTextColor(getResources().getColor(R.color.gray));
                selectionBg.setVisibility(View.GONE);
            }
            calendarGrid.addView(dayCell);
        }
    }

    private View createDayView() {
        View view = getLayoutInflater().inflate(R.layout.e2_calendar_day, calendarGrid, false);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = 0;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(2, 2, 2, 2);
        view.setLayoutParams(params);

        GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (Math.abs(velocityX) > Math.abs(velocityY)) {
                    if (e1.getX() - e2.getX() > 50) {
                        moveToNextMonth();
                    } else if (e2.getX() - e1.getX() > 50) {
                        moveToPreviousMonth();
                    }
                }
                return true;
            }
        });
        view.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
        return view;
    }

    private void moveToNextMonth() {
        calendar.add(Calendar.MONTH, 1);
        updateCalendar();
    }

    private void moveToPreviousMonth() {
        calendar.add(Calendar.MONTH, -1);
        updateCalendar();
    }

    private void loadEventsFromFirebase() {
        if (eventsRef == null) return;

        eventsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                eventMap.clear();

                for (DataSnapshot eventSnapshot : snapshot.getChildren()) {
                    E_Calendar_Event event = eventSnapshot.getValue(E_Calendar_Event.class);

                    if (event != null) {
                        event.setId(eventSnapshot.getKey());
                        String date = event.getDate();
                        if (date != null && !date.isEmpty()) {
                            if (!eventMap.containsKey(date)) {
                                eventMap.put(date, new ArrayList<>());
                            }
                            eventMap.get(date).add(event);
                        }
                        E_Calendar_ReminderUtils.scheduleReminder(E_CalendarActivity.this, event);
                    }
                }

                loadEventsForSelectedDate();
                updateCalendar();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void loadEventsForSelectedDate() {
        List<E_Calendar_Event> selectedCalendarEvents =
                eventMap.getOrDefault(selectedDateString, new ArrayList<>());
        calendarEventList.clear();
        calendarEventList.addAll(selectedCalendarEvents);
        calendarEventAdapter.notifyDataSetChanged();
    }

    private void showAddEventDialog() {
        android.util.Log.d("CalendarActivity", "showAddEventDialog called");
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_add_event);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        EditText etTitle = dialog.findViewById(R.id.et_event_title);
        EditText etDate = dialog.findViewById(R.id.et_event_date);
        EditText etTime = dialog.findViewById(R.id.et_event_time);
        Spinner spinnerReminder = dialog.findViewById(R.id.spinner_reminder);
        Button btnSave = dialog.findViewById(R.id.btn_save_event);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel_event);

        etDate.setText(selectedDateString);

        etDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                c.setTime(sdf.parse(etDate.getText().toString()));
            } catch (Exception ignored) {}

            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(year, month, dayOfMonth);
                etDate.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selected.getTime()));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        etTime.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                String time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                etTime.setText(time);
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String date = etDate.getText().toString().trim();
            String time = etTime.getText().toString().trim();
            String reminder = spinnerReminder.getSelectedItem().toString();

            if (title.isEmpty() || date.isEmpty() || time.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            saveEventToFirebase(title, date, time, reminder);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void saveEventToFirebase(String title, String date, String time, String reminder) {
        if (eventsRef == null) return;

        String id = eventsRef.push().getKey();
        if (id == null) return;

        E_Calendar_Event event = new E_Calendar_Event(id, title, date, time, "", reminder, System.currentTimeMillis());
        eventsRef.child(id).setValue(event)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Event Saved", Toast.LENGTH_SHORT).show();
                    E_Calendar_ReminderUtils.scheduleReminder(this, event);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save", Toast.LENGTH_SHORT).show());
    }

    public void onButtonClicked(View view) {
        Intent intent = null;
        int viewId = view.getId();

        if (viewId == R.id.home_menu) {
            intent = new Intent(this, D_FeedActivity.class);
        } else if (viewId == R.id.calendar_menu) {
            return;
        } else if (viewId == R.id.camera_menu) {
            intent = new Intent(this, F1_CameraActivity.class);
        } else if (viewId == R.id.closet_menu) {
            intent = new Intent(this, G1_ClosetActivity.class);
        } else if (viewId == R.id.profile_menu) {
            intent = new Intent(this, I_ProfileActivity.class);
        } else if (viewId == R.id.wardrobe_menu) {
            intent = new Intent(this, WardrobeActivity.class);
        } else if (viewId == R.id.ai_menu) {
            intent = new Intent(this, AiActivity.class);
        }

        if (intent != null) {
            startActivity(intent);
            finish();
        }
    }

    private String getAdjacentDay(int cellIndex, int firstDayOfWeek, int daysInMonth) {
        Calendar temp = (Calendar) calendar.clone();

        if (cellIndex < firstDayOfWeek) {
            temp.add(Calendar.MONTH, -1);
            int prevMonthDays = temp.getActualMaximum(Calendar.DAY_OF_MONTH);
            int day = prevMonthDays - (firstDayOfWeek - cellIndex) + 1;
            return String.valueOf(day);
        } else {
            int day = (cellIndex - firstDayOfWeek - daysInMonth) + 1;
            return String.valueOf(day);
        }
    }
}

package com.example.project;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
// Removed Firestore imports as we are using Realtime Database
// import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class E1_CalendarActivity extends AppCompatActivity {

    private GridLayout calendarGrid;
    private TextView selectedDayLabel;
    private TextView monthLabel, yearLabel;
    private Calendar calendar;
    private TextView selectedDayView = null;

    private RecyclerView eventRecyclerView;
    private E4_Calendar_EventAdapter calendarEventAdapter;
    private List<E2_Calendar_Event> calendarEventList = new ArrayList<>();

    private Map<String, List<E2_Calendar_Event>> eventMap = new HashMap<>();
    private String selectedDateString = "";

    private DatabaseReference eventsRef;
    private String userId;

    private GestureDetector gestureDetector;
    // private ConstraintLayout constraintLayout6; // Unused based on snippet

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.e1_calendar);

        // Firebase setup
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            // Path matches H1 save logic: CalendarEvents -> userId
            eventsRef = FirebaseDatabase.getInstance().getReference("CalendarEvents").child(userId);
        } else {
            // Handle not logged in
            finish();
            return;
        }

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        calendarGrid = findViewById(R.id.calendar_grid);
        monthLabel = findViewById(R.id.month_label);
        yearLabel = findViewById(R.id.year_label);
        selectedDayLabel = findViewById(R.id.selected_day_label);

        // Setup RecyclerView
        eventRecyclerView = findViewById(R.id.eventRecyclerView);
        eventRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Initialize Adapter with Click/Delete Listener
        calendarEventAdapter = new E4_Calendar_EventAdapter(calendarEventList, (event, position) -> {
            if (event.getId() != null) {
                // 1. Cancel local notification
                E3_Calendar_ReminderUtils.cancelReminder(E1_CalendarActivity.this, event);

                // 2. Remove from Realtime Database
                // Note: H1 saves directly under userId, NOT under a date node.
                eventsRef.child(event.getId()).removeValue()
                        .addOnSuccessListener(aVoid ->
                                Toast.makeText(E1_CalendarActivity.this, "Event Deleted", Toast.LENGTH_SHORT).show()
                        )
                        .addOnFailureListener(e ->
                                Toast.makeText(E1_CalendarActivity.this, "Delete Failed", Toast.LENGTH_SHORT).show()
                        );
            }
        });
        eventRecyclerView.setAdapter(calendarEventAdapter);

        calendar = Calendar.getInstance();

        // Default selected date = today
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        selectedDateString = df.format(calendar.getTime());

        updateCalendar();

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1.getX() - e2.getX() > 50) {
                    // Swipe left: next month
                    calendar.add(Calendar.MONTH, 1);
                    updateCalendar();
                    return true;
                } else if (e2.getX() - e1.getX() > 50) {
                    // Swipe right: previous month
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
        int today = todayCalendar.get(Calendar.DAY_OF_MONTH);
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

                if (eventMap.containsKey(dateKey) && !eventMap.get(dateKey).isEmpty()) {
                    eventBg.setVisibility(View.VISIBLE);
                    dayNumber.setTextColor(getResources().getColor(android.R.color.white));
                } else {
                    eventBg.setVisibility(View.GONE);
                    dayNumber.setTextColor(getResources().getColor(android.R.color.black));
                }

                if (dateKey.equals(selectedDateString) || (isCurrentMonth && day == today && selectedDayView == null)) {
                    selectionBg.setVisibility(View.VISIBLE);
                    selectedDayView = dayNumber;
                    selectedDateString = dateKey;
                    selectedDayLabel.setText(getFullDateString(tempDate));
                } else {
                    selectionBg.setVisibility(View.GONE);
                }

                dayCell.setOnClickListener(v -> {
                    if (selectedDayView != null) {
                        View oldSelectionBg = ((View) selectedDayView.getParent())
                                .findViewById(R.id.selection_background);
                        oldSelectionBg.setVisibility(View.GONE);
                    }
                    selectionBg.setVisibility(View.VISIBLE);
                    selectedDayView = dayNumber;
                    selectedDateString = dateKey;
                    loadEventsForSelectedDate();
                    selectedDayLabel.setText(getFullDateString(tempDate));
                });

            } else {
                dayNumber.setText(getAdjacentDay(i, firstDayOfWeek, daysInMonth));
                dayNumber.setTextColor(getResources().getColor(R.color.gray));
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
                    // --------------------------------------------------------
                    // Cloudinary Image Logic:
                    // The Cloudinary URL is stored in the "imagePath" field of
                    // the Firebase object (saved in H1 Activity).
                    // E2_Calendar_Event automatically deserializes this URL
                    // into its imagePath field.
                    // --------------------------------------------------------
                    E2_Calendar_Event event = eventSnapshot.getValue(E2_Calendar_Event.class);

                    if (event != null) {
                        event.setId(eventSnapshot.getKey());
                        String date = event.getDate();
                        if (date != null && !date.isEmpty()) {
                            if (!eventMap.containsKey(date)) {
                                eventMap.put(date, new ArrayList<>());
                            }
                            eventMap.get(date).add(event);
                        }
                        E3_Calendar_ReminderUtils.scheduleReminder(E1_CalendarActivity.this, event);
                    }
                }

                loadEventsForSelectedDate();
                updateCalendar();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void loadEventsForSelectedDate() {
        List<E2_Calendar_Event> selectedCalendarEvents =
                eventMap.getOrDefault(selectedDateString, new ArrayList<>());
        calendarEventList.clear();
        calendarEventList.addAll(selectedCalendarEvents);
        // The Adapter (E4) will take these events and use Glide to load the Cloudinary URL
        calendarEventAdapter.notifyDataSetChanged();
    }

    public void onButtonClicked(View view) {
        Intent intent = null;
        int viewId = view.getId();

        if (viewId == R.id.home_menu) {
            intent = new Intent(this, D1_FeedActivity.class);
        } else if (viewId == R.id.calendar_menu) {
            return;
        } else if (viewId == R.id.camera_menu) {
            intent = new Intent(this, F1_CameraActivity.class);
        } else if (viewId == R.id.closet_menu) {
            intent = new Intent(this, G1_ClosetActivity.class);
        } else if (viewId == R.id.profile_menu) {
            intent = new Intent(this, I1_ProfileActivity.class);
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

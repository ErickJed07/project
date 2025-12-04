package com.example.project;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide; // IMPORT GLIDE
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class E4_Calendar_EventAdapter extends RecyclerView.Adapter<E4_Calendar_EventAdapter.EventViewHolder> {

    private final List<E2_Calendar_Event> calendarEventList;
    private final OnEventDeleteListener deleteListener;

    public interface OnEventDeleteListener {
        void onEventDeleted(E2_Calendar_Event event, int position);
    }

    public E4_Calendar_EventAdapter(List<E2_Calendar_Event> calendarEventList, OnEventDeleteListener deleteListener) {
        this.calendarEventList = calendarEventList;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.e3_calendar_item_event, parent, false);
        return new EventViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        E2_Calendar_Event event = calendarEventList.get(position);

        holder.eventTitle.setText(event.getTitle());

        // Update: Pass the Cloudinary URL
        setImage(holder.eventImage, event.getImageUrl(), R.drawable.image1);

        // --- NEW: CHECK FOR AUTO-DELETE (7 Minutes) ---
        checkAndAutoDelete(event, holder.getAdapterPosition(), holder.itemView.getContext());
        // ----------------------------------------------

        View.OnClickListener showDialogListener = v -> showEventDialog(v.getContext(), holder, event);
        holder.eventMore.setOnClickListener(showDialogListener);
        holder.itemView.setOnClickListener(showDialogListener);
    }

    // -----------------------------------------------------------------------
    // HELPER: Check Time and Delete if > 7 minutes passed
    // -----------------------------------------------------------------------
      // -----------------------------------------------------------------------
    // HELPER: Check Time and Delete if > 7 DAYS passed
    // -----------------------------------------------------------------------
    private void checkAndAutoDelete(E2_Calendar_Event event, int position, Context context) {
        try {
            // 1. Get Date and Time strings
            // NOTE: You need to combine your Date and Time to check for "7 Days"
            // If your event object separates them, combine them like:
            // String fullDateTime = event.getDate() + " " + event.getTime();

            // Assuming event.getTime() holds the full date/time OR you have a getDate() method:
            // Change "dd/MM/yyyy HH:mm" to match EXACTLY how you save data in Firebase
            String dateString = event.getDate() + " " + event.getTime();

            if (dateString == null || dateString.trim().isEmpty()) return;

            // 2. Parse the Full Date
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
            java.util.Date eventDate = sdf.parse(dateString);

            java.util.Calendar now = java.util.Calendar.getInstance();

            // 3. Calculate Difference
            long diffInMillis = now.getTimeInMillis() - eventDate.getTime();
            long diffInDays = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diffInMillis);

            // Debug Log
            android.util.Log.d("AutoDelete", "Event: " + event.getTitle() + " | Days Old: " + diffInDays);

            // 4. Check if older than 7 Days
            if (diffInDays >= 7) {
                // Execute deletion on the main UI thread
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    performDeleteEvent(event, position, context, null);
                });
            }

        } catch (Exception e) {
            // If parsing fails (e.g. date format is wrong), just print error and don't delete
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return calendarEventList.size();
    }

    // -----------------------------------------------------------------------
    // UPDATE: Switched from BitmapFactory (Local File) to Glide (Remote URL)
    // -----------------------------------------------------------------------
    private void setImage(ImageView imageView, String path, int placeholderRes) {
        if (path != null && !path.isEmpty()) {
            try {
                Glide.with(imageView.getContext())
                        .load(path) // This now handles http/https URLs from Cloudinary
                        .placeholder(placeholderRes)
                        .error(placeholderRes) // Fallback if URL is broken
                        .into(imageView);
            } catch (Exception e) {
                imageView.setImageResource(placeholderRes);
            }
        } else {
            imageView.setImageResource(placeholderRes);
        }
    }

    private void showEventDialog(Context context, EventViewHolder holder, E2_Calendar_Event event) {
        Dialog dialog = new Dialog(context);

        View dialogView = LayoutInflater.from(context)
                .inflate(R.layout.e4_calendar_popup_event_info, null);
        dialog.setContentView(dialogView);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        ImageView popupImage = dialogView.findViewById(R.id.popupImage);
        TextView popupTitle = dialogView.findViewById(R.id.popupTitle);
        TextView popupTime = dialogView.findViewById(R.id.popupTime);
        TextView popupReminder = dialogView.findViewById(R.id.popupReminder);
        Button closeButton = dialogView.findViewById(R.id.closeButton);
        Button deleteButton = dialogView.findViewById(R.id.deleteButton);

        // ... inside showEventDialog ...

        popupTitle.setText("Name: " + event.getTitle());
        popupTime.setText("Time: " + event.getTime());
        popupReminder.setText("Reminder: " + event.getReminder());

        // Update: Uses Glide via helper method
        setImage(popupImage, event.getImageUrl(), R.drawable.image3);

        // ... (text setup code) ...

        closeButton.setOnClickListener(v -> dialog.dismiss());

        // KEEP THIS ONE (It handles Firebase + Listener + Local List + Dialog)
        deleteButton.setOnClickListener(v -> {
            performDeleteEvent(event, holder.getAdapterPosition(), context, dialog);
        });




        deleteButton.setOnClickListener(v -> {
            E2_Calendar_Event eventToDelete = event;
            if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            String eventId = eventToDelete.getId();

            FirebaseDatabase.getInstance()
                    .getReference("Users")          // <--- CORRECT NEW PATH
                    .child(userId)
                    .child("Events")                // <--- Don't forget the "Events" node
                    .child(eventId)
                    .removeValue()

                    .addOnSuccessListener(unused -> {
                        // Notify the Activity listener (optional cleanup)
                        if (deleteListener != null) {
                            deleteListener.onEventDeleted(eventToDelete, holder.getAdapterPosition());
                        }

                        // Remove locally from list to update UI immediately
                        int index = -1;
                        for (int i = 0; i < calendarEventList.size(); i++) {
                            if (calendarEventList.get(i).getId().equals(eventId)) {
                                index = i;
                                break;
                            }
                        }

                        if (index != -1) {
                            calendarEventList.remove(index);
                            notifyItemRemoved(index);
                            notifyItemRangeChanged(index, calendarEventList.size());
                        }

                        dialog.dismiss();
                        Toast.makeText(holder.itemView.getContext(),
                                "Deleted: " + eventToDelete.getTitle(),
                                Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(holder.itemView.getContext(),
                                    "Delete failed: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show()
                    );
        });

        dialog.show();
    }
    // -----------------------------------------------------------------------
    // HELPER: Centralized Delete Logic (Firebase + Local List)
    // -----------------------------------------------------------------------
    private void performDeleteEvent(E2_Calendar_Event event, int position, Context context, Dialog dialogToClose) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String eventId = event.getId();

        FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(userId)
                .child("Events")
                .child(eventId)
                .removeValue()
                .addOnSuccessListener(unused -> {
                    // 1. Notify Listener
                    if (deleteListener != null) {
                        deleteListener.onEventDeleted(event, position);
                    }

                    // 2. Remove from Local List
                    int index = -1;
                    for (int i = 0; i < calendarEventList.size(); i++) {
                        if (calendarEventList.get(i).getId().equals(eventId)) {
                            index = i;
                            break;
                        }
                    }

                    if (index != -1) {
                        calendarEventList.remove(index);
                        notifyItemRemoved(index);
                        notifyItemRangeChanged(index, calendarEventList.size());
                    }

                    // 3. Close Dialog if open
                    if (dialogToClose != null && dialogToClose.isShowing()) {
                        dialogToClose.dismiss();
                    }

                    Toast.makeText(context, "Deleted: " + event.getTitle(), Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Delete failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    public static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView eventTitle, eventMore;
        ImageView eventImage;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            eventTitle = itemView.findViewById(R.id.eventTitle);
            eventMore = itemView.findViewById(R.id.eventMore);
            eventImage = itemView.findViewById(R.id.eventImage);
        }
    }
}

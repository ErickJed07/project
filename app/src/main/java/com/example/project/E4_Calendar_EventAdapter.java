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
        setImage(holder.eventImage, event.getImagePath(), R.drawable.image1);

        View.OnClickListener showDialogListener = v -> showEventDialog(v.getContext(), holder, event);
        holder.eventMore.setOnClickListener(showDialogListener);
        holder.itemView.setOnClickListener(showDialogListener);
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

        popupTitle.setText(event.getTitle());
        popupTime.setText("Time: " + event.getTime());
        popupReminder.setText(event.getReminder());

        // Update: Uses Glide via helper method
        setImage(popupImage, event.getImagePath(), R.drawable.image3);

        closeButton.setOnClickListener(v -> dialog.dismiss());

        deleteButton.setOnClickListener(v -> {
            E2_Calendar_Event eventToDelete = event;
            if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            String eventId = eventToDelete.getId();

            // Delete from Firebase Realtime Database
            FirebaseDatabase.getInstance()
                    .getReference("CalendarEvents")
                    .child(userId)
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

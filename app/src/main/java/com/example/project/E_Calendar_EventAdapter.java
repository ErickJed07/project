package com.example.project;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;

public class E_Calendar_EventAdapter extends RecyclerView.Adapter<E_Calendar_EventAdapter.EventViewHolder> {

    private final List<E_Calendar_Event> calendarEventList;
    private final OnEventDeleteListener deleteListener;

    public interface OnEventDeleteListener {
        void onEventDeleted(E_Calendar_Event event, int position);
    }

    public E_Calendar_EventAdapter(List<E_Calendar_Event> calendarEventList, OnEventDeleteListener deleteListener) {
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
        E_Calendar_Event event = calendarEventList.get(position);
        holder.eventTitle.setText(event.getTitle());
        holder.eventTime.setText(event.getTime());
        
        String reminder = event.getReminder();
        if (reminder != null && !reminder.equals("None")) {
            holder.reminderLayout.setVisibility(View.VISIBLE);
            holder.eventReminder.setText(reminder);
        } else {
            holder.reminderLayout.setVisibility(View.GONE);
        }

        setImage(holder.eventImage, event.getImageUrl(), R.drawable.dress);
        checkAndAutoDelete(event, holder.getAdapterPosition(), holder.itemView.getContext());

        View.OnClickListener showDetailsListener = v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, OutfitDetailsActivity.class);
            intent.putExtra(OutfitDetailsActivity.EXTRA_EVENT_ID, event.getId());
            intent.putExtra(OutfitDetailsActivity.EXTRA_EVENT_TITLE, event.getTitle());
            intent.putExtra(OutfitDetailsActivity.EXTRA_EVENT_DATE, event.getDate());
            intent.putExtra(OutfitDetailsActivity.EXTRA_EVENT_TIME, event.getTime());
            intent.putExtra(OutfitDetailsActivity.EXTRA_EVENT_REMINDER, event.getReminder());
            intent.putExtra(OutfitDetailsActivity.EXTRA_EVENT_IMAGE_URL, event.getImageUrl());
            context.startActivity(intent);
        };
        holder.itemView.setOnClickListener(showDetailsListener);
    }

    private void checkAndAutoDelete(E_Calendar_Event event, int position, Context context) {
        try {
            String dateString = event.getDate() + " " + event.getTime();
            if (dateString == null || dateString.trim().isEmpty()) return;

            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
            java.util.Date eventDate = sdf.parse(dateString);
            java.util.Calendar now = java.util.Calendar.getInstance();

            long diffInMillis = now.getTimeInMillis() - eventDate.getTime();
            long diffInDays = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diffInMillis);

            if (diffInDays >= 7) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    performDeleteEvent(event, position, context, null);
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return calendarEventList.size();
    }

    private void setImage(ImageView imageView, String path, int placeholderRes) {
        if (path != null && !path.isEmpty()) {
            try {
                // Remove any tint for actual photos
                imageView.setImageTintList(null);
                Glide.with(imageView.getContext())
                        .load(path)
                        .placeholder(placeholderRes)
                        .error(placeholderRes)
                        .centerCrop()
                        .into(imageView);
            } catch (Exception e) {
                imageView.setImageResource(placeholderRes);
            }
        } else {
            // Apply tint for placeholder icon
            imageView.setImageResource(placeholderRes);
            Context context = imageView.getContext();
            int color = androidx.core.content.ContextCompat.getColor(context, R.color.ai_accent);
            imageView.setImageTintList(android.content.res.ColorStateList.valueOf(color));
        }
    }

    public void performDeleteEvent(E_Calendar_Event event, int position, Context context, Dialog dialogToClose) {
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
                    if (deleteListener != null) {
                        deleteListener.onEventDeleted(event, position);
                    }
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
        TextView eventTitle, eventTime, eventReminder;
        ImageView eventImage;
        View reminderLayout;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            eventTitle = itemView.findViewById(R.id.eventTitle);
            eventTime = itemView.findViewById(R.id.eventTime);
            eventReminder = itemView.findViewById(R.id.eventReminder);
            reminderLayout = itemView.findViewById(R.id.reminderLayout);
            eventImage = itemView.findViewById(R.id.eventImage);
        }
    }
}

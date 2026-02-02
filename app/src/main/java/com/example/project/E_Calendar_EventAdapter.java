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

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

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
        setImage(holder.eventImage, event.getImageUrl(), R.drawable.image1);
        checkAndAutoDelete(event, holder.getAdapterPosition(), holder.itemView.getContext());

        View.OnClickListener showDialogListener = v -> showEventDialog(v.getContext(), holder, event);
        holder.eventMore.setOnClickListener(showDialogListener);
        holder.itemView.setOnClickListener(showDialogListener);
    }

    private void checkAndAutoDelete(E_Calendar_Event event, int position, Context context) {
        try {
            String dateString = event.getDate() + " " + event.getTime();
            if (dateString == null || dateString.trim().isEmpty()) return;

            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
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
                Glide.with(imageView.getContext())
                        .load(path)
                        .placeholder(placeholderRes)
                        .error(placeholderRes)
                        .into(imageView);
            } catch (Exception e) {
                imageView.setImageResource(placeholderRes);
            }
        } else {
            imageView.setImageResource(placeholderRes);
        }
    }

    private void showEventDialog(Context context, EventViewHolder holder, E_Calendar_Event event) {
        Dialog dialog = new Dialog(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.e4_calendar_popup_event_info, null);
        dialog.setContentView(dialogView);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        ImageView popupImage = dialogView.findViewById(R.id.popupImage);
        TextView popupTitle = dialogView.findViewById(R.id.popupTitle);
        TextView popupTime = dialogView.findViewById(R.id.popupTime);
        TextView popupReminder = dialogView.findViewById(R.id.popupReminder);
        Button closeButton = dialogView.findViewById(R.id.closeButton);
        Button deleteButton = dialogView.findViewById(R.id.deleteButton);

        popupTitle.setText("Name: " + event.getTitle());
        popupTime.setText("Time: " + event.getTime());
        popupReminder.setText("Reminder: " + event.getReminder());
        setImage(popupImage, event.getImageUrl(), R.drawable.image3);

        closeButton.setOnClickListener(v -> dialog.dismiss());
        deleteButton.setOnClickListener(v -> performDeleteEvent(event, holder.getAdapterPosition(), context, dialog));

        dialog.show();
    }

    private void performDeleteEvent(E_Calendar_Event event, int position, Context context, Dialog dialogToClose) {
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

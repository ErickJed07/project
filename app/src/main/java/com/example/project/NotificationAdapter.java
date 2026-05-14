package com.example.project;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private Context context;
    private List<Notification> notifications;

    public NotificationAdapter(Context context, List<Notification> notifications) {
        this.context = context;
        this.notifications = notifications;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        holder.title.setText(notification.getTitle());
        holder.message.setText(notification.getMessage());
        holder.time.setText(getRelativeTime(notification.getTimestamp()));
        holder.unreadDot.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);
    }

    @Override
    public int getItemCount() { return notifications.size(); }

    private String getRelativeTime(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) return "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            Date date = sdf.parse(timestamp);
            if (date == null) return "";
            long diff = (System.currentTimeMillis() - date.getTime()) / 1000;
            if (diff < 60) return "Just now";
            if (diff < 3600) return (diff / 60) + "m ago";
            if (diff < 86400) return (diff / 3600) + "h ago";
            return new SimpleDateFormat("MMM d", Locale.US).format(date);
        } catch (Exception e) { return ""; }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, message, time;
        View unreadDot;
        ImageView icon;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tv_notif_title);
            message = itemView.findViewById(R.id.tv_notif_message);
            time = itemView.findViewById(R.id.tv_notif_time);
            unreadDot = itemView.findViewById(R.id.v_unread_dot);
            icon = itemView.findViewById(R.id.iv_notif_icon);
        }
    }
}

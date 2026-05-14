package com.example.project;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationHelper {

    public static void sendNotification(String targetUserId, String title, String message, String fromUserId) {
        if (targetUserId == null || targetUserId.isEmpty()) return;
        
        DatabaseReference notifRef = FirebaseDatabase.getInstance().getReference("Notifications").child(targetUserId);
        String notifId = notifRef.push().getKey();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(new Date());

        Notification notification = new Notification(notifId, title, message, timestamp, fromUserId);
        if (notifId != null) {
            notifRef.child(notifId).setValue(notification);
        }
    }
}

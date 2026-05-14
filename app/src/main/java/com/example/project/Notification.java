package com.example.project;

public class Notification {
    private String notificationId;
    private String title;
    private String message;
    private String timestamp;
    private String fromUserId;
    private boolean read;

    public Notification() {}

    public Notification(String notificationId, String title, String message, String timestamp, String fromUserId) {
        this.notificationId = notificationId;
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
        this.fromUserId = fromUserId;
        this.read = false;
    }

    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public String getFromUserId() { return fromUserId; }
    public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}

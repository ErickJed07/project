package com.example.project;

public class E2_Calendar_Event {

    private String id;
    private String title;
    private String date;      // Matches Firebase key "date"
    private String time;      // Matches Firebase key "time"
    private String reminder;  // Matches Firebase key "reminder"
    private String imageUrl;  // <--- CRITICAL CHANGE: Matches Firebase key "imageUrl"
    private long timestamp;

    // Required empty constructor for Firebase
    public E2_Calendar_Event() { }

    public E2_Calendar_Event(String id, String title, String date, String time, String imageUrl, String reminder, long timestamp) {
        this.id = id;
        this.title = title;
        this.date = date;
        this.time = time;
        this.imageUrl = imageUrl;
        this.reminder = reminder;
        this.timestamp = timestamp;
    }

    // --- GETTERS AND SETTERS ---
    // Firebase looks for these specifically. They must match the field names.

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    // IMPORTANT: The getter/setter names must match "ImageUrl"
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getReminder() { return reminder; }
    public void setReminder(String reminder) { this.reminder = reminder; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}

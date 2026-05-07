package com.example.project;

import java.util.ArrayList;
import java.util.List;

public class E_Calendar_Event {

    private String id;
    private String title;
    private String date;      // Matches Firebase key "date"
    private String time;      // Matches Firebase key "time"
    private String reminder;  // Matches Firebase key "reminder"
    private String imageUrl;  // <--- CRITICAL CHANGE: Matches Firebase key "imageUrl"
    private long timestamp;
    private List<ClothingItem> items; // Items associated with this outfit

    // Required empty constructor for Firebase
    public E_Calendar_Event() { }

    public E_Calendar_Event(String id, String title, String date, String time, String imageUrl, String reminder, long timestamp) {
        this.id = id;
        this.title = title;
        this.date = date;
        this.time = time;
        this.imageUrl = imageUrl;
        this.reminder = reminder;
        this.timestamp = timestamp;
        this.items = new ArrayList<>();
    }

    public E_Calendar_Event(String id, String title, String date, String time, String imageUrl, String reminder, long timestamp, List<ClothingItem> items) {
        this.id = id;
        this.title = title;
        this.date = date;
        this.time = time;
        this.imageUrl = imageUrl;
        this.reminder = reminder;
        this.timestamp = timestamp;
        this.items = items;
    }

    // --- GETTERS AND SETTERS ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getReminder() { return reminder; }
    public void setReminder(String reminder) { this.reminder = reminder; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public List<ClothingItem> getItems() { return items; }
    public void setItems(List<ClothingItem> items) { this.items = items; }
}

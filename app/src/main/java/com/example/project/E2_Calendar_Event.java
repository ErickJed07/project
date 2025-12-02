package com.example.project;

public class E2_Calendar_Event {
    private String id;
    private String title;
    private String date;      // Format: "yyyy-MM-dd"
    private String time;      // Format: "HH:mm"
    private String imagePath; // This holds the Cloudinary URL
    private String reminder;

    public E2_Calendar_Event() { } // Required empty constructor for Firebase

    public E2_Calendar_Event(String id, String title, String date, String time, String imagePath, String reminder) {
        this.id = id;
        this.title = title;
        this.date = date;
        this.time = time;
        this.imagePath = imagePath;
        this.reminder = reminder;
    }

    // Getters and Setters matching the fields above...
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public String getDate() { return date; }
    public String getTime() { return time; }
    public String getImagePath() { return imagePath; } // Important for Cloudinary loading
    public String getReminder() { return reminder; }
}

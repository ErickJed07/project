package com.example.project;

public class E2_Calendar_Event {

    private String id;
    private String title;
    private String date;
    private String time;
    private String imagePath;
    private String reminder;

    // ✅ No-argument constructor required by Firebase
    public E2_Calendar_Event() {
    }

    // Optional: full constructor
    public E2_Calendar_Event(String id, String title, String date, String time,
                             String imagePath, String reminder) {
        this.id = id;
        this.title = title;
        this.date = date;
        this.time = time;
        this.imagePath = imagePath;
        this.reminder = reminder;
    }

    // ✅ Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDate() { return date; }
    public String getTime() { return time; }
    public String getImagePath() { return imagePath; }
    public String getReminder() { return reminder; }

    // ✅ Setters (needed for Firebase)
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDate(String date) { this.date = date; }
    public void setTime(String time) { this.time = time; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public void setReminder(String reminder) { this.reminder = reminder; }
}

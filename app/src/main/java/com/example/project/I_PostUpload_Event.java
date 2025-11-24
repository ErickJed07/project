package com.example.project;

import java.util.List;

public class I_PostUpload_Event {
    private String username;
    private String caption;
    private List<String> imageUrls;
    private String postId;
    private String date;
    private String userId;  // Added userId to the post data

    // Default constructor for Firebase to work
    public I_PostUpload_Event() {
        // Empty constructor
    }

    // Constructor with all fields
    public I_PostUpload_Event(String username, String caption, List<String> imageUrls, String postId, String date, String userId) {
        this.username = username;
        this.caption = caption;
        this.imageUrls = imageUrls;
        this.postId = postId;
        this.date = date;
        this.userId = userId;
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}

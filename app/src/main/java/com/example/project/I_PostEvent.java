package com.example.project;

import java.util.List;
import java.util.Map;

public class I_PostEvent {
    private String postId;
    private String username;
    private String caption;
    private List<String> imageUrls;
    private String date;
    private String userId;
    private int heartCount;
    private Map<String, Boolean> heartLiked;
    private int favCount;
    private Map<String, Boolean> favList;

    public I_PostEvent() {}

    public I_PostEvent(String username, String caption, List<String> imageUrls, String postId, String date, String userId, int heartCount, Map<String, Boolean> heartLiked) {
        this.username = username;
        this.caption = caption;
        this.imageUrls = imageUrls;
        this.postId = postId;
        this.date = date;
        this.userId = userId;
        this.heartCount = heartCount;
        this.heartLiked = heartLiked;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }
    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public int getHeartCount() { return heartCount; }
    public void setHeartCount(int heartCount) { this.heartCount = heartCount; }
    public Map<String, Boolean> getHeartLiked() { return heartLiked; }
    public void setHeartLiked(Map<String, Boolean> heartLiked) { this.heartLiked = heartLiked; }
    public Map<String, Boolean> getFavList() { return favList; }
    public void setFavList(Map<String, Boolean> favList) { this.favList = favList; }
    public int getFavCount() { return favCount; }
    public void setFavCount(int favCount) { this.favCount = favCount; }
}

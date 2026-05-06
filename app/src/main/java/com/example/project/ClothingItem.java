package com.example.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ClothingItem {
    private String id;
    private String imageUrl;
    private String categoryId;
    private String size;
    private String season;
    private String color;
    private List<String> occasions;
    private boolean favorite;
    private long timestamp;

    public ClothingItem() {
        // Default constructor for Firebase
        this.occasions = new ArrayList<>();
    }

    public ClothingItem(String id, String imageUrl, String categoryId) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.categoryId = categoryId;
        this.occasions = new ArrayList<>();
    }

    public ClothingItem(String id, String imageUrl, String categoryId, String size, String season, String color, List<String> occasions, boolean favorite, long timestamp) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.categoryId = categoryId;
        this.size = size;
        this.season = season;
        this.color = color;
        this.occasions = occasions != null ? occasions : new ArrayList<>();
        this.favorite = favorite;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }

    public String getSeason() { return season; }
    public void setSeason(String season) { this.season = season; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public List<String> getOccasions() { return occasions; }
    public void setOccasions(List<String> occasions) { this.occasions = occasions; }

    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClothingItem that = (ClothingItem) o;
        return Objects.equals(id, that.id) && Objects.equals(imageUrl, that.imageUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, imageUrl);
    }
}

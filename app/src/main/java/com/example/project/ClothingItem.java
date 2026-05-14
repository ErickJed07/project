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
    private String originalCategory;
    private String name;
    private String status;

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

    public String getName() { return name; }
    public void setName(Object name) { this.name = name != null ? name.toString() : null; }

    public String getStatus() { return status; }
    public void setStatus(Object status) { this.status = status != null ? status.toString() : null; }

    public String getOriginalCategory() { return originalCategory; }
    public void setOriginalCategory(Object originalCategory) { this.originalCategory = originalCategory != null ? originalCategory.toString() : null; }

    public String getId() { return id; }
    public void setId(Object id) { this.id = id != null ? id.toString() : null; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(Object imageUrl) { this.imageUrl = imageUrl != null ? imageUrl.toString() : null; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(Object categoryId) { this.categoryId = categoryId != null ? categoryId.toString() : null; }

    public String getSize() { return size; }
    public void setSize(Object size) { this.size = size != null ? size.toString() : null; }

    public String getSeason() { return season; }
    public void setSeason(Object season) {
        if (season instanceof List) {
            List<?> list = (List<?>) season;
            this.season = !list.isEmpty() ? list.get(0).toString() : null;
        } else {
            this.season = season != null ? season.toString() : null;
        }
    }

    public String getColor() { return color; }
    public void setColor(Object color) {
        if (color instanceof List) {
            List<?> list = (List<?>) color;
            this.color = !list.isEmpty() ? list.get(0).toString() : null;
        } else {
            this.color = color != null ? color.toString() : null;
        }
    }

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

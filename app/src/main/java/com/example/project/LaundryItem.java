package com.example.project;

import java.util.List;

public class LaundryItem extends ClothingItem {
    private long movedToUsedAt;
    private String originalCategory;

    public LaundryItem() {
        super();
    }

    public long getMovedToUsedAt() {
        return movedToUsedAt;
    }

    public void setMovedToUsedAt(long movedToUsedAt) {
        this.movedToUsedAt = movedToUsedAt;
    }

    public String getOriginalCategory() {
        return originalCategory;
    }

    public void setOriginalCategory(String originalCategory) {
        this.originalCategory = originalCategory;
    }
}

package com.example.project;

import java.util.List;

public class LaundryItem extends ClothingItem {
    private long movedToUsedAt;

    public LaundryItem() {
        super();
    }

    public long getMovedToUsedAt() {
        return movedToUsedAt;
    }

    public void setMovedToUsedAt(long movedToUsedAt) {
        this.movedToUsedAt = movedToUsedAt;
    }
}

package com.example.project;

public class G6_Closet_CategoryFirebase {
    public String categoryId;
    public String name;
    public String imageUrl; // New field

    public G6_Closet_CategoryFirebase() {
        // Default constructor required for calls to DataSnapshot.getValue
    }

    public G6_Closet_CategoryFirebase(String categoryId, String name, String imageUrl) {
        this.categoryId = categoryId;
        this.name = name;
        this.imageUrl = imageUrl;
    }
}

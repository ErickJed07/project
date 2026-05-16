package com.example.project;

public class AiModel {
    public String id;
    public String url;
    public String size; // Avatar's body size (e.g., S, M, L, XL)

    public AiModel() {
        // Required for Firebase
    }

    public AiModel(String id, String url) {
        this.id = id;
        this.url = url;
        this.size = "M"; // Default
    }

    public AiModel(String id, String url, String size) {
        this.id = id;
        this.url = url;
        this.size = size;
    }
}

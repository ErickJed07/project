package com.example.project;

public class AiModel {
    public String id;
    public String url;

    public AiModel() {
        // Required for Firebase
    }

    public AiModel(String id, String url) {
        this.id = id;
        this.url = url;
    }
}

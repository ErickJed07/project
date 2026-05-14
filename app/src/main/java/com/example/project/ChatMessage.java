package com.example.project;

import java.util.List;

public class ChatMessage {
    private String text;
    private boolean isAi;
    private long timestamp;
    private List<String> selectedItemIds;

    public ChatMessage() {
        // Required for Firebase
    }

    public ChatMessage(String text, boolean isAi, long timestamp, List<String> selectedItemIds) {
        this.text = text;
        this.isAi = isAi;
        this.timestamp = timestamp;
        this.selectedItemIds = selectedItemIds;
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public boolean isAi() { return isAi; }
    public void setAi(boolean ai) { isAi = ai; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public List<String> getSelectedItemIds() { return selectedItemIds; }
    public void setSelectedItemIds(List<String> selectedItemIds) { this.selectedItemIds = selectedItemIds; }
}

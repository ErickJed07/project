package com.example.project;

import java.io.Serializable;

public class ColorOption implements Serializable {
    private String name;
    private String hexCode;
    private boolean isSelected;

    public ColorOption(String name, String hexCode) {
        this.name = name;
        this.hexCode = hexCode;
        this.isSelected = false;
    }

    public String getName() { return name; }
    public String getHexCode() { return hexCode; }
    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }
}
package com.example.project;

import java.util.ArrayList;
import java.util.List;

public class CategoryManager {

    public static class CategoryItem {
        public String id;
        public String name;
        public String description;
        public int iconRes;
        public int colorRes;

        public CategoryItem(String id, String name, String description, int iconRes, int colorRes) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.iconRes = iconRes;
            this.colorRes = colorRes;
        }
    }

    public static List<CategoryItem> getCategories(boolean isWoman) {
        List<CategoryItem> categories = new ArrayList<>();

        // Apparel (Clothing)
        categories.add(new CategoryItem("Tops", "Tops", 
                isWoman ? "T-shirts, blouses, sweaters" : "T-shirts, shirts, sweaters", 
                isWoman ? R.drawable.topwoman : R.drawable.topman, R.color.cat_blue_bg));
        
        categories.add(new CategoryItem("Bottoms", "Bottoms", 
                isWoman ? "Pants, skirts, shorts" : "Pants, shorts", 
                isWoman ? R.drawable.bottomwoman : R.drawable.bottom_man, R.color.cat_pink_bg));
        
        if (isWoman) {
            categories.add(new CategoryItem("Dresses", "Dresses", "Dresses, jumpsuits", R.drawable.dress, R.color.cat_teal_bg));
        }
        
        categories.add(new CategoryItem("Outerwear", "Outerwear", "Jackets, coats, blazers", 
                isWoman ? R.drawable.outerwoman : R.drawable.outer_man, R.color.cat_green_bg));
        
        categories.add(new CategoryItem("Swimwear", "Swimwear", 
                isWoman ? "Bikinis, suits" : "Trunks, suits", 
                isWoman ? R.drawable.swimwearwoman : R.drawable.swimwearman, R.color.cat_teal_bg));

        // Footwear & Legwear
        categories.add(new CategoryItem("Socks & Tights", "Socks & Tights", "Socks, tights", 
                isWoman ? R.drawable.sockswoman : R.drawable.socksman, R.color.cat_yellow_bg));
        
        categories.add(new CategoryItem("Footwear", "Footwear", 
                isWoman ? "Shoes, heels, sandals" : "Shoes, boots, sneakers", 
                isWoman ? R.drawable.heelswoman : R.drawable.shoesman, R.color.cat_yellow_bg));

        // Accessories
        categories.add(new CategoryItem("Headwear", "Headwear", "Hats, headbands", 
                isWoman ? R.drawable.hatwoman : R.drawable.capman, R.color.cat_purple_bg));
        
        categories.add(new CategoryItem("Eyewear", "Eyewear", "Glasses, sunglasses", 
                isWoman ? R.drawable.glasseswoman : R.drawable.glassesman, R.color.cat_purple_bg));
        
        categories.add(new CategoryItem("Neckwear", "Neckwear", "Scarves, ties", 
                R.drawable.neckwoman_man, R.color.cat_purple_bg));
        
        categories.add(new CategoryItem("Handwear", "Handwear", "Gloves", 
                isWoman ? R.drawable.glovewoman : R.drawable.handwear_man, R.color.cat_purple_bg));
        
        categories.add(new CategoryItem("Belts", "Belts", "Belts", 
                R.drawable.belt_woman_man, R.color.cat_purple_bg));
        
        categories.add(new CategoryItem("Jewelry", "Jewelry", "Earrings, necklaces, bracelets, rings", 
                R.drawable.acc_woman_man, R.color.cat_purple_bg));
        
        categories.add(new CategoryItem("Watches", "Watches", "Watches", 
                R.drawable.watch_woman_man, R.color.cat_purple_bg));
        
        categories.add(new CategoryItem("Bags", "Bags", "Handbags, backpacks",
                isWoman ? R.drawable.bagwoman : R.drawable.bag_man, R.color.cat_purple_bg));

        // Archive / Special Categories
        categories.add(new CategoryItem("used_clothes", "Wash", "Items to be washed",
                R.drawable.ic_check_badge, R.color.cat_green_bg));

        return categories;
    }

    // Default to Woman for backward compatibility
    public static List<CategoryItem> getCategories() {
        return getCategories(true);
    }

    public static CategoryItem getCategoryById(String id, boolean isWoman) {
        for (CategoryItem item : getCategories(isWoman)) {
            if (item.id.equals(id)) {
                return item;
            }
        }
        return null;
    }
    
    public static CategoryItem getCategoryById(String id) {
        CategoryItem item = getCategoryById(id, true);
        if (item == null) {
            item = getCategoryById(id, false);
        }
        return item;
    }
}

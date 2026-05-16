package com.example.project;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class OutfitItemAdapter extends RecyclerView.Adapter<OutfitItemAdapter.ViewHolder> {

    private final List<ClothingItem> items;

    public OutfitItemAdapter(List<ClothingItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.e4_outfit_item_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ClothingItem item = items.get(position);
        
        // Resolve category name
        String categoryId = item.getCategoryId();
        String originalCategory = item.getOriginalCategory();
        
        // If archived, use original category for the label
        String lookupId = ("used_clothes".equals(categoryId) && originalCategory != null) ? originalCategory : categoryId;
        
        String displayName = lookupId;
        if (lookupId != null) {
            CategoryManager.CategoryItem cat = CategoryManager.getCategoryById(lookupId, true);
            if (cat == null) cat = CategoryManager.getCategoryById(lookupId, false);
            if (cat != null) displayName = cat.name;
        }
        
        holder.itemTitle.setText(displayName != null ? displayName : "Item");
        
        StringBuilder subtitle = new StringBuilder();
        String colorStr = item.getColor();
        if (colorStr != null && !colorStr.isEmpty()) {
            subtitle.append(colorStr);
            
            // Try to set color indicator visually
            holder.viewColorIndicator.setVisibility(View.VISIBLE);
            try {
                if (colorStr.startsWith("#")) {
                    holder.viewColorIndicator.getBackground().setTint(android.graphics.Color.parseColor(colorStr));
                } else {
                    // Simple name to color mapping for common colors
                    int colorInt = getColorFromName(colorStr);
                    if (colorInt != 0) {
                        holder.viewColorIndicator.getBackground().setTint(colorInt);
                    } else {
                        holder.viewColorIndicator.setVisibility(View.GONE);
                    }
                }
            } catch (Exception e) {
                holder.viewColorIndicator.setVisibility(View.GONE);
            }
        } else {
            holder.viewColorIndicator.setVisibility(View.GONE);
        }

        if (item.getSize() != null && !item.getSize().isEmpty()) {
            if (subtitle.length() > 0) subtitle.append(" • ");
            subtitle.append("Size: ").append(item.getSize());
        }
        holder.itemSubtitle.setText(subtitle.toString());

        android.content.Context context = holder.itemView.getContext();
        if (context instanceof android.app.Activity) {
            android.app.Activity activity = (android.app.Activity) context;
            if (activity.isFinishing() || activity.isDestroyed()) return;
        }

        Glide.with(context)
                .load(item.getImageUrl())
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_placeholder)
                .into(holder.itemImage);
    }

    private int getColorFromName(String name) {
        if (name == null) return 0;
        switch (name.toLowerCase()) {
            case "black": return android.graphics.Color.BLACK;
            case "white": return android.graphics.Color.WHITE;
            case "red": return android.graphics.Color.RED;
            case "blue": return android.graphics.Color.BLUE;
            case "green": return android.graphics.Color.GREEN;
            case "yellow": return android.graphics.Color.YELLOW;
            case "cyan": return android.graphics.Color.CYAN;
            case "magenta": return android.graphics.Color.MAGENTA;
            case "gray": case "grey": return android.graphics.Color.GRAY;
            case "darkgray": case "darkgrey": return android.graphics.Color.DKGRAY;
            case "lightgray": case "lightgrey": return android.graphics.Color.LTGRAY;
            case "emerald green": return android.graphics.Color.parseColor("#50C878");
            case "gold": return android.graphics.Color.parseColor("#FFD700");
            case "silver": return android.graphics.Color.parseColor("#C0C0C0");
            case "beige": return android.graphics.Color.parseColor("#F5F5DC");
            case "brown": return android.graphics.Color.parseColor("#A52A2A");
            case "orange": return android.graphics.Color.parseColor("#FFA500");
            case "purple": return android.graphics.Color.parseColor("#800080");
            case "pink": return android.graphics.Color.parseColor("#FFC0CB");
            default: return 0;
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView itemImage;
        TextView itemTitle;
        TextView itemSubtitle;
        View viewColorIndicator;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemImage = itemView.findViewById(R.id.itemImage);
            itemTitle = itemView.findViewById(R.id.itemTitle);
            itemSubtitle = itemView.findViewById(R.id.itemSubtitle);
            viewColorIndicator = itemView.findViewById(R.id.viewColorIndicator);
        }
    }
}

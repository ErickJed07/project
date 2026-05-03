package com.example.project;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private final List<CategoryItem> categories;
    private int selectedPosition = -1;
    private final OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(CategoryItem category);
    }

    public static class CategoryItem {
        String id;
        String name;
        int iconRes;

        public CategoryItem(String id, String name, int iconRes) {
            this.id = id;
            this.name = name;
            this.iconRes = iconRes;
        }
    }

    public CategoryAdapter(List<CategoryItem> categories, String currentCategory, OnCategoryClickListener listener) {
        this.categories = categories;
        this.listener = listener;
        if (currentCategory != null && !currentCategory.isEmpty()) {
            for (int i = 0; i < categories.size(); i++) {
                if (categories.get(i).id.equals(currentCategory) || categories.get(i).name.equals(currentCategory)) {
                    selectedPosition = i;
                    break;
                }
            }
        }
    }

    public void setSelectedCategory(String currentCategory) {
        if (currentCategory != null && !currentCategory.isEmpty()) {
            for (int i = 0; i < categories.size(); i++) {
                if (categories.get(i).id.equals(currentCategory) || categories.get(i).name.equals(currentCategory)) {
                    selectedPosition = i;
                    break;
                }
            }
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_selection, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        CategoryItem item = categories.get(position);
        holder.tvName.setText(item.name);
        holder.ivIcon.setImageResource(item.iconRes);

        if (position == selectedPosition) {
            holder.cardRoot.setStrokeColor(Color.parseColor("#6C28D9"));
            holder.cardRoot.setStrokeWidth(4);
            holder.cardRoot.setCardBackgroundColor(Color.parseColor("#F5F3FF"));
            holder.tvName.setTextColor(Color.parseColor("#6C28D9"));
            holder.ivIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#6C28D9")));
        } else {
            holder.cardRoot.setStrokeColor(Color.parseColor("#D1D5DB"));
            holder.cardRoot.setStrokeWidth(2);
            holder.cardRoot.setCardBackgroundColor(Color.parseColor("#F3F4F9"));
            holder.tvName.setTextColor(Color.parseColor("#1A1C1E"));
            holder.ivIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#1A1C1E")));
        }

        holder.itemView.setOnClickListener(v -> {
            int previousPosition = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(previousPosition);
            notifyItemChanged(selectedPosition);
            if (listener != null) {
                listener.onCategoryClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardRoot;
        ImageView ivIcon;
        TextView tvName;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.cv_category_root);
            ivIcon = itemView.findViewById(R.id.iv_category_icon);
            tvName = itemView.findViewById(R.id.tv_category_name);
        }
    }
}

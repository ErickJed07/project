package com.example.project;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class WardrobeCategoryAdapter extends RecyclerView.Adapter<WardrobeCategoryAdapter.ViewHolder> implements Filterable {

    private Context context;
    private List<ViewCategoriesActivity.CategoryModel> categoryList;
    private List<ViewCategoriesActivity.CategoryModel> categoryListFull;

    public WardrobeCategoryAdapter(Context context, List<ViewCategoriesActivity.CategoryModel> categoryList) {
        this.context = context;
        this.categoryList = categoryList;
        this.categoryListFull = new ArrayList<>(categoryList);
    }

    public interface OnCategoryLongClickListener {
        void onCategoryLongClick(ViewCategoriesActivity.CategoryModel category);
    }

    private OnCategoryLongClickListener longClickListener;

    public void setOnCategoryLongClickListener(OnCategoryLongClickListener listener) {
        this.longClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category_grid, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ViewCategoriesActivity.CategoryModel category = categoryList.get(position);
        holder.tvName.setText(category.name);
        holder.tvCount.setText(category.itemCount + (category.itemCount == 1 ? " Item" : " Items"));
        
        // Set icon and background based on stored icon or name
        String iconKey = (category.icon != null && !category.icon.isEmpty()) ? category.icon : category.name;
        holder.ivIcon.setImageResource(getCategoryIcon(iconKey));
        holder.cardView.setCardBackgroundColor(context.getColor(getCategoryColor(iconKey)));

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, G2_Closet_CategoryActivity.class);
            intent.putExtra("CATEGORY_NAME", category.name);
            intent.putExtra("CATEGORY_ID", category.id);
            context.startActivity(intent);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if ("all_clothes".equals(category.id)) {
                return true; // Consume long click for All Clothes to prevent deletion
            }
            if (longClickListener != null) {
                longClickListener.onCategoryLongClick(category);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    public void updateList(List<ViewCategoriesActivity.CategoryModel> newList) {
        this.categoryList = newList;
        this.categoryListFull = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    @Override
    public Filter getFilter() {
        return categoryFilter;
    }

    private Filter categoryFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<ViewCategoriesActivity.CategoryModel> filteredList = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(categoryListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();

                for (ViewCategoriesActivity.CategoryModel item : categoryListFull) {
                    if (item.name.toLowerCase().contains(filterPattern)) {
                        filteredList.add(item);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            categoryList.clear();
            categoryList.addAll((List) results.values);
            notifyDataSetChanged();
        }
    };

    private int getCategoryIcon(String categoryName) {
        switch (categoryName) {
            case "All Clothes": return R.drawable.hanger;
            case "PreOutfit": return R.drawable.preoutfit;
            case "Hat": return R.drawable.hat;
            case "Accessories": return R.drawable.accesories;
            case "Outer": return R.drawable.outer;
            case "Top": return R.drawable.top;
            case "Bag": return R.drawable.bag;
            case "Bottom": return R.drawable.botttom;
            case "Shoes": return R.drawable.shoes;
            case "Dress": return R.drawable.dresss;
            default: return R.drawable.shirt;
        }
    }

    private int getCategoryColor(String categoryName) {
        switch (categoryName) {
            case "Top": return R.color.cat_blue_bg;
            case "Bottom": return R.color.cat_pink_bg;
            case "Outer": return R.color.cat_green_bg;
            case "Accessories": return R.color.cat_purple_bg;
            case "Shoes": return R.color.cat_yellow_bg;
            case "Dress": return R.color.cat_teal_bg;
            default: return R.color.cat_blue_bg;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvName, tvCount;
        MaterialCardView cardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_category_icon);
            tvName = itemView.findViewById(R.id.tv_category_name);
            tvCount = itemView.findViewById(R.id.tv_item_count);
            cardView = itemView.findViewById(R.id.cv_category_card);
        }
    }
}

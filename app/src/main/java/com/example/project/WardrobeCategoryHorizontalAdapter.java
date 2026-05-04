package com.example.project;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class WardrobeCategoryHorizontalAdapter extends RecyclerView.Adapter<WardrobeCategoryHorizontalAdapter.ViewHolder> {

    private Context context;
    private List<ViewCategoriesActivity.CategoryModel> categoryList;

    public WardrobeCategoryHorizontalAdapter(Context context, List<ViewCategoriesActivity.CategoryModel> categoryList) {
        this.context = context;
        this.categoryList = categoryList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category_horizontal, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ViewCategoriesActivity.CategoryModel category = categoryList.get(position);
        holder.tvName.setText(category.name);
        holder.tvCount.setText(category.itemCount + (category.itemCount == 1 ? " Item" : " Items"));
        
        holder.ivIcon.setImageResource(category.iconRes);
        
        int colorRes = R.color.cat_blue_bg; // Default
        CategoryManager.CategoryItem fixed = CategoryManager.getCategoryById(category.id);
        if (fixed != null) {
            colorRes = fixed.colorRes;
        } else if ("all_clothes".equals(category.id)) {
            colorRes = R.color.wardrobe_accent_teal;
        }
        holder.cardView.setCardBackgroundColor(context.getColor(colorRes));

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, G2_Closet_CategoryActivity.class);
            intent.putExtra("CATEGORY_NAME", category.name);
            intent.putExtra("CATEGORY_ID", category.id);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    public void updateList(List<ViewCategoriesActivity.CategoryModel> newList) {
        this.categoryList = newList;
        notifyDataSetChanged();
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

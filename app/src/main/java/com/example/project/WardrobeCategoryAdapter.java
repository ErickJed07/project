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

import java.util.List;

public class WardrobeCategoryAdapter extends RecyclerView.Adapter<WardrobeCategoryAdapter.ViewHolder> {

    private Context context;
    private List<ViewCategoriesActivity.CategoryModel> categoryList;

    public WardrobeCategoryAdapter(Context context, List<ViewCategoriesActivity.CategoryModel> categoryList) {
        this.context = context;
        this.categoryList = categoryList;
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
        holder.tvCount.setText(category.itemCount + (category.itemCount == 1 ? " item" : " items"));
        
        // Set icon based on name (reusing logic from G1_ClosetActivity)
        holder.ivIcon.setImageResource(getCategoryIcon(category.name));
        
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

    private int getCategoryIcon(String categoryName) {
        switch (categoryName) {
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

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvName, tvCount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_category_icon);
            tvName = itemView.findViewById(R.id.tv_category_name);
            tvCount = itemView.findViewById(R.id.tv_item_count);
        }
    }
}

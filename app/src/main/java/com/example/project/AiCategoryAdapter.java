package com.example.project;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import android.graphics.Typeface;
import androidx.core.content.ContextCompat;
import java.util.List;

public class AiCategoryAdapter extends RecyclerView.Adapter<AiCategoryAdapter.ViewHolder> {

    private Context context;
    private List<ViewCategoriesActivity.CategoryModel> categoryList;
    private int selectedPosition = 0;
    private OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(ViewCategoriesActivity.CategoryModel category);
    }

    public AiCategoryAdapter(Context context, List<ViewCategoriesActivity.CategoryModel> categoryList, OnCategoryClickListener listener) {
        this.context = context;
        this.categoryList = categoryList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category_tab, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ViewCategoriesActivity.CategoryModel category = categoryList.get(position);
        holder.tvName.setText(category.name);

        if (category.iconRes != 0) {
            holder.ivIcon.setImageResource(category.iconRes);
            holder.ivIcon.setVisibility(View.VISIBLE);
        } else {
            holder.ivIcon.setVisibility(View.GONE);
        }

        boolean isSelected = selectedPosition == position;
        holder.tvName.setTextColor(ContextCompat.getColor(context, isSelected ? R.color.ai_accent : R.color.ai_chip_unselected_text));
        holder.tvName.setTypeface(null, isSelected ? Typeface.BOLD : Typeface.NORMAL);
        holder.indicator.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        holder.cardView.setCardBackgroundColor(Color.TRANSPARENT);
        holder.cardView.setStrokeWidth(0);

        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(previousSelected);
            notifyItemChanged(selectedPosition);
            if (listener != null) {
                listener.onCategoryClick(category);
            }
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

    public void setSelectedPosition(int position) {
        this.selectedPosition = position;
        notifyDataSetChanged();
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        View indicator;
        ImageView ivIcon;
        MaterialCardView cardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_category_name);
            indicator = itemView.findViewById(R.id.indicator);
            ivIcon = itemView.findViewById(R.id.iv_category_icon);
            cardView = itemView.findViewById(R.id.cv_category_root);
        }
    }
}

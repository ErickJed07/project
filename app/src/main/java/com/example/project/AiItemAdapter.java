package com.example.project;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AiItemAdapter extends RecyclerView.Adapter<AiItemAdapter.ViewHolder> {

    private static final int VIEW_TYPE_ADD = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    private Context context;
    private List<ClothingItem> itemList;
    private Set<ClothingItem> selectedItems = new HashSet<>();
    private OnItemClickListener listener;
    private OnItemLongClickListener longClickListener;
    private OnAddClickListener addClickListener;

    private String currentSeason = "All";
    private String currentOccasion = "All";
    private String currentSort = "All";
    private String currentCategoryName = "";
    private String currentCategoryId = "";

    public interface OnItemClickListener {
        void onItemClick(ClothingItem item, boolean isSelected);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(ClothingItem item);
    }

    public interface OnAddClickListener {
        void onAddClick(String category);
    }

    public AiItemAdapter(Context context, List<ClothingItem> itemList, OnItemClickListener listener, OnItemLongClickListener longClickListener, OnAddClickListener addClickListener) {
        this.context = context;
        this.itemList = itemList;
        this.listener = listener;
        this.longClickListener = longClickListener;
        this.addClickListener = addClickListener;
    }

    @Override
    public int getItemViewType(int position) {
        if ("all_clothes".equals(currentCategoryId)) {
            return VIEW_TYPE_ITEM;
        }
        return position == itemList.size() ? VIEW_TYPE_ADD : VIEW_TYPE_ITEM;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_ADD) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_add_wardrobe_card, parent, false);
            return new ViewHolder(view);
        }
        View view = LayoutInflater.from(context).inflate(R.layout.item_wardrobe_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_ADD) {
            if (holder.cvAddRoot != null) {
                holder.cvAddRoot.setOnClickListener(v -> {
                    if (addClickListener != null) addClickListener.onAddClick(currentCategoryId);
                });
            } else {
                holder.itemView.setOnClickListener(v -> {
                    if (addClickListener != null) addClickListener.onAddClick(currentCategoryId);
                });
            }
            return;
        }

        ClothingItem item = itemList.get(position);
        
        Glide.with(context)
                .load(item.getImageUrl())
                .placeholder(R.drawable.box_background)
                .into(holder.ivItemImage);

        boolean isSelected = selectedItems.contains(item);
        if (holder.ivCheckBadge != null) holder.ivCheckBadge.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        
        // Filter badge logic
        if (holder.ivFilterBadge != null) {
            boolean matchesSeason = !"All".equalsIgnoreCase(currentSeason) && currentSeason.equalsIgnoreCase(item.getSeason());
            boolean matchesOccasion = !"All".equalsIgnoreCase(currentOccasion) && item.getOccasions() != null && item.getOccasions().contains(currentOccasion);
            boolean matchesFave = "Faves".equalsIgnoreCase(currentSort) && item.isFavorite();
            
            if (matchesSeason || matchesOccasion || matchesFave) {
                holder.ivFilterBadge.setVisibility(View.VISIBLE);
            } else {
                holder.ivFilterBadge.setVisibility(View.GONE);
            }
        }

        if (holder.cardRoot != null) {
            int accentColor = ContextCompat.getColor(context, R.color.ai_accent);
            holder.cardRoot.setStrokeColor(isSelected ? accentColor : Color.parseColor("#E2E8F0"));
            holder.cardRoot.setStrokeWidth(isSelected ? 6 : 2);
        }

        holder.itemView.setOnClickListener(v -> {
            if (selectedItems.contains(item)) {
                selectedItems.remove(item);
            } else {
                selectedItems.add(item);
            }
            notifyItemChanged(holder.getAdapterPosition());
            if (listener != null) {
                listener.onItemClick(item, selectedItems.contains(item));
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onItemLongClick(item);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        if ("all_clothes".equals(currentCategoryId)) {
            return itemList.size();
        }
        return itemList.size() + 1;
    }

    public void updateFilters(String season, String occasion, String sort) {
        this.currentSeason = season;
        this.currentOccasion = occasion;
        this.currentSort = sort;
        notifyDataSetChanged();
    }

    public void updateList(List<ClothingItem> newList) {
        this.itemList = newList;
        notifyDataSetChanged();
    }

    public void setSelectedItems(Set<ClothingItem> selectedItems) {
        this.selectedItems = selectedItems;
        notifyDataSetChanged();
    }

    public void setCurrentCategory(String categoryId, String categoryName) {
        this.currentCategoryId = categoryId;
        this.currentCategoryName = categoryName;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivItemImage, ivCheckBadge, ivFilterBadge;
        MaterialCardView cardRoot, cvAddRoot;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivItemImage = itemView.findViewById(R.id.iv_item_image);
            ivCheckBadge = itemView.findViewById(R.id.iv_check_badge);
            ivFilterBadge = itemView.findViewById(R.id.iv_filter_badge);
            cardRoot = itemView.findViewById(R.id.cv_item_root);
            cvAddRoot = itemView.findViewById(R.id.cv_add_item_root);
        }
    }
}

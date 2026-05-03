package com.example.project;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class AiPreviewAdapter extends RecyclerView.Adapter<AiPreviewAdapter.ViewHolder> {

    private Context context;
    private List<ClothingItem> selectedItems;
    private OnRemoveClickListener removeListener;
    private OnItemClickListener itemClickListener;
    private boolean isCollapsed = false;

    public interface OnRemoveClickListener {
        void onRemoveClick(ClothingItem item);
    }

    public interface OnItemClickListener {
        void onItemClick();
    }

    public AiPreviewAdapter(Context context, List<ClothingItem> selectedItems, OnRemoveClickListener removeListener, OnItemClickListener itemClickListener) {
        this.context = context;
        this.selectedItems = selectedItems;
        this.removeListener = removeListener;
        this.itemClickListener = itemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_selected_preview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int actualPosition = isCollapsed ? selectedItems.size() - 1 : position;
        ClothingItem item = selectedItems.get(actualPosition);
        
        Glide.with(context)
                .load(item.getImageUrl())
                .placeholder(R.drawable.box_background)
                .into(holder.ivSelectedImage);

        // Show remove button whenever NOT collapsed (even for 1 item)
        if (!isCollapsed) {
            holder.btnRemove.setVisibility(View.VISIBLE);
            holder.btnRemove.setOnClickListener(v -> {
                if (removeListener != null) {
                    removeListener.onRemoveClick(item);
                }
            });
            holder.itemView.setOnClickListener(null);
        } else {
            // Collapsed state
            holder.btnRemove.setVisibility(View.GONE);
            holder.itemView.setOnClickListener(v -> {
                if (itemClickListener != null) {
                    itemClickListener.onItemClick();
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        if (selectedItems.isEmpty()) return 0;
        return isCollapsed ? 1 : selectedItems.size();
    }

    public void setCollapsed(boolean collapsed) {
        this.isCollapsed = collapsed;
        notifyDataSetChanged();
    }

    public boolean isCollapsed() {
        return isCollapsed;
    }

    public void updateList(List<ClothingItem> newList) {
        this.selectedItems = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivSelectedImage;
        View btnRemove;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivSelectedImage = itemView.findViewById(R.id.iv_selected_image);
            btnRemove = itemView.findViewById(R.id.btn_remove_preview);
        }
    }
}

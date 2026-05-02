package com.example.project;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class IconSelectionAdapter extends RecyclerView.Adapter<IconSelectionAdapter.ViewHolder> {
    private List<IconItem> icons;
    private int selectedPosition = 0;
    private OnIconSelectedListener listener;

    public interface OnIconSelectedListener {
        void onIconSelected(String iconName);
    }

    public IconSelectionAdapter(List<IconItem> icons, OnIconSelectedListener listener) {
        this.icons = icons;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_icon_selection, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        IconItem item = icons.get(position);
        holder.ivIcon.setImageResource(item.resId);

        if (selectedPosition == position) {
            holder.card.setStrokeWidth(4);
            holder.card.setCardBackgroundColor(holder.itemView.getContext().getResources().getColor(R.color.wardrobe_accent_teal));
        } else {
            holder.card.setStrokeWidth(0);
            holder.card.setCardBackgroundColor(Color.WHITE);
        }

        holder.itemView.setOnClickListener(v -> {
            int previous = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(previous);
            notifyItemChanged(selectedPosition);
            listener.onIconSelected(item.name);
        });
    }

    @Override
    public int getItemCount() {
        return icons.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        MaterialCardView card;

        public ViewHolder(View view) {
            super(view);
            ivIcon = view.findViewById(R.id.iv_icon);
            card = view.findViewById(R.id.cv_icon_container);
        }
    }
}

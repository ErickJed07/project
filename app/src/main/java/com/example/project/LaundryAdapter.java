package com.example.project;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class LaundryAdapter extends RecyclerView.Adapter<LaundryAdapter.LaundryViewHolder> {

    private final List<LaundryItem> laundryItems;
    private final OnCleanedClickListener cleanedClickListener;

    public interface OnCleanedClickListener {
        void onCleanedClick(LaundryItem item);
    }

    public LaundryAdapter(List<LaundryItem> laundryItems, OnCleanedClickListener cleanedClickListener) {
        this.laundryItems = laundryItems;
        this.cleanedClickListener = cleanedClickListener;
    }

    @NonNull
    @Override
    public LaundryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_laundry, parent, false);
        return new LaundryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LaundryViewHolder holder, int position) {
        LaundryItem item = laundryItems.get(position);
        
        String displayName = item.getOriginalCategory();
        if (displayName == null || displayName.isEmpty()) displayName = "Used Item";
        holder.tvItemName.setText(displayName);
        
        long now = System.currentTimeMillis();
        long diff = (item.getMovedToUsedAt() + TimeUnit.DAYS.toMillis(7)) - now;
        
        if (diff > 0) {
            long days = TimeUnit.MILLISECONDS.toDays(diff);
            long hours = TimeUnit.MILLISECONDS.toHours(diff) % 24;
            if (days > 0) {
                holder.tvCleanTimer.setText("Ready in " + days + "d " + hours + "h");
            } else {
                holder.tvCleanTimer.setText("Ready in " + hours + "h");
            }
        } else {
            holder.tvCleanTimer.setText("Ready to clean");
        }

        Glide.with(holder.itemView.getContext())
                .load(item.getImageUrl())
                .placeholder(R.drawable.dress)
                .into(holder.ivItemPhoto);

        holder.btnCleaned.setOnClickListener(v -> {
            if (cleanedClickListener != null) {
                cleanedClickListener.onCleanedClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return laundryItems.size();
    }

    public static class LaundryViewHolder extends RecyclerView.ViewHolder {
        ImageView ivItemPhoto;
        TextView tvItemName, tvCleanTimer;
        MaterialButton btnCleaned;

        public LaundryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivItemPhoto = itemView.findViewById(R.id.iv_item_photo);
            tvItemName = itemView.findViewById(R.id.tv_item_name);
            tvCleanTimer = itemView.findViewById(R.id.tv_clean_timer);
            btnCleaned = itemView.findViewById(R.id.btn_cleaned);
        }
    }
}

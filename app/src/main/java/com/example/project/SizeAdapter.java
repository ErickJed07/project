package com.example.project;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class SizeAdapter extends RecyclerView.Adapter<SizeAdapter.SizeViewHolder> {

    private final List<String> sizes;
    private int selectedPosition = -1;
    private final OnSizeClickListener listener;

    public interface OnSizeClickListener {
        void onSizeClick(String size);
    }

    public SizeAdapter(List<String> sizes, String currentSize, OnSizeClickListener listener) {
        this.sizes = sizes;
        this.listener = listener;
        if (currentSize != null && !currentSize.isEmpty()) {
            for (int i = 0; i < sizes.size(); i++) {
                if (sizes.get(i).equals(currentSize)) {
                    selectedPosition = i;
                    break;
                }
            }
        }
    }

    @NonNull
    @Override
    public SizeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_size_grid, parent, false);
        return new SizeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SizeViewHolder holder, int position) {
        String size = sizes.get(position);
        holder.tvSizeName.setText(size);

        if (position == selectedPosition) {
            holder.cardRoot.setStrokeColor(Color.parseColor("#6C28D9"));
            holder.cardRoot.setStrokeWidth(4);
            holder.cardRoot.setCardBackgroundColor(Color.parseColor("#F5F3FF"));
            holder.tvSizeName.setTextColor(Color.parseColor("#6C28D9"));
        } else {
            holder.cardRoot.setStrokeColor(Color.parseColor("#D1D5DB"));
            holder.cardRoot.setStrokeWidth(2);
            holder.cardRoot.setCardBackgroundColor(Color.parseColor("#F3F4F9"));
            holder.tvSizeName.setTextColor(Color.parseColor("#1A1C1E"));
        }

        holder.itemView.setOnClickListener(v -> {
            int previousPosition = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(previousPosition);
            notifyItemChanged(selectedPosition);
            if (listener != null) {
                listener.onSizeClick(size);
            }
        });
    }

    @Override
    public int getItemCount() {
        return sizes.size();
    }

    public String getSelectedSize() {
        if (selectedPosition != -1) {
            return sizes.get(selectedPosition);
        }
        return "";
    }

    static class SizeViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardRoot;
        TextView tvSizeName;

        public SizeViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.cv_size_root);
            tvSizeName = itemView.findViewById(R.id.tv_size_name);
        }
    }
}

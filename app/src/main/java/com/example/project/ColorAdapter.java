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

public class ColorAdapter extends RecyclerView.Adapter<ColorAdapter.ColorViewHolder> {

    private final List<ColorOption> colors;
    private final boolean isMultipleMode;
    private final OnColorClickListener listener;

    public interface OnColorClickListener {
        void onColorClick(ColorOption color);
    }

    public ColorAdapter(List<ColorOption> colors, boolean isMultipleMode, OnColorClickListener listener) {
        this.colors = colors;
        this.isMultipleMode = isMultipleMode;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ColorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_color_picker, parent, false);
        return new ColorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ColorViewHolder holder, int position) {
        ColorOption color = colors.get(position);
        holder.bind(color, isMultipleMode, listener);
    }

    @Override
    public int getItemCount() {
        return colors.size();
    }

    static class ColorViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView container;
        private final View colorFill;
        private final ImageView ivChecked;

        public ColorViewHolder(@NonNull View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.cv_color_container);
            colorFill = itemView.findViewById(R.id.v_color_fill);
            ivChecked = itemView.findViewById(R.id.iv_checked);
        }

        public void bind(ColorOption color, boolean isMultipleMode, OnColorClickListener listener) {
            colorFill.setBackgroundColor(Color.parseColor(color.getHexCode()));
            
            if (color.isSelected()) {
                container.setStrokeWidth(4);
                ivChecked.setVisibility(View.VISIBLE);
                // Adjust checkmark color based on background brightness if needed, 
                // but for now white is fine for dark colors and maybe we need dark for light colors.
                if (color.getHexCode().equalsIgnoreCase("#FFFFFF")) {
                    ivChecked.setColorFilter(Color.BLACK);
                } else {
                    ivChecked.setColorFilter(Color.WHITE);
                }
            } else {
                container.setStrokeWidth(0);
                ivChecked.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> listener.onColorClick(color));
        }
    }
}
package com.example.project;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import androidx.core.content.ContextCompat;
import java.util.List;

public class AiSortAdapter extends RecyclerView.Adapter<AiSortAdapter.ViewHolder> {

    private final Context context;
    private final List<String> sortOptions;
    private int selectedPosition = 0;
    private final OnSortOptionClickListener listener;

    public interface OnSortOptionClickListener {
        void onSortOptionClick(String option);
    }

    public AiSortAdapter(Context context, List<String> sortOptions, OnSortOptionClickListener listener) {
        this.context = context;
        this.sortOptions = sortOptions;
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
        String option = sortOptions.get(position);
        holder.tvName.setText(option);

        boolean isSelected = selectedPosition == position;

        if (option.equalsIgnoreCase("Fave")) {
            holder.ivIcon.setVisibility(View.VISIBLE);
            holder.ivIcon.setImageResource(R.drawable.fav);
            holder.ivIcon.setColorFilter(ContextCompat.getColor(context, isSelected ? R.color.ai_accent : R.color.ai_chip_unselected_text));
        } else {
            holder.ivIcon.setVisibility(View.GONE);
        }

        if (isSelected) {
            holder.cardView.setCardBackgroundColor(Color.TRANSPARENT);
            holder.cardView.setStrokeColor(ContextCompat.getColorStateList(context, R.color.ai_accent));
            holder.cardView.setStrokeWidth(3);
            holder.tvName.setTextColor(ContextCompat.getColor(context, R.color.ai_accent));
        } else {
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.ai_chip_unselected_bg));
            holder.cardView.setStrokeWidth(0);
            holder.tvName.setTextColor(ContextCompat.getColor(context, R.color.ai_chip_unselected_text));
        }
        holder.indicator.setVisibility(View.GONE);

        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(previousSelected);
            notifyItemChanged(selectedPosition);
            if (listener != null) {
                listener.onSortOptionClick(option);
            }
        });
    }

    @Override
    public int getItemCount() {
        return sortOptions.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        ImageView ivIcon;
        View indicator;
        MaterialCardView cardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_category_name);
            ivIcon = itemView.findViewById(R.id.iv_category_icon);
            indicator = itemView.findViewById(R.id.indicator);
            cardView = itemView.findViewById(R.id.cv_category_root);
        }
    }
}

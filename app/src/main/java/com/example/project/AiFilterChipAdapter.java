package com.example.project;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class AiFilterChipAdapter extends RecyclerView.Adapter<AiFilterChipAdapter.ViewHolder> {

    private final Context context;
    private final List<String> items;
    private int selectedPosition = 0;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(String item, int position);
    }

    public AiFilterChipAdapter(Context context, List<String> items, OnItemClickListener listener) {
        this.context = context;
        this.items = items;
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
        String item = items.get(position);
        holder.tvName.setText(item);

        boolean isSelected = selectedPosition == position;
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
        
        // Ensure underline indicator is hidden for filter chips
        View indicator = holder.itemView.findViewById(R.id.indicator);
        if (indicator != null) indicator.setVisibility(View.GONE);

        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(previousSelected);
            notifyItemChanged(selectedPosition);
            if (listener != null) {
                listener.onItemClick(item, selectedPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void reset() {
        int previous = selectedPosition;
        selectedPosition = 0;
        notifyItemChanged(previous);
        notifyItemChanged(0);
    }

    public void setSelected(String value) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).equalsIgnoreCase(value)) {
                int previous = selectedPosition;
                selectedPosition = i;
                notifyItemChanged(previous);
                notifyItemChanged(selectedPosition);
                break;
            }
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        MaterialCardView cardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_category_name);
            cardView = itemView.findViewById(R.id.cv_category_root);
        }
    }
}

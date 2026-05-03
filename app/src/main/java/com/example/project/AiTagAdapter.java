package com.example.project;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import androidx.core.content.ContextCompat;
import java.util.List;

public class AiTagAdapter extends RecyclerView.Adapter<AiTagAdapter.ViewHolder> {

    private final Context context;
    private final List<String> tagList;
    private int selectedPosition = 0;
    private final OnTagClickListener listener;

    public interface OnTagClickListener {
        void onTagClick(String tag);
    }

    public AiTagAdapter(Context context, List<String> tagList, OnTagClickListener listener) {
        this.context = context;
        this.tagList = tagList;
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
        String tag = tagList.get(position);
        holder.tvName.setText(tag);

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
        holder.indicator.setVisibility(View.GONE);

        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(previousSelected);
            notifyItemChanged(selectedPosition);
            if (listener != null) {
                listener.onTagClick(tag);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tagList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        View indicator;
        MaterialCardView cardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_category_name);
            indicator = itemView.findViewById(R.id.indicator);
            cardView = itemView.findViewById(R.id.cv_category_root);
        }
    }
}

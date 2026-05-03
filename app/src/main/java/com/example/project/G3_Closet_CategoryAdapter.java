package com.example.project;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class G3_Closet_CategoryAdapter extends RecyclerView.Adapter<G3_Closet_CategoryAdapter.PhotoHolder> {

    private final Context context;
    private final List<String> imageUrls;
    private final OnImageClickListener clickListener;
    private final OnImageLongClickListener longClickListener;
    private final UrlSelectionChecker selectionChecker;
    private final MultiSelectChecker multiSelectChecker;
    private final OnFavoriteClickListener favoriteClickListener;
    private final FavoriteChecker favoriteChecker;

    public interface OnImageClickListener {
        void onClick(String url);
    }

    public interface OnImageLongClickListener {
        void onLongClick(String url);
    }

    public interface UrlSelectionChecker {
        boolean isSelected(String url);
    }

    public interface MultiSelectChecker {
        boolean isMultiSelectMode();
    }

    public interface OnFavoriteClickListener {
        void onFavoriteClick(String url, boolean isFavorite);
    }

    public interface FavoriteChecker {
        boolean isFavorite(String url);
    }

    public G3_Closet_CategoryAdapter(
            Context context,
            List<String> imageUrls,
            OnImageClickListener clickListener,
            OnImageLongClickListener longClickListener,
            UrlSelectionChecker selectionChecker,
            MultiSelectChecker multiSelectChecker,
            OnFavoriteClickListener favoriteClickListener,
            FavoriteChecker favoriteChecker
    ) {
        this.context = context;
        this.imageUrls = imageUrls;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
        this.selectionChecker = selectionChecker;
        this.multiSelectChecker = multiSelectChecker;
        this.favoriteClickListener = favoriteClickListener;
        this.favoriteChecker = favoriteChecker;
    }

    @NonNull
    @Override
    public PhotoHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Updated to use the correct item layout
        View view = LayoutInflater.from(context).inflate(R.layout.g3_closet_category_item, parent, false);
        return new PhotoHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoHolder holder, int position) {
        String url = imageUrls.get(position);

        Glide.with(context)
                .load(url)
                .placeholder(R.drawable.box_background)
                .centerCrop()
                .into(holder.imageView);

        holder.itemView.setAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_scale));

        // Mock data for new UI elements
        if (holder.sizeLabel != null) {
            String[] sizes = {"S", "M", "L", "XL"};
            holder.sizeLabel.setText(sizes[position % sizes.length]);
        }
        if (holder.favoriteIcon != null) {
            boolean isFavorite = favoriteChecker.isFavorite(url);
            if (isFavorite) {
                holder.favoriteIcon.setImageResource(R.drawable.heart2);
                holder.favoriteIcon.setColorFilter(android.graphics.Color.RED);
            } else {
                holder.favoriteIcon.setImageResource(R.drawable.heart);
                holder.favoriteIcon.setColorFilter(android.graphics.Color.GRAY);
            }
            holder.favoriteIcon.setOnClickListener(v -> favoriteClickListener.onFavoriteClick(url, !isFavorite));
        }

        boolean selected = selectionChecker.isSelected(url);
        boolean isMultiSelect = multiSelectChecker.isMultiSelectMode();

        holder.checkBox.setVisibility(isMultiSelect ? View.VISIBLE : View.GONE);
        holder.checkBox.setChecked(selected);
        
        // Optional: show overlay when selected in multi-select mode
        View overlay = holder.itemView.findViewById(R.id.selectionOverlay);
        if (overlay != null) {
            overlay.setVisibility(selected ? View.VISIBLE : View.GONE);
        }

        holder.itemView.setOnClickListener(v -> clickListener.onClick(url));
        holder.itemView.setOnLongClickListener(v -> {
            longClickListener.onLongClick(url);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    static class PhotoHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        CheckBox checkBox;
        TextView sizeLabel;
        ImageView favoriteIcon;

        public PhotoHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.photoImageView);
            checkBox = itemView.findViewById(R.id.photoCheckBox);
            sizeLabel = itemView.findViewById(R.id.sizeLabel);
            favoriteIcon = itemView.findViewById(R.id.favoriteIcon);
        }
    }
}

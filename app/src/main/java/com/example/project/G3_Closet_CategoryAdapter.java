package com.example.project;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide; // Import Glide

import java.util.List;

public class G3_Closet_CategoryAdapter extends RecyclerView.Adapter<G3_Closet_CategoryAdapter.PhotoHolder> {

    private final Context context;
    private final List<String> imageUrls; // Changed from File to String
    private final OnImageClickListener clickListener;
    private final OnImageLongClickListener longClickListener;
    private final UrlSelectionChecker selectionChecker; // Renamed interface
    private final MultiSelectChecker multiSelectChecker;

    // Interface updated to pass String (URL)
    public interface OnImageClickListener {
        void onClick(String url);
    }

    // Interface updated to pass String (URL)
    public interface OnImageLongClickListener {
        void onLongClick(String url);
    }

    // Interface updated to pass String (URL)
    public interface UrlSelectionChecker {
        boolean isSelected(String url);
    }

    public interface MultiSelectChecker {
        boolean isMultiSelectMode();
    }

    public G3_Closet_CategoryAdapter(
            Context context,
            List<String> imageUrls, // Changed from File to String
            OnImageClickListener clickListener,
            OnImageLongClickListener longClickListener,
            UrlSelectionChecker selectionChecker,
            MultiSelectChecker multiSelectChecker
    ) {
        this.context = context;
        this.imageUrls = imageUrls;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
        this.selectionChecker = selectionChecker;
        this.multiSelectChecker = multiSelectChecker;
    }

    @NonNull
    @Override
    public PhotoHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.g_closet_item_photo, parent, false);
        return new PhotoHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoHolder holder, int position) {
        String url = imageUrls.get(position);

        // Use Glide to load image from URL
        Glide.with(context)
                .load(url)
                .placeholder(R.drawable.box_background) // Optional placeholder
                .centerCrop()
                .into(holder.imageView);

        holder.itemView.setAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_scale));

        boolean selected = selectionChecker.isSelected(url);
        boolean showCheckbox = multiSelectChecker.isMultiSelectMode();

        holder.checkBox.setVisibility(showCheckbox ? View.VISIBLE : View.GONE);
        holder.checkBox.setChecked(selected);

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

        public PhotoHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.photoImageView);
            checkBox = itemView.findViewById(R.id.photoCheckBox);
        }
    }
}

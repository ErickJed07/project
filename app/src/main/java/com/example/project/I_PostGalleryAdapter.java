package com.example.project;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;

public class I_PostGalleryAdapter extends RecyclerView.Adapter<I_PostGalleryAdapter.ViewHolder> {

    private List<String> imageUris;
    private OnItemClickListener onItemClickListener;
    private ArrayList<String> selectedImages;

    public I_PostGalleryAdapter(List<String> imageUris, ArrayList<String> selectedImages, OnItemClickListener onItemClickListener) {
        this.imageUris = imageUris;
        this.selectedImages = selectedImages;
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.i_newpostupload_item_images, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String imageUri = imageUris.get(position);
        Glide.with(holder.itemView.getContext()).load(imageUri).into(holder.galleryImageView);

        if (selectedImages.contains(imageUri)) {
            holder.overlay.setVisibility(View.VISIBLE);
            holder.galleryImageView.setAlpha(0.5f);
            holder.circleIndicator.setVisibility(View.VISIBLE);
            int orderIndex = selectedImages.indexOf(imageUri) + 1;
            holder.numberText.setText(String.valueOf(orderIndex));
        } else {
            holder.overlay.setVisibility(View.GONE);
            holder.galleryImageView.setAlpha(1.0f);
            holder.circleIndicator.setVisibility(View.GONE);
        }

        if (onItemClickListener != null) {
            holder.itemView.setOnClickListener(v -> onItemClickListener.onItemClick(imageUri));
        }
    }

    @Override
    public int getItemCount() { return imageUris.size(); }

    public interface OnItemClickListener { void onItemClick(String imageUri); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView galleryImageView;
        View overlay;
        RelativeLayout circleIndicator;
        TextView numberText;

        public ViewHolder(View itemView) {
            super(itemView);
            galleryImageView = itemView.findViewById(R.id.galleryImageView);
            overlay = itemView.findViewById(R.id.overlay);
            circleIndicator = itemView.findViewById(R.id.circleIndicator);
            numberText = itemView.findViewById(R.id.numberText);
        }
    }
}

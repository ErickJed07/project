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

public class I_NewPost_GalleryAdapter extends RecyclerView.Adapter<I_NewPost_GalleryAdapter.ViewHolder> {

    private List<String> imageUris;
    private OnItemClickListener onItemClickListener;
    private ArrayList<String> selectedImages;  // Add this to hold selected images

    public I_NewPost_GalleryAdapter(List<String> imageUris, ArrayList<String> selectedImages, OnItemClickListener onItemClickListener) {
        this.imageUris = imageUris;
        this.selectedImages = selectedImages;  // Initialize selectedImages
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.i_newpostupload_item_images, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String imageUri = imageUris.get(position);

        // Load image with Glide
        Glide.with(holder.itemView.getContext())
                .load(imageUri)
                .into(holder.galleryImageView);  // Updated reference here

        // Set overlay and circle indicator for selected images
        if (selectedImages.contains(imageUri)) {
            holder.overlay.setVisibility(View.VISIBLE);  // Show overlay
            holder.galleryImageView.setAlpha(0.5f); // Reduce opacity for selected images

            // Always show "1" for the first selected image, regardless of order in the list
            holder.circleIndicator.setVisibility(View.VISIBLE);  // Show the circle indicator

            // Get the current selected index based on the order of selection (1-based index)
            int orderIndex = selectedImages.indexOf(imageUri) + 1;
            holder.numberText.setText(String.valueOf(orderIndex));  // Display the number based on selection order
        } else {
            holder.overlay.setVisibility(View.GONE);  // Hide overlay
            holder.galleryImageView.setAlpha(1.0f); // Full opacity for unselected images

            // Hide the circle indicator when the image is not selected
            holder.circleIndicator.setVisibility(View.GONE);
        }

        // This code handles item click for an external click listener, if necessary
        if (onItemClickListener != null) {
            holder.itemView.setOnClickListener(v -> onItemClickListener.onItemClick(imageUri));
        }
    }

    @Override
    public int getItemCount() {
        return imageUris.size();
    }

    public interface OnItemClickListener {
        void onItemClick(String imageUri); // Callback for image click
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView galleryImageView;  // Updated reference to galleryImageView
        View overlay;  // This is your overlay view
        RelativeLayout circleIndicator;  // Circle indicator layout
        TextView numberText;  // TextView inside the circle for the number

        public ViewHolder(View itemView) {
            super(itemView);
            galleryImageView = itemView.findViewById(R.id.galleryImageView);  // Updated ID reference
            overlay = itemView.findViewById(R.id.overlay);  // Reference to overlay view
            circleIndicator = itemView.findViewById(R.id.circleIndicator);  // Circle indicator view
            numberText = itemView.findViewById(R.id.numberText);  // Text inside the circle
        }
    }
}

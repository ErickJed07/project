package com.example.project;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Photo adapter manages a list of photo paths (List<String>).
 * Handles both local file paths (Permanent Category) and Web URLs (Cloudinary).
 */
public class H5_Dress_PhotoAdapter extends RecyclerView.Adapter<H5_Dress_PhotoAdapter.ViewHolder> {

    private Context context;
    private List<String> photos = new ArrayList<>();
    private OnPhotoClickListener listener;

    public interface OnPhotoClickListener {
        void onPhotoClick(String photoPath);
    }

    public H5_Dress_PhotoAdapter(Context context, List<String> photos, OnPhotoClickListener listener) {
        this.context = context;
        if (photos != null) this.photos = photos;
        this.listener = listener;
    }

    @NonNull
    @Override
    public H5_Dress_PhotoAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.h4_dresser_photo, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull H5_Dress_PhotoAdapter.ViewHolder holder, int position) {
        String path = photos.get(position);

        if (path != null && !path.isEmpty()) {
            // Check if it's a web URL (Cloudinary)
            if (path.startsWith("http") || path.startsWith("https")) {
                Glide.with(context)
                        .load(path)
                        .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache remote images
                        .placeholder(android.R.drawable.ic_menu_gallery) // Optional placeholder
                        .into(holder.imgPhoto);
            }
            // Otherwise, treat as a Local File
            else {
                File f = new File(path);
                Glide.with(context)
                        .load(f) // Glide handles File objects efficiently
                        .diskCacheStrategy(DiskCacheStrategy.NONE) // Local files might change, usually safer not to cache heavily or use NONE/DATA
                        .skipMemoryCache(false)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(holder.imgPhoto);
            }
        } else {
            // Clear image if path is null
            Glide.with(context).clear(holder.imgPhoto);
            holder.imgPhoto.setImageDrawable(null);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onPhotoClick(path);
        });
    }

    @Override
    public int getItemCount() {
        return photos == null ? 0 : photos.size();
    }

    /**
     * Replace photos list and refresh the UI.
     * Renamed to match H1_DressActivity call.
     */
    public void updatePhotos(List<String> newPhotos) {
        if (newPhotos == null) {
            this.photos = new ArrayList<>();
        } else {
            this.photos = new ArrayList<>(newPhotos);
        }
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPhoto;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPhoto = itemView.findViewById(R.id.imgPhoto);
        }
    }
}

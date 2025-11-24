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
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Photo adapter manages a list of local photo file paths (List<String>).
 * Provides setPhotos(List<String>) to replace photos safely.
 * Click listener returns the String path of the clicked photo.
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
        Uri uri = null;
        if (path != null) {
            File f = new File(path);
            if (f.exists()) {
                uri = Uri.fromFile(f);
            } else {
                // try parse as Uri string
                uri = Uri.parse(path);
            }
        }
        // Use Glide to load into ImageView. Defensive checks applied.
        if (uri != null) {
            Glide.with(context).load(uri).into(holder.imgPhoto);
        } else {
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
     * Using notifyDataSetChanged() because we replace the whole list.
     */
    public void setPhotos(List<String> newPhotos) {
        if (newPhotos == null) {
            this.photos = new ArrayList<>();
        } else {
            this.photos = new ArrayList<>(newPhotos);
        }
        android.util.Log.d("H5_Dress_PhotoAdapter", "Photos updated: " + this.photos.size());
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
package com.example.project;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class I_NewPost_ViewPageAdapter extends RecyclerView.Adapter<I_NewPost_ViewPageAdapter.ImageViewHolder> {

    private Context context;
    private ArrayList<String> imageUris;


    public I_NewPost_ViewPageAdapter(Context context, ArrayList<String> imageUris) {
        this.context = context;
        this.imageUris = imageUris;


    }
    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.i_newpostupload_image_view, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String imageUri = imageUris.get(position);

        // Load image with Glide
        Glide.with(context)
                .load(imageUri)
                .into(holder.VPageImageView);

    }


    @Override
    public int getItemCount() {
        return imageUris.size();
    }


    public static class ImageViewHolder extends RecyclerView.ViewHolder {

        ImageView VPageImageView;

        public ImageViewHolder(View itemView) {
            super(itemView);
            VPageImageView = itemView.findViewById(R.id.viewPageImageView); // Ensure this matches your layout ID
        }
    }
}

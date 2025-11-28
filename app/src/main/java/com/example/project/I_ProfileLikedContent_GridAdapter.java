package com.example.project;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

public class I_ProfileLikedContent_GridAdapter extends RecyclerView.Adapter<I_ProfileLikedContent_GridAdapter.ViewHolder> {

    private Context context;
    private List<I_NewPost_Event> mPosts;
    private OnItemClickListener mListener;

    // Constructor
    public I_ProfileLikedContent_GridAdapter(Context context, List<I_NewPost_Event> mPosts, OnItemClickListener listener) {
        this.context = context;
        this.mPosts = mPosts;
        this.mListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.i_profile_contents_gridimage, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        I_NewPost_Event post = mPosts.get(position);

        // Load image using Glide
        // Assuming the first image in the list is the thumbnail
        String imageUrlToLoad = "";
        if (post.getImageUrls() != null && !post.getImageUrls().isEmpty()) {
            imageUrlToLoad = post.getImageUrls().get(0);
        }

        Glide.with(context)
                .load(imageUrlToLoad)
                .apply(new RequestOptions().centerCrop())
                .placeholder(R.drawable.ic_placeholder) // Ensure you have this drawable or remove this line
                .into(holder.imageView);

        // Handle click event
        holder.itemView.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onImageClick(post, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mPosts.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView imageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ensure this ID matches the ImageView in i3_grid_item.xml
            imageView = itemView.findViewById(R.id.gridImageView);
        }
    }

    // Interface for click handling
    public interface OnItemClickListener {
        void onImageClick(I_NewPost_Event post, int position);
    }
}

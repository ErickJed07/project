package com.example.project;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class D_Feed_ImageViewAdapter extends RecyclerView.Adapter<D_Feed_ImageViewAdapter.ImageViewHolder> {

    private Context context;
    private List<String> imageUrls;

    public D_Feed_ImageViewAdapter(Context context, List<String> imageUrls) {
        this.context = context;
        this.imageUrls = imageUrls;
    }

    @Override
    public ImageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.d_feed_item_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ImageViewHolder holder, int position) {
        String imageUrl = imageUrls.get(position);
        Glide.with(context).load(imageUrl).into(holder.feedItemImageView);
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    public class ImageViewHolder extends RecyclerView.ViewHolder {

        private ImageView feedItemImageView;

        public ImageViewHolder(View itemView) {
            super(itemView);
            feedItemImageView = itemView.findViewById(R.id.FeedItemImageView);  // Ensure this ID is correct in layout
        }
    }
}

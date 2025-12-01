package com.example.project;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class G5_Closet_Category_PhotoPagerAdapter extends RecyclerView.Adapter<G5_Closet_Category_PhotoPagerAdapter.ViewHolder> {

    private final Context context;
    private final List<String> imageUrls;

    public G5_Closet_Category_PhotoPagerAdapter(Context context, List<String> imageUrls) {
        this.context = context;
        this.imageUrls = imageUrls;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.g4_closet_category_item_photo_fullscreen, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (imageUrls == null || position < 0 || position >= imageUrls.size()) {
            return;
        }

        String url = imageUrls.get(position);

        // CHANGED: Use Glide to load the Cloudinary URL directly
        Glide.with(context)
                .load(url)
                // You can use the same placeholder you used in the grid adapter
                .placeholder(R.drawable.box_background)
                .error(R.drawable.ic_launcher_foreground) // Fallback if URL fails
                .into(holder.fullImage);
    }

    @Override
    public int getItemCount() {
        return imageUrls == null ? 0 : imageUrls.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView fullImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            fullImage = itemView.findViewById(R.id.fullImage);
        }
    }
}

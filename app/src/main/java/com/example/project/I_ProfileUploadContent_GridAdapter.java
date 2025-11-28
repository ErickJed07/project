package com.example.project;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import java.util.ArrayList;
import java.util.List;

public class I_ProfileUploadContent_GridAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_ADD = 0;
    private static final int VIEW_TYPE_IMAGE = 1;

    private Context context;
    // CHANGED: We now store the full Post objects so we can get IDs
    private List<I_NewPost_Event> postList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onAddClick();
        void onImageClick(I_NewPost_Event post, int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    // CHANGED: Constructor now takes List<I_NewPost_Event> instead of List<String>
    public I_ProfileUploadContent_GridAdapter(Context context, List<I_NewPost_Event> postList) {
        this.context = context;
        this.postList = postList;
    }

    @Override
    public int getItemViewType(int position) {
        return (position == 0) ? VIEW_TYPE_ADD : VIEW_TYPE_IMAGE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.i_profile_contents_gridimage, parent, false);
        if (viewType == VIEW_TYPE_ADD) {
            return new AddViewHolder(view);
        } else {
            return new ImageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == VIEW_TYPE_ADD) {
            AddViewHolder addHolder = (AddViewHolder) holder;
            addHolder.imageView.setImageResource(R.drawable.plus_upload);
            addHolder.imageView.setScaleType(ImageView.ScaleType.CENTER);
            addHolder.imageView.setBackgroundColor(Color.LTGRAY);

            addHolder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onAddClick();
            });

        } else {
            ImageViewHolder imageHolder = (ImageViewHolder) holder;

            // Adjust position because index 0 is the add button
            int realListPosition = position - 1;
            I_NewPost_Event currentPost = postList.get(realListPosition);

            // Get the FIRST image from the list to show in grid
            String coverUrl = "";
            if (currentPost.getImageUrls() != null && !currentPost.getImageUrls().isEmpty()) {
                coverUrl = currentPost.getImageUrls().get(0);
            }

            Glide.with(context)
                    .load(coverUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(android.R.color.darker_gray)
                    .centerCrop()
                    .into(imageHolder.imageView);

            imageHolder.itemView.setOnClickListener(v -> {
                // --- CRITICAL FIX ---
                Intent intent = new Intent(context, I_ProfileUploadContent_GridViewer.class);

                // 1. Pass URLs
                intent.putStringArrayListExtra("URLS", (ArrayList<String>) currentPost.getImageUrls());

                // 2. Pass Username/Caption (Optional)
                intent.putExtra("USERNAME", currentPost.getUsername());
                intent.putExtra("CAPTION", currentPost.getCaption());

                // 3. PASS THE ID! (This fixes the null error)
                intent.putExtra("POST_ID", currentPost.getPostId());

                context.startActivity(intent);

                // Also trigger listener if needed
                if (listener != null) listener.onImageClick(currentPost, realListPosition);
            });
        }
    }

    @Override
    public int getItemCount() {
        // +1 for the "Add" button
        return (postList != null) ? postList.size() + 1 : 1;
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.gridImageView);
        }
    }

    public static class AddViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        public AddViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.gridImageView);
        }
    }
}

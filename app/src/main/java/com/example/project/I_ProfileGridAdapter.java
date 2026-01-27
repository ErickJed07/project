package com.example.project;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import java.util.List;

public class I_ProfileGridAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_ADD = 0;
    private static final int VIEW_TYPE_IMAGE = 1;

    private Context context;
    private List<I_PostEvent> postList;
    private OnItemClickListener listener;
    private boolean showAddButton;

    public interface OnItemClickListener {
        void onAddClick();
        void onImageClick(I_PostEvent post, int position);
    }

    public I_ProfileGridAdapter(Context context, List<I_PostEvent> postList, boolean showAddButton, OnItemClickListener listener) {
        this.context = context;
        this.postList = postList;
        this.showAddButton = showAddButton;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return (showAddButton && position == 0) ? VIEW_TYPE_ADD : VIEW_TYPE_IMAGE;
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
            int realPosition = showAddButton ? position - 1 : position;
            I_PostEvent currentPost = postList.get(realPosition);

            String coverUrl = "";
            if (currentPost.getImageUrls() != null && !currentPost.getImageUrls().isEmpty()) {
                coverUrl = currentPost.getImageUrls().get(0);
            }

            Glide.with(context)
                    .load(coverUrl)
                    .apply(new RequestOptions().centerCrop())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_placeholder)
                    .into(imageHolder.imageView);

            imageHolder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onImageClick(currentPost, realPosition);
            });
        }
    }

    @Override
    public int getItemCount() {
        int count = (postList != null) ? postList.size() : 0;
        return showAddButton ? count + 1 : count;
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

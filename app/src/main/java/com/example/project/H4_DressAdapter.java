package com.example.project;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class H4_DressAdapter extends RecyclerView.Adapter<H4_DressAdapter.CategoryViewHolder> {

    private final Context context;
    private List<String> categoryNames = new ArrayList<>();
    private List<String> categoryThumbs = new ArrayList<>();
    private final OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(String categoryName);
    }

    public H4_DressAdapter(Context context, List<String> names, OnCategoryClickListener listener) {
        this.context = context;
        this.categoryNames = names;
        this.listener = listener;
        this.categoryThumbs = new ArrayList<>();
    }

    public void updateData(List<String> names, List<String> thumbs) {
        this.categoryNames = names;
        this.categoryThumbs = thumbs;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.h3_dresser_carousel_item, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        String name = categoryNames.get(position);
        String thumbPath = position < categoryThumbs.size() ? categoryThumbs.get(position) : null;

        holder.txtName.setText(name);

        if (thumbPath != null) {
            File f = new File(thumbPath);
            Uri uri = Uri.fromFile(f);
            Glide.with(context)
                    .load(uri)
                    .placeholder(R.drawable.ic_placeholder) // make sure you have a placeholder drawable
                    .into(holder.imgThumb);
        } else {
            holder.imgThumb.setImageResource(R.drawable.ic_placeholder);
        }

        holder.itemView.setOnClickListener(v -> listener.onCategoryClick(name));
    }

    @Override
    public int getItemCount() {
        return categoryNames.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView txtName;
        ImageView imgThumb;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.tvCategoryName);
            imgThumb = itemView.findViewById(R.id.ivCategoryThumb);
        }
    }
}

package com.example.project;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class G7_ModelAdapter extends RecyclerView.Adapter<G7_ModelAdapter.ViewHolder> {

    private Context context;
    private List<String> modelUrls;
    private OnModelClickListener listener;

    public interface OnModelClickListener {
        void onModelClick(String url);
        void onAddModelClick();
    }

    public G7_ModelAdapter(Context context, List<String> modelUrls, OnModelClickListener listener) {
        this.context = context;
        this.modelUrls = modelUrls;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.g7_item_model_select, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position == 0) {
            holder.addModelIcon.setVisibility(View.VISIBLE);
            holder.modelImage.setVisibility(View.GONE);
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAddModelClick();
                }
            });
        } else {
            holder.addModelIcon.setVisibility(View.GONE);
            holder.modelImage.setVisibility(View.VISIBLE);
            String url = modelUrls.get(position);
            Glide.with(context).load(url).into(holder.modelImage);

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onModelClick(url);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return modelUrls.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView modelImage, addModelIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            modelImage = itemView.findViewById(R.id.model_image);
            addModelIcon = itemView.findViewById(R.id.add_model_icon);
        }
    }
}

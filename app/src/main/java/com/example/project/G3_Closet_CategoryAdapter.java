package com.example.project;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

public class G3_Closet_CategoryAdapter extends RecyclerView.Adapter<G3_Closet_CategoryAdapter.PhotoHolder> {

    private final Context context;
    private final List<File> imageFiles;
    private final OnImageClickListener clickListener;
    private final OnImageLongClickListener longClickListener;
    private final FileSelectionChecker selectionChecker;
    private final MultiSelectChecker multiSelectChecker;

    public interface OnImageClickListener {
        void onClick(File file);
    }

    public interface OnImageLongClickListener {
        void onLongClick(File file);
    }

    public interface FileSelectionChecker {
        boolean isSelected(File file);
    }

    public interface MultiSelectChecker {
        boolean isMultiSelectMode();
    }

    public G3_Closet_CategoryAdapter(
            Context context,
            List<File> imageFiles,
            OnImageClickListener clickListener,
            OnImageLongClickListener longClickListener,
            FileSelectionChecker selectionChecker,
            MultiSelectChecker multiSelectChecker
    ) {
        this.context = context;
        this.imageFiles = imageFiles;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
        this.selectionChecker = selectionChecker;
        this.multiSelectChecker = multiSelectChecker;
    }

    @NonNull
    @Override
    public PhotoHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.g_closet_item_photo, parent, false);
        return new PhotoHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoHolder holder, int position) {
        File file = imageFiles.get(position);
        holder.imageView.setImageBitmap(BitmapFactory.decodeFile(file.getAbsolutePath()));
        holder.itemView.setAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_scale));

        boolean selected = selectionChecker.isSelected(file);
        boolean showCheckbox = multiSelectChecker.isMultiSelectMode();

        holder.checkBox.setVisibility(showCheckbox ? View.VISIBLE : View.GONE);
        holder.checkBox.setChecked(selected);

        holder.itemView.setOnClickListener(v -> clickListener.onClick(file));
        holder.itemView.setOnLongClickListener(v -> {
            longClickListener.onLongClick(file);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return imageFiles.size();
    }

    static class PhotoHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        CheckBox checkBox;

        public PhotoHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.photoImageView);
            checkBox = itemView.findViewById(R.id.photoCheckBox);
        }
    }
}

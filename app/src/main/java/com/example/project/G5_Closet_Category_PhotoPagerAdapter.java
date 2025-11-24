package com.example.project;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

public class G5_Closet_Category_PhotoPagerAdapter extends RecyclerView.Adapter<G5_Closet_Category_PhotoPagerAdapter.ViewHolder> {

    private final Context context;
    private final List<String> imagePaths;

    public G5_Closet_Category_PhotoPagerAdapter(Context context, List<String> imagePaths) {
        this.context = context;
        this.imagePaths = imagePaths;
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
        if (imagePaths == null || position < 0 || position >= imagePaths.size()) {
            // Defensive fallback
            holder.fullImage.setImageDrawable(ContextCompat.getDrawable(holder.fullImage.getContext(), R.drawable.ic_launcher_foreground));
            return;
        }

        String path = imagePaths.get(position);
        if (path == null) {
            holder.fullImage.setImageDrawable(ContextCompat.getDrawable(holder.fullImage.getContext(), R.drawable.ic_launcher_foreground));
            return;
        }

        File file = new File(path);
        if (!file.exists()) {
            // Try setting URI if path is actually a content Uri string
            try {
                Uri uri = Uri.parse(path);
                holder.fullImage.setImageURI(uri);
            } catch (Exception e) {
                holder.fullImage.setImageDrawable(ContextCompat.getDrawable(holder.fullImage.getContext(), R.drawable.ic_launcher_foreground));
            }
            return;
        }

        // Load a sampled bitmap to avoid OOM for large images.
        try {
            // Use view size if available, otherwise use a sensible default
            int reqW = holder.fullImage.getWidth();
            int reqH = holder.fullImage.getHeight();
            if (reqW <= 0) reqW = 1080;
            if (reqH <= 0) reqH = 1680;

            Bitmap bmp = decodeSampledBitmapFromFile(path, reqW, reqH);
            if (bmp != null) {
                holder.fullImage.setImageBitmap(bmp);
            } else {
                holder.fullImage.setImageDrawable(ContextCompat.getDrawable(holder.fullImage.getContext(), R.drawable.ic_launcher_foreground));
            }
        } catch (Exception e) {
            // Last-resort: use URI
            try {
                holder.fullImage.setImageURI(Uri.fromFile(file));
            } catch (Exception ex) {
                holder.fullImage.setImageDrawable(ContextCompat.getDrawable(holder.fullImage.getContext(), R.drawable.ic_launcher_foreground));
            }
        }
    }

    @Override
    public int getItemCount() {
        return imagePaths == null ? 0 : imagePaths.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView fullImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            fullImage = itemView.findViewById(R.id.fullImage);
        }
    }

    // --- Bitmap sampling helpers ---
    private static Bitmap decodeSampledBitmapFromFile(String path, int reqWidth, int reqHeight) {
        try {
            // First decode with inJustDecodeBounds=true to check dimensions
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.RGB_565; // less memory than ARGB_8888
            return BitmapFactory.decodeFile(path, options);
        } catch (Exception e) {
            return null;
        }
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}

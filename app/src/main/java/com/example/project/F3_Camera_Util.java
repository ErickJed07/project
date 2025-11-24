package com.example.project;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.net.Uri;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class F3_Camera_Util {

    /**
     * Decode bitmap from file path with sampling to avoid OOM
     */
    public static Bitmap decodeSampledBitmapFromFile(String path, int reqWidth, int reqHeight) {
        if (path == null || path.isEmpty() || !fileExists(path)) return null;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        // Calculate sample size
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888; // Transparency-safe

        Bitmap bmp = BitmapFactory.decodeFile(path, options);

        // Fix rotation if needed
        return rotateIfRequired(bmp, path);
    }

    /**
     * Decode bitmap from URI with sampling
     */
    public static Bitmap decodeSampledBitmapFromUri(Context context, Uri uri, int reqWidth, int reqHeight) {
        if (uri == null) return null;
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            // Get image size without loading full bitmap
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, options);

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            try (InputStream input2 = context.getContentResolver().openInputStream(uri)) {
                Bitmap bmp = BitmapFactory.decodeStream(input2, null, options);
                String path = uri.getPath();
                return rotateIfRequired(bmp, path);
            }
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Calculate an inSampleSize for BitmapFactory.Options
     */
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight &&
                    (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    /**
     * Scale bitmap to fit inside given dimensions while maintaining aspect ratio
     */
    public static Bitmap scaleToFit(Bitmap src, int maxWidth, int maxHeight) {
        if (src == null) return null;

        float widthRatio = (float) maxWidth / src.getWidth();
        float heightRatio = (float) maxHeight / src.getHeight();
        float scale = Math.min(widthRatio, heightRatio);

        if (scale >= 1) return src; // No scaling needed

        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);

        return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
    }

    /**
     * Fix image rotation from EXIF metadata
     */
    private static Bitmap rotateIfRequired(Bitmap bitmap, String path) {
        if (bitmap == null || path == null) return bitmap;

        try {
            ExifInterface exif = new ExifInterface(path);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;
                default:
                    return bitmap;
            }

            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (rotated != bitmap) {
                bitmap.recycle();
            }
            return rotated;

        } catch (IOException e) {
            return bitmap;
        }
    }

    /**
     * Convert dp to px
     */
    public static int dpToPx(Context context, float dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    /**
     * Run code after view has been laid out
     */
    public static void runAfterLayout(final View view, final Runnable callback) {
        if (view.isLaidOut()) {
            callback.run();
        } else {
            view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    callback.run();
                }
            });
        }
    }

    /**
     * Get center point of a bitmap
     */
    public static PointF getBitmapCenter(Bitmap bitmap) {
        if (bitmap == null) return new PointF(0, 0);
        return new PointF(bitmap.getWidth() / 2f, bitmap.getHeight() / 2f);
    }

    /**
     * Check if a file exists
     */
    public static boolean fileExists(String path) {
        return path != null && new File(path).exists();
    }

    /**
     * Safely recycle a bitmap to free memory
     */
    public static void recycleBitmap(Bitmap bmp) {
        if (bmp != null && !bmp.isRecycled()) {
            bmp.recycle();
        }
    }
}

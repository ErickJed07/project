package com.example.project;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.Nullable;

public class H2_Dresser_OutfitView extends FrameLayout {

    private ImageView selectedView;
    private OnSelectionChangedListener listener;
    public ImageButton btnRotate; // Can be linked from Activity if needed

    public H2_Dresser_OutfitView(Context context) { super(context); init(); }
    public H2_Dresser_OutfitView(Context context, @Nullable AttributeSet attrs) { super(context, attrs); init(); }
    public H2_Dresser_OutfitView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); init(); }

    private void init() {
        setClipChildren(false);
    }

    // --- DELETED onMeasure ---
    // This allows the view to match the parent height defined in XML
    // instead of forcing a square shape.

    /**
     * Adds a new image to the canvas.
     */
    public ImageView addImage(Bitmap bitmap) {
        ImageView iv = new ImageView(getContext());
        iv.setImageBitmap(bitmap);

        // Use FIT_CENTER so the image fills its box
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);

        iv.setClickable(true);
        iv.setFocusable(true);

        // Set a default size (e.g., 400x400)
        int defaultSize = 400;
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(defaultSize, defaultSize);
        params.gravity = Gravity.CENTER;
        iv.setLayoutParams(params);

        addView(iv);
        return iv;
    }

    /**
     * Helper to get the bitmap of the entire composition for saving.
     */
    public Bitmap getCanvasBitmap() {
        if (getWidth() <= 0 || getHeight() <= 0) return null;

        // Create a bitmap of the specific view size
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Draw the background color (if any) or transparency
        Drawable bg = getBackground();
        if (bg != null) {
            bg.draw(canvas);
        } else {
            // Draw white background if none set, or leave transparent
            canvas.drawColor(0xFFFFFFFF);
        }

        draw(canvas);
        return bitmap;
    }

    public void setSelectedView(@Nullable ImageView view) {
        this.selectedView = view;

        if (view != null) {
            view.bringToFront(); // Visual Z-index update
            // Force layout update to ensure hit-testing works for the top item
            requestLayout();
            invalidate();
        }

        if (listener != null && view != null) {
            listener.onSelectionChanged(view, getCurrentScale(view));
        }
    }

    private float getCurrentScale(ImageView iv) {
        float[] values = new float[9];
        if (iv.getImageMatrix() != null) {
            iv.getImageMatrix().getValues(values);
            return values[Matrix.MSCALE_X];
        }
        return 1f;
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener l) {
        this.listener = l;
    }

    public Bitmap exportToBitmap() {
        return getCanvasBitmap();
    }

    public interface OnSelectionChangedListener {
        void onSelectionChanged(ImageView selected, float currentScale);
    }
}

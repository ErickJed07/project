package com.example.project;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.Nullable;

public class H2_Dresser_OutfitView extends FrameLayout {

    private ImageView selectedView;
    private OnSelectionChangedListener listener;
    public ImageButton btnRotate;

    public H2_Dresser_OutfitView(Context context) { super(context); init(); }
    public H2_Dresser_OutfitView(Context context, @Nullable AttributeSet attrs) { super(context, attrs); init(); }
    public H2_Dresser_OutfitView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); init(); }

    private void init() {
        setClipChildren(false);

        if (btnRotate != null) {
            btnRotate.setVisibility(GONE);
            btnRotate.setOnClickListener(v -> resetSelectedRotation());
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // 1. Get the width defined by the layout
        int width = MeasureSpec.getSize(widthMeasureSpec);

        // 2. Create a new height specification that exactly matches the width
        int heightSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);

        // 3. Apply the square dimensions (Width x Width)
        super.onMeasure(widthMeasureSpec, heightSpec);
    }


    public ImageView addImage(Bitmap bitmap) {
        ImageView iv = new ImageView(getContext());
        iv.setImageBitmap(bitmap);
        iv.setScaleType(ImageView.ScaleType.MATRIX); // Important for Matrix transformations

        // Default size logic
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                500, // Default width (adjust as needed)
                500  // Default height
        );
        params.gravity = Gravity.CENTER;
        iv.setLayoutParams(params);

        // Add touch listener for moving
        iv.setOnTouchListener(new MultiTouchListener());

        addView(iv);

        // Automatically select the new image
        setSelectedView(iv);

        return iv;
    }

    public void rotateSelected(float degrees) {
        if (selectedView == null) return;

        Matrix m = new Matrix(selectedView.getImageMatrix());
        // Rotate around the center of the view
        m.postRotate(degrees, selectedView.getWidth() / 2f, selectedView.getHeight() / 2f);
        selectedView.setImageMatrix(m);
        selectedView.invalidate();
    }

    public void resetSelectedRotation() {
        if (selectedView == null) return;

        // Reset to identity matrix (original state)
        Matrix reset = new Matrix();
        // Optional: If you want to keep position but reset rotation, you need more complex logic.
        // For now, this resets everything to center/default.
        selectedView.setImageMatrix(reset);
        selectedView.invalidate();
    }

    public void setSelectedView(@Nullable ImageView view) {
        this.selectedView = view;

        if (listener != null && view != null) {
            listener.onSelectionChanged(view, getCurrentScale(view));
        }

        if (btnRotate != null) {
            btnRotate.setVisibility(view != null ? VISIBLE : GONE);
        }
    }

    private float getCurrentScale(ImageView iv) {
        float[] values = new float[9];
        iv.getImageMatrix().getValues(values);
        return values[Matrix.MSCALE_X];
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener l) {
        this.listener = l;
    }

    public interface OnSelectionChangedListener {
        void onSelectionChanged(ImageView selected, float currentScale);
    }

    public Bitmap exportToBitmap() {
        // Create a bitmap of the specific view size
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        draw(canvas);
        return bitmap;
    }

    /**
     * Inner class to handle touch events (Drag, Zoom, Rotate logic can be added here)
     * Currently implements Drag (Move).
     */
    private class MultiTouchListener implements OnTouchListener {
        private float lastX, lastY;
        private int mode = 0;
        private static final int DRAG = 1;

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            ImageView view = (ImageView) v;

            // Select this view when touched
            if (selectedView != view) {
                setSelectedView(view);
            }

            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    lastX = event.getRawX();
                    lastY = event.getRawY();
                    mode = DRAG;
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (mode == DRAG) {
                        float dx = event.getRawX() - lastX;
                        float dy = event.getRawY() - lastY;

                        Matrix matrix = new Matrix(view.getImageMatrix());
                        matrix.postTranslate(dx, dy);
                        view.setImageMatrix(matrix);
                        view.invalidate();

                        lastX = event.getRawX();
                        lastY = event.getRawY();
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    mode = 0;
                    break;
            }
            return true;
        }
    }
}

package com.example.project;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.graphics.Matrix;

import androidx.annotation.Nullable;

public class H2_Dresser_OutfitView extends FrameLayout {

    private ImageView selectedView;
    private OnSelectionChangedListener listener;
    public ImageButton btnRotate; // your existing rotate/reset button

    public H2_Dresser_OutfitView(Context context) { super(context); init(); }
    public H2_Dresser_OutfitView(Context context, @Nullable AttributeSet attrs) { super(context, attrs); init(); }
    public H2_Dresser_OutfitView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); init(); }

    private void init() {
        setClipChildren(false);

        // use your btnRotate from layout
        // it must be already added in your layout xml or dynamically
        // initially hide it
        if (btnRotate != null) {
            btnRotate.setVisibility(GONE);
            btnRotate.setOnClickListener(v -> resetSelectedRotation());
        }
    }

    public ImageView addImage(Uri uri) {
        ImageView iv = new ImageView(getContext());
        iv.setImageURI(uri);
        iv.setScaleType(ImageView.ScaleType.MATRIX);

        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.leftMargin = 100;
        lp.topMargin = 100;
        addView(iv, lp);

        // Start small
        Matrix startMatrix = new Matrix();
        startMatrix.setScale(0.3f, 0.3f, 0, 0);
        iv.setImageMatrix(startMatrix);

        // Touch handler for drag/zoom
        new H3_Dresser_TouchHandler(iv, selected -> setSelectedView(selected));

        setSelectedView(iv);
        return iv;
    }

    public void rotateSelected(float degrees) {
        if (selectedView == null) return;

        Matrix m = new Matrix(selectedView.getImageMatrix());
        m.postRotate(degrees, selectedView.getWidth() / 2f, selectedView.getHeight() / 2f);
        selectedView.setImageMatrix(m);
    }

    public void resetSelectedRotation() {
        if (selectedView == null) return;

        // Get current matrix values
        float[] values = new float[9];
        selectedView.getImageMatrix().getValues(values);

        float scaleX = values[Matrix.MSCALE_X];
        float scaleY = values[Matrix.MSCALE_Y];
        float transX = values[Matrix.MTRANS_X];
        float transY = values[Matrix.MTRANS_Y];

        // Build a new matrix without rotation
        Matrix reset = new Matrix();
        reset.setScale(scaleX, scaleY);
        reset.postTranslate(transX, transY);

        selectedView.setImageMatrix(reset);
    }


    public void setSelectedView(@Nullable ImageView view) {
        this.selectedView = view;

        if (listener != null && view != null) {
            listener.onSelectionChanged(view, getCurrentScale(view));
        }

        // Show or hide btnRotate based on selection
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
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        draw(canvas); // draw the whole outfitView (background + images)
        return bitmap;
    }

}

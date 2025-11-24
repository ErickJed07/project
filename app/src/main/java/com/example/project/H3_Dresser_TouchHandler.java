package com.example.project;

import android.graphics.Matrix;
import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

public class H3_Dresser_TouchHandler implements View.OnTouchListener {

    private final ImageView target;
    private final OnSelectListener selectListener;

    private final Matrix matrix = new Matrix();
    private final Matrix savedMatrix = new Matrix();

    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;

    private int mode = NONE;
    private final PointF start = new PointF();
    private final PointF mid = new PointF();
    private float oldDist = 1f;

    public H3_Dresser_TouchHandler(ImageView target, OnSelectListener listener) {
        this.target = target;
        this.selectListener = listener;
        target.setOnTouchListener(this);
    }

    private float oldRotation = 0f;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        ImageView view = (ImageView) v;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                matrix.set(view.getImageMatrix());
                savedMatrix.set(matrix);
                start.set(event.getX(), event.getY());
                mode = DRAG;
                selectListener.onSelect(view);
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                oldDist = spacing(event);
                oldRotation = rotation(event);
                if (oldDist > 10f) {
                    savedMatrix.set(matrix);
                    midPoint(mid, event);
                    mode = ZOOM;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (mode == DRAG) {
                    matrix.set(savedMatrix);
                    float dx = event.getX() - start.x;
                    float dy = event.getY() - start.y;
                    matrix.postTranslate(dx, dy);
                } else if (mode == ZOOM && event.getPointerCount() >= 2) {
                    matrix.set(savedMatrix);

                    // Zoom
                    float newDist = spacing(event);
                    if (newDist > 10f) {
                        float scale = newDist / oldDist;
                        matrix.postScale(scale, scale, mid.x, mid.y);
                    }

                    // Rotation
                    float newRotation = rotation(event);
                    float deltaAngle = (newRotation - oldRotation) * 0.2f; // slower rotation
                    matrix.postRotate(deltaAngle, mid.x, mid.y);

                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                break;
        }

        view.setImageMatrix(matrix);
        return true;
    }

    // Helper: angle between two fingers
    private float rotation(MotionEvent event) {
        if (event.getPointerCount() < 2) return 0f;
        double deltaX = (event.getX(0) - event.getX(1));
        double deltaY = (event.getY(0) - event.getY(1));
        return (float) Math.toDegrees(Math.atan2(deltaY, deltaX));
    }


    private float spacing(MotionEvent event) {
        if (event.getPointerCount() < 2) return 1f;
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private void midPoint(PointF point, MotionEvent event) {
        if (event.getPointerCount() < 2) return;
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    public interface OnSelectListener {
        void onSelect(ImageView selected);
    }
}

package com.example.project;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.util.Stack;

public class F5_Camera_Edit_ToolsView extends View {

    public enum Mode { NONE, LASSO }

    private Mode currentMode = Mode.NONE;

    private Bitmap originalBitmap;
    private Bitmap workingBitmap;
    private Canvas workingCanvas;

    private Path lassoPath = new Path();
    private Paint lassoPaint;

    private final Stack<Bitmap> undoStack = new Stack<>();
    private final Stack<Bitmap> redoStack = new Stack<>();
    private static final int MAX_HISTORY = 10;

    // Zoom & Pan
    private ScaleGestureDetector scaleDetector;
    private float scaleFactor = 1.f;
    private float focusX, focusY;
    private float offsetX = 0, offsetY = 0;
    private float lastTouchX, lastTouchY;
    private boolean isPanning = false;

    // The invert flag: false = delete inside, true = delete outside
    private boolean invertDelete = false;

    public F5_Camera_Edit_ToolsView(Context context) {
        super(context);
        init(context);
    }

    public F5_Camera_Edit_ToolsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public F5_Camera_Edit_ToolsView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    // F5_Camera_Edit_ToolsView.java  (ADD THIS METHOD ANYWHERE IN THE CLASS)
    public Bitmap getCurrentBitmapCopy() {
        if (workingBitmap == null) return null;
        return workingBitmap.copy(Bitmap.Config.ARGB_8888, false);
    }


    private void init(Context context) {
        lassoPaint = new Paint();
        lassoPaint.setColor(Color.RED);
        lassoPaint.setStyle(Paint.Style.STROKE);
        lassoPaint.setStrokeWidth(3);
        lassoPaint.setAntiAlias(true);

        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    public void setImageBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            originalBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            workingBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            workingCanvas = new Canvas(workingBitmap);
            clearHistory();
            invalidate();
        }
    }

    public void setMode(Mode mode) {
        currentMode = mode;
        lassoPath.reset();
        invalidate();
    }

    public Mode getMode() {
        return currentMode;
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public void resetZoom() {
        scaleFactor = 1f;
        offsetX = 0f;
        offsetY = 0f;
        invalidate();
    }

    private void clearHistory() {
        undoStack.clear();
        redoStack.clear();
    }

    private void pushUndo() {
        if (workingBitmap == null) return;

        if (!undoStack.isEmpty() && workingBitmap.sameAs(undoStack.peek())) {
            return;
        }

        if (undoStack.size() >= MAX_HISTORY) {
            undoStack.remove(0);
        }
        undoStack.push(workingBitmap.copy(Bitmap.Config.ARGB_8888, true));
        redoStack.clear();
    }

    public void undo() {
        if (undoStack.isEmpty()) return;

        if (workingBitmap != null) {
            if (redoStack.size() >= MAX_HISTORY) {
                redoStack.remove(0);
            }
            redoStack.push(workingBitmap.copy(Bitmap.Config.ARGB_8888, true));
        }

        workingBitmap = undoStack.pop();
        workingCanvas = new Canvas(workingBitmap);
        invalidate();
    }

    public void redo() {
        if (redoStack.isEmpty()) return;

        if (workingBitmap != null) {
            if (undoStack.size() >= MAX_HISTORY) {
                undoStack.remove(0);
            }
            undoStack.push(workingBitmap.copy(Bitmap.Config.ARGB_8888, true));
        }

        workingBitmap = redoStack.pop();
        workingCanvas = new Canvas(workingBitmap);
        invalidate();
    }

    public void restoreOriginal() {
        if (originalBitmap != null) {
            pushUndo();
            workingBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
            workingCanvas = new Canvas(workingBitmap);
            invalidate();
        }
    }

    // Setter for invert flag
    public void setInvertDelete(boolean invert) {
        invertDelete = invert;
    }

    // Getter for invert flag
    public boolean isInvertDelete() {
        return invertDelete;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (workingBitmap != null) {
            canvas.save();
            canvas.translate(offsetX, offsetY);
            canvas.scale(scaleFactor, scaleFactor, focusX, focusY);
            canvas.drawBitmap(workingBitmap, 0, 0, null);
            canvas.restore();
        }

        if (currentMode == Mode.LASSO && !lassoPath.isEmpty()) {
            canvas.save();
            canvas.translate(offsetX, offsetY);
            canvas.scale(scaleFactor, scaleFactor, focusX, focusY);
            canvas.drawPath(lassoPath, lassoPaint);
            canvas.restore();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);

        float touchX = event.getX();
        float touchY = event.getY();

        float xInBitmap = (touchX - offsetX) / scaleFactor;
        float yInBitmap = (touchY - offsetY) / scaleFactor;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (currentMode == Mode.LASSO) {
                    lassoPath.moveTo(xInBitmap, yInBitmap);
                    isPanning = false;
                } else {
                    lastTouchX = touchX;
                    lastTouchY = touchY;
                    isPanning = true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (currentMode == Mode.LASSO && !isPanning) {
                    lassoPath.lineTo(xInBitmap, yInBitmap);
                } else if (isPanning && currentMode != Mode.LASSO) {
                    offsetX += touchX - lastTouchX;
                    offsetY += touchY - lastTouchY;
                    lastTouchX = touchX;
                    lastTouchY = touchY;
                }
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                if (currentMode == Mode.LASSO && !isPanning) {
                    lassoPath.close();
                    pushUndo();

                    // Delete inside or outside based on invertDelete flag
                    deleteLassoArea(invertDelete);

                    invalidate();
                }
                isPanning = false;
                break;
        }
        return true;
    }

    private Region pathToRegion(Path path, int width, int height) {
        Region region = new Region();
        RectF bounds = new RectF();
        path.computeBounds(bounds, true);
        Region clip = new Region(0, 0, width, height);
        region.setPath(path, clip);
        return region;
    }

    // Unified delete method based on invert flag
    public void deleteLassoArea(boolean invert) {
        if (workingBitmap == null || lassoPath.isEmpty()) return;

        pushUndo();

        Region lassoRegion = pathToRegion(lassoPath, workingBitmap.getWidth(), workingBitmap.getHeight());

        int width = workingBitmap.getWidth();
        int height = workingBitmap.getHeight();
        int[] pixels = new int[width * height];
        workingBitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                boolean contains = lassoRegion.contains(x, y);
                if (invert) {
                    if (!contains) {
                        pixels[index] = Color.TRANSPARENT;
                    }
                } else {
                    if (contains) {
                        pixels[index] = Color.TRANSPARENT;
                    }
                }
            }
        }

        workingBitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        lassoPath.reset();
        invalidate();
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (currentMode == Mode.LASSO) {
                return false;
            }
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(0.5f, Math.min(scaleFactor, 5.0f));
            focusX = detector.getFocusX();
            focusY = detector.getFocusY();
            invalidate();
            return true;
        }
    }
}

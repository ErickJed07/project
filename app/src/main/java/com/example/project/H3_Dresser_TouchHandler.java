package com.example.project;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

public class H3_Dresser_TouchHandler implements View.OnTouchListener {

    private final OnSelectListener selectListener;
    private final GestureDetector gestureDetector;

    // Dragging state
    private float dX, dY;
    private boolean isDragging = false;
    private boolean isLocked = false; // New state for locking

    public H3_Dresser_TouchHandler(ImageView target, OnSelectListener listener) {
        this.selectListener = listener;
        target.setOnTouchListener(this);

        // Initialize Gesture Detector for Long Press
        gestureDetector = new GestureDetector(target.getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent e) {
                // Trigger the Lock/Unlock menu on long press
                if (selectListener != null) {
                    selectListener.onLongClick((ImageView) target, isLocked, H3_Dresser_TouchHandler.this);
                }
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                target.performClick();
                return true;
            }
        });
    }

    public void setLocked(boolean locked) {
        this.isLocked = locked;
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        // 1. Let Gesture Detector handle Long Press first
        if (gestureDetector.onTouchEvent(event)) {
            return true;
        }

        // 2. If Locked, prevent dragging but consume event so it doesn't pass through
        if (isLocked) {
            return true;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                dX = view.getX() - event.getRawX();
                dY = view.getY() - event.getRawY();
                isDragging = false;

                view.bringToFront();
                if (selectListener != null) {
                    selectListener.onSelect((ImageView) view);
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                view.animate()
                        .x(event.getRawX() + dX)
                        .y(event.getRawY() + dY)
                        .setDuration(0)
                        .start();
                isDragging = true;
                return true;

            case MotionEvent.ACTION_UP:
                return true;

            default:
                return false;
        }
    }

    public interface OnSelectListener {
        void onSelect(ImageView selected);
        // New method to handle long clicks
        void onLongClick(ImageView view, boolean isCurrentlyLocked, H3_Dresser_TouchHandler handler);
    }
}

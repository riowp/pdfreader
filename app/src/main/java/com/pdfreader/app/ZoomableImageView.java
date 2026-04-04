package com.pdfreader.app;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import androidx.appcompat.widget.AppCompatImageView;

public class ZoomableImageView extends AppCompatImageView {

    private static final float MIN_ZOOM = 1.0f;
    private static final float MAX_ZOOM = 5.0f;

    private Matrix matrix = new Matrix();
    private float[] matrixValues = new float[9];

    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;

    private float currentScale = 1f;
    private float lastX, lastY;
    private boolean isDragging = false;
    private int pointerCount = 0;

    private OnTapListener tapListener;

    public interface OnTapListener {
        void onTap();
    }

    public ZoomableImageView(Context context) {
        super(context);
        init(context);
    }

    public ZoomableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ZoomableImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        setScaleType(ScaleType.MATRIX);

        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float scaleFactor = detector.getScaleFactor();
                float newScale = currentScale * scaleFactor;
                newScale = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, newScale));
                float actualScale = newScale / currentScale;

                float focusX = detector.getFocusX();
                float focusY = detector.getFocusY();

                matrix.postScale(actualScale, actualScale, focusX, focusY);
                currentScale = newScale;
                clampMatrix();
                setImageMatrix(matrix);
                return true;
            }
        });

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (currentScale > 1.5f) {
                    // Reset to fit
                    resetZoom();
                } else {
                    // Zoom in to 2.5x at tap point
                    float scale = 2.5f / currentScale;
                    matrix.postScale(scale, scale, e.getX(), e.getY());
                    currentScale = 2.5f;
                    clampMatrix();
                    setImageMatrix(matrix);
                }
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (tapListener != null) tapListener.onTap();
                return true;
            }
        });
    }

    public void setOnTapListener(OnTapListener listener) {
        this.tapListener = listener;
    }

    public void resetZoom() {
        matrix.reset();
        currentScale = 1f;
        setScaleType(ScaleType.FIT_CENTER);
        setScaleType(ScaleType.MATRIX);
        // Re-fit image
        fitImageToView();
        setImageMatrix(matrix);
    }

    public void zoomIn() {
        float newScale = Math.min(currentScale + 0.5f, MAX_ZOOM);
        float scale = newScale / currentScale;
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        matrix.postScale(scale, scale, cx, cy);
        currentScale = newScale;
        clampMatrix();
        setImageMatrix(matrix);
    }

    public void zoomOut() {
        float newScale = Math.max(currentScale - 0.5f, MIN_ZOOM);
        float scale = newScale / currentScale;
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        matrix.postScale(scale, scale, cx, cy);
        currentScale = newScale;
        clampMatrix();
        setImageMatrix(matrix);
    }

    private void fitImageToView() {
        if (getDrawable() == null) return;
        int dw = getDrawable().getIntrinsicWidth();
        int dh = getDrawable().getIntrinsicHeight();
        int vw = getWidth();
        int vh = getHeight();
        if (dw <= 0 || dh <= 0 || vw <= 0 || vh <= 0) return;

        float scale = Math.min((float) vw / dw, (float) vh / dh);
        float dx = (vw - dw * scale) / 2f;
        float dy = (vh - dh * scale) / 2f;
        matrix.reset();
        matrix.postScale(scale, scale);
        matrix.postTranslate(dx, dy);
        currentScale = scale;
    }

    private void clampMatrix() {
        if (getDrawable() == null) return;
        matrix.getValues(matrixValues);
        float transX = matrixValues[Matrix.MTRANS_X];
        float transY = matrixValues[Matrix.MTRANS_Y];
        float scaleX = matrixValues[Matrix.MSCALE_X];

        int dw = getDrawable().getIntrinsicWidth();
        int dh = getDrawable().getIntrinsicHeight();
        int vw = getWidth();
        int vh = getHeight();

        float scaledW = dw * scaleX;
        float scaledH = dh * scaleX;

        float fixX = 0, fixY = 0;

        if (scaledW <= vw) {
            fixX = (vw - scaledW) / 2f - transX;
        } else {
            if (transX > 0) fixX = -transX;
            else if (transX < vw - scaledW) fixX = (vw - scaledW) - transX;
        }

        if (scaledH <= vh) {
            fixY = (vh - scaledH) / 2f - transY;
        } else {
            if (transY > 0) fixY = -transY;
            else if (transY < vh - scaledH) fixY = (vh - scaledH) - transY;
        }

        matrix.postTranslate(fixX, fixY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);

        pointerCount = event.getPointerCount();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = event.getX();
                lastY = event.getY();
                isDragging = false;
                break;

            case MotionEvent.ACTION_MOVE:
                if (pointerCount == 1 && !scaleDetector.isInProgress()) {
                    float dx = event.getX() - lastX;
                    float dy = event.getY() - lastY;
                    if (Math.abs(dx) > 5 || Math.abs(dy) > 5) {
                        isDragging = true;
                    }
                    if (isDragging && currentScale > 1.01f) {
                        matrix.postTranslate(dx, dy);
                        clampMatrix();
                        setImageMatrix(matrix);
                    }
                    lastX = event.getX();
                    lastY = event.getY();
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                break;
        }

        return true;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (getDrawable() != null && currentScale == 1f) {
            fitImageToView();
            setImageMatrix(matrix);
        }
    }

    @Override
    public void setImageBitmap(android.graphics.Bitmap bm) {
        super.setImageBitmap(bm);
        // Reset and fit after new bitmap
        post(() -> {
            matrix.reset();
            currentScale = 1f;
            fitImageToView();
            setImageMatrix(matrix);
        });
    }
}

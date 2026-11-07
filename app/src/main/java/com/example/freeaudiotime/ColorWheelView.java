package com.example.freeaudiotime;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.SweepGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/** Circular HSV color selector: angle controls hue and radius controls saturation. */
public class ColorWheelView extends View {
    public interface OnColorChangedListener {
        void onColorChanged(int color);
    }

    private final Paint wheelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fadePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int[] wheelColors = {
            Color.RED, Color.MAGENTA, Color.BLUE, Color.CYAN,
            Color.GREEN, Color.YELLOW, Color.RED
    };

    private float hue;
    private float saturation = 1f;
    private float brightness = 1f;
    private float centerX;
    private float centerY;
    private float radius;
    private int selectedColor = Color.RED;
    private OnColorChangedListener listener;

    public ColorWheelView(Context context) {
        super(context);
        init();
    }

    public ColorWheelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        selectorPaint.setStyle(Paint.Style.STROKE);
        selectorPaint.setStrokeWidth(dp(3));
        selectorPaint.setColor(Color.WHITE);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void setOnColorChangedListener(OnColorChangedListener listener) {
        this.listener = listener;
    }

    public void setColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hue = hsv[0];
        saturation = hsv[1];
        brightness = hsv[2];
        updateColor(false);
        invalidate();
    }

    public int getColor() {
        return selectedColor;
    }

    public void setBrightness(float value) {
        brightness = Math.max(0.08f, Math.min(1f, value));
        updateColor(true);
        invalidate();
    }

    private void updateColor(boolean notify) {
        selectedColor = Color.HSVToColor(new float[]{hue, saturation, brightness});
        if (notify && listener != null) listener.onColorChanged(selectedColor);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        centerX = getWidth() / 2f;
        centerY = getHeight() / 2f;
        radius = Math.min(centerX, centerY) - dp(14);
        if (radius <= 0) return;

        wheelPaint.setShader(new SweepGradient(centerX, centerY, wheelColors, null));
        canvas.drawCircle(centerX, centerY, radius, wheelPaint);

        fadePaint.setShader(new RadialGradient(
                centerX, centerY, radius,
                new int[]{Color.WHITE, Color.TRANSPARENT},
                new float[]{0f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(centerX, centerY, radius, fadePaint);

        double angle = Math.toRadians(hue);
        float selectorRadius = radius * saturation;
        float selectorX = centerX + (float) Math.cos(angle) * selectorRadius;
        float selectorY = centerY + (float) Math.sin(angle) * selectorRadius;

        selectorPaint.setColor(Color.BLACK);
        canvas.drawCircle(selectorX, selectorY, dp(10), selectorPaint);
        selectorPaint.setColor(Color.WHITE);
        canvas.drawCircle(selectorX, selectorY, dp(7), selectorPaint);
        selectorPaint.setStyle(Paint.Style.FILL);
        selectorPaint.setColor(selectedColor);
        canvas.drawCircle(selectorX, selectorY, dp(5), selectorPaint);
        selectorPaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN
                || event.getAction() == MotionEvent.ACTION_MOVE
                || event.getAction() == MotionEvent.ACTION_UP) {
            float dx = event.getX() - centerX;
            float dy = event.getY() - centerY;
            float distance = (float) Math.hypot(dx, dy);
            saturation = Math.max(0f, Math.min(1f, distance / Math.max(1f, radius)));
            hue = (float) Math.toDegrees(Math.atan2(dy, dx));
            if (hue < 0) hue += 360f;
            updateColor(true);
            invalidate();
            return true;
        }
        return true;
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}

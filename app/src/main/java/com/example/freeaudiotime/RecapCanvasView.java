package com.example.freeaudiotime;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Interactive Canva-like Canvas View for Recap editing.
 * Handles touch gestures:
 * - Single tap to select element
 * - Drag to move selected element
 * - Pinch-to-zoom to resize selected element
 * - Visual feedback with bounding box & handles
 */
public class RecapCanvasView extends View {

    private RecapTemplate template;
    private DynamicRecapData recapData;
    private Bitmap artistImage;
    private int dynamicBgColor = 0;
    private int dynamicAccentColor = 0;

    private String selectedElementId = null;
    private OnElementSelectionListener selectionListener;
    private OnTemplateChangeListener templateChangeListener;

    // Gesture detection
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;

    // Drag tracking
    private float lastTouchX;
    private float lastTouchY;
    private boolean isDragging = false;
    private boolean isScaling = false;

    public interface OnElementSelectionListener {
        void onElementSelected(@Nullable RecapElementModel element);
    }

    public interface OnTemplateChangeListener {
        void onTemplateChanged();
    }

    public RecapCanvasView(Context context) {
        super(context);
        init(context);
    }

    public RecapCanvasView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public RecapCanvasView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                handleTap(e.getX(), e.getY());
                return true;
            }
        });

        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (selectedElementId != null && template != null) {
                    RecapElementModel elem = findElementById(selectedElementId);
                    if (elem != null) {
                        float factor = detector.getScaleFactor();
                        elem.setWidth(Math.max(0.05f, Math.min(1.0f, elem.getWidth() * factor)));
                        elem.setHeight(Math.max(0.02f, Math.min(1.0f, elem.getHeight() * factor)));
                        if (elem.getBinding() != RecapElementBinding.CUSTOM_STATIC_IMAGE
                                && elem.getBinding() != RecapElementBinding.TOP_ARTIST_IMAGE) {
                            elem.setFontSizeSp(Math.max(8f, Math.min(80f, elem.getFontSizeSp() * factor)));
                        }
                        invalidate();
                        notifyChange();
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                isScaling = true;
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                isScaling = false;
            }
        });
    }

    public void setTemplate(RecapTemplate template) {
        this.template = template;
        invalidate();
    }

    public RecapTemplate getTemplate() {
        return template;
    }

    public void setRecapData(DynamicRecapData recapData) {
        this.recapData = recapData;
        invalidate();
    }

    public void setArtistImage(Bitmap artistImage) {
        this.artistImage = artistImage;
        invalidate();
    }

    public void setDynamicColors(int bgColor, int accentColor) {
        this.dynamicBgColor = bgColor;
        this.dynamicAccentColor = accentColor;
        invalidate();
    }

    public void setOnElementSelectionListener(OnElementSelectionListener listener) {
        this.selectionListener = listener;
    }

    public void setOnTemplateChangeListener(OnTemplateChangeListener listener) {
        this.templateChangeListener = listener;
    }

    public void setSelectedElementId(@Nullable String elementId) {
        this.selectedElementId = elementId;
        invalidate();
        if (selectionListener != null) {
            selectionListener.onElementSelected(findElementById(elementId));
        }
    }

    public String getSelectedElementId() {
        return selectedElementId;
    }

    public RecapElementModel getSelectedElement() {
        return findElementById(selectedElementId);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (template != null) {
            RecapCanvasRenderer.render(
                    getContext(),
                    canvas,
                    getWidth(),
                    getHeight(),
                    template,
                    recapData,
                    artistImage,
                    dynamicBgColor,
                    dynamicAccentColor,
                    selectedElementId
            );
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);

        float touchX = event.getX();
        float touchY = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = touchX;
                lastTouchY = touchY;
                if (selectedElementId != null) {
                    RecapElementModel elem = findElementById(selectedElementId);
                    if (elem != null && isPointInsideElement(touchX, touchY, elem)) {
                        isDragging = true;
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (isDragging && !isScaling && selectedElementId != null && template != null) {
                    RecapElementModel elem = findElementById(selectedElementId);
                    if (elem != null && getWidth() > 0 && getHeight() > 0) {
                        float dx = (touchX - lastTouchX) / getWidth();
                        float dy = (touchY - lastTouchY) / getHeight();

                        elem.setX(Math.max(0.0f, Math.min(1.0f - elem.getWidth(), elem.getX() + dx)));
                        elem.setY(Math.max(0.0f, Math.min(1.0f - elem.getHeight(), elem.getY() + dy)));

                        lastTouchX = touchX;
                        lastTouchY = touchY;

                        invalidate();
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isDragging) {
                    isDragging = false;
                    notifyChange();
                }
                break;
        }

        return true;
    }

    private void handleTap(float touchX, float touchY) {
        if (template == null || template.getElements() == null) return;

        List<RecapElementModel> elements = template.getElements();
        RecapElementModel hit = null;

        // Iterate backwards so top-most element gets selected first
        for (int i = elements.size() - 1; i >= 0; i--) {
            RecapElementModel elem = elements.get(i);
            if (!elem.isVisible()) continue;
            if (isPointInsideElement(touchX, touchY, elem)) {
                hit = elem;
                break;
            }
        }

        if (hit != null) {
            setSelectedElementId(hit.getId());
        } else {
            setSelectedElementId(null);
        }
    }

    private boolean isPointInsideElement(float touchX, float touchY, RecapElementModel elem) {
        float x = elem.getX() * getWidth();
        float y = elem.getY() * getHeight();
        float w = elem.getWidth() * getWidth();
        float h = elem.getHeight() * getHeight();
        // Give a generous touch hit area (minimum 36px padding)
        float pad = 24f;
        return touchX >= x - pad && touchX <= x + w + pad && touchY >= y - pad && touchY <= y + h + pad;
    }

    public RecapElementModel findElementById(String id) {
        if (id == null || template == null || template.getElements() == null) return null;
        for (RecapElementModel elem : template.getElements()) {
            if (id.equals(elem.getId())) return elem;
        }
        return null;
    }

    private void notifyChange() {
        if (templateChangeListener != null) {
            templateChangeListener.onTemplateChanged();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int availableWidth = MeasureSpec.getSize(widthMeasureSpec);
        int availableHeight = MeasureSpec.getSize(heightMeasureSpec);

        float aspectRatio = 9f / 16f;

        int width = availableWidth;
        int height = (int) (width / aspectRatio);

        if (height > availableHeight) {
            height = availableHeight;
            width = (int) (height * aspectRatio);
        }

        setMeasuredDimension(width, height);
    }
}

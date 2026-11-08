package com.example.freeaudiotime;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Universal Canvas Renderer for Recap Templates.
 * Given a template, dynamic recap data, and artist bitmap, renders pixel-perfect onto any Canvas or generates a Bitmap.
 * Supports:
 * - Dynamic color extraction (reusing the artist palette or template colors).
 * - Background styles: Solid, Linear Gradient, Radial Gradient, Dot Pattern, Grid Pattern.
 * - Element rotation, custom images, stickers, and typography.
 * - Hideable or replaceable missing elements.
 * - Selection highlight / bounding box for interactive editor.
 */
public class RecapCanvasRenderer {

    public static Bitmap renderToBitmap(
            Context context,
            RecapTemplate template,
            DynamicRecapData data,
            Bitmap artistImage,
            int targetWidth,
            int targetHeight,
            int dynamicBackgroundColor,
            int dynamicAccentColor
    ) {
        if (targetWidth <= 0) targetWidth = 1080;
        if (targetHeight <= 0) targetHeight = 1920;

        Bitmap bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        render(context, canvas, targetWidth, targetHeight, template, data, artistImage, dynamicBackgroundColor, dynamicAccentColor, null);
        return bitmap;
    }

    public static void render(
            Context context,
            Canvas canvas,
            int canvasWidth,
            int canvasHeight,
            RecapTemplate template,
            DynamicRecapData data,
            Bitmap artistImage,
            int dynamicBackgroundColor,
            int dynamicAccentColor
    ) {
        render(context, canvas, canvasWidth, canvasHeight, template, data, artistImage, dynamicBackgroundColor, dynamicAccentColor, null);
    }

    public static void render(
            Context context,
            Canvas canvas,
            int canvasWidth,
            int canvasHeight,
            RecapTemplate template,
            DynamicRecapData data,
            Bitmap artistImage,
            int dynamicBackgroundColor,
            int dynamicAccentColor,
            String selectedElementId
    ) {
        if (template == null || canvas == null) return;

        // 1. Resolve Palette Colors
        int bgColor = parseColor(template.getBackgroundColorHex(), Color.rgb(8, 10, 8));
        int bgSecColor = parseColor(template.getBgSecondaryColorHex(), Color.rgb(22, 29, 21));
        int accentColor = parseColor(template.getAccentColorHex(), Color.rgb(183, 255, 0));
        int secBgColor = parseColor(template.getSecondaryBgColorHex(), Color.rgb(17, 21, 17));
        int textColor = parseColor(template.getTextColorHex(), Color.rgb(240, 235, 221));

        if (template.isUseDynamicPalette()) {
            if (dynamicBackgroundColor != 0) {
                bgColor = dynamicBackgroundColor;
            }
            if (dynamicAccentColor != 0) {
                accentColor = dynamicAccentColor;
            }
        }

        // 2. Draw Background according to bgStyle
        drawBackground(canvas, canvasWidth, canvasHeight, template.getBgStyle(), bgColor, bgSecColor, accentColor);

        // 3. Render Elements sorted by zIndex
        List<RecapElementModel> elements = template.getElements();
        if (elements == null || elements.isEmpty()) return;

        for (RecapElementModel elem : elements) {
            if (!elem.isVisible()) continue;

            float elemX = elem.getX() * canvasWidth;
            float elemY = elem.getY() * canvasHeight;
            float elemW = elem.getWidth() * canvasWidth;
            float elemH = elem.getHeight() * canvasHeight;

            canvas.save();
            if (elem.getRotation() != 0) {
                canvas.rotate(elem.getRotation(), elemX + elemW / 2f, elemY + elemH / 2f);
            }

            // Draw element background if specified
            if (elem.getBackgroundColorHex() != null && !elem.getBackgroundColorHex().isEmpty()) {
                int elemBg = parseColor(elem.getBackgroundColorHex(), 0);
                if (elemBg != 0) {
                    Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    boxPaint.setColor(elemBg);
                    if (elem.getOpacity() < 1.0f) {
                        boxPaint.setAlpha((int) (elem.getOpacity() * 255));
                    }
                    canvas.drawRect(elemX, elemY, elemX + elemW, elemY + elemH, boxPaint);
                }
            }

            RecapElementBinding binding = elem.getBinding();
            if (binding != null) {
                switch (binding) {
                    case TOP_ARTIST_IMAGE:
                        renderArtistImage(canvas, elemX, elemY, elemW, elemH, artistImage);
                        break;

                    case SECONDARY_ARTISTS_LIST:
                        renderSecondaryArtists(canvas, elemX, elemY, elemW, elemH, data, elem, accentColor, textColor);
                        break;

                    case TOP_TRACKS_LIST:
                        renderTopTracks(context, canvas, elemX, elemY, elemW, elemH, data, elem, accentColor, textColor);
                        break;

                    case ACHIEVEMENTS_LIST:
                        renderAchievements(canvas, elemX, elemY, elemW, elemH, data, elem, accentColor, textColor);
                        break;

                    case CUSTOM_STATIC_IMAGE:
                        renderCustomImageOrSticker(context, canvas, elemX, elemY, elemW, elemH, elem);
                        break;

                    default:
                        renderTextElement(context, canvas, elemX, elemY, elemW, elemH, elem, data, accentColor, textColor);
                        break;
                }
            }

            // Draw selection border if selected in editor
            if (selectedElementId != null && selectedElementId.equals(elem.getId())) {
                drawSelectionBorder(canvas, elemX, elemY, elemW, elemH, accentColor);
            }

            canvas.restore();
        }
    }

    private static void drawBackground(Canvas canvas, int width, int height, String style, int color1, int color2, int accent) {
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        if ("GRADIENT_LINEAR".equalsIgnoreCase(style)) {
            Shader shader = new LinearGradient(0, 0, 0, height, color1, color2, Shader.TileMode.CLAMP);
            bgPaint.setShader(shader);
            canvas.drawRect(0, 0, width, height, bgPaint);
        } else if ("GRADIENT_RADIAL".equalsIgnoreCase(style)) {
            float radius = (float) Math.hypot(width / 2f, height / 2f);
            Shader shader = new RadialGradient(width / 2f, height / 2f, radius, color2, color1, Shader.TileMode.CLAMP);
            bgPaint.setShader(shader);
            canvas.drawRect(0, 0, width, height, bgPaint);
        } else if ("PATTERN_DOTS".equalsIgnoreCase(style)) {
            bgPaint.setColor(color1);
            canvas.drawRect(0, 0, width, height, bgPaint);

            Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            dotPaint.setColor(color2);
            dotPaint.setAlpha(120);
            int step = (int) (width / 25f);
            float radius = width / 120f;
            for (int x = step / 2; x < width; x += step) {
                for (int y = step / 2; y < height; y += step) {
                    canvas.drawCircle(x, y, radius, dotPaint);
                }
            }
        } else if ("PATTERN_GRID".equalsIgnoreCase(style)) {
            bgPaint.setColor(color1);
            canvas.drawRect(0, 0, width, height, bgPaint);

            Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            gridPaint.setColor(color2);
            gridPaint.setStrokeWidth(1.5f);
            gridPaint.setAlpha(90);
            int step = (int) (width / 16f);
            for (int x = 0; x <= width; x += step) {
                canvas.drawLine(x, 0, x, height, gridPaint);
            }
            for (int y = 0; y <= height; y += step) {
                canvas.drawLine(0, y, width, y, gridPaint);
            }
        } else {
            // Default SOLID
            bgPaint.setColor(color1);
            canvas.drawRect(0, 0, width, height, bgPaint);
        }
    }

    private static void drawSelectionBorder(Canvas canvas, float x, float y, float w, float h, int accentColor) {
        Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setColor(accentColor);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(4f);
        canvas.drawRect(x - 2, y - 2, x + w + 2, y + h + 2, strokePaint);

        // Draw Corner Handles
        Paint handlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        handlePaint.setColor(Color.WHITE);
        handlePaint.setStyle(Paint.Style.FILL);
        float handleR = 8f;
        canvas.drawCircle(x, y, handleR, handlePaint);
        canvas.drawCircle(x + w, y, handleR, handlePaint);
        canvas.drawCircle(x, y + h, handleR, handlePaint);
        canvas.drawCircle(x + w, y + h, handleR, handlePaint);

        handlePaint.setColor(accentColor);
        handlePaint.setStyle(Paint.Style.STROKE);
        handlePaint.setStrokeWidth(2f);
        canvas.drawCircle(x, y, handleR, handlePaint);
        canvas.drawCircle(x + w, y, handleR, handlePaint);
        canvas.drawCircle(x, y + h, handleR, handlePaint);
        canvas.drawCircle(x + w, y + h, handleR, handlePaint);
    }

    private static void renderCustomImageOrSticker(Context context, Canvas canvas, float x, float y, float w, float h, RecapElementModel elem) {
        RectF destRect = new RectF(x, y, x + w, y + h);

        // Check if sticker
        if (elem.getStickerType() != null && !elem.getStickerType().isEmpty()) {
            renderSticker(canvas, destRect, elem.getStickerType(), elem.getFontColorHex());
            return;
        }

        // Custom image from storage
        if (elem.getImagePath() != null && !elem.getImagePath().isEmpty()) {
            File imgFile = new File(elem.getImagePath());
            if (imgFile.exists()) {
                Bitmap bmp = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                if (bmp != null) {
                    Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
                    if (elem.getOpacity() < 1.0f) {
                        p.setAlpha((int) (elem.getOpacity() * 255));
                    }
                    canvas.drawBitmap(bmp, null, destRect, p);
                    return;
                }
            }
        }

        // Fallback placeholder box
        Paint placeholderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        placeholderPaint.setColor(Color.rgb(45, 50, 42));
        canvas.drawRect(destRect, placeholderPaint);
    }

    private static void renderSticker(Canvas canvas, RectF destRect, String stickerType, String colorHex) {
        drawStickerPreview(canvas, destRect, stickerType, colorHex);
    }

    /** Draws a platform-independent vector sticker for both the canvas and picker preview. */
    public static void drawStickerPreview(Canvas canvas, RectF bounds, String stickerType, String colorHex) {
        if (canvas == null || bounds == null || bounds.width() <= 0 || bounds.height() <= 0) return;

        int accent = parseColor(colorHex, Color.rgb(183, 255, 0));
        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        stroke.setStrokeWidth(7f);
        stroke.setColor(accent);

        canvas.save();
        canvas.translate(bounds.left, bounds.top);
        canvas.scale(bounds.width() / 100f, bounds.height() / 100f);

        String type = stickerType == null ? "" : stickerType.toUpperCase(java.util.Locale.ROOT);
        Path path = new Path();

        switch (type) {
            case "DISCO":
                fill.setColor(Color.rgb(24, 29, 26));
                canvas.drawCircle(50, 50, 44, fill);
                stroke.setColor(accent);
                stroke.setStrokeWidth(4f);
                canvas.drawCircle(50, 50, 40, stroke);
                canvas.drawCircle(50, 50, 27, stroke);
                canvas.drawCircle(50, 50, 13, stroke);
                fill.setColor(accent);
                canvas.drawCircle(50, 50, 5, fill);
                break;
            case "NOTA":
            case "MUSIC_NOTE":
                path.moveTo(58, 18); path.lineTo(58, 68);
                path.lineTo(38, 73); path.lineTo(38, 35);
                path.lineTo(75, 27); path.lineTo(75, 62);
                canvas.drawPath(path, stroke);
                fill.setColor(accent);
                canvas.drawOval(24, 65, 45, 82, fill);
                canvas.drawOval(61, 58, 82, 75, fill);
                break;
            case "ONDA":
                stroke.setStrokeWidth(8f);
                for (int i = 0; i < 3; i++) {
                    float y = 30 + i * 20;
                    path.reset();
                    path.moveTo(8, y);
                    path.cubicTo(25, y - 18, 25, y + 18, 42, y);
                    path.cubicTo(59, y - 18, 59, y + 18, 76, y);
                    path.cubicTo(84, y - 8, 90, y - 5, 94, y);
                    canvas.drawPath(path, stroke);
                }
                break;
            case "STAR":
            case "SPARKLE":
                for (int i = 0; i < (type.equals("SPARKLE") ? 2 : 1); i++) {
                    float cx = i == 0 ? 50 : 78, cy = i == 0 ? 50 : 24, r = i == 0 ? 42 : 15;
                    path.reset();
                    for (int j = 0; j < 10; j++) {
                        double a = -Math.PI / 2 + j * Math.PI / 5;
                        float rr = (j % 2 == 0) ? r : r * 0.42f;
                        float px = cx + (float) Math.cos(a) * rr;
                        float py = cy + (float) Math.sin(a) * rr;
                        if (j == 0) path.moveTo(px, py); else path.lineTo(px, py);
                    }
                    path.close();
                    fill.setColor(accent);
                    canvas.drawPath(path, fill);
                }
                break;
            case "HEART":
                path.moveTo(50, 84);
                path.cubicTo(42, 77, 8, 54, 8, 32);
                path.cubicTo(8, 8, 37, 4, 50, 25);
                path.cubicTo(63, 4, 92, 8, 92, 32);
                path.cubicTo(92, 54, 58, 77, 50, 84);
                path.close();
                fill.setColor(accent);
                canvas.drawPath(path, fill);
                break;
            case "FIRE":
                path.moveTo(50, 92);
                path.cubicTo(12, 88, 18, 54, 39, 34);
                path.cubicTo(38, 52, 53, 53, 52, 12);
                path.cubicTo(84, 42, 91, 67, 72, 87);
                path.cubicTo(66, 95, 57, 94, 50, 92);
                path.close();
                fill.setColor(accent);
                canvas.drawPath(path, fill);
                fill.setColor(Color.rgb(255, 190, 70));
                path.reset();
                path.moveTo(51, 82); path.cubicTo(33, 71, 44, 57, 55, 45);
                path.cubicTo(57, 62, 70, 67, 62, 81); path.close();
                canvas.drawPath(path, fill);
                break;
            case "TROPHY":
                fill.setColor(accent);
                path.moveTo(25, 15); path.lineTo(75, 15); path.lineTo(70, 48);
                path.cubicTo(67, 65, 58, 70, 50, 70);
                path.cubicTo(42, 70, 33, 65, 30, 48); path.close();
                canvas.drawPath(path, fill);
                stroke.setColor(accent);
                path.reset(); path.moveTo(25, 25); path.cubicTo(5, 22, 8, 52, 34, 55);
                canvas.drawPath(path, stroke);
                path.reset(); path.moveTo(75, 25); path.cubicTo(95, 22, 92, 52, 66, 55);
                canvas.drawPath(path, stroke);
                fill.setColor(Color.rgb(35, 40, 35));
                canvas.drawRect(44, 68, 56, 82, fill);
                canvas.drawRoundRect(30, 82, 70, 94, 5, 5, fill);
                break;
            case "CROWN":
                path.moveTo(10, 30); path.lineTo(30, 48); path.lineTo(42, 15);
                path.lineTo(58, 48); path.lineTo(78, 15); path.lineTo(90, 30);
                path.lineTo(82, 72); path.lineTo(18, 72); path.close();
                fill.setColor(accent);
                canvas.drawPath(path, fill);
                stroke.setColor(Color.rgb(30, 35, 30));
                canvas.drawLine(20, 82, 80, 82, stroke);
                break;
            case "HEADPHONES":
                stroke.setColor(accent);
                stroke.setStrokeWidth(11f);
                canvas.drawArc(18, 12, 82, 82, 180, 180, false, stroke);
                fill.setColor(accent);
                canvas.drawRoundRect(12, 48, 30, 82, 7, 7, fill);
                canvas.drawRoundRect(70, 48, 88, 82, 7, 7, fill);
                break;
            case "CASSETTE":
                fill.setColor(Color.rgb(30, 36, 31));
                canvas.drawRoundRect(8, 18, 92, 82, 8, 8, fill);
                stroke.setColor(accent);
                canvas.drawRoundRect(8, 18, 92, 82, 8, 8, stroke);
                canvas.drawCircle(32, 50, 14, stroke);
                canvas.drawCircle(68, 50, 14, stroke);
                canvas.drawLine(32, 50, 68, 50, stroke);
                canvas.drawLine(24, 72, 76, 72, stroke);
                break;
            case "MIC":
                fill.setColor(accent);
                canvas.drawRoundRect(38, 10, 62, 58, 12, 12, fill);
                stroke.setColor(accent);
                canvas.drawArc(25, 25, 75, 75, 0, 180, false, stroke);
                canvas.drawLine(50, 75, 50, 88, stroke);
                canvas.drawLine(35, 88, 65, 88, stroke);
                break;
            case "EQ":
                fill.setColor(accent);
                canvas.drawRoundRect(12, 48, 25, 88, 5, 5, fill);
                canvas.drawRoundRect(32, 20, 45, 88, 5, 5, fill);
                canvas.drawRoundRect(52, 35, 65, 88, 5, 5, fill);
                canvas.drawRoundRect(72, 8, 85, 88, 5, 5, fill);
                break;
            case "SPEAKER":
                fill.setColor(accent);
                path.moveTo(12, 35); path.lineTo(35, 35); path.lineTo(65, 15);
                path.lineTo(65, 85); path.lineTo(35, 65); path.lineTo(12, 65); path.close();
                canvas.drawPath(path, fill);
                stroke.setColor(accent);
                canvas.drawArc(55, 25, 95, 75, -60, 120, false, stroke);
                canvas.drawArc(55, 10, 110, 90, -60, 120, false, stroke);
                break;
            default:
                fill.setColor(accent);
                canvas.drawCircle(50, 50, 35, fill);
                break;
        }

        canvas.restore();
    }

    private static void renderTextElement(
            Context context,
            Canvas canvas,
            float x, float y, float w, float h,
            RecapElementModel elem,
            DynamicRecapData data,
            int accentColor,
            int defaultTextColor
    ) {
        String displayText;
        if (elem.getBinding() == RecapElementBinding.CUSTOM_STATIC_TEXT) {
            displayText = elem.getCustomText();
        } else {
            displayText = data != null ? data.getValue(elem.getBinding()) : null;
            if (displayText == null || displayText.trim().isEmpty()) {
                if (elem.isHideIfMissing()) {
                    return; // Skip rendering
                }
                displayText = elem.getFallbackReplacementText();
            }
        }

        if (displayText == null || displayText.isEmpty()) return;

        TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        int elemColor = defaultTextColor;
        if (elem.getFontColorHex() != null && !elem.getFontColorHex().isEmpty()) {
            elemColor = parseColor(elem.getFontColorHex(), defaultTextColor);
        } else {
            if (elem.getBinding() == RecapElementBinding.APP_BRAND
                    || elem.getBinding() == RecapElementBinding.PERIOD_RANGE
                    || elem.getBinding() == RecapElementBinding.TOP_ARTIST_TIME
                    || elem.getBinding() == RecapElementBinding.BEST_DAY_TIME
                    || elem.getBinding() == RecapElementBinding.DISCOVERY_LABEL
                    || elem.getBinding() == RecapElementBinding.BEST_DAY_TITLE
                    || elem.getBinding() == RecapElementBinding.ACHIEVEMENTS_TITLE
                    || elem.getBinding() == RecapElementBinding.TOP_TRACKS_TITLE
                    || elem.getBinding() == RecapElementBinding.FOOTER_LINE_1) {
                elemColor = accentColor;
            }
        }
        textPaint.setColor(elemColor);
        if (elem.getOpacity() < 1.0f) {
            textPaint.setAlpha((int) (elem.getOpacity() * 255));
        }
        textPaint.setTextSize(elem.getFontSizeSp() * (canvas.getWidth() / 360f));
        textPaint.setFakeBoldText(elem.isBold());
        if (elem.isItalic()) {
            textPaint.setTextSkewX(-0.25f);
        }

        // Apply the selected system font family while keeping older saved templates valid.
        textPaint.setTypeface(resolveTypeface(context, elem.getFontFamily()));

        Paint.Align align = Paint.Align.LEFT;
        float textX = x;
        if ("CENTER".equalsIgnoreCase(elem.getAlignment())) {
            align = Paint.Align.CENTER;
            textX = x + (w / 2f);
        } else if ("RIGHT".equalsIgnoreCase(elem.getAlignment())) {
            align = Paint.Align.RIGHT;
            textX = x + w;
        }
        textPaint.setTextAlign(align);

        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float baseline = y + (h / 2f) - (fm.ascent + fm.descent) / 2f;

        String fittedText = TextUtils.ellipsize(displayText, textPaint, w, TextUtils.TruncateAt.END).toString();
        canvas.drawText(fittedText, textX, baseline, textPaint);
    }

    private static void renderArtistImage(
            Canvas canvas,
            float x, float y, float w, float h,
            Bitmap artistImage
    ) {
        RectF destRect = new RectF(x, y, x + w, y + h);
        if (artistImage != null && !artistImage.isRecycled()) {
            drawBitmapCenterCrop(canvas, artistImage, destRect);
        } else {
            Paint fallbackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            fallbackPaint.setColor(Color.rgb(37, 40, 35));
            canvas.drawRect(destRect, fallbackPaint);
        }
    }

    private static void drawBitmapCenterCrop(Canvas canvas, Bitmap bitmap, RectF destination) {
        if (bitmap == null || bitmap.isRecycled()
                || destination.width() <= 0 || destination.height() <= 0) return;
        float sourceRatio = (float) bitmap.getWidth() / bitmap.getHeight();
        float destinationRatio = destination.width() / destination.height();
        Rect source;
        if (sourceRatio > destinationRatio) {
            int cropWidth = Math.max(1, (int) (bitmap.getHeight() * destinationRatio));
            int left = (bitmap.getWidth() - cropWidth) / 2;
            source = new Rect(left, 0, left + cropWidth, bitmap.getHeight());
        } else {
            int cropHeight = Math.max(1, (int) (bitmap.getWidth() / destinationRatio));
            int top = (bitmap.getHeight() - cropHeight) / 2;
            source = new Rect(0, top, bitmap.getWidth(), top + cropHeight);
        }
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(bitmap, source, destination, paint);
    }

    private static void renderSecondaryArtists(
            Canvas canvas,
            float x, float y, float w, float h,
            DynamicRecapData data,
            RecapElementModel elem,
            int accentColor,
            int textColor
    ) {
        if (data == null || data.getSecondaryArtists().isEmpty()) {
            if (elem.isHideIfMissing()) return;
        }

        float scale = canvas.getWidth() / 360f;
        TextPaint headerPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        headerPaint.setColor(accentColor);
        headerPaint.setTextSize(11f * scale);
        headerPaint.setFakeBoldText(true);
        canvas.drawText("ALSO IN ROTATION", x + 10 * scale, y + 30 * scale, headerPaint);

        if (data == null) return;
        float rowY = y + 72 * scale;
        TextPaint posPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        posPaint.setColor(accentColor);
        posPaint.setTextSize(14f * scale);
        posPaint.setFakeBoldText(true);

        TextPaint namePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        namePaint.setColor(textColor);
        namePaint.setTextSize(12f * scale);
        namePaint.setFakeBoldText(true);

        for (DynamicRecapData.TopArtistItem artist : data.getSecondaryArtists()) {
            if (rowY + 38 * scale > y + h) break;
            String pos = String.format("%02d", artist.rank);
            canvas.drawText(pos, x + 10 * scale, rowY, posPaint);
            String info = artist.name.toUpperCase() + (artist.timeFormatted.isEmpty() ? "" : " — " + artist.timeFormatted);
            canvas.drawText(info, x + 50 * scale, rowY, namePaint);
            rowY += 48 * scale;
        }
    }

    private static void renderTopTracks(
            Context context,
            Canvas canvas,
            float x, float y, float w, float h,
            DynamicRecapData data,
            RecapElementModel elem,
            int accentColor,
            int textColor
    ) {
        if (data == null || data.getTopTracks().isEmpty()) {
            if (elem.isHideIfMissing()) return;
        }

        float scale = canvas.getWidth() / 360f;
        TextPaint titlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(accentColor);
        titlePaint.setTextSize(13f * scale);
        titlePaint.setFakeBoldText(true);
        canvas.drawText("YOUR TOP TRACKS", x, y + 30 * scale, titlePaint);

        if (data == null) return;
        float itemY = y + 70 * scale;
        float itemHeight = 60f * scale;

        TextPaint trackTitlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        trackTitlePaint.setColor(Color.WHITE);
        trackTitlePaint.setTextSize(11f * scale);
        trackTitlePaint.setFakeBoldText(true);

        TextPaint trackArtistPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        trackArtistPaint.setColor(textColor);
        trackArtistPaint.setTextSize(10f * scale);

        for (DynamicRecapData.TopTrackItem track : data.getTopTracks()) {
            if (itemY + itemHeight > y + h) break;

            float thumbSize = itemHeight - 10 * scale;
            RectF thumbRect = new RectF(x, itemY, x + thumbSize, itemY + thumbSize);
            Bitmap artworkBitmap = null;
            if (track.artworkPath != null && !track.artworkPath.isEmpty() && context != null) {
                File file = new File(track.artworkPath);
                if (!file.isAbsolute()) {
                    file = new File(context.getFilesDir(), track.artworkPath);
                }
                if (file.exists()) {
                    artworkBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                }
            }

            if (artworkBitmap != null) {
                drawBitmapCenterCrop(canvas, artworkBitmap, thumbRect);
                artworkBitmap.recycle();
            } else {
                Paint p = new Paint();
                p.setColor(Color.rgb(30, 30, 30));
                canvas.drawRect(thumbRect, p);
            }

            canvas.drawText(track.title, x + thumbSize + 18 * scale, itemY + 23 * scale, trackTitlePaint);
            canvas.drawText(track.artist + (track.duration.isEmpty() ? "" : " • " + track.duration),
                    x + thumbSize + 18 * scale, itemY + 48 * scale, trackArtistPaint);
            itemY += itemHeight;
        }
    }

    private static void renderAchievements(
            Canvas canvas,
            float x, float y, float w, float h,
            DynamicRecapData data,
            RecapElementModel elem,
            int accentColor,
            int textColor
    ) {
        if (data == null || data.getUnlockedAchievements().isEmpty()) {
            if (elem.isHideIfMissing()) return;
        }

        float scale = canvas.getWidth() / 360f;
        TextPaint titlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(accentColor);
        titlePaint.setTextSize(12f * scale);
        titlePaint.setFakeBoldText(true);
        canvas.drawText("UNLOCKED ACHIEVEMENTS", x, y + 30 * scale, titlePaint);

        if (data == null) return;
        float itemY = y + 72 * scale;
        TextPaint achPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        achPaint.setColor(textColor);
        achPaint.setTextSize(11f * scale);

        for (DynamicRecapData.AchievementItem ach : data.getUnlockedAchievements()) {
            if (itemY + 34 * scale > y + h) break;
            canvas.drawText("🏆 " + ach.title + " (" + ach.formattedDate + ")",
                    x, itemY, achPaint);
            itemY += 46 * scale;
        }
    }

    private static Typeface resolveTypeface(Context context, String family) {
        if (family == null) return Typeface.SANS_SERIF;
        if (family.startsWith("CUSTOM_FONT:") && context != null) {
            File fontFile = new File(context.getFilesDir(), family.substring("CUSTOM_FONT:".length()));
            if (fontFile.isFile()) {
                try { return Typeface.createFromFile(fontFile); }
                catch (RuntimeException ignored) { return Typeface.SANS_SERIF; }
            }
        }
        switch (family.toUpperCase(java.util.Locale.ROOT)) {
            case "SERIF": return Typeface.SERIF;
            case "SERIF_BOLD": return Typeface.create("serif", Typeface.BOLD);
            case "SERIF_ITALIC": return Typeface.create("serif", Typeface.ITALIC);
            case "MONOSPACE": return Typeface.MONOSPACE;
            case "CONDENSED": return Typeface.create("sans-serif-condensed", Typeface.NORMAL);
            case "CONDENSED_BOLD": return Typeface.create("sans-serif-condensed", Typeface.BOLD);
            case "SANS_LIGHT": return Typeface.create("sans-serif-light", Typeface.NORMAL);
            case "SANS_MEDIUM": return Typeface.create("sans-serif-medium", Typeface.NORMAL);
            case "SANS_BLACK": return Typeface.create("sans-serif-black", Typeface.NORMAL);
            case "SANS_EXTRA_BLACK": return Typeface.create("sans-serif-black", Typeface.BOLD);
            case "SANS_ROUNDED": return Typeface.create("sans-serif-rounded", Typeface.NORMAL);
            case "SANS_THIN": return Typeface.create("sans-serif-thin", Typeface.NORMAL);
            case "CURSIVE": return Typeface.create("cursive", Typeface.NORMAL);
            case "CASUAL": return Typeface.create("casual", Typeface.NORMAL);
            case "FANTASY": return Typeface.create("fantasy", Typeface.NORMAL);
            case "SANS_BOLD_ITALIC": return Typeface.create("sans-serif", Typeface.BOLD_ITALIC);
            case "SMALL_CAPS": return Typeface.create("sans-serif-smallcaps", Typeface.NORMAL);
            default: return Typeface.SANS_SERIF;
        }
    }

    private static int parseColor(String hex, int fallback) {
        if (hex == null || hex.trim().isEmpty()) return fallback;
        try {
            return Color.parseColor(hex.trim());
        } catch (Exception e) {
            return fallback;
        }
    }
}

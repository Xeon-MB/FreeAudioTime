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
                        renderTextElement(canvas, elemX, elemY, elemW, elemH, elem, data, accentColor, textColor);
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
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(parseColor(colorHex, Color.rgb(183, 255, 0)));
        p.setTextSize(Math.min(destRect.width(), destRect.height()) * 0.8f);
        p.setTextAlign(Paint.Align.CENTER);

        String emoji = "🎵";
        if ("DISCO".equalsIgnoreCase(stickerType)) emoji = "💿";
        else if ("NOTA".equalsIgnoreCase(stickerType)) emoji = "🎵";
        else if ("ONDA".equalsIgnoreCase(stickerType)) emoji = "🌊";
        else if ("STAR".equalsIgnoreCase(stickerType)) emoji = "⭐";
        else if ("HEART".equalsIgnoreCase(stickerType)) emoji = "❤️";
        else if ("FIRE".equalsIgnoreCase(stickerType)) emoji = "🔥";
        else if ("TROPHY".equalsIgnoreCase(stickerType)) emoji = "🏆";
        else if ("CROWN".equalsIgnoreCase(stickerType)) emoji = "👑";
        else if ("HEADPHONES".equalsIgnoreCase(stickerType)) emoji = "🎧";

        Paint.FontMetrics fm = p.getFontMetrics();
        float baseline = destRect.centerY() - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(emoji, destRect.centerX(), baseline, p);
    }

    private static void renderTextElement(
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

        // Apply Font Family
        if ("SERIF".equalsIgnoreCase(elem.getFontFamily())) {
            textPaint.setTypeface(Typeface.SERIF);
        } else if ("MONOSPACE".equalsIgnoreCase(elem.getFontFamily())) {
            textPaint.setTypeface(Typeface.MONOSPACE);
        } else if ("CONDENSED".equalsIgnoreCase(elem.getFontFamily())) {
            textPaint.setTypeface(Typeface.create("sans-serif-condensed", elem.isBold() ? Typeface.BOLD : Typeface.NORMAL));
        } else {
            textPaint.setTypeface(Typeface.SANS_SERIF);
        }

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

    private static int parseColor(String hex, int fallback) {
        if (hex == null || hex.trim().isEmpty()) return fallback;
        try {
            return Color.parseColor(hex.trim());
        } catch (Exception e) {
            return fallback;
        }
    }
}


package com.example.freeaudiotime;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Map;

public class DailyActivityChart extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Map<String, Long> dailyTotals;

    private final int green;
    private final int lightGreen;
    private final int muted;
    private final int textColor;

    public DailyActivityChart(Context context, AttributeSet attrs) {
        super(context, attrs);

        green = ContextCompat.getColor(context, R.color.card_dark);
        lightGreen = ContextCompat.getColor(context, R.color.divider_color);
        muted = ContextCompat.getColor(context, R.color.text_secondary);
        textColor = ContextCompat.getColor(context, R.color.text_primary);
    }

    public void setData(Map<String, Long> data) {
        dailyTotals = data;
        invalidate();
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (dailyTotals == null) return;

        float width = getWidth();
        float height = getHeight();

        float left = dp(12);
        float right = width - dp(12);
        float top = dp(25);
        float bottom = height - dp(35);

        float chartHeight = bottom - top;
        float slotWidth = (right - left) / 7f;
        float barWidth = slotWidth * 0.48f;

        LocalDate today = LocalDate.now();

        long maxValue = 0;

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            long value = dailyTotals.getOrDefault(date.toString(), 0L);
            maxValue = Math.max(maxValue, value);
        }

        if (maxValue == 0) maxValue = 60;

        // Linha de base
        paint.setColor(lightGreen);
        paint.setStrokeWidth(dp(1));
        canvas.drawLine(left, bottom, right, bottom, paint);

        for (int i = 0; i < 7; i++) {

            LocalDate date = today.minusDays(6 - i);

            long seconds = dailyTotals.getOrDefault(
                    date.toString(), 0L
            );

            float centerX = left + slotWidth * (i + 0.5f);

            float barHeight = (seconds / (float) maxValue)
                    * chartHeight * 0.85f;

            if (seconds > 0) {
                barHeight = Math.max(barHeight, dp(4));
            }

            float barTop = bottom - barHeight;

            // Barra
            paint.setColor(seconds > 0 ? green : lightGreen);

            RectF bar = new RectF(
                    centerX - barWidth / 2,
                    barTop,
                    centerX + barWidth / 2,
                    bottom
            );

            canvas.drawRoundRect(
                    bar,
                    dp(7),
                    dp(7),
                    paint
            );

            // Tempo acima da barra
            if (seconds > 0) {
                paint.setColor(textColor);
                paint.setTextSize(dp(10));
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setFakeBoldText(true);

                String valueText = (seconds / 60) + "m";

                canvas.drawText(
                        valueText,
                        centerX,
                        Math.max(dp(13), barTop - dp(7)),
                        paint
                );
            }

            // Dia da semana
            paint.setColor(muted);
            paint.setTextSize(dp(10));
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(false);

            String day = date.getDayOfWeek()
                    .getDisplayName(
                            TextStyle.SHORT,
                            new Locale("pt", "BR")
                    )
                    .toUpperCase(new Locale("pt", "BR"))
                    .replace(".", "");

            canvas.drawText(
                    day,
                    centerX,
                    height - dp(10),
                    paint
            );
        }
    }
}

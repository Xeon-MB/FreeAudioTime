
package com.example.freeaudiotime;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class DailyActivityChart extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Map<String, Long> dailyTotals;
    private Map<String, Long> monthlyTotals;
    private Map<String, Long> yearlyTotals;

    private static final int MODE_WEEK = 0;
    private static final int MODE_MONTH = 1;
    private static final int MODE_YEAR = 2;

    private static final int MODE_DAY = 3;

    private int currentMode = MODE_WEEK;

    private final int green = Color.rgb(183, 255, 0);
    private final int darkGreen = Color.rgb(45, 60, 30);
    private final int gridColor = Color.rgb(52, 58, 48);
    private final int muted = Color.rgb(133, 141, 125);
    private final int textColor = Color.rgb(240, 235, 221);

    public DailyActivityChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    // Gráfico semanal: mantém os últimos 7 dias
    public void setData(Map<String, Long> data) {
        dailyTotals = data;
        currentMode = MODE_WEEK;
        invalidate();
    }

    // Gráfico mensal: recebe os últimos 7 meses
    public void setMonthlyData(Map<String, Long> data) {
        monthlyTotals = data;
        currentMode = MODE_MONTH;
        invalidate();
    }

    // Gráfico anual: recebe os últimos 5 anos
    public void setYearlyData(Map<String, Long> data) {
        yearlyTotals = data;
        currentMode = MODE_YEAR;
        invalidate();
    }

    public void setDailyData(String date, long seconds) {
        dailyTotals = new java.util.HashMap<>();
        dailyTotals.put(date, seconds);
        currentMode = MODE_DAY;
        invalidate();
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (currentMode == MODE_WEEK) {
            drawWeekly(canvas);
        } else if (currentMode == MODE_MONTH) {
            drawAggregated(canvas, monthlyTotals, MODE_MONTH);
        } else if (currentMode == MODE_YEAR) {
            drawAggregated(canvas, yearlyTotals, MODE_YEAR);
        } else if (currentMode == MODE_DAY) {
            drawDaily(canvas);
        }
    }

    // Gráfico semanal original
    private void drawWeekly(Canvas canvas) {

        if (dailyTotals == null) return;

        float width = getWidth();
        float height = getHeight();

        float left = dp(18);
        float right = width - dp(18);
        float top = dp(30);
        float bottom = height - dp(38);

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

        paint.setColor(gridColor);
        paint.setStrokeWidth(dp(1));

        for (int i = 0; i <= 3; i++) {
            float y = top + (chartHeight * i / 3f);
            canvas.drawLine(left, y, right, y, paint);
        }

        for (int i = 0; i < 7; i++) {

            LocalDate date = today.minusDays(6 - i);

            long seconds = dailyTotals.getOrDefault(
                    date.toString(), 0L
            );

            float centerX = left + slotWidth * (i + 0.5f);

            float barHeight = (seconds / (float) maxValue)
                    * chartHeight * 0.85f;

            boolean isBestDay = seconds == maxValue && seconds > 0;

            if (seconds > 0) {
                barHeight = Math.max(barHeight, dp(4));
            }

            float barTop = bottom - barHeight;

            RectF backgroundBar = new RectF(
                    centerX - barWidth / 2,
                    top,
                    centerX + barWidth / 2,
                    bottom
            );

            paint.setColor(darkGreen);
            canvas.drawRoundRect(
                    backgroundBar, dp(7), dp(7), paint
            );

            if (seconds > 0) {

                RectF bar = new RectF(
                        centerX - barWidth / 2,
                        barTop,
                        centerX + barWidth / 2,
                        bottom
                );

                paint.setColor(green);
                canvas.drawRoundRect(
                        bar, dp(7), dp(7), paint
                );
            }

            if (seconds > 0) {

                paint.setColor(isBestDay ? green : textColor);
                paint.setTextSize(dp(10));
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setFakeBoldText(true);

                String valueText = (seconds / 60) + "m";

                canvas.drawText(
                        valueText,
                        centerX,
                        Math.max(dp(15), barTop - dp(8)),
                        paint
                );
            }

            paint.setColor(date.equals(today) ? green : muted);
            paint.setTextSize(dp(10));
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(date.equals(today));

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
                    height - dp(12),
                    paint
            );
        }
    }

    private void drawDaily(Canvas canvas) {

        if (dailyTotals == null || dailyTotals.isEmpty()) return;

        float width = getWidth();
        float height = getHeight();

        float left = dp(18);
        float right = width - dp(18);
        float top = dp(30);
        float bottom = height - dp(38);

        float centerX = (left + right) / 2f;
        float barWidth = dp(55);
        float chartHeight = bottom - top;

        Map.Entry<String, Long> entry =
                dailyTotals.entrySet().iterator().next();

        String date = entry.getKey();
        long seconds = entry.getValue();

        long maxValue = Math.max(seconds, 60);

        float barHeight = seconds > 0
                ? Math.max((seconds / (float) maxValue) * chartHeight * 0.85f, dp(4))
                : 0;

        float barTop = bottom - barHeight;

        paint.setColor(gridColor);
        paint.setStrokeWidth(dp(1));

        for (int i = 0; i <= 3; i++) {
            float y = top + (chartHeight * i / 3f);
            canvas.drawLine(left, y, right, y, paint);
        }

        RectF backgroundBar = new RectF(
                centerX - barWidth / 2,
                top,
                centerX + barWidth / 2,
                bottom
        );

        paint.setColor(darkGreen);
        canvas.drawRoundRect(backgroundBar, dp(7), dp(7), paint);

        if (seconds > 0) {
            RectF bar = new RectF(
                    centerX - barWidth / 2,
                    barTop,
                    centerX + barWidth / 2,
                    bottom
            );

            paint.setColor(green);
            canvas.drawRoundRect(bar, dp(7), dp(7), paint);
        }

        paint.setColor(textColor);
        paint.setTextSize(dp(12));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);

        String valueText = formatarTempoGrafico(seconds);

        canvas.drawText(
                valueText,
                centerX,
                Math.max(dp(15), barTop - dp(8)),
                paint
        );

        paint.setColor(muted);
        paint.setTextSize(dp(11));
        paint.setFakeBoldText(false);

        canvas.drawText(
                LocalDate.parse(date).format(
                        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
                ),
                centerX,
                height - dp(12),
                paint
        );
    }

    private String formatarTempoGrafico(long seconds) {

        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;

        if (hours > 0) {
            return hours + "h " + minutes + "min";
        }

        return minutes + " min";
    }

    // Gráficos mensal e anual
    private void drawAggregated(
            Canvas canvas,
            Map<String, Long> data,
            int mode
    ) {

        if (data == null || data.isEmpty()) return;

        int count = mode == MODE_MONTH ? 7 : 5;

        float width = getWidth();
        float height = getHeight();

        float left = dp(18);
        float right = width - dp(18);
        float top = dp(30);
        float bottom = height - dp(38);

        float chartHeight = bottom - top;
        float slotWidth = (right - left) / count;
        float barWidth = slotWidth * 0.48f;

        TreeMap<String, Long> orderedData = new TreeMap<>(data);

        long maxValue = 0;

        for (long value : orderedData.values()) {
            maxValue = Math.max(maxValue, value);
        }

        if (maxValue == 0) maxValue = 60;

        paint.setColor(gridColor);
        paint.setStrokeWidth(dp(1));

        for (int i = 0; i <= 3; i++) {
            float y = top + (chartHeight * i / 3f);
            canvas.drawLine(left, y, right, y, paint);
        }

        int index = 0;

        for (Map.Entry<String, Long> entry : orderedData.entrySet()) {

            if (index >= count) break;

            String key = entry.getKey();
            long seconds = entry.getValue();

            float centerX = left + slotWidth * (index + 0.5f);

            float barHeight = (seconds / (float) maxValue)
                    * chartHeight * 0.85f;

            if (seconds > 0) {
                barHeight = Math.max(barHeight, dp(4));
            }

            float barTop = bottom - barHeight;

            RectF backgroundBar = new RectF(
                    centerX - barWidth / 2,
                    top,
                    centerX + barWidth / 2,
                    bottom
            );

            paint.setColor(darkGreen);
            canvas.drawRoundRect(
                    backgroundBar, dp(7), dp(7), paint
            );

            if (seconds > 0) {

                RectF bar = new RectF(
                        centerX - barWidth / 2,
                        barTop,
                        centerX + barWidth / 2,
                        bottom
                );

                paint.setColor(green);
                canvas.drawRoundRect(
                        bar, dp(7), dp(7), paint
                );

                paint.setColor(textColor);
                paint.setTextSize(dp(9));
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setFakeBoldText(true);

                String valueText = (seconds / 3600) + "h";

                canvas.drawText(
                        valueText,
                        centerX,
                        Math.max(dp(15), barTop - dp(8)),
                        paint
                );
            }

            String label;

            if (mode == MODE_MONTH) {

                YearMonth month = YearMonth.parse(key);

                label = month.getMonth()
                        .getDisplayName(
                                TextStyle.SHORT,
                                new Locale("pt", "BR")
                        )
                        .replace(".", "")
                        .toUpperCase(new Locale("pt", "BR"));

            } else {

                label = key;
            }

            paint.setColor(muted);
            paint.setTextSize(dp(10));
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(false);

            canvas.drawText(
                    label,
                    centerX,
                    height - dp(12),
                    paint
            );

            index++;
        }
    }
}

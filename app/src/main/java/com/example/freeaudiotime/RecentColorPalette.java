package com.example.freeaudiotime;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.LinkedHashSet;
import java.util.Set;

/** Shared recent-color history used by every color wheel in the app. */
public final class RecentColorPalette {
    private static final String PREFS = "shared_recent_color_palette";
    private static final String KEY_COLORS = "colors";
    private static final int MAX_COLORS = 12;

    public interface OnRecentColorSelected {
        void onSelected(int color);
    }

    private RecentColorPalette() { }

    public static void remember(Context context, int color) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        LinkedHashSet<String> colors = new LinkedHashSet<>();
        colors.add(String.format(java.util.Locale.ROOT, "#%08X", color));
        String stored = prefs.getString(KEY_COLORS, "");
        if (stored != null && !stored.isEmpty()) {
            for (String item : stored.split(",")) {
                if (colors.size() >= MAX_COLORS) break;
                if (item != null && item.matches("#[0-9A-Fa-f]{8}")) colors.add(item.toUpperCase(java.util.Locale.ROOT));
            }
        }
        StringBuilder value = new StringBuilder();
        for (String item : colors) {
            if (value.length() > 0) value.append(',');
            value.append(item);
        }
        prefs.edit().putString(KEY_COLORS, value.toString()).apply();
    }

    public static void addTo(Context context, LinearLayout parent, ColorWheelView wheel,
                             int horizontalPadding, OnRecentColorSelected listener) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String stored = prefs.getString(KEY_COLORS, "");
        if (stored == null || stored.trim().isEmpty()) return;

        TextView title = new TextView(context);
        title.setText("CORES RECENTES");
        title.setTextColor(Color.WHITE);
        title.setTextSize(12);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(0, dp(context, 10), 0, dp(context, 6));
        parent.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        HorizontalScrollView scroll = new HorizontalScrollView(context);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        String[] colors = stored.split(",");
        for (String value : colors) {
            try {
                int color = Color.parseColor(value);
                View swatch = new View(context);
                GradientDrawable shape = new GradientDrawable();
                shape.setShape(GradientDrawable.OVAL);
                shape.setColor(color);
                shape.setStroke(dp(context, 2), Color.WHITE);
                swatch.setBackground(shape);
                swatch.setContentDescription("Usar cor " + value);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(context, 36), dp(context, 36));
                params.rightMargin = dp(context, 8);
                swatch.setLayoutParams(params);
                swatch.setOnClickListener(v -> {
                    wheel.setColor(color);
                    if (listener != null) listener.onSelected(color);
                });
                row.addView(swatch);
            } catch (IllegalArgumentException ignored) { }
        }
        scroll.addView(row);
        parent.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 44)));
    }

    private static int dp(Context context, float value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}

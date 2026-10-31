
package com.example.freeaudiotime;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemeManager {

    private static final String PREFS = "app_settings";
    private static final String DARK_MODE = "dark_mode";

    public static void applySavedTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
                PREFS, Context.MODE_PRIVATE
        );

        boolean darkMode = prefs.getBoolean(DARK_MODE, false);

        AppCompatDelegate.setDefaultNightMode(
                darkMode
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    public static void toggleTheme(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(
                PREFS, Context.MODE_PRIVATE
        );

        boolean darkMode = prefs.getBoolean(DARK_MODE, false);

        prefs.edit()
                .putBoolean(DARK_MODE, !darkMode)
                .apply();

        AppCompatDelegate.setDefaultNightMode(
                !darkMode
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    public static boolean isDarkMode(Context context) {
        return context.getSharedPreferences(
                PREFS, Context.MODE_PRIVATE
        ).getBoolean(DARK_MODE, false);
    }
}

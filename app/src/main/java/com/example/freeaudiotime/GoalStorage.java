package com.example.freeaudiotime;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.YearMonth;

public class GoalStorage {

    private static final String PREFS_NAME = "goals";

    // Critério fixo para um dia válido na sequência
    public static final long MINIMUM_LISTENING_SECONDS = 30;

    // Proteções da sequência
    public static final int MONTHLY_PROTECTIONS = 3;

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
        );
    }

    // META DIÁRIA

    public static void saveDailyGoal(Context context, long seconds) {
        getPreferences(context)
                .edit()
                .putLong("daily_goal", seconds)
                .apply();
    }

    public static long getDailyGoal(Context context) {
        return getPreferences(context)
                .getLong("daily_goal", 0);
    }

    // META MENSAL

    public static void saveMonthlyGoal(Context context, long seconds) {
        getPreferences(context)
                .edit()
                .putLong("monthly_goal", seconds)
                .apply();
    }

    public static long getMonthlyGoal(Context context) {
        return getPreferences(context)
                .getLong("monthly_goal", 0);
    }

    // META DE SEQUÊNCIA

    public static void saveStreakGoal(Context context, int days) {
        getPreferences(context)
                .edit()
                .putInt("streak_goal", days)
                .apply();
    }

    public static int getStreakGoal(Context context) {
        return getPreferences(context)
                .getInt("streak_goal", 0);
    }

    // ESTADO DA SEQUÊNCIA

    public static void saveCurrentStreak(Context context, int days) {
        getPreferences(context)
                .edit()
                .putInt("current_streak", days)
                .apply();
    }

    public static int getCurrentStreak(Context context) {
        return getPreferences(context)
                .getInt("current_streak", 0);
    }

    public static void saveBestStreak(Context context, int days) {
        getPreferences(context)
                .edit()
                .putInt("best_streak", days)
                .apply();
    }

    public static int getBestStreak(Context context) {
        return getPreferences(context)
                .getInt("best_streak", 0);
    }

    // PROTEÇÕES MENSAIS

    public static int getProtectionsRemaining(Context context) {
        SharedPreferences prefs = getPreferences(context);

        String currentMonth = YearMonth.now().toString();
        String savedMonth = prefs.getString("protection_month", "");

        if (!currentMonth.equals(savedMonth)) {
            prefs.edit()
                    .putString("protection_month", currentMonth)
                    .putInt("protections_remaining", MONTHLY_PROTECTIONS)
                    .apply();

            return MONTHLY_PROTECTIONS;
        }

        return prefs.getInt(
                "protections_remaining",
                MONTHLY_PROTECTIONS
        );
    }
    // INÍCIO OFICIAL DO SISTEMA DE SEQUÊNCIA

    public static void initializeStreak(Context context) {
        SharedPreferences prefs = getPreferences(context);

        if (prefs.getBoolean("streak_initialized", false)) {
            return;
        }

        String today = java.time.LocalDate.now().toString();
        String yesterday = java.time.LocalDate.now()
                .minusDays(1)
                .toString();

        prefs.edit()
                .putBoolean("streak_initialized", true)
                .putString("streak_start_date", today)
                .putString("streak_last_processed_date", yesterday)
                .putString("streak_last_valid_date", "")
                .putInt("current_streak", 0)
                .putInt("best_streak", 0)
                .putString("protection_month",
                        java.time.YearMonth.now().toString())
                .putInt("protections_remaining", MONTHLY_PROTECTIONS)
                .apply();
    }

    public static String getStreakStartDate(Context context) {
        return getPreferences(context)
                .getString("streak_start_date", "");
    }

    public static String getLastProcessedDate(Context context) {
        return getPreferences(context)
                .getString("streak_last_processed_date", "");
    }

    public static void saveLastProcessedDate(
            Context context, String date) {

        getPreferences(context)
                .edit()
                .putString("streak_last_processed_date", date)
                .apply();
    }
    public static void saveLastValidDate(
            Context context, String date) {

        getPreferences(context)
                .edit()
                .putString("streak_last_valid_date", date)
                .apply();
    }

    // PROTEÇÕES PARA O MÊS PROCESSADO

    public static int getProtectionsRemainingForMonth(
            Context context, YearMonth month) {

        SharedPreferences prefs = getPreferences(context);
        String monthKey = month.toString();
        String savedMonth = prefs.getString("protection_month", "");

        if (!monthKey.equals(savedMonth)) {
            prefs.edit()
                    .putString("protection_month", monthKey)
                    .putInt("protections_remaining", MONTHLY_PROTECTIONS)
                    .apply();

            return MONTHLY_PROTECTIONS;
        }

        return prefs.getInt(
                "protections_remaining",
                MONTHLY_PROTECTIONS
        );
    }

    public static boolean useProtectionForMonth(
            Context context, YearMonth month) {

        int remaining = getProtectionsRemainingForMonth(context, month);

        if (remaining <= 0) {
            return false;
        }

        getPreferences(context)
                .edit()
                .putInt("protections_remaining", remaining - 1)
                .apply();

        return true;
    }


}

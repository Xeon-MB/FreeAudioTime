package com.example.freeaudiotime;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class AchievementManager {

    public static void checkAchievements(
            Context context,
            JSONObject history
    ) {
        long totalTime = history.optLong("totalTime", 0);

        // Tempo de reprodução
        unlockIfReached(context, "first_10_hours", totalTime, 10 * 3600L);
        unlockIfReached(context, "hours_50", totalTime, 50 * 3600L);
        unlockIfReached(context, "hours_100", totalTime, 100 * 3600L);
        unlockIfReached(context, "hours_500", totalTime, 500 * 3600L);
        unlockIfReached(context, "hours_1000", totalTime, 1000 * 3600L);

        // Sequências
        int streak = PlaybackStatistics.getCurrentStreak(history, context);

        unlockIfReached(context, "streak_3", streak, 3);
        unlockIfReached(context, "streak_7", streak, 7);
        unlockIfReached(context, "streak_30", streak, 30);
        unlockIfReached(context, "streak_100", streak, 100);

        // Descobertas
        Set<String> artists = new HashSet<>();
        Set<String> tracks = new HashSet<>();
        Set<String> listeningDays = new HashSet<>();

        JSONArray sessions = history.optJSONArray("sessions");

        if (sessions != null) {
            for (int i = 0; i < sessions.length(); i++) {
                JSONObject session = sessions.optJSONObject(i);

                if (session == null) continue;

                String artist = session.optString("artist", "").trim();
                String title = session.optString("title", "").trim();
                String date = session.optString("date", "");

                if (!artist.isEmpty() && !artist.equalsIgnoreCase("Desconhecido")) {
                    artists.add(artist.toLowerCase());
                }

                if (!title.isEmpty() && !title.equalsIgnoreCase("Desconhecido")) {
                    tracks.add(title.toLowerCase());
                }

                if (!date.isEmpty() && session.optLong("duration", 0) > 0) {
                    listeningDays.add(date.substring(0, Math.min(10, date.length())));
                }
            }
        }

        unlockIfReached(context, "artists_5", artists.size(), 5);
        unlockIfReached(context, "artists_10", artists.size(), 10);
        unlockIfReached(context, "artists_25", artists.size(), 25);
        unlockIfReached(context, "tracks_10", tracks.size(), 10);
        unlockIfReached(context, "tracks_50", tracks.size(), 50);
        unlockIfReached(context, "tracks_100", tracks.size(), 100);

        // Consistência
        unlockIfReached(context, "listening_days_7", listeningDays.size(), 7);
        unlockIfReached(context, "listening_days_30", listeningDays.size(), 30);
        unlockIfReached(context, "listening_days_100", listeningDays.size(), 100);

        // Metas
        int dailyGoalDays =
                PlaybackStatistics.getMonthlyDaysGoalReached(history, context);

        unlockIfReached(context, "daily_goal_7_days", dailyGoalDays, 7);

        int monthlyGoalProgress =
                PlaybackStatistics.getMonthlyGoalProgress(history, context);

        unlockIfReached(context, "monthly_goal", monthlyGoalProgress, 100);
    }

    private static void unlockIfReached(
            Context context,
            String id,
            long current,
            long required
    ) {
        if (current >= required) {
            AchievementStorage.unlock(context, id);
        }
    }
}
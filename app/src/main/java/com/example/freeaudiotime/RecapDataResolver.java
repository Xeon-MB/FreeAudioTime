package com.example.freeaudiotime;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Resolves dynamic recap statistics on-the-fly for any period and date.
 * Reuses PlaybackStatistics, AchievementStorage, and existing calculation formulas.
 */
public class RecapDataResolver {

    public static DynamicRecapData resolve(Context context, JSONObject history, RecapCategory category, LocalDate selectedDate) {
        if (selectedDate == null) {
            selectedDate = LocalDate.now();
        }

        String periodTitle;
        String dateRangeText;
        Locale brasil = new Locale("pt", "BR");
        DateTimeFormatter formatoCurto = DateTimeFormatter.ofPattern("dd MMM", brasil);

        if (category == RecapCategory.DAILY) {
            periodTitle = "RECAP DIÁRIA";
            dateRangeText = selectedDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy", brasil)).toUpperCase(brasil);
        } else if (category == RecapCategory.WEEKLY) {
            periodTitle = "RECAP SEMANAL";
            LocalDate inicio = selectedDate.with(DayOfWeek.MONDAY);
            LocalDate fim = inicio.plusDays(6);
            dateRangeText = (inicio.format(formatoCurto) + " — " + fim.format(formatoCurto) + " " + fim.getYear()).toUpperCase(brasil);
        } else if (category == RecapCategory.MONTHLY) {
            periodTitle = "RECAP MENSAL";
            dateRangeText = selectedDate.format(DateTimeFormatter.ofPattern("MMMM 'DE' yyyy", brasil)).toUpperCase(brasil);
        } else {
            periodTitle = "RECAP ANUAL";
            dateRangeText = String.valueOf(selectedDate.getYear());
        }

        DynamicRecapData data = new DynamicRecapData(category, periodTitle, dateRangeText);

        data.putValue(RecapElementBinding.APP_BRAND, "FREEAUDIOTIME®");
        data.putValue(RecapElementBinding.VOLUME_TAG, "VOL. 001");
        data.putValue(RecapElementBinding.PERIOD_TITLE, periodTitle);
        data.putValue(RecapElementBinding.PERIOD_RANGE, dateRangeText);
        data.putValue(RecapElementBinding.TOTAL_TIME_LABEL, "TOTAL LISTENING TIME");
        data.putValue(RecapElementBinding.TOP_ARTIST_TAG, "ARTIST Nº 01");
        data.putValue(RecapElementBinding.DISCOVERY_LABEL, "DISCOVERY");
        data.putValue(RecapElementBinding.UNIQUE_ARTISTS_LABEL, "ARTISTS");
        data.putValue(RecapElementBinding.ALSO_IN_ROTATION_TITLE, "ALSO IN ROTATION");
        data.putValue(RecapElementBinding.BEST_DAY_TITLE, "PEAK DAY / BEST MOMENT");
        data.putValue(RecapElementBinding.ACHIEVEMENTS_TITLE, "UNLOCKED ACHIEVEMENTS");
        data.putValue(RecapElementBinding.TOP_TRACKS_TITLE, "YOUR TOP TRACKS");
        data.putValue(RecapElementBinding.FOOTER_LINE_1, "A PERSONAL MUSIC ARCHIVE");
        data.putValue(RecapElementBinding.FOOTER_LINE_2, "EVERY MINUTE HAS A STORY.  //  FAT® " + LocalDate.now().getYear());

        // 1. Calculate Totals
        long totalSeconds = 0;
        String rawArtistsRanking = "";
        String rawTracksRanking = "";

        LocalDate periodStart;
        LocalDate periodEnd;

        if (category == RecapCategory.DAILY) {
            periodStart = selectedDate;
            periodEnd = selectedDate;
        } else if (category == RecapCategory.WEEKLY) {
            periodStart = selectedDate.with(DayOfWeek.MONDAY);
            periodEnd = periodStart.plusDays(6);
        } else if (category == RecapCategory.MONTHLY) {
            periodStart = selectedDate.withDayOfMonth(1);
            periodEnd = selectedDate.withDayOfMonth(selectedDate.lengthOfMonth());
        } else {
            periodStart = selectedDate.withDayOfYear(1);
            periodEnd = selectedDate.withDayOfYear(selectedDate.lengthOfYear());
        }

        totalSeconds = PlaybackStatistics.getTotalBetweenDates(
                history, periodStart, periodEnd);
        rawArtistsRanking = PlaybackStatistics.getTopArtistsBetweenDates(
                history, periodStart, periodEnd);
        rawTracksRanking = PlaybackStatistics.getTopTracksBetweenDates(
                history, periodStart, periodEnd);

        data.putValue(RecapElementBinding.TOTAL_TIME_FORMATTED, formatTime(totalSeconds));
        data.putValue(RecapElementBinding.TOTAL_MINUTES_SUB, "/// " + (totalSeconds / 60) + " MIN");

        // 2. Parse Top Artist and Rotation
        String firstArtistLine = extractFirstLine(rawArtistsRanking);
        if (!firstArtistLine.equals("Nenhum registro")) {
            String cleanName = firstArtistLine.replaceFirst("^\\d+\\.\\s*", "");
            String artistName = cleanName;
            String artistDuration = "—";
            if (cleanName.contains(" — ")) {
                String[] parts = cleanName.split(" — ", 2);
                artistName = parts[0].trim();
                artistDuration = parts[1].trim();
            }
            data.putValue(RecapElementBinding.TOP_ARTIST_NAME, artistName.toUpperCase(brasil));
            data.putValue(RecapElementBinding.TOP_ARTIST_TIME, artistDuration.toUpperCase(brasil));
        } else {
            data.putValue(RecapElementBinding.TOP_ARTIST_NAME, "NENHUM REGISTRO");
            data.putValue(RecapElementBinding.TOP_ARTIST_TIME, "0MIN");
        }

        // Secondary Artists (top 2 and 3)
        if (rawArtistsRanking != null && !rawArtistsRanking.isEmpty()) {
            String[] lines = rawArtistsRanking.split("\n");
            for (int i = 1; i < Math.min(lines.length, 3); i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) continue;
                line = line.replaceFirst("^\\d+\\.\\s*", "");
                String artName = line;
                String artTime = "";
                if (line.contains(" — ")) {
                    String[] parts = line.split(" — ", 2);
                    artName = parts[0].trim();
                    artTime = parts[1].trim();
                }
                data.getSecondaryArtists().add(new DynamicRecapData.TopArtistItem(i + 1, artName, artTime));
            }
        }

        // 3. Count Unique Artists & Tracks (Discovery)
        int[] counts = countUniqueInPeriod(history, category, selectedDate);
        data.putValue(RecapElementBinding.UNIQUE_ARTISTS_COUNT, String.valueOf(counts[0]));

        // 4. Peak Day / Best Moment
        resolvePeakMoment(data, history, category, selectedDate);

        // 5. Top Tracks
        resolveTopTracks(data, history, rawTracksRanking);

        // 6. Unlocked Achievements
        resolveAchievements(context, data, category, selectedDate);

        return data;
    }

    private static String extractFirstLine(String ranking) {
        if (ranking == null || ranking.trim().isEmpty() || ranking.startsWith("Nenhum")) {
            return "Nenhum registro";
        }
        String[] lines = ranking.split("\n");
        return lines[0].trim();
    }

    public static String formatTime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;

        if (hours > 0) {
            return hours + "H " + minutes + "MIN";
        }
        if (minutes > 0) {
            return minutes + "MIN";
        }
        return remainingSeconds + "S";
    }

    private static int[] countUniqueInPeriod(JSONObject history, RecapCategory category, LocalDate selectedDate) {
        JSONArray sessions = history.optJSONArray("sessions");
        if (sessions == null) return new int[]{0, 0};

        java.util.Set<String> artists = new java.util.HashSet<>();
        java.util.Set<String> tracks = new java.util.HashSet<>();

        LocalDate start;
        LocalDate end;
        if (category == RecapCategory.DAILY) {
            start = selectedDate;
            end = selectedDate;
        } else if (category == RecapCategory.WEEKLY) {
            start = selectedDate.with(DayOfWeek.MONDAY);
            end = start.plusDays(6);
        } else if (category == RecapCategory.MONTHLY) {
            start = selectedDate.withDayOfMonth(1);
            end = selectedDate.withDayOfMonth(selectedDate.lengthOfMonth());
        } else {
            start = selectedDate.withDayOfYear(1);
            end = selectedDate.withDayOfYear(selectedDate.lengthOfYear());
        }

        for (int i = 0; i < sessions.length(); i++) {
            JSONObject session = sessions.optJSONObject(i);
            if (session == null) continue;
            String dateText = session.optString("date", "");
            try {
                if (dateText.length() < 10) continue;
                LocalDate d = LocalDate.parse(dateText.substring(0, 10));
                if (d.isBefore(start) || d.isAfter(end)) continue;

                String artist = session.optString("artist", "").trim();
                String title = session.optString("title", "").trim();
                if (!artist.isEmpty()) artists.add(artist);
                if (!title.isEmpty() && !artist.isEmpty()) tracks.add(title + "|" + artist);
            } catch (Exception ignored) {}
        }
        return new int[]{artists.size(), tracks.size()};
    }

    private static void resolvePeakMoment(DynamicRecapData data, JSONObject history, RecapCategory category, LocalDate selectedDate) {
        Locale brasil = new Locale("pt", "BR");
        Map<String, Long> dailyTotals = PlaybackStatistics.getDailyTotals(history);

        if (category == RecapCategory.DAILY) {
            int uniqueTracks = PlaybackStatistics.getDailyUniqueTracks(history, selectedDate.toString());
            data.putValue(RecapElementBinding.BEST_DAY_TITLE, "MÚSICAS DIFERENTES");
            data.putValue(RecapElementBinding.BEST_DAY_NAME, String.valueOf(uniqueTracks));
            data.putValue(RecapElementBinding.BEST_DAY_TIME, uniqueTracks == 1 ? "MÚSICA REGISTRADA" : "MÚSICAS REGISTRADAS");
        } else if (category == RecapCategory.WEEKLY) {
            data.putValue(RecapElementBinding.BEST_DAY_TITLE, "PEAK DAY / BEST MOMENT");
            LocalDate bestDate = null;
            long maxTime = 0;
            for (int i = 6; i >= 0; i--) {
                LocalDate d = selectedDate.minusDays(i);
                long t = dailyTotals.getOrDefault(d.toString(), 0L);
                if (t > maxTime) {
                    maxTime = t;
                    bestDate = d;
                }
            }
            if (bestDate != null && maxTime > 0) {
                String dayName = bestDate.getDayOfWeek().getDisplayName(TextStyle.FULL, brasil).toUpperCase(brasil);
                data.putValue(RecapElementBinding.BEST_DAY_NAME, dayName);
                data.putValue(RecapElementBinding.BEST_DAY_TIME, formatTime(maxTime));
            } else {
                data.putValue(RecapElementBinding.BEST_DAY_NAME, "SEM REPRODUÇÃO");
                data.putValue(RecapElementBinding.BEST_DAY_TIME, "0MIN");
            }
        } else if (category == RecapCategory.MONTHLY) {
            data.putValue(RecapElementBinding.BEST_DAY_TITLE, "PEAK DAY OF WEEK");
            Map<Integer, Long> byDayOfWeek = new HashMap<>();
            for (Map.Entry<String, Long> entry : dailyTotals.entrySet()) {
                try {
                    LocalDate d = LocalDate.parse(entry.getKey());
                    if (d.getYear() == selectedDate.getYear() && d.getMonthValue() == selectedDate.getMonthValue()) {
                        int dow = d.getDayOfWeek().getValue();
                        byDayOfWeek.put(dow, byDayOfWeek.getOrDefault(dow, 0L) + entry.getValue());
                    }
                } catch (Exception ignored) {}
            }
            int bestDow = 0;
            long maxTime = 0;
            for (Map.Entry<Integer, Long> e : byDayOfWeek.entrySet()) {
                if (e.getValue() > maxTime) {
                    bestDow = e.getKey();
                    maxTime = e.getValue();
                }
            }
            if (bestDow > 0) {
                String dayName = DayOfWeek.of(bestDow).getDisplayName(TextStyle.FULL, brasil).toUpperCase(brasil);
                data.putValue(RecapElementBinding.BEST_DAY_NAME, dayName);
                data.putValue(RecapElementBinding.BEST_DAY_TIME, formatTime(maxTime));
            } else {
                data.putValue(RecapElementBinding.BEST_DAY_NAME, "SEM REPRODUÇÃO");
                data.putValue(RecapElementBinding.BEST_DAY_TIME, "0MIN");
            }
        } else {
            data.putValue(RecapElementBinding.BEST_DAY_TITLE, "PEAK MONTH / MELHOR MÊS");
            Map<Integer, Long> byMonth = new HashMap<>();
            for (Map.Entry<String, Long> entry : dailyTotals.entrySet()) {
                try {
                    LocalDate d = LocalDate.parse(entry.getKey());
                    if (d.getYear() == selectedDate.getYear()) {
                        int m = d.getMonthValue();
                        byMonth.put(m, byMonth.getOrDefault(m, 0L) + entry.getValue());
                    }
                } catch (Exception ignored) {}
            }
            int bestMonth = 0;
            long maxTime = 0;
            for (Map.Entry<Integer, Long> e : byMonth.entrySet()) {
                if (e.getValue() > maxTime) {
                    bestMonth = e.getKey();
                    maxTime = e.getValue();
                }
            }
            if (bestMonth > 0) {
                String mName = java.time.Month.of(bestMonth).getDisplayName(TextStyle.FULL, brasil).toUpperCase(brasil);
                data.putValue(RecapElementBinding.BEST_DAY_NAME, mName);
                data.putValue(RecapElementBinding.BEST_DAY_TIME, formatTime(maxTime));
            } else {
                data.putValue(RecapElementBinding.BEST_DAY_NAME, "SEM REPRODUÇÃO");
                data.putValue(RecapElementBinding.BEST_DAY_TIME, "0MIN");
            }
        }
    }

    private static void resolveTopTracks(DynamicRecapData data, JSONObject history, String rawTracksRanking) {
        if (rawTracksRanking == null || rawTracksRanking.isEmpty() || rawTracksRanking.equals("Nenhuma música")) {
            return;
        }

        JSONArray tracksJson = history.optJSONArray("tracks");
        JSONArray sessionsJson = history.optJSONArray("sessions");
        String[] lines = rawTracksRanking.split("\n");

        for (int i = 0; i < Math.min(lines.length, 5); i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split(" — ", 3);
            if (parts.length < 2) continue;

            String title = parts[0].replaceFirst("^\\d+\\.\\s*", "").trim();
            String artist = parts[1].trim();
            String duration = parts.length >= 3 ? parts[2].trim() : "";

            String artworkPath = "";
            if (tracksJson != null) {
                for (int t = 0; t < tracksJson.length(); t++) {
                    JSONObject tr = tracksJson.optJSONObject(t);
                    if (tr != null && title.equals(tr.optString("title")) && artist.equals(tr.optString("artist"))) {
                        artworkPath = tr.optString("artwork", "");
                        break;
                    }
                }
            }
            if (artworkPath.isEmpty() && sessionsJson != null) {
                for (int s = 0; s < sessionsJson.length(); s++) {
                    JSONObject se = sessionsJson.optJSONObject(s);
                    if (se != null && title.equals(se.optString("title")) && artist.equals(se.optString("artist"))) {
                        artworkPath = se.optString("artwork", "");
                        if (!artworkPath.isEmpty()) break;
                    }
                }
            }
            data.getTopTracks().add(new DynamicRecapData.TopTrackItem(title, artist, duration, artworkPath));
        }
    }

    private static void resolveAchievements(Context context, DynamicRecapData data, RecapCategory category, LocalDate selectedDate) {
        JSONObject unlockedJson = AchievementStorage.load(context);
        if (unlockedJson == null || unlockedJson.length() == 0) return;

        LocalDate start;
        LocalDate end;
        if (category == RecapCategory.DAILY) {
            start = selectedDate;
            end = selectedDate;
        } else if (category == RecapCategory.WEEKLY) {
            start = selectedDate.with(DayOfWeek.MONDAY);
            end = start.plusDays(6);
        } else if (category == RecapCategory.MONTHLY) {
            start = selectedDate.withDayOfMonth(1);
            end = selectedDate.withDayOfMonth(selectedDate.lengthOfMonth());
        } else {
            start = selectedDate.withDayOfYear(1);
            end = selectedDate.withDayOfYear(selectedDate.lengthOfYear());
        }

        String[] ids = {
                "first_10_hours", "hours_50", "hours_100", "hours_500", "hours_1000",
                "streak_3", "streak_7", "streak_30", "streak_100",
                "artists_5", "artists_10", "artists_25",
                "tracks_10", "tracks_50", "tracks_100",
                "listening_days_7", "listening_days_30", "listening_days_100",
                "daily_goal_7_days", "monthly_goal"
        };
        String[] titles = {
                "Primeiras 10 horas", "50 horas de música", "100 horas de música", "500 horas de música", "1.000 horas de música",
                "Sequência de 3 dias", "Sequência de 7 dias", "Sequência de 30 dias", "Sequência de 100 dias",
                "5 artistas descobertos", "10 artistas descobertos", "25 artistas descobertos",
                "10 músicas diferentes", "50 músicas diferentes", "100 músicas diferentes",
                "7 dias de reprodução", "30 dias de reprodução", "100 dias de reprodução",
                "Meta diária por 7 dias", "Meta mensal alcançada"
        };

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (int i = 0; i < ids.length; i++) {
            if (!unlockedJson.has(ids[i])) continue;
            long ts = unlockedJson.optLong(ids[i], 0);
            if (ts <= 0) continue;
            LocalDate unlockedDate = Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate();
            if (unlockedDate.isBefore(start) || unlockedDate.isAfter(end)) continue;

            data.getUnlockedAchievements().add(new DynamicRecapData.AchievementItem(
                    ids[i], titles[i], unlockedDate.format(dtf)
            ));
        }
    }
}

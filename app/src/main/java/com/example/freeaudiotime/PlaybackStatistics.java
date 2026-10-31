package com.example.freeaudiotime;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Set;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;


public class PlaybackStatistics {

    private static JSONArray getUniqueSessions(JSONObject history) {

        JSONArray sessions = history.optJSONArray("sessions");
        JSONArray uniqueSessions = new JSONArray();

        if (sessions == null) {
            return uniqueSessions;
        }

        Set<String> seenSessions = new HashSet<>();

        for (int i = 0; i < sessions.length(); i++) {

            JSONObject session = sessions.optJSONObject(i);

            if (session == null) {
                continue;
            }

            String date = session.optString("date", "");
            String title = session.optString("title", "");
            String artist = session.optString("artist", "");
            long duration = session.optLong("duration", 0);

            String key = new JSONArray()
                    .put(date)
                    .put(title)
                    .put(artist)
                    .put(duration)
                    .toString();

            if (seenSessions.add(key)) {
                uniqueSessions.put(session);
            }
        }

        return uniqueSessions;
    }

    public static Map<String, Long> getDailyTotals(JSONObject history) {

        Map<String, Long> dailyTotals = new HashMap<>();

        JSONArray sessions = getUniqueSessions(history);

        if (sessions == null) {
            return dailyTotals;
        }

        SimpleDateFormat dateFormat =
                new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (int i = 0; i < sessions.length(); i++) {

            JSONObject session = sessions.optJSONObject(i);

            if (session == null) {
                continue;
            }

            String date = session.optString("date", "");
            long duration = session.optLong("duration", 0);

            if (date.length() < 10 || duration <= 0) {
                continue;
            }

            String day = date.substring(0, 10);

            long previous = dailyTotals.getOrDefault(day, 0L);

            dailyTotals.put(day, previous + duration);
        }

        return dailyTotals;
    }

    // Total de reprodução dos últimos 7 meses
    public static Map<String, Long> getMonthlyTotalsLastMonths(
            JSONObject history, int count) {

        Map<String, Long> dailyTotals = getDailyTotals(history);
        Map<String, Long> monthlyTotals = new LinkedHashMap<>();

        YearMonth currentMonth = YearMonth.now();

        // Inicializa os meses, inclusive os que não tiveram reprodução
        for (int i = count - 1; i >= 0; i--) {
            YearMonth month = currentMonth.minusMonths(i);
            monthlyTotals.put(month.toString(), 0L);
        }

        // Soma os segundos de cada dia dentro do respectivo mês
        for (Map.Entry<String, Long> entry : dailyTotals.entrySet()) {

            String date = entry.getKey();

            if (date.length() < 10) continue;

            String month = date.substring(0, 7);

            if (monthlyTotals.containsKey(month)) {
                monthlyTotals.put(
                        month,
                        monthlyTotals.get(month) + entry.getValue()
                );
            }
        }

        return monthlyTotals;
    }


    // Total de reprodução dos últimos 5 anos
    public static Map<String, Long> getYearlyTotalsLastYears(
            JSONObject history, int count) {

        Map<String, Long> dailyTotals = getDailyTotals(history);
        Map<String, Long> yearlyTotals = new LinkedHashMap<>();

        int currentYear = LocalDate.now().getYear();

        // Inicializa os anos, inclusive os que não tiveram reprodução
        for (int i = count - 1; i >= 0; i--) {
            String year = String.valueOf(currentYear - i);
            yearlyTotals.put(year, 0L);
        }

        // Soma os segundos de cada dia dentro do respectivo ano
        for (Map.Entry<String, Long> entry : dailyTotals.entrySet()) {

            String date = entry.getKey();

            if (date.length() < 10) continue;

            String year = date.substring(0, 4);

            if (yearlyTotals.containsKey(year)) {
                yearlyTotals.put(
                        year,
                        yearlyTotals.get(year) + entry.getValue()
                );
            }
        }

        return yearlyTotals;
    }
    public static long getWeeklyTotal(JSONObject history) {

        Map<String, Long> dailyTotals = getDailyTotals(history);

        java.util.Calendar calendar = java.util.Calendar.getInstance();

        // Segunda-feira como primeiro dia da semana
        calendar.setFirstDayOfWeek(java.util.Calendar.MONDAY);

        // Início da semana atual
        calendar.set(java.util.Calendar.DAY_OF_WEEK,
                java.util.Calendar.MONDAY);

        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);

        SimpleDateFormat dateFormat =
                new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        long total = 0;

        for (int i = 0; i < 7; i++) {

            String day = dateFormat.format(calendar.getTime());

            total += dailyTotals.getOrDefault(day, 0L);

            calendar.add(java.util.Calendar.DAY_OF_MONTH, 1);
        }

        return total;
    }
    public static long getMonthlyTotal(JSONObject history) {

        Map<String, Long> dailyTotals = getDailyTotals(history);

        java.util.Calendar calendar = java.util.Calendar.getInstance();

        int currentMonth = calendar.get(java.util.Calendar.MONTH);
        int currentYear = calendar.get(java.util.Calendar.YEAR);

        long total = 0;

        for (String day : dailyTotals.keySet()) {

            String[] parts = day.split("-");

            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]) - 1;

            if (year == currentYear && month == currentMonth) {
                total += dailyTotals.get(day);
            }
        }

        return total;
    }
    public static long getYearlyTotal(JSONObject history) {

        Map<String, Long> dailyTotals = getDailyTotals(history);

        int currentYear = java.util.Calendar.getInstance()
                .get(java.util.Calendar.YEAR);

        long total = 0;

        for (String day : dailyTotals.keySet()) {

            int year = Integer.parseInt(day.substring(0, 4));

            if (year == currentYear) {
                total += dailyTotals.get(day);
            }
        }

        return total;
    }
    public static String getMostPlayedArtist(JSONObject history) {

        JSONArray sessions = getUniqueSessions(history);

        if (sessions == null) {
            return "Nenhum artista";
        }

        Map<String, Long> artistTimes = new HashMap<>();

        for (int i = 0; i < sessions.length(); i++) {

            JSONObject session = sessions.optJSONObject(i);

            if (session == null) {
                continue;
            }

            String artist = session.optString("artist", "");
            long duration = session.optLong("duration", 0);

            if (artist.isEmpty() || duration <= 0) {
                continue;
            }

            long previous = artistTimes.getOrDefault(artist, 0L);

            artistTimes.put(artist, previous + duration);
        }

        String mostPlayedArtist = "Nenhum artista";
        long highestTime = 0;

        for (String artist : artistTimes.keySet()) {

            if (artistTimes.get(artist) > highestTime) {
                highestTime = artistTimes.get(artist);
                mostPlayedArtist = artist;
            }
        }

        return mostPlayedArtist;
    }

    public static String getMostPlayedTrack(JSONObject history) {

        JSONArray sessions = getUniqueSessions(history);

        if (sessions == null) {
            return "Nenhuma música";
        }

        Map<String, Long> trackTimes = new HashMap<>();

        for (int i = 0; i < sessions.length(); i++) {

            JSONObject session = sessions.optJSONObject(i);

            if (session == null) {
                continue;
            }

            String title = session.optString("title", "");
            String artist = session.optString("artist", "");
            long duration = session.optLong("duration", 0);

            if (title.isEmpty() || duration <= 0) {
                continue;
            }

            String trackKey = title + " - " + artist;

            long previous = trackTimes.getOrDefault(trackKey, 0L);

            trackTimes.put(trackKey, previous + duration);
        }

        String mostPlayedTrack = "Nenhuma música";
        long highestTime = 0;

        for (String track : trackTimes.keySet()) {

            if (trackTimes.get(track) > highestTime) {
                highestTime = trackTimes.get(track);
                mostPlayedTrack = track;
            }
        }

        return mostPlayedTrack;
    }
    public static String getTopArtists(JSONObject history) {

        JSONArray sessions = getUniqueSessions(history);

        if (sessions == null) {
            return "Nenhum artista";
        }

        Map<String, Long> artistTimes = new HashMap<>();

        for (int i = 0; i < sessions.length(); i++) {

            JSONObject session = sessions.optJSONObject(i);

            if (session == null) {
                continue;
            }

            String artist = session.optString("artist", "");
            long duration = session.optLong("duration", 0);

            if (artist.isEmpty() || duration <= 0) {
                continue;
            }

            long previous = artistTimes.getOrDefault(artist, 0L);

            artistTimes.put(artist, previous + duration);
        }

        java.util.List<Map.Entry<String, Long>> ranking =
                new java.util.ArrayList<>(artistTimes.entrySet());

        ranking.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < Math.min(5, ranking.size()); i++) {

            Map.Entry<String, Long> artist = ranking.get(i);

            result.append(i + 1)
                    .append(". ")
                    .append(artist.getKey())
                    .append(" — ")
                    .append(formatarMinutos(artist.getValue()))
                    .append("\n");
        }

        return result.toString();
    }
    public static String getTopTracks(JSONObject history) {

        JSONArray sessions = getUniqueSessions(history);

        if (sessions == null) {
            return "Nenhuma música";
        }

        Map<String, Long> trackTimes = new HashMap<>();

        for (int i = 0; i < sessions.length(); i++) {

            JSONObject session = sessions.optJSONObject(i);

            if (session == null) {
                continue;
            }

            String title = session.optString("title", "");
            String artist = session.optString("artist", "");
            long duration = session.optLong("duration", 0);

            if (title.isEmpty() || duration <= 0) {
                continue;
            }

            String trackKey = title + "\n" + artist;

            long previous = trackTimes.getOrDefault(trackKey, 0L);

            trackTimes.put(trackKey, previous + duration);
        }

        java.util.List<Map.Entry<String, Long>> ranking =
                new java.util.ArrayList<>(trackTimes.entrySet());

        ranking.sort((a, b) ->
                Long.compare(b.getValue(), a.getValue()));

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < Math.min(5, ranking.size()); i++) {

            Map.Entry<String, Long> track = ranking.get(i);

            String[] info = track.getKey().split("\n", 2);

            result.append(i + 1)
                    .append(". ")
                    .append(info[0])
                    .append(" — ")
                    .append(info.length > 1 ? info[1] : "Artista desconhecido")
                    .append(" — ")
                    .append(formatarMinutos(track.getValue()))
                    .append("\n");
        }

        return result.toString();
    }
    public static String getWeeklyTopArtists(JSONObject history) {

        JSONArray sessions = getUniqueSessions(history);

        if (sessions == null) {
            return "Nenhum artista";
        }

        java.util.Calendar calendar = java.util.Calendar.getInstance();

        calendar.setFirstDayOfWeek(java.util.Calendar.MONDAY);
        calendar.set(java.util.Calendar.DAY_OF_WEEK,
                java.util.Calendar.MONDAY);
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);

        java.text.SimpleDateFormat formatter =
                new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);

        String weekStart = formatter.format(calendar.getTime());

        calendar.add(java.util.Calendar.DAY_OF_YEAR, 7);

        String weekEnd = formatter.format(calendar.getTime());

        Map<String, Long> artistTimes = new HashMap<>();

        for (int i = 0; i < sessions.length(); i++) {

            JSONObject session = sessions.optJSONObject(i);

            if (session == null) continue;

            String date = session.optString("date", "");
            String artist = session.optString("artist", "");
            long duration = session.optLong("duration", 0);

            if (date.length() < 10 || artist.isEmpty() || duration <= 0) {
                continue;
            }

            String day = date.substring(0, 10);

            if (day.compareTo(weekStart) >= 0 &&
                    day.compareTo(weekEnd) < 0) {

                long previous = artistTimes.getOrDefault(artist, 0L);

                artistTimes.put(artist, previous + duration);
            }
        }

        java.util.List<Map.Entry<String, Long>> ranking =
                new java.util.ArrayList<>(artistTimes.entrySet());

        ranking.sort((a, b) ->
                Long.compare(b.getValue(), a.getValue()));

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < Math.min(5, ranking.size()); i++) {

            Map.Entry<String, Long> artist = ranking.get(i);

            result.append(i + 1)
                    .append(". ")
                    .append(artist.getKey())
                    .append(" — ")
                    .append(formatarMinutos(artist.getValue()))
                    .append("\n");
        }

        return result.length() == 0 ? "Nenhum artista" : result.toString();
    }
    public static String getWeeklyTopTracks(JSONObject history) {

        JSONArray sessions = getUniqueSessions(history);

        if (sessions == null) {
            return "Nenhuma música";
        }

        java.util.Calendar calendar = java.util.Calendar.getInstance();

        calendar.setFirstDayOfWeek(java.util.Calendar.MONDAY);
        calendar.set(java.util.Calendar.DAY_OF_WEEK,
                java.util.Calendar.MONDAY);
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);

        java.text.SimpleDateFormat formatter =
                new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);

        String weekStart = formatter.format(calendar.getTime());

        calendar.add(java.util.Calendar.DAY_OF_YEAR, 7);

        String weekEnd = formatter.format(calendar.getTime());

        Map<String, Long> trackTimes = new HashMap<>();

        for (int i = 0; i < sessions.length(); i++) {

            JSONObject session = sessions.optJSONObject(i);

            if (session == null) continue;

            String date = session.optString("date", "");
            String title = session.optString("title", "");
            String artist = session.optString("artist", "");
            long duration = session.optLong("duration", 0);

            if (date.length() < 10 || title.isEmpty() || duration <= 0) {
                continue;
            }

            String day = date.substring(0, 10);

            if (day.compareTo(weekStart) >= 0 &&
                    day.compareTo(weekEnd) < 0) {

                String key = title + "\n" + artist;

                long previous = trackTimes.getOrDefault(key, 0L);

                trackTimes.put(key, previous + duration);
            }
        }

        java.util.List<Map.Entry<String, Long>> ranking =
                new java.util.ArrayList<>(trackTimes.entrySet());

        ranking.sort((a, b) ->
                Long.compare(b.getValue(), a.getValue()));

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < Math.min(5, ranking.size()); i++) {

            Map.Entry<String, Long> track = ranking.get(i);

            String[] info = track.getKey().split("\n", 2);

            result.append(i + 1)
                    .append(". ")
                    .append(info[0])
                    .append(" — ")
                    .append(info.length > 1 ? info[1] : "Artista desconhecido")
                    .append(" — ")
                    .append(formatarMinutos(track.getValue()))
                    .append("\n");
        }

        return result.length() == 0 ? "Nenhuma música" : result.toString();
    }
    public static String getMonthlyTopArtists(JSONObject history) {

        JSONArray sessions = getUniqueSessions(history);

        if (sessions == null) return "Nenhum artista";

        java.util.Calendar calendar = java.util.Calendar.getInstance();

        int currentYear = calendar.get(java.util.Calendar.YEAR);
        int currentMonth = calendar.get(java.util.Calendar.MONTH) + 1;

        Map<String, Long> artistTimes = new HashMap<>();

        for (int i = 0; i < sessions.length(); i++) {

            JSONObject session = sessions.optJSONObject(i);
            if (session == null) continue;

            String date = session.optString("date", "");
            String artist = session.optString("artist", "");
            long duration = session.optLong("duration", 0);

            if (date.length() < 7 || artist.isEmpty() || duration <= 0) {
                continue;
            }

            try {
                int year = Integer.parseInt(date.substring(0, 4));
                int month = Integer.parseInt(date.substring(5, 7));

                if (year == currentYear && month == currentMonth) {
                    artistTimes.put(
                            artist,
                            artistTimes.getOrDefault(artist, 0L) + duration
                    );
                }

            } catch (NumberFormatException ignored) {
            }
        }

        java.util.List<Map.Entry<String, Long>> ranking =
                new java.util.ArrayList<>(artistTimes.entrySet());

        ranking.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < Math.min(5, ranking.size()); i++) {
            Map.Entry<String, Long> artist = ranking.get(i);

            result.append(i + 1)
                    .append(". ")
                    .append(artist.getKey())
                    .append(" — ")
                    .append(formatarMinutos(artist.getValue()))
                    .append("\n");
        }

        return result.length() == 0 ? "Nenhum artista" : result.toString();
    }
    public static String getMonthlyTopTracks(JSONObject history) {

        JSONArray sessions = getUniqueSessions(history);

        if (sessions == null) return "Nenhuma música";

        java.util.Calendar calendar = java.util.Calendar.getInstance();

        int currentYear = calendar.get(java.util.Calendar.YEAR);
        int currentMonth = calendar.get(java.util.Calendar.MONTH) + 1;

        Map<String, Long> trackTimes = new HashMap<>();

        for (int i = 0; i < sessions.length(); i++) {

            JSONObject session = sessions.optJSONObject(i);
            if (session == null) continue;

            String date = session.optString("date", "");
            String title = session.optString("title", "");
            String artist = session.optString("artist", "");
            long duration = session.optLong("duration", 0);

            if (date.length() < 7 || title.isEmpty() || duration <= 0) {
                continue;
            }

            try {
                int year = Integer.parseInt(date.substring(0, 4));
                int month = Integer.parseInt(date.substring(5, 7));

                if (year == currentYear && month == currentMonth) {

                    String key = title + "\n" + artist;

                    trackTimes.put(
                            key,
                            trackTimes.getOrDefault(key, 0L) + duration
                    );
                }

            } catch (NumberFormatException ignored) {
            }
        }

        java.util.List<Map.Entry<String, Long>> ranking =
                new java.util.ArrayList<>(trackTimes.entrySet());

        ranking.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < Math.min(5, ranking.size()); i++) {

            Map.Entry<String, Long> track = ranking.get(i);

            String[] info = track.getKey().split("\n", 2);

            result.append(i + 1)
                    .append(". ")
                    .append(info[0])
                    .append(" — ")
                    .append(info.length > 1 ? info[1] : "Artista desconhecido")
                    .append(" — ")
                    .append(formatarMinutos(track.getValue()))
                    .append("\n");
        }

        return result.length() == 0 ? "Nenhuma música" : result.toString();
    }
    public static String getYearlyTopArtists(JSONObject history) {

        JSONArray sessions = getUniqueSessions(history);

        if (sessions == null) return "Nenhum artista";

        int currentYear = java.util.Calendar.getInstance()
                .get(java.util.Calendar.YEAR);

        Map<String, Long> artistTimes = new HashMap<>();

        for (int i = 0; i < sessions.length(); i++) {

            JSONObject session = sessions.optJSONObject(i);
            if (session == null) continue;

            String date = session.optString("date", "");
            String artist = session.optString("artist", "");
            long duration = session.optLong("duration", 0);

            if (date.length() < 4 || artist.isEmpty() || duration <= 0) {
                continue;
            }

            try {
                int year = Integer.parseInt(date.substring(0, 4));

                if (year == currentYear) {
                    artistTimes.put(
                            artist,
                            artistTimes.getOrDefault(artist, 0L) + duration
                    );
                }

            } catch (NumberFormatException ignored) {
            }
        }

        java.util.List<Map.Entry<String, Long>> ranking =
                new java.util.ArrayList<>(artistTimes.entrySet());

        ranking.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < Math.min(5, ranking.size()); i++) {

            Map.Entry<String, Long> artist = ranking.get(i);

            result.append(i + 1)
                    .append(". ")
                    .append(artist.getKey())
                    .append(" — ")
                    .append(formatarMinutos(artist.getValue()))
                    .append("\n");
        }

        return result.length() == 0 ? "Nenhum artista" : result.toString();
    }
    public static String getYearlyTopTracks(JSONObject history) {

        JSONArray sessions = getUniqueSessions(history);

        if (sessions == null) return "Nenhuma música";

        int currentYear = java.util.Calendar.getInstance()
                .get(java.util.Calendar.YEAR);

        Map<String, Long> trackTimes = new HashMap<>();

        for (int i = 0; i < sessions.length(); i++) {

            JSONObject session = sessions.optJSONObject(i);
            if (session == null) continue;

            String date = session.optString("date", "");
            String title = session.optString("title", "");
            String artist = session.optString("artist", "");
            long duration = session.optLong("duration", 0);

            if (date.length() < 4 || title.isEmpty() || duration <= 0) {
                continue;
            }

            try {
                int year = Integer.parseInt(date.substring(0, 4));

                if (year == currentYear) {

                    String key = title + "\n" + artist;

                    trackTimes.put(
                            key,
                            trackTimes.getOrDefault(key, 0L) + duration
                    );
                }

            } catch (NumberFormatException ignored) {
            }
        }

        java.util.List<Map.Entry<String, Long>> ranking =
                new java.util.ArrayList<>(trackTimes.entrySet());

        ranking.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < Math.min(5, ranking.size()); i++) {

            Map.Entry<String, Long> track = ranking.get(i);

            String[] info = track.getKey().split("\n", 2);

            result.append(i + 1)
                    .append(". ")
                    .append(info[0])
                    .append(" — ")
                    .append(info.length > 1 ? info[1] : "Artista desconhecido")
                    .append(" — ")
                    .append(formatarMinutos(track.getValue()))
                    .append("\n");
        }

        return result.length() == 0 ? "Nenhuma música" : result.toString();
    }

    public static String getBestDay(JSONObject history) {

        Map<String, Long> dailyTotals = getDailyTotals(history);

        String bestDay = "Nenhum registro";
        long highestTime = 0;

        for (Map.Entry<String, Long> entry : dailyTotals.entrySet()) {

            if (entry.getValue() > highestTime) {
                highestTime = entry.getValue();
                bestDay = entry.getKey();
            }
        }

        return bestDay;
    }

    public static long getBestDayTime(JSONObject history) {

        Map<String, Long> dailyTotals = getDailyTotals(history);

        long highestTime = 0;

        for (long duration : dailyTotals.values()) {

            if (duration > highestTime) {
                highestTime = duration;
            }
        }

        return highestTime;
    }

    public static int getLongestStreak(JSONObject history) {

        Map<String, Long> dailyTotals = getDailyTotals(history);

        Set<LocalDate> playbackDays = new HashSet<>();

        for (String date : dailyTotals.keySet()) {

            try {
                playbackDays.add(LocalDate.parse(date));
            } catch (DateTimeParseException ignored) {
            }
        }

        int longestStreak = 0;

        for (LocalDate day : playbackDays) {

            // Só começa a contar quando não existe reprodução no dia anterior.
            if (!playbackDays.contains(day.minusDays(1))) {

                int currentStreak = 1;
                LocalDate nextDay = day.plusDays(1);

                while (playbackDays.contains(nextDay)) {
                    currentStreak++;
                    nextDay = nextDay.plusDays(1);
                }

                if (currentStreak > longestStreak) {
                    longestStreak = currentStreak;
                }
            }
        }

        return longestStreak;
    }

    public static long getNextMilestone(long totalTime) {

        long[] milestones = {
                10 * 3600L,
                50 * 3600L,
                100 * 3600L,
                500 * 3600L
        };

        for (long milestone : milestones) {

            if (totalTime < milestone) {
                return milestone;
            }
        }

        return 0;
    }
    private static String formatarMinutos(long segundos) {

        long minutos = segundos / 60;

        if (minutos == 0) {
            return "<1 min";
        }

        return minutos + " min";
    }
}
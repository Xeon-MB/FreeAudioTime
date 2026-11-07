package com.example.freeaudiotime;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PlaybackStatisticsPeriodTest {

    private JSONObject sampleHistory() throws Exception {
        JSONArray sessions = new JSONArray();

        sessions.put(new JSONObject()
                .put("date", "2026-10-26")
                .put("duration", 120)
                .put("artist", "Artista A")
                .put("title", "Faixa 1")
                .put("playbackId", "play-1"));

        sessions.put(new JSONObject()
                .put("date", "2026-10-28")
                .put("duration", 60)
                .put("artist", "Artista B")
                .put("title", "Faixa 2")
                .put("playbackId", "play-2"));

        // Fora do intervalo: não deve entrar nos resultados.
        sessions.put(new JSONObject()
                .put("date", "2026-11-02")
                .put("duration", 900)
                .put("artist", "Artista C")
                .put("title", "Faixa 3")
                .put("playbackId", "play-3"));

        return new JSONObject().put("sessions", sessions);
    }

    @Test
    public void totalsOnlyIncludeSelectedDateRange() throws Exception {
        JSONObject history = sampleHistory();

        long total = PlaybackStatistics.getTotalBetweenDates(
                history,
                LocalDate.parse("2026-10-26"),
                LocalDate.parse("2026-11-01"));

        assertEquals(180L, total);
    }

    @Test
    public void artistRankingOnlyIncludesSelectedDateRange() throws Exception {
        String ranking = PlaybackStatistics.getTopArtistsBetweenDates(
                sampleHistory(),
                LocalDate.parse("2026-10-26"),
                LocalDate.parse("2026-11-01"));

        assertTrue(ranking.contains("Artista A"));
        assertTrue(ranking.contains("Artista B"));
        assertTrue(!ranking.contains("Artista C"));
    }

    @Test
    public void trackRankingOnlyIncludesSelectedDateRange() throws Exception {
        String ranking = PlaybackStatistics.getTopTracksBetweenDates(
                sampleHistory(),
                LocalDate.parse("2026-10-26"),
                LocalDate.parse("2026-11-01"));

        assertTrue(ranking.contains("Faixa 1"));
        assertTrue(ranking.contains("Faixa 2"));
        assertTrue(!ranking.contains("Faixa 3"));
    }
}

package com.example.freeaudiotime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolved dynamic recap statistics for a specific period and date.
 * Populated at runtime from PlaybackStatistics, GoalStorage, and AchievementStorage.
 */
public class DynamicRecapData {

    private final RecapCategory category;
    private final String periodTitle;
    private final String periodDateRange;
    private final Map<RecapElementBinding, String> textValues;
    private final List<TopArtistItem> secondaryArtists;
    private final List<TopTrackItem> topTracks;
    private final List<AchievementItem> unlockedAchievements;

    public DynamicRecapData(RecapCategory category, String periodTitle, String periodDateRange) {
        this.category = category;
        this.periodTitle = periodTitle;
        this.periodDateRange = periodDateRange;
        this.textValues = new HashMap<>();
        this.secondaryArtists = new ArrayList<>();
        this.topTracks = new ArrayList<>();
        this.unlockedAchievements = new ArrayList<>();
    }

    public void putValue(RecapElementBinding binding, String value) {
        textValues.put(binding, value);
    }

    public String getValue(RecapElementBinding binding) {
        return textValues.get(binding);
    }

    public boolean hasValue(RecapElementBinding binding) {
        String val = textValues.get(binding);
        return val != null && !val.trim().isEmpty() && !val.equals("—");
    }

    public RecapCategory getCategory() {
        return category;
    }

    public String getPeriodTitle() {
        return periodTitle;
    }

    public String getPeriodDateRange() {
        return periodDateRange;
    }

    public List<TopArtistItem> getSecondaryArtists() {
        return secondaryArtists;
    }

    public List<TopTrackItem> getTopTracks() {
        return topTracks;
    }

    public List<AchievementItem> getUnlockedAchievements() {
        return unlockedAchievements;
    }

    // Inner model items
    public static class TopArtistItem {
        public final int rank;
        public final String name;
        public final String timeFormatted;

        public TopArtistItem(int rank, String name, String timeFormatted) {
            this.rank = rank;
            this.name = name;
            this.timeFormatted = timeFormatted;
        }
    }

    public static class TopTrackItem {
        public final String title;
        public final String artist;
        public final String duration;
        public String artworkPath;

        public TopTrackItem(String title, String artist, String duration, String artworkPath) {
            this.title = title;
            this.artist = artist;
            this.duration = duration;
            this.artworkPath = artworkPath;
        }
    }

    public static class AchievementItem {
        public final String id;
        public final String title;
        public final String formattedDate;

        public AchievementItem(String id, String title, String formattedDate) {
            this.id = id;
            this.title = title;
            this.formattedDate = formattedDate;
        }
    }
}

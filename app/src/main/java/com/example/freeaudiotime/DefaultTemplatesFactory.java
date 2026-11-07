package com.example.freeaudiotime;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates built-in default templates.
 * Preserves the exact Brutalist / High-Contrast aesthetic from recap_share.xml as the standard fallback.
 */
public class DefaultTemplatesFactory {

    /**
     * Creates the classic Brutalist template matching recap_share.xml layout.
     */
    public static RecapTemplate createDefaultBrutalist() {
        RecapTemplate template = new RecapTemplate(
                RecapTemplate.DEFAULT_TEMPLATE_ID,
                "Brutalista Clássico (Padrão)",
                true
        );
        template.setDescription("Design oficial de alta densidade e tipografia contrastante do FreeAudioTime.");
        template.setBackgroundColorHex("#080A08");
        template.setAccentColorHex("#B7FF00");
        template.setSecondaryBgColorHex("#111511");
        template.setTextColorHex("#F0EBDD");
        template.setUseDynamicPalette(true);

        List<RecapElementModel> elements = new ArrayList<>();

        // 1. Header: Brand & Volume
        RecapElementModel brand = new RecapElementModel(RecapElementBinding.APP_BRAND, 0.05f, 0.02f, 0.65f, 0.04f);
        brand.setFontSizeSp(24f);
        brand.setBold(true);
        brand.setFontColorHex("#B7FF00");
        elements.add(brand);

        RecapElementModel volume = new RecapElementModel(RecapElementBinding.VOLUME_TAG, 0.72f, 0.025f, 0.23f, 0.03f);
        volume.setFontSizeSp(10f);
        volume.setFontColorHex("#F0EBDD");
        volume.setAlignment("RIGHT");
        elements.add(volume);

        // Period Title & Range
        RecapElementModel period = new RecapElementModel(RecapElementBinding.PERIOD_RANGE, 0.05f, 0.065f, 0.90f, 0.035f);
        period.setFontSizeSp(13f);
        period.setBold(true);
        period.setFontColorHex("#B7FF00");
        elements.add(period);

        // 2. Listening Time Card
        RecapElementModel totalTimeLabel = new RecapElementModel(RecapElementBinding.TOTAL_TIME_LABEL, 0.08f, 0.11f, 0.84f, 0.025f);
        totalTimeLabel.setFontSizeSp(11f);
        totalTimeLabel.setBold(true);
        totalTimeLabel.setFontColorHex("#080A08");
        totalTimeLabel.setBackgroundColorHex("#B7FF00");
        elements.add(totalTimeLabel);

        RecapElementModel totalTime = new RecapElementModel(RecapElementBinding.TOTAL_TIME_FORMATTED, 0.08f, 0.135f, 0.84f, 0.07f);
        totalTime.setFontSizeSp(44f);
        totalTime.setBold(true);
        totalTime.setFontColorHex("#080A08");
        elements.add(totalTime);

        RecapElementModel totalMinutes = new RecapElementModel(RecapElementBinding.TOTAL_MINUTES_SUB, 0.08f, 0.205f, 0.84f, 0.025f);
        totalMinutes.setFontSizeSp(11f);
        totalMinutes.setBold(true);
        totalMinutes.setAlignment("RIGHT");
        totalMinutes.setFontColorHex("#080A08");
        elements.add(totalMinutes);

        // 3. Top Artist Section
        RecapElementModel artistImage = new RecapElementModel(RecapElementBinding.TOP_ARTIST_IMAGE, 0.05f, 0.25f, 0.52f, 0.19f);
        artistImage.setBackgroundColorHex("#252823");
        elements.add(artistImage);

        RecapElementModel artistTag = new RecapElementModel(RecapElementBinding.TOP_ARTIST_TAG, 0.05f, 0.45f, 0.35f, 0.025f);
        artistTag.setFontSizeSp(10f);
        artistTag.setBold(true);
        artistTag.setFontColorHex("#080A08");
        artistTag.setBackgroundColorHex("#B7FF00");
        elements.add(artistTag);

        RecapElementModel artistName = new RecapElementModel(RecapElementBinding.TOP_ARTIST_NAME, 0.05f, 0.48f, 0.52f, 0.05f);
        artistName.setFontSizeSp(24f);
        artistName.setBold(true);
        artistName.setFontColorHex("#F0EBDD");
        elements.add(artistName);

        RecapElementModel artistTime = new RecapElementModel(RecapElementBinding.TOP_ARTIST_TIME, 0.05f, 0.535f, 0.52f, 0.035f);
        artistTime.setFontSizeSp(17f);
        artistTime.setBold(true);
        artistTime.setFontColorHex("#B7FF00");
        elements.add(artistTime);

        // 4. Discovery Card
        RecapElementModel discoveryLabel = new RecapElementModel(RecapElementBinding.DISCOVERY_LABEL, 0.60f, 0.25f, 0.35f, 0.025f);
        discoveryLabel.setFontSizeSp(12f);
        discoveryLabel.setBold(true);
        discoveryLabel.setFontColorHex("#B7FF00");
        discoveryLabel.setBackgroundColorHex("#111511");
        elements.add(discoveryLabel);

        RecapElementModel artistCount = new RecapElementModel(RecapElementBinding.UNIQUE_ARTISTS_COUNT, 0.60f, 0.285f, 0.35f, 0.07f);
        artistCount.setFontSizeSp(36f);
        artistCount.setBold(true);
        artistCount.setFontColorHex("#F0EBDD");
        elements.add(artistCount);

        RecapElementModel artistCountLabel = new RecapElementModel(RecapElementBinding.UNIQUE_ARTISTS_LABEL, 0.60f, 0.355f, 0.35f, 0.025f);
        artistCountLabel.setFontSizeSp(10f);
        artistCountLabel.setBold(true);
        artistCountLabel.setFontColorHex("#F0EBDD");
        elements.add(artistCountLabel);

        // 5. Also in Rotation
        RecapElementModel rotationList = new RecapElementModel(RecapElementBinding.SECONDARY_ARTISTS_LIST, 0.05f, 0.58f, 0.90f, 0.09f);
        rotationList.setBackgroundColorHex("#111511");
        rotationList.setHideIfMissing(true);
        elements.add(rotationList);

        // 6. Peak Moment / Best Day
        RecapElementModel bestDayTitle = new RecapElementModel(RecapElementBinding.BEST_DAY_TITLE, 0.05f, 0.68f, 0.90f, 0.025f);
        bestDayTitle.setFontSizeSp(11f);
        bestDayTitle.setBold(true);
        bestDayTitle.setFontColorHex("#B7FF00");
        bestDayTitle.setBackgroundColorHex("#111511");
        elements.add(bestDayTitle);

        RecapElementModel bestDayName = new RecapElementModel(RecapElementBinding.BEST_DAY_NAME, 0.05f, 0.705f, 0.90f, 0.04f);
        bestDayName.setFontSizeSp(22f);
        bestDayName.setBold(true);
        bestDayName.setFontColorHex("#F0EBDD");
        elements.add(bestDayName);

        RecapElementModel bestDayTime = new RecapElementModel(RecapElementBinding.BEST_DAY_TIME, 0.05f, 0.745f, 0.90f, 0.03f);
        bestDayTime.setFontSizeSp(14f);
        bestDayTime.setBold(true);
        bestDayTime.setFontColorHex("#B7FF00");
        elements.add(bestDayTime);

        // 7. Unlocked Achievements
        RecapElementModel achievements = new RecapElementModel(RecapElementBinding.ACHIEVEMENTS_LIST, 0.05f, 0.785f, 0.90f, 0.08f);
        achievements.setHideIfMissing(true); // Can hide if none unlocked in period
        achievements.setFallbackReplacementText("");
        elements.add(achievements);

        // 8. Top Tracks
        RecapElementModel topTracks = new RecapElementModel(RecapElementBinding.TOP_TRACKS_LIST, 0.05f, 0.87f, 0.90f, 0.085f);
        topTracks.setHideIfMissing(false);
        elements.add(topTracks);

        // 9. Footer
        RecapElementModel footer1 = new RecapElementModel(RecapElementBinding.FOOTER_LINE_1, 0.05f, 0.96f, 0.90f, 0.018f);
        footer1.setFontSizeSp(11f);
        footer1.setBold(true);
        footer1.setFontColorHex("#B7FF00");
        elements.add(footer1);

        RecapElementModel footer2 = new RecapElementModel(RecapElementBinding.FOOTER_LINE_2, 0.05f, 0.98f, 0.90f, 0.018f);
        footer2.setFontSizeSp(9f);
        footer2.setFontColorHex("#F0EBDD");
        elements.add(footer2);

        template.setElements(elements);
        return template;
    }

    /**
     * Clean Minimalist alternative preset.
     */
    public static RecapTemplate createMinimalistDark() {
        RecapTemplate template = new RecapTemplate(
                "preset_minimalist_dark",
                "Minimalista Noturno",
                true
        );
        template.setDescription("Foco visual em tipografia limpa e destaque sutil no artista favorito.");
        template.setBackgroundColorHex("#121212");
        template.setAccentColorHex("#1DB954");
        template.setSecondaryBgColorHex("#282828");
        template.setTextColorHex("#FFFFFF");
        template.setUseDynamicPalette(true);

        List<RecapElementModel> elements = new ArrayList<>();

        RecapElementModel brand = new RecapElementModel(RecapElementBinding.APP_BRAND, 0.08f, 0.04f, 0.84f, 0.04f);
        brand.setFontSizeSp(20f);
        brand.setBold(true);
        brand.setFontColorHex("#1DB954");
        elements.add(brand);

        RecapElementModel period = new RecapElementModel(RecapElementBinding.PERIOD_RANGE, 0.08f, 0.085f, 0.84f, 0.03f);
        period.setFontSizeSp(14f);
        period.setFontColorHex("#B3B3B3");
        elements.add(period);

        RecapElementModel total = new RecapElementModel(RecapElementBinding.TOTAL_TIME_FORMATTED, 0.08f, 0.13f, 0.84f, 0.08f);
        total.setFontSizeSp(48f);
        total.setBold(true);
        total.setFontColorHex("#FFFFFF");
        elements.add(total);

        RecapElementModel artistImg = new RecapElementModel(RecapElementBinding.TOP_ARTIST_IMAGE, 0.08f, 0.23f, 0.84f, 0.32f);
        artistImg.setBackgroundColorHex("#181818");
        elements.add(artistImg);

        RecapElementModel artistName = new RecapElementModel(RecapElementBinding.TOP_ARTIST_NAME, 0.08f, 0.57f, 0.84f, 0.06f);
        artistName.setFontSizeSp(28f);
        artistName.setBold(true);
        artistName.setFontColorHex("#FFFFFF");
        elements.add(artistName);

        RecapElementModel artistTime = new RecapElementModel(RecapElementBinding.TOP_ARTIST_TIME, 0.08f, 0.63f, 0.84f, 0.035f);
        artistTime.setFontSizeSp(18f);
        artistTime.setBold(true);
        artistTime.setFontColorHex("#1DB954");
        elements.add(artistTime);

        RecapElementModel tracks = new RecapElementModel(RecapElementBinding.TOP_TRACKS_LIST, 0.08f, 0.69f, 0.84f, 0.22f);
        elements.add(tracks);

        template.setElements(elements);
        return template;
    }
}

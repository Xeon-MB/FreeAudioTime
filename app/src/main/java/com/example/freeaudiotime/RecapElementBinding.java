package com.example.freeaudiotime;

public enum RecapElementBinding {
    // Header & Period
    APP_BRAND("app_brand", "Marca (FREEAUDIOTIME®)", "FREEAUDIOTIME®"),
    VOLUME_TAG("volume_tag", "Volume / Edição", "VOL. 001"),
    PERIOD_TITLE("period_title", "Título do Período", "RECAP SEMANAL"),
    PERIOD_RANGE("period_range", "Intervalo de Datas", "01 NOV — 07 NOV 2026"),

    // Listening Time
    TOTAL_TIME_LABEL("total_time_label", "Rótulo Tempo Total", "TOTAL LISTENING TIME"),
    TOTAL_TIME_FORMATTED("total_time_formatted", "Tempo Total (Horas/Minutos)", "18H 42MIN"),
    TOTAL_MINUTES_SUB("total_minutes_sub", "Total em Minutos", "/// 1122 MIN"),

    // Top Artist
    TOP_ARTIST_TAG("top_artist_tag", "Rótulo Artista Nº 1", "ARTIST Nº 01"),
    TOP_ARTIST_NAME("top_artist_name", "Nome do Artista Nº 1", "LAUFEY"),
    TOP_ARTIST_TIME("top_artist_time", "Tempo do Artista Nº 1", "7H 25MIN"),
    TOP_ARTIST_IMAGE("top_artist_image", "Foto do Artista Nº 1", null),

    // Discovery / Counts
    DISCOVERY_LABEL("discovery_label", "Rótulo Descoberta", "DISCOVERY"),
    UNIQUE_ARTISTS_COUNT("unique_artists_count", "Total de Artistas Diferentes", "12"),
    UNIQUE_ARTISTS_LABEL("unique_artists_label", "Subtítulo Artistas", "ARTISTS"),

    // Secondary Artists & Best Day
    ALSO_IN_ROTATION_TITLE("also_in_rotation_title", "Rótulo Também Ouvidos", "ALSO IN ROTATION"),
    SECONDARY_ARTISTS_LIST("secondary_artists_list", "Top 2 e Top 3 Artistas", null),
    BEST_DAY_TITLE("best_day_title", "Título Melhor Momento", "PEAK DAY / BEST MOMENT"),
    BEST_DAY_NAME("best_day_name", "Nome do Melhor Dia / Mês", "FRIDAY"),
    BEST_DAY_TIME("best_day_time", "Tempo do Melhor Dia", "3H 18MIN"),

    // Unlocked Achievements
    ACHIEVEMENTS_TITLE("achievements_title", "Título Conquistas", "UNLOCKED ACHIEVEMENTS"),
    ACHIEVEMENTS_LIST("achievements_list", "Lista de Conquistas", null),

    // Top Tracks
    TOP_TRACKS_TITLE("top_tracks_title", "Título Melhores Músicas", "YOUR TOP TRACKS"),
    TOP_TRACKS_LIST("top_tracks_list", "Lista Top Músicas com Capa", null),

    // Footer
    FOOTER_LINE_1("footer_line_1", "Linha 1 Rodapé", "A PERSONAL MUSIC ARCHIVE"),
    FOOTER_LINE_2("footer_line_2", "Linha 2 Rodapé", "EVERY MINUTE HAS A STORY.  //  FAT® 2026"),

    // Custom Static Elements
    CUSTOM_STATIC_TEXT("custom_static_text", "Texto Personalizado", "Texto"),
    CUSTOM_STATIC_IMAGE("custom_static_image", "Imagem / Ícone Personalizado", null);

    private final String key;
    private final String displayName;
    private final String fallbackDefault;

    RecapElementBinding(String key, String displayName, String fallbackDefault) {
        this.key = key;
        this.displayName = displayName;
        this.fallbackDefault = fallbackDefault;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFallbackDefault() {
        return fallbackDefault;
    }

    public static RecapElementBinding fromKey(String key) {
        if (key == null) return CUSTOM_STATIC_TEXT;
        for (RecapElementBinding binding : values()) {
            if (binding.key.equalsIgnoreCase(key) || binding.name().equalsIgnoreCase(key)) {
                return binding;
            }
        }
        return CUSTOM_STATIC_TEXT;
    }
}

package com.example.freeaudiotime;

public enum RecapCategory {
    DAILY("dia", "Diário"),
    WEEKLY("semana", "Semanal"),
    MONTHLY("mes", "Mensal"),
    YEARLY("ano", "Anual");

    private final String id;
    private final String displayName;

    RecapCategory(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static RecapCategory fromPeriodInt(int period) {
        switch (period) {
            case 3: // DIA
                return DAILY;
            case 1: // MES
                return MONTHLY;
            case 2: // ANO
                return YEARLY;
            case 0: // SEMANA
            default:
                return WEEKLY;
        }
    }

    public static RecapCategory fromId(String id) {
        if (id == null) return WEEKLY;
        for (RecapCategory cat : values()) {
            if (cat.id.equalsIgnoreCase(id) || cat.name().equalsIgnoreCase(id)) {
                return cat;
            }
        }
        return WEEKLY;
    }
}

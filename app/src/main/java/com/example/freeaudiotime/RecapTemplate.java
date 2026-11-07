package com.example.freeaudiotime;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Model representing a complete Recap Template.
 * Stores only visual layout and design configurations, without hardcoding static statistical values.
 */
public class RecapTemplate {

    public static final String DEFAULT_TEMPLATE_ID = "default_brutalist";

    private String id;
    private String name;
    private String description;
    private boolean isSystemPreset;
    private long createdAt;
    private long updatedAt;

    // Canvas Configuration
    private int canvasWidthPx;
    private int canvasHeightPx;
    private String backgroundColorHex; // e.g. "#080A08"
    private String bgSecondaryColorHex; // For gradients (e.g. "#1B2A1E")
    private String bgStyle;             // "SOLID", "GRADIENT_LINEAR", "GRADIENT_RADIAL", "PATTERN_DOTS", "PATTERN_GRID"
    private String accentColorHex;      // e.g. "#B7FF00"
    private String secondaryBgColorHex; // e.g. "#111511"
    private String textColorHex;        // e.g. "#F0EBDD"
    private boolean useDynamicPalette;  // If true, colors adapt dynamically from top artist image

    // List of configurable elements
    private List<RecapElementModel> elements;

    public RecapTemplate() {
        this.id = UUID.randomUUID().toString();
        this.name = "Novo Modelo";
        this.description = "";
        this.isSystemPreset = false;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.canvasWidthPx = 1080;
        this.canvasHeightPx = 1920;
        this.backgroundColorHex = "#080A08";
        this.bgSecondaryColorHex = "#161D15";
        this.bgStyle = "SOLID";
        this.accentColorHex = "#B7FF00";
        this.secondaryBgColorHex = "#111511";
        this.textColorHex = "#F0EBDD";
        this.useDynamicPalette = true;
        this.elements = new ArrayList<>();
    }

    public RecapTemplate(String id, String name, boolean isSystemPreset) {
        this();
        this.id = id;
        this.name = name;
        this.isSystemPreset = isSystemPreset;
    }

    /**
     * Creates an independent deep copy of this template.
     * Guaranteed to have a new unique ID and not affect the original.
     */
    public RecapTemplate duplicate(String newName) {
        RecapTemplate copy = new RecapTemplate();
        copy.id = UUID.randomUUID().toString();
        copy.name = newName != null && !newName.trim().isEmpty() ? newName.trim() : (this.name + " (Cópia)");
        copy.description = this.description;
        copy.isSystemPreset = false; // Duplicates are always user-editable
        copy.createdAt = System.currentTimeMillis();
        copy.updatedAt = System.currentTimeMillis();
        copy.canvasWidthPx = this.canvasWidthPx;
        copy.canvasHeightPx = this.canvasHeightPx;
        copy.backgroundColorHex = this.backgroundColorHex;
        copy.bgSecondaryColorHex = this.bgSecondaryColorHex;
        copy.bgStyle = this.bgStyle;
        copy.accentColorHex = this.accentColorHex;
        copy.secondaryBgColorHex = this.secondaryBgColorHex;
        copy.textColorHex = this.textColorHex;
        copy.useDynamicPalette = this.useDynamicPalette;

        List<RecapElementModel> clonedElements = new ArrayList<>();
        if (this.elements != null) {
            for (RecapElementModel element : this.elements) {
                clonedElements.add(element.copy());
            }
        }
        copy.elements = clonedElements;
        return copy;
    }

    /**
     * Creates a history snapshot that preserves template and element identities.
     * Unlike duplicate(), this is intended for undo/redo, not for creating a new template.
     */
    public RecapTemplate snapshot() {
        RecapTemplate copy = duplicate(this.name);
        copy.id = this.id;
        copy.isSystemPreset = this.isSystemPreset;
        copy.createdAt = this.createdAt;
        copy.updatedAt = this.updatedAt;

        if (this.elements != null && copy.elements != null) {
            int count = Math.min(this.elements.size(), copy.elements.size());
            for (int i = 0; i < count; i++) {
                copy.elements.get(i).setId(this.elements.get(i).getId());
            }
        }
        return copy;
    }

    public JSONObject toJsonObject() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("name", name != null ? name : "Sem Nome");
        json.put("description", description != null ? description : "");
        json.put("isSystemPreset", isSystemPreset);
        json.put("createdAt", createdAt);
        json.put("updatedAt", updatedAt);
        json.put("canvasWidthPx", canvasWidthPx);
        json.put("canvasHeightPx", canvasHeightPx);
        json.put("backgroundColorHex", backgroundColorHex != null ? backgroundColorHex : "#080A08");
        json.put("bgSecondaryColorHex", bgSecondaryColorHex != null ? bgSecondaryColorHex : "#161D15");
        json.put("bgStyle", bgStyle != null ? bgStyle : "SOLID");
        json.put("accentColorHex", accentColorHex != null ? accentColorHex : "#B7FF00");
        json.put("secondaryBgColorHex", secondaryBgColorHex != null ? secondaryBgColorHex : "#111511");
        json.put("textColorHex", textColorHex != null ? textColorHex : "#F0EBDD");
        json.put("useDynamicPalette", useDynamicPalette);

        JSONArray elementsArray = new JSONArray();
        if (elements != null) {
            for (RecapElementModel element : elements) {
                elementsArray.put(element.toJsonObject());
            }
        }
        json.put("elements", elementsArray);
        return json;
    }

    public static RecapTemplate fromJsonObject(JSONObject json) {
        if (json == null) return null;
        RecapTemplate template = new RecapTemplate();
        template.id = json.optString("id", UUID.randomUUID().toString());
        template.name = json.optString("name", "Modelo");
        template.description = json.optString("description", "");
        template.isSystemPreset = json.optBoolean("isSystemPreset", false);
        template.createdAt = json.optLong("createdAt", System.currentTimeMillis());
        template.updatedAt = json.optLong("updatedAt", System.currentTimeMillis());
        template.canvasWidthPx = json.optInt("canvasWidthPx", 1080);
        template.canvasHeightPx = json.optInt("canvasHeightPx", 1920);
        template.backgroundColorHex = json.optString("backgroundColorHex", "#080A08");
        template.bgSecondaryColorHex = json.optString("bgSecondaryColorHex", "#161D15");
        template.bgStyle = json.optString("bgStyle", "SOLID");
        template.accentColorHex = json.optString("accentColorHex", "#B7FF00");
        template.secondaryBgColorHex = json.optString("secondaryBgColorHex", "#111511");
        template.textColorHex = json.optString("textColorHex", "#F0EBDD");
        template.useDynamicPalette = json.optBoolean("useDynamicPalette", true);

        List<RecapElementModel> elementList = new ArrayList<>();
        JSONArray elementsArray = json.optJSONArray("elements");
        if (elementsArray != null) {
            for (int i = 0; i < elementsArray.length(); i++) {
                JSONObject elemJson = elementsArray.optJSONObject(i);
                if (elemJson != null) {
                    RecapElementModel elem = RecapElementModel.fromJsonObject(elemJson);
                    if (elem != null) {
                        elementList.add(elem);
                    }
                }
            }
        }
        template.elements = elementList;
        return template;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isSystemPreset() { return isSystemPreset; }
    public void setSystemPreset(boolean systemPreset) { isSystemPreset = systemPreset; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public int getCanvasWidthPx() { return canvasWidthPx; }
    public void setCanvasWidthPx(int canvasWidthPx) { this.canvasWidthPx = canvasWidthPx; }

    public int getCanvasHeightPx() { return canvasHeightPx; }
    public void setCanvasHeightPx(int canvasHeightPx) { this.canvasHeightPx = canvasHeightPx; }

    public String getBackgroundColorHex() { return backgroundColorHex; }
    public void setBackgroundColorHex(String backgroundColorHex) { this.backgroundColorHex = backgroundColorHex; }

    public String getBgSecondaryColorHex() { return bgSecondaryColorHex; }
    public void setBgSecondaryColorHex(String bgSecondaryColorHex) { this.bgSecondaryColorHex = bgSecondaryColorHex; }

    public String getBgStyle() { return bgStyle; }
    public void setBgStyle(String bgStyle) { this.bgStyle = bgStyle; }

    public String getAccentColorHex() { return accentColorHex; }
    public void setAccentColorHex(String accentColorHex) { this.accentColorHex = accentColorHex; }

    public String getSecondaryBgColorHex() { return secondaryBgColorHex; }
    public void setSecondaryBgColorHex(String secondaryBgColorHex) { this.secondaryBgColorHex = secondaryBgColorHex; }

    public String getTextColorHex() { return textColorHex; }
    public void setTextColorHex(String textColorHex) { this.textColorHex = textColorHex; }

    public boolean isUseDynamicPalette() { return useDynamicPalette; }
    public void setUseDynamicPalette(boolean useDynamicPalette) { this.useDynamicPalette = useDynamicPalette; }

    public List<RecapElementModel> getElements() { return elements; }
    public void setElements(List<RecapElementModel> elements) { this.elements = elements; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RecapTemplate)) return false;
        RecapTemplate that = (RecapTemplate) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

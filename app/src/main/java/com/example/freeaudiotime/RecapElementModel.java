package com.example.freeaudiotime;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a single visual element inside a Recap Template.
 * Uses normalized coordinates (0.0 to 1.0) so it scales perfectly across preview and export.
 */
public class RecapElementModel {

    // Identity and Binding
    private String id;
    private RecapElementBinding binding;
    private String customText; // used for custom text or label overrides

    // Normalized Layout (0.0f - 1.0f relative to canvas width/height)
    private float x;
    private float y;
    private float width;
    private float height;
    private float rotation; // degrees

    // Visual Styling
    private int zIndex;
    private boolean visible;
    private String fontColorHex; // e.g. "#B7FF00" or empty for dynamic palette
    private String backgroundColorHex; // e.g. "#111511" or empty
    private float fontSizeSp;
    private boolean bold;
    private boolean italic;
    private String fontFamily; // "SANS_SERIF", "SERIF", "MONOSPACE", "CONDENSED"
    private String alignment; // "LEFT", "CENTER", "RIGHT"
    private float opacity; // 0.0f - 1.0f

    // Custom Image / Sticker
    private String imagePath; // Local path in app files dir or content URI
    private String stickerType; // e.g., "DISCO", "NOTA", "ONDA", "STAR", "HEART", "TROPHY", "FIRE"

    // Dynamic Data Behavior
    private boolean hideIfMissing;
    private String fallbackReplacementText;

    public RecapElementModel() {
        this.id = UUID.randomUUID().toString();
        this.binding = RecapElementBinding.CUSTOM_STATIC_TEXT;
        this.customText = "";
        this.x = 0.05f;
        this.y = 0.05f;
        this.width = 0.9f;
        this.height = 0.05f;
        this.rotation = 0.0f;
        this.zIndex = 0;
        this.visible = true;
        this.fontColorHex = "";
        this.backgroundColorHex = "";
        this.fontSizeSp = 14f;
        this.bold = false;
        this.italic = false;
        this.fontFamily = "SANS_SERIF";
        this.alignment = "LEFT";
        this.opacity = 1.0f;
        this.imagePath = "";
        this.stickerType = "";
        this.hideIfMissing = true;
        this.fallbackReplacementText = "—";
    }

    public RecapElementModel(RecapElementBinding binding, float x, float y, float width, float height) {
        this();
        this.binding = binding;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Creates an independent deep copy of this element.
     */
    public RecapElementModel copy() {
        RecapElementModel copy = new RecapElementModel();
        copy.id = UUID.randomUUID().toString();
        copy.binding = this.binding;
        copy.customText = this.customText;
        copy.x = this.x;
        copy.y = this.y;
        copy.width = this.width;
        copy.height = this.height;
        copy.rotation = this.rotation;
        copy.zIndex = this.zIndex;
        copy.visible = this.visible;
        copy.fontColorHex = this.fontColorHex;
        copy.backgroundColorHex = this.backgroundColorHex;
        copy.fontSizeSp = this.fontSizeSp;
        copy.bold = this.bold;
        copy.italic = this.italic;
        copy.fontFamily = this.fontFamily;
        copy.alignment = this.alignment;
        copy.opacity = this.opacity;
        copy.imagePath = this.imagePath;
        copy.stickerType = this.stickerType;
        copy.hideIfMissing = this.hideIfMissing;
        copy.fallbackReplacementText = this.fallbackReplacementText;
        return copy;
    }

    public JSONObject toJsonObject() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("binding", binding != null ? binding.getKey() : RecapElementBinding.CUSTOM_STATIC_TEXT.getKey());
        json.put("customText", customText != null ? customText : "");
        json.put("x", (double) x);
        json.put("y", (double) y);
        json.put("width", (double) width);
        json.put("height", (double) height);
        json.put("rotation", (double) rotation);
        json.put("zIndex", zIndex);
        json.put("visible", visible);
        json.put("fontColorHex", fontColorHex != null ? fontColorHex : "");
        json.put("backgroundColorHex", backgroundColorHex != null ? backgroundColorHex : "");
        json.put("fontSizeSp", (double) fontSizeSp);
        json.put("bold", bold);
        json.put("italic", italic);
        json.put("fontFamily", fontFamily != null ? fontFamily : "SANS_SERIF");
        json.put("alignment", alignment != null ? alignment : "LEFT");
        json.put("opacity", (double) opacity);
        json.put("imagePath", imagePath != null ? imagePath : "");
        json.put("stickerType", stickerType != null ? stickerType : "");
        json.put("hideIfMissing", hideIfMissing);
        json.put("fallbackReplacementText", fallbackReplacementText != null ? fallbackReplacementText : "—");
        return json;
    }

    public static RecapElementModel fromJsonObject(JSONObject json) {
        if (json == null) return null;
        RecapElementModel model = new RecapElementModel();
        model.id = json.optString("id", UUID.randomUUID().toString());
        model.binding = RecapElementBinding.fromKey(json.optString("binding", ""));
        model.customText = json.optString("customText", "");
        model.x = (float) json.optDouble("x", 0.0);
        model.y = (float) json.optDouble("y", 0.0);
        model.width = (float) json.optDouble("width", 0.9);
        model.height = (float) json.optDouble("height", 0.05);
        model.rotation = (float) json.optDouble("rotation", 0.0);
        model.zIndex = json.optInt("zIndex", 0);
        model.visible = json.optBoolean("visible", true);
        model.fontColorHex = json.optString("fontColorHex", "");
        model.backgroundColorHex = json.optString("backgroundColorHex", "");
        model.fontSizeSp = (float) json.optDouble("fontSizeSp", 14.0);
        model.bold = json.optBoolean("bold", false);
        model.italic = json.optBoolean("italic", false);
        model.fontFamily = json.optString("fontFamily", "SANS_SERIF");
        model.alignment = json.optString("alignment", "LEFT");
        model.opacity = (float) json.optDouble("opacity", 1.0);
        model.imagePath = json.optString("imagePath", "");
        model.stickerType = json.optString("stickerType", "");
        model.hideIfMissing = json.optBoolean("hideIfMissing", true);
        model.fallbackReplacementText = json.optString("fallbackReplacementText", "—");
        return model;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public RecapElementBinding getBinding() { return binding; }
    public void setBinding(RecapElementBinding binding) { this.binding = binding; }

    public String getCustomText() { return customText; }
    public void setCustomText(String customText) { this.customText = customText; }

    public float getX() { return x; }
    public void setX(float x) { this.x = x; }

    public float getY() { return y; }
    public void setY(float y) { this.y = y; }

    public float getWidth() { return width; }
    public void setWidth(float width) { this.width = width; }

    public float getHeight() { return height; }
    public void setHeight(float height) { this.height = height; }

    public float getRotation() { return rotation; }
    public void setRotation(float rotation) { this.rotation = rotation; }

    public int getZIndex() { return zIndex; }
    public void setZIndex(int zIndex) { this.zIndex = zIndex; }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }

    public String getFontColorHex() { return fontColorHex; }
    public void setFontColorHex(String fontColorHex) { this.fontColorHex = fontColorHex; }

    public String getBackgroundColorHex() { return backgroundColorHex; }
    public void setBackgroundColorHex(String backgroundColorHex) { this.backgroundColorHex = backgroundColorHex; }

    public float getFontSizeSp() { return fontSizeSp; }
    public void setFontSizeSp(float fontSizeSp) { this.fontSizeSp = fontSizeSp; }

    public boolean isBold() { return bold; }
    public void setBold(boolean bold) { this.bold = bold; }

    public boolean isItalic() { return italic; }
    public void setItalic(boolean italic) { this.italic = italic; }

    public String getFontFamily() { return fontFamily; }
    public void setFontFamily(String fontFamily) { this.fontFamily = fontFamily; }

    public String getAlignment() { return alignment; }
    public void setAlignment(String alignment) { this.alignment = alignment; }

    public float getOpacity() { return opacity; }
    public void setOpacity(float opacity) { this.opacity = opacity; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public String getStickerType() { return stickerType; }
    public void setStickerType(String stickerType) { this.stickerType = stickerType; }

    public boolean isHideIfMissing() { return hideIfMissing; }
    public void setHideIfMissing(boolean hideIfMissing) { this.hideIfMissing = hideIfMissing; }

    public String getFallbackReplacementText() { return fallbackReplacementText; }
    public void setFallbackReplacementText(String fallbackReplacementText) { this.fallbackReplacementText = fallbackReplacementText; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RecapElementModel)) return false;
        RecapElementModel that = (RecapElementModel) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

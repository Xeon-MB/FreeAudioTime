package com.example.freeaudiotime;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages template persistence, duplication, category default assignments, and lifecycle.
 * All modifications operate on local storage and guarantee that modifying a recap does not alter the underlying template.
 */
public class TemplateStorage {

    private static final String TAG = "TemplateStorage";
    private static final String FILE_TEMPLATES = "recap_templates.json";
    private static final String PREFS_DEFAULTS = "recap_template_defaults";

    private static final Object LOCK = new Object();

    /**
     * Retrieves all templates (system presets + user created/edited templates).
     */
    public static List<RecapTemplate> getAllTemplates(Context context) {
        synchronized (LOCK) {
            Map<String, RecapTemplate> map = new LinkedHashMap<>();

            // 1. Always load standard built-in system presets first
            RecapTemplate defaultBrutalist = DefaultTemplatesFactory.createDefaultBrutalist();
            map.put(defaultBrutalist.getId(), defaultBrutalist);

            RecapTemplate minimalist = DefaultTemplatesFactory.createMinimalistDark();
            map.put(minimalist.getId(), minimalist);

            // 2. Load custom user templates from internal JSON
            File file = new File(context.getFilesDir(), FILE_TEMPLATES);
            if (file.exists()) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[(int) file.length()];
                    int read = fis.read(buffer);
                    if (read > 0) {
                        String jsonStr = new String(buffer, StandardCharsets.UTF_8);
                        JSONArray array = new JSONArray(jsonStr);
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.optJSONObject(i);
                            if (obj != null) {
                                RecapTemplate t = RecapTemplate.fromJsonObject(obj);
                                if (t != null) {
                                    map.put(t.getId(), t);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error loading user templates", e);
                }
            }

            return new ArrayList<>(map.values());
        }
    }

    /**
     * Gets a template by ID, falling back to the standard Brutalist template if not found.
     */
    public static RecapTemplate getTemplateById(Context context, String templateId) {
        if (templateId == null || templateId.trim().isEmpty()) {
            return DefaultTemplatesFactory.createDefaultBrutalist();
        }

        List<RecapTemplate> all = getAllTemplates(context);
        for (RecapTemplate t : all) {
            if (t.getId().equals(templateId)) {
                return t;
            }
        }
        return DefaultTemplatesFactory.createDefaultBrutalist();
    }

    /**
     * Saves or updates a user template. If a template has the same ID, it is updated.
     * System presets cannot be overwritten; an attempt to do so creates a user copy.
     */
    public static boolean saveTemplate(Context context, RecapTemplate template) {
        if (template == null) return false;
        synchronized (LOCK) {
            try {
                // If trying to modify a system preset directly, convert it to a user copy
                if (template.isSystemPreset()) {
                    template = template.duplicate(template.getName() + " (Personalizado)");
                }

                template.setUpdatedAt(System.currentTimeMillis());

                List<RecapTemplate> userTemplates = getUserTemplatesOnly(context);
                boolean updated = false;
                for (int i = 0; i < userTemplates.size(); i++) {
                    if (userTemplates.get(i).getId().equals(template.getId())) {
                        userTemplates.set(i, template);
                        updated = true;
                        break;
                    }
                }

                if (!updated) {
                    userTemplates.add(template);
                }

                return writeUserTemplates(context, userTemplates);
            } catch (Exception e) {
                Log.e(TAG, "Failed to save template: " + template.getId(), e);
                return false;
            }
        }
    }

    /**
     * Duplicates a template and saves the independent copy with a new unique ID.
     */
    public static RecapTemplate duplicateTemplate(Context context, String sourceTemplateId, String newName) {
        synchronized (LOCK) {
            RecapTemplate source = getTemplateById(context, sourceTemplateId);
            RecapTemplate duplicated = source.duplicate(newName);
            saveTemplate(context, duplicated);
            return duplicated;
        }
    }

    /**
     * Renames an existing user template.
     */
    public static boolean renameTemplate(Context context, String templateId, String newName) {
        if (templateId == null || newName == null || newName.trim().isEmpty()) {
            return false;
        }
        synchronized (LOCK) {
            RecapTemplate t = getTemplateById(context, templateId);
            if (t.isSystemPreset()) {
                // Duplicate as user template with the new name
                duplicateTemplate(context, templateId, newName.trim());
                return true;
            } else {
                t.setName(newName.trim());
                return saveTemplate(context, t);
            }
        }
    }

    /**
     * Deletes a user template. System presets cannot be deleted.
     */
    public static boolean deleteTemplate(Context context, String templateId) {
        if (templateId == null || RecapTemplate.DEFAULT_TEMPLATE_ID.equals(templateId)) {
            return false; // Never delete standard fallback
        }

        synchronized (LOCK) {
            List<RecapTemplate> userTemplates = getUserTemplatesOnly(context);
            boolean removed = false;
            for (int i = 0; i < userTemplates.size(); i++) {
                if (userTemplates.get(i).getId().equals(templateId)) {
                    userTemplates.remove(i);
                    removed = true;
                    break;
                }
            }

            if (removed) {
                writeUserTemplates(context, userTemplates);

                // If any category default was using this deleted template, revert it to default fallback
                SharedPreferences prefs = context.getSharedPreferences(PREFS_DEFAULTS, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                for (RecapCategory cat : RecapCategory.values()) {
                    if (templateId.equals(prefs.getString(cat.getId(), ""))) {
                        editor.putString(cat.getId(), RecapTemplate.DEFAULT_TEMPLATE_ID);
                    }
                }
                editor.apply();
                return true;
            }
            return false;
        }
    }

    // Category-specific default template assignment

    /**
     * Sets the default template ID for a specific recap category (Daily, Weekly, Monthly, Yearly).
     */
    public static void setDefaultTemplateForCategory(Context context, RecapCategory category, String templateId) {
        if (category == null || templateId == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_DEFAULTS, Context.MODE_PRIVATE);
        prefs.edit().putString(category.getId(), templateId).apply();
    }

    /**
     * Gets the active default template for a given recap category.
     * Guaranteed to return a valid template (falls back to DEFAULT_TEMPLATE_ID).
     */
    public static RecapTemplate getDefaultTemplateForCategory(Context context, RecapCategory category) {
        if (category == null) {
            category = RecapCategory.WEEKLY;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_DEFAULTS, Context.MODE_PRIVATE);
        String assignedId = prefs.getString(category.getId(), RecapTemplate.DEFAULT_TEMPLATE_ID);
        return getTemplateById(context, assignedId);
    }

    /**
     * Gets the assigned template ID for a category.
     */
    public static String getDefaultTemplateIdForCategory(Context context, RecapCategory category) {
        if (category == null) return RecapTemplate.DEFAULT_TEMPLATE_ID;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_DEFAULTS, Context.MODE_PRIVATE);
        return prefs.getString(category.getId(), RecapTemplate.DEFAULT_TEMPLATE_ID);
    }

    private static List<RecapTemplate> getUserTemplatesOnly(Context context) {
        List<RecapTemplate> list = new ArrayList<>();
        File file = new File(context.getFilesDir(), FILE_TEMPLATES);
        if (!file.exists()) return list;

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[(int) file.length()];
            int read = fis.read(buffer);
            if (read > 0) {
                String jsonStr = new String(buffer, StandardCharsets.UTF_8);
                JSONArray array = new JSONArray(jsonStr);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.optJSONObject(i);
                    if (obj != null) {
                        RecapTemplate t = RecapTemplate.fromJsonObject(obj);
                        if (t != null && !t.isSystemPreset()) {
                            list.add(t);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading user templates file", e);
        }
        return list;
    }

    private static boolean writeUserTemplates(Context context, List<RecapTemplate> templates) {
        try {
            JSONArray array = new JSONArray();
            for (RecapTemplate t : templates) {
                if (!t.isSystemPreset()) {
                    array.put(t.toJsonObject());
                }
            }

            File file = new File(context.getFilesDir(), FILE_TEMPLATES);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(array.toString(2).getBytes(StandardCharsets.UTF_8));
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error writing user templates to disk", e);
            return false;
        }
    }
}

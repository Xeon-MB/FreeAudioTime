package com.example.freeaudiotime;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Local, app-private storage for user-created stickers.
 * The PNG files and their display names survive activity recreation and app restarts.
 */
public final class StickerStorage {

    private static final String PREFS_NAME = "sticker_library";
    private static final String KEY_ITEMS = "items";
    private static final String DIRECTORY_NAME = "stickers";

    private StickerStorage() {
    }

    public static final class StickerItem {
        private final String id;
        private final String name;
        private final String path;

        StickerItem(String id, String name, String path) {
            this.id = id;
            this.name = name;
            this.path = path;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getPath() {
            return path;
        }
    }

    public static StickerItem saveSticker(Context context, String name, File source)
            throws IOException {
        String safeName = name == null ? "" : name.trim();
        if (safeName.isEmpty()) {
            throw new IllegalArgumentException("O nome do sticker não pode ficar vazio.");
        }
        if (source == null || !source.isFile()) {
            throw new IOException("Arquivo de sticker não encontrado.");
        }

        File directory = new File(context.getFilesDir(), DIRECTORY_NAME);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Não foi possível criar a galeria de stickers.");
        }

        String id = UUID.randomUUID().toString();
        File destination = new File(directory, "sticker_" + id + ".png");

        try (FileInputStream input = new FileInputStream(source);
             FileOutputStream output = new FileOutputStream(destination)) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            output.flush();
        } catch (IOException e) {
            destination.delete();
            throw e;
        }

        StickerItem item = new StickerItem(id, safeName, destination.getAbsolutePath());
        JSONArray current = readItems(context);
        JSONArray updated = new JSONArray();

        try {
            updated.put(toJson(item));
            for (int i = 0; i < current.length(); i++) {
                JSONObject existing = current.optJSONObject(i);
                if (existing != null) updated.put(existing);
            }
        } catch (JSONException e) {
            destination.delete();
            throw new IOException("Não foi possível registrar o sticker.", e);
        }

        boolean stored = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ITEMS, updated.toString())
                .commit();

        if (!stored) {
            destination.delete();
            throw new IOException("Não foi possível salvar a galeria.");
        }

        return item;
    }

    public static List<StickerItem> getStickers(Context context) {
        JSONArray items = readItems(context);
        List<StickerItem> result = new ArrayList<>();

        for (int i = 0; i < items.length(); i++) {
            JSONObject json = items.optJSONObject(i);
            if (json == null) continue;

            String id = json.optString("id", "");
            String name = json.optString("name", "").trim();
            String path = json.optString("path", "");

            if (id.isEmpty() || name.isEmpty() || path.isEmpty()) continue;
            if (!new File(path).isFile()) continue;

            result.add(new StickerItem(id, name, path));
        }
        return result;
    }

    private static JSONArray readItems(Context context) {
        String raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_ITEMS, "[]");
        try {
            return new JSONArray(raw);
        } catch (JSONException ignored) {
            return new JSONArray();
        }
    }

    private static JSONObject toJson(StickerItem item) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", item.getId());
        json.put("name", item.getName());
        json.put("path", item.getPath());
        return json;
    }
}

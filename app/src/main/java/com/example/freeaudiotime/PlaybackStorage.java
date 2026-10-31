package com.example.freeaudiotime;

import android.content.Context;

import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class PlaybackStorage {

    private static final String FILE_NAME = "playback_history.json";

    public static void save(Context context, JSONObject data) {

        File file = new File(context.getFilesDir(), FILE_NAME);

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(data.toString(4));

            android.util.Log.d("AudioTime", "Histórico salvo!");

        } catch (Exception e) {
            android.util.Log.e("AudioTime", "Erro ao salvar histórico", e);
        }
    }

    public static JSONObject load(Context context) {

        File file = new File(context.getFilesDir(), FILE_NAME);

        if (!file.exists()) {
            return new JSONObject();
        }

        try {
            String content = new String(
                    Files.readAllBytes(file.toPath()),
                    StandardCharsets.UTF_8
            );

            return new JSONObject(content);

        } catch (Exception e) {
            android.util.Log.e("AudioTime", "Erro ao carregar histórico", e);
            return new JSONObject();
        }
    }
}

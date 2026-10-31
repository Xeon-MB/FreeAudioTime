
package com.example.freeaudiotime;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DeezerService {

    private static final String TAG = "DeezerService";

    private static final ExecutorService executor =
            Executors.newFixedThreadPool(2);

    private static final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    public interface ArtistCallback {
        void onResult(Bitmap image);
    }

    public static void buscarFotoArtista(
            String artist,
            ArtistCallback callback
    ) {
        executor.execute(() -> {
            Log.d(TAG, "BUSCA INICIADA: " + artist);

            Bitmap image = null;
            try {
                String query = URLEncoder.encode(
                        artist,
                        StandardCharsets.UTF_8.name()
                );

                URL searchUrl = new URL(
                        "https://api.deezer.com/search/artist?q=" + query
                );

                HttpURLConnection connection =
                        (HttpURLConnection) searchUrl.openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                try {
                    if (connection.getResponseCode() == 200) {
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(
                                        connection.getInputStream()
                                )
                        );

                        StringBuilder response = new StringBuilder();
                        String line;

                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }

                        reader.close();

                        JSONObject json =
                                new JSONObject(response.toString());

                        JSONArray artists = json.optJSONArray("data");

                        if (artists != null && artists.length() > 0) {
                            JSONObject firstArtist =
                                    artists.getJSONObject(0);

                            String imageUrl =
                                    firstArtist.optString("picture_big", "");

                            if (!imageUrl.isEmpty()) {
                                URL photoUrl = new URL(imageUrl);

                                HttpURLConnection photoConnection =
                                        (HttpURLConnection)
                                                photoUrl.openConnection();

                                photoConnection.setConnectTimeout(10000);
                                photoConnection.setReadTimeout(10000);

                                try {
                                    if (photoConnection.getResponseCode() == 200) {
                                        try (InputStream input =
                                                     photoConnection.getInputStream()) {
                                            image = BitmapFactory.decodeStream(input);
                                        }
                                    }
                                } finally {
                                    photoConnection.disconnect();
                                }
                            }
                        }
                    }
                } finally {
                    connection.disconnect();
                }

            } catch (Exception e) {
                Log.e(TAG, "Erro ao buscar foto de " + artist, e);
            }

            Bitmap result = image;

            mainHandler.post(() -> callback.onResult(result));
        });
    }
}

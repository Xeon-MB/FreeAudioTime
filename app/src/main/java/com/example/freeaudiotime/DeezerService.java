
package com.example.freeaudiotime;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.text.Normalizer;

public class DeezerService {

    private static final String TAG = "DeezerService";

    private static final ExecutorService executor =
            Executors.newFixedThreadPool(2);

    private static final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    public interface ArtistCallback {
        void onResult(Bitmap image);
    }

    public interface AlbumCallback {
        void onResult(Bitmap image, String artworkPath);
    }

    public static void buscarFotoArtista(
            Context context,
            String artist,
            ArtistCallback callback
    ) {
        Context appContext = context.getApplicationContext();

        executor.execute(() -> {
            Log.d(TAG, "BUSCA INICIADA: " + artist);

            Bitmap image = null;

            File cacheDir = new File(
                    appContext.getFilesDir(),
                    "artist_cache"
            );

            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }

            String nomeArquivo =
                    artist.replaceAll("[^a-zA-Z0-9]", "_") + ".png";

            File arquivoCache = new File(cacheDir, nomeArquivo);



            // Primeiro tenta carregar a imagem salva.
            if (arquivoCache.exists()) {
                image = BitmapFactory.decodeFile(
                        arquivoCache.getAbsolutePath()
                );

                if (image != null) {
                    Log.d(TAG, "Foto carregada do cache: " + artist);
                }
            }

            // Se não encontrou no cache, consulta o Deezer.
            if (image == null) {
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

                // Salva a foto baixada para as próximas consultas.
                if (image != null) {
                    try (FileOutputStream output =
                                 new FileOutputStream(arquivoCache)) {
                        image.compress(Bitmap.CompressFormat.PNG, 100, output);
                        Log.d(TAG, "Foto salva no cache: " + artist);
                    } catch (Exception e) {
                        Log.e(TAG, "Erro ao salvar cache", e);
                    }
                }
            }

            Bitmap result = image;

            mainHandler.post(() -> callback.onResult(result));
        });
    }

    public static void buscarCapaMusica(
            Context context,
            String title,
            String artist,
            AlbumCallback callback
    ) {
        Context appContext = context.getApplicationContext();

        executor.execute(() -> {
            Bitmap image = null;
            String artworkPath = "";

            try {
                File cacheDir = new File(
                        appContext.getFilesDir(),
                        "album_art_cache"
                );

                if (!cacheDir.exists()) {
                    cacheDir.mkdirs();
                }

                String identificador = title + "|" + artist;

                String nomeArquivo = java.util.UUID.nameUUIDFromBytes(
                        identificador.getBytes(StandardCharsets.UTF_8)
                ) + ".png";

                File arquivoCache = new File(cacheDir, nomeArquivo);

                File arquivoSemResultado = new File(
                        cacheDir,
                        nomeArquivo + ".not_found"
                );

                artworkPath = "album_art_cache/" + nomeArquivo;

                // Primeiro verifica o cache.
                if (arquivoCache.exists()) {
                    image = BitmapFactory.decodeFile(
                            arquivoCache.getAbsolutePath()
                    );

                    Log.d(TAG, "Capa carregada do cache: " + title);
                }

                // Se não existir, pesquisa no Deezer.
                if (image == null && !arquivoSemResultado.exists()) {

                    String query = URLEncoder.encode(
                            title + " " + artist,
                            StandardCharsets.UTF_8.name()
                    );

                    URL searchUrl = new URL(
                            "https://api.deezer.com/search?q=" + query
                    );

                    HttpURLConnection connection =
                            (HttpURLConnection) searchUrl.openConnection();

                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(10000);

                    try {
                        if (connection.getResponseCode() == 200) {

                            BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(connection.getInputStream())
                            );

                            StringBuilder response = new StringBuilder();
                            String line;

                            while ((line = reader.readLine()) != null) {
                                response.append(line);
                            }

                            reader.close();

                            JSONObject json = new JSONObject(response.toString());
                            JSONArray results = json.optJSONArray("data");

                            if (results != null && results.length() > 0) {

                                JSONObject music = results.getJSONObject(0);
                                JSONObject album = music.optJSONObject("album");

                                if (album != null) {
                                    String imageUrl = album.optString("cover_xl", "");

                                    if (!imageUrl.isEmpty()) {

                                        URL photoUrl = new URL(imageUrl);

                                        HttpURLConnection photoConnection =
                                                (HttpURLConnection) photoUrl.openConnection();

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
                        }
                    } finally {
                        connection.disconnect();
                    }

                    // Salva a capa encontrada.
                    if (image != null) {
                        try (FileOutputStream output =
                                     new FileOutputStream(arquivoCache)) {

                            image.compress(Bitmap.CompressFormat.PNG, 100, output);

                            Log.d(TAG, "Nova capa salva: " + title);

                        }
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Erro ao buscar capa: " + title, e);
            }

            Bitmap result = image;
            String finalPath = result != null ? artworkPath : "";

            mainHandler.post(() ->
                    callback.onResult(result, finalPath)
            );
        });
    }

    private static String normalizarTexto(String texto) {
        if (texto == null) return "";

        return Normalizer.normalize(
                        texto,
                        Normalizer.Form.NFD
                )
                .replaceAll("\\p{M}+", "")
                .toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }
}

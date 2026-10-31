package com.example.freeaudiotime;

import android.content.ComponentName;
import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.service.notification.NotificationListenerService;
import android.util.Log;
import android.os.SystemClock;
import android.graphics.Bitmap;
import java.io.File;
import java.io.FileOutputStream;
import android.content.Intent;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.util.List;
import java.util.HashMap;
import java.util.Map;


public class MediaNotificationListener extends NotificationListenerService {

    private long playbackStartTime = 0;
    private long accumulatedTime = 0;
    private boolean isPlaying = false;

    private final Handler saveHandler = new Handler(Looper.getMainLooper());

    private final Runnable periodicSave = new Runnable() {
        @Override
        public void run() {
            if (isPlaying) {
                saveElapsedTime();
                savePlaybackHistory();

                Log.d(TAG, "Salvamento periódico realizado.");

                saveHandler.postDelayed(this, 30_000);
            }
        }
    };
    private final Map<String, Long> trackTimes = new HashMap<>();
    private final Map<String, String> trackArtists = new HashMap<>();

    private final JSONArray playbackSessions = new JSONArray();

    private static final String TAG = "AudioTime";
    private static final String TARGET_PACKAGE = "com.stash.app";

    private MediaSessionManager manager;
    private ComponentName componentName;
    private MediaController currentController;

    private MediaController.Callback controllerCallback;

    private MediaSessionManager.OnActiveSessionsChangedListener sessionsListener;
    private String currentTrackTitle = null;

    private long currentTrackDuration = 0;
    private long currentTrackPosition = 0;

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        JSONObject history = PlaybackStorage.load(this);

        // Limpa os dados que já estavam na memória
        while (playbackSessions.length() > 0) {
            playbackSessions.remove(playbackSessions.length() - 1);
        }

        trackTimes.clear();
        trackArtists.clear();


        JSONArray savedSessions = history.optJSONArray("sessions");

        if (savedSessions != null) {
            for (int i = 0; i < savedSessions.length(); i++) {
                try {
                    playbackSessions.put(savedSessions.getJSONObject(i));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        accumulatedTime = history.optLong("totalTime", 0) * 1000;

        JSONArray tracks = history.optJSONArray("tracks");

        if (tracks != null) {
            for (int i = 0; i < tracks.length(); i++) {
                JSONObject track;
                try {
                    track = tracks.getJSONObject(i);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

                String title = track.optString("title");
                String artist = track.optString("artist");
                long duration = track.optLong("duration", 0) * 1000;

                trackTimes.put(title, duration);

                if (!artist.isEmpty()) {
                    trackArtists.put(title, artist);
                }
            }
        }

        Log.d(TAG, "Histórico anterior carregado!");

        Log.d(TAG, "Serviço conectado com sucesso!");

        manager = (MediaSessionManager)
                getSystemService(Context.MEDIA_SESSION_SERVICE);

        componentName = new ComponentName(
                this,
                MediaNotificationListener.class
        );

        controllerCallback = new MediaController.Callback() {

            @Override
            public void onPlaybackStateChanged(PlaybackState state) {
                logPlaybackState(state);
            }

            @Override
            public void onMetadataChanged(MediaMetadata metadata) {
                logMetadata(metadata);
            }

            @Override
            public void onSessionDestroyed() {

                Log.d(TAG, "Sessão do Stash encerrada!");

                if (isPlaying) {
                    saveElapsedTime();
                    isPlaying = false;
                }

                savePlaybackHistory();

                currentTrackTitle = null;

                refreshSessions();
            }
        };

        sessionsListener = controllers -> {
            Log.d(TAG, "Lista de sessões atualizada!");
            updateStashSession(controllers);
        };

        try {
            manager.addOnActiveSessionsChangedListener(
                    sessionsListener,
                    componentName
            );

            refreshSessions();

        } catch (SecurityException e) {
            Log.e(TAG, "Sem autorização para acessar sessões", e);
        }
    }

    private void startPeriodicSave() {
        saveHandler.removeCallbacks(periodicSave);
        saveHandler.postDelayed(periodicSave, 30_000);
    }

    private void stopPeriodicSave() {
        saveHandler.removeCallbacks(periodicSave);
    }

    private void saveElapsedTime() {

        if (!isPlaying || currentTrackTitle == null) {
            return;
        }

        long currentTime = SystemClock.elapsedRealtime();

        long elapsed = currentTime - playbackStartTime;

        accumulatedTime += elapsed;

        long previousTime = trackTimes.getOrDefault(
                currentTrackTitle, 0L
        );

        trackTimes.put(currentTrackTitle, previousTime + elapsed);

        try {
            JSONObject session = new JSONObject();

            session.put("date", new java.text.SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss",
                    java.util.Locale.getDefault()
            ).format(new java.util.Date()));

            session.put("title", currentTrackTitle);
            session.put("artist", trackArtists.get(currentTrackTitle));
            session.put("duration", elapsed / 1000);

            playbackSessions.put(session);

        } catch (Exception e) {
            Log.e(TAG, "Erro ao registrar sessão", e);
        }

        playbackStartTime = currentTime;

        Log.d(TAG, "Tempo de " + currentTrackTitle + ": " +
                trackTimes.get(currentTrackTitle) / 1000 + " segundos");
    }
    private void updatePlaybackTimer(PlaybackState state) {

        if (state == null) {
            return;
        }

        int playbackState = state.getState();

        if (playbackState == PlaybackState.STATE_PLAYING) {

            if (!isPlaying) {

                playbackStartTime = SystemClock.elapsedRealtime();
                isPlaying = true;

                startPeriodicSave();

                Log.d(TAG, "Contagem iniciada!");
            }

        } else {

            if (isPlaying) {

                saveElapsedTime();

                isPlaying = false;

                stopPeriodicSave();

                savePlaybackHistory();

                Log.d(TAG, "Contagem pausada!");
                Log.d(TAG, "Tempo acumulado: " +
                        accumulatedTime / 1000 + " segundos");
            }
        }
    }
    private void refreshSessions() {
        if (manager == null) {
            return;
        }

        try {
            List<MediaController> sessions =
                    manager.getActiveSessions(componentName);

            Log.d(TAG, "Sessões encontradas: " + sessions.size());

            updateStashSession(sessions);

        } catch (SecurityException e) {
            Log.e(TAG, "Erro ao consultar sessões", e);
        }
    }

    private void updateStashSession(List<MediaController> sessions) {

        MediaController stashController = null;

        for (MediaController controller : sessions) {

            if (TARGET_PACKAGE.equals(controller.getPackageName())) {
                stashController = controller;
                break;
            }
        }

        if (stashController == null) {

            unregisterCurrentController();

            Log.d(TAG, "Nenhuma sessão do Stash ativa.");
            return;
        }

        if (currentController != null &&
                currentController.getSessionToken().equals(
                        stashController.getSessionToken())) {
            return;
        }

        unregisterCurrentController();

        currentController = stashController;

        currentController.registerCallback(controllerCallback);

        Log.d(TAG, "Monitorando o Stash!");

        logMetadata(currentController.getMetadata());
        logPlaybackState(currentController.getPlaybackState());
    }
    private void savePlaybackHistory() {

        try {
            org.json.JSONObject history = new org.json.JSONObject();
            org.json.JSONArray tracks = new org.json.JSONArray();

            for (String title : trackTimes.keySet()) {

                String artist = trackArtists.get(title);

                org.json.JSONObject track = new org.json.JSONObject();

                track.put("title", title);
                track.put("artist", artist);
                track.put("duration", trackTimes.get(title) / 1000);

                File artworkFile = getAlbumArtFile(title, artist);

                if (artworkFile.exists()) {
                    track.put(
                            "artwork",
                            "album_art_cache/" + artworkFile.getName()
                    );
                }

                tracks.put(track);
            }

            history.put("totalTime", accumulatedTime / 1000);
            history.put("tracks", tracks);
            history.put("sessions", playbackSessions);

            PlaybackStorage.save(this, history);

            Log.d(TAG, "Histórico de reprodução atualizado!");

        } catch (Exception e) {
            Log.e(TAG, "Erro ao montar histórico", e);
        }
    }

    private void logMetadata(MediaMetadata metadata) {

        if (metadata == null) {
            Log.d(TAG, "Metadados indisponíveis.");
            return;
        }

        String title = metadata.getString(
                MediaMetadata.METADATA_KEY_TITLE
        );

        String artist = metadata.getString(
                MediaMetadata.METADATA_KEY_ARTIST
        );

        long duration = metadata.getLong(
                MediaMetadata.METADATA_KEY_DURATION
        );

        currentTrackDuration = duration;



        getSharedPreferences("current_track", MODE_PRIVATE)
                .edit()
                .putLong("duration", duration)
                .apply();

        if (title == null && metadata.getDescription() != null) {
            title = String.valueOf(metadata.getDescription().getTitle());
        }

        if (artist == null && metadata.getDescription() != null) {
            artist = metadata.getDescription().getSubtitle() != null
                    ? metadata.getDescription().getSubtitle().toString()
                    : null;
        }

        if (title != null && artist != null) {
            trackArtists.put(title, artist);
        }

        salvarDadosMusica(metadata, title, artist);

        if (title != null && !title.equals(currentTrackTitle)) {
            saveElapsedTime();
            savePlaybackHistory();

            currentTrackTitle = title;

            Log.d(TAG, "Nova faixa: " + currentTrackTitle);

            if (isPlaying) {
                playbackStartTime = SystemClock.elapsedRealtime();
            }
        }

        Log.d(TAG, "Música: " + title);
        Log.d(TAG, "Artista: " + artist);
    }

    private void logPlaybackState(PlaybackState state) {

        if (state == null) {
            Log.d(TAG, "Estado de reprodução indisponível.");
            return;
        }

        int playbackState = state.getState();
        updatePlaybackTimer(state);

        currentTrackPosition = Math.max(0, state.getPosition());

        getSharedPreferences("current_track", MODE_PRIVATE)
                .edit()
                .putLong("position", currentTrackPosition)
                .putLong("positionUpdatedAt", SystemClock.elapsedRealtime())
                .putLong("duration", currentTrackDuration)
                .putBoolean("isPlaying",
                        playbackState == PlaybackState.STATE_PLAYING)
                .apply();

        Intent intent = new Intent(
                "com.example.freeaudiotime.TRACK_UPDATED"
        );
        intent.setPackage(getPackageName());
        sendBroadcast(intent);

        String description;

        switch (playbackState) {
            case PlaybackState.STATE_PLAYING:
                description = "REPRODUZINDO";
                break;

            case PlaybackState.STATE_PAUSED:
                description = "PAUSADO";
                break;

            case PlaybackState.STATE_STOPPED:
                description = "PARADO";
                break;

            case PlaybackState.STATE_BUFFERING:
                description = "CARREGANDO";
                break;

            case PlaybackState.STATE_NONE:
                description = "SEM REPRODUÇÃO";
                break;

            default:
                description = "OUTRO ESTADO: " + playbackState;
                break;
        }

        Log.d(TAG, "Estado: " + description);
    }

    private void unregisterCurrentController() {

        if (isPlaying) {
            saveElapsedTime();
            isPlaying = false;
            stopPeriodicSave();
            savePlaybackHistory();
        }

        if (currentController != null && controllerCallback != null) {

            currentController.unregisterCallback(controllerCallback);

            currentController = null;

            Log.d(TAG, "Monitoramento da sessão encerrado.");
        }
    }

    @Override
    public void onListenerDisconnected() {

        if (isPlaying) {
            saveElapsedTime();
            isPlaying = false;
        }

        stopPeriodicSave();
        savePlaybackHistory();

        if (manager != null && sessionsListener != null) {
            manager.removeOnActiveSessionsChangedListener(
                    sessionsListener
            );
        }

        unregisterCurrentController();

        super.onListenerDisconnected();

        Log.d(TAG, "Serviço desconectado!");
    }


    private void salvarDadosMusica(MediaMetadata metadata, String title, String artist) {

        Bitmap artwork = metadata.getBitmap(
                MediaMetadata.METADATA_KEY_ALBUM_ART
        );

        if (artwork == null) {
            artwork = metadata.getBitmap(
                    MediaMetadata.METADATA_KEY_ART
            );
        }

        File albumArtFile = getAlbumArtFile(title, artist);

        try {
            // Salva a capa permanente, caso tenha recebido uma imagem.
            if (artwork != null) {
                try (FileOutputStream output = new FileOutputStream(albumArtFile)) {
                    artwork.compress(Bitmap.CompressFormat.PNG, 100, output);
                }
            }

            // Atualiza a imagem atual usada pelo restante do aplicativo.
            File currentImage = new File(getFilesDir(), "current_album_art.png");

            if (albumArtFile.exists()) {
                try (
                        FileInputStream input = new FileInputStream(albumArtFile);
                        FileOutputStream output = new FileOutputStream(currentImage)
                ) {
                    byte[] buffer = new byte[8192];
                    int length;

                    while ((length = input.read(buffer)) != -1) {
                        output.write(buffer, 0, length);
                    }
                }
            } else {
                currentImage.delete();
            }

            getSharedPreferences("current_track", MODE_PRIVATE)
                    .edit()
                    .putString("title", title)
                    .putString("artist", artist)
                    .putString(
                            "artwork",
                            albumArtFile.exists()
                                    ? "album_art_cache/" + albumArtFile.getName()
                                    : ""
                    )
                    .apply();

        } catch (Exception e) {
            Log.e(TAG, "Erro ao salvar capa da música", e);
        }

        Intent intent = new Intent(
                "com.example.freeaudiotime.TRACK_UPDATED"
        );

        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }

    private File getAlbumArtFile(String title, String artist) {

        File cacheDir = new File(getFilesDir(), "album_art_cache");

        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }

        String id = UUID.nameUUIDFromBytes(
                (title + "|" + artist).getBytes(StandardCharsets.UTF_8)
        ).toString();

        return new File(cacheDir, id + ".png");
    }
}



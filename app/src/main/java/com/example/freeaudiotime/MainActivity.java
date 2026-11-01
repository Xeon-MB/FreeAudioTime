
package com.example.freeaudiotime;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.io.File;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import android.widget.ProgressBar;
import android.os.SystemClock;
import android.provider.Settings;
import android.content.ComponentName;
import androidx.appcompat.app.AlertDialog;

import android.content.Intent;
import android.widget.Button;


import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            atualizarProgresso();
            progressHandler.postDelayed(this, 1000);
        }
    };

    private Handler progressHandler = new Handler(Looper.getMainLooper());


    private ProgressBar currentTrackProgress;
    private TextView currentTrackElapsed;
    private TextView currentTrackRemaining;
    private boolean receiverRegistrado = false;

    private final BroadcastReceiver trackReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            carregarMusicaAtual();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applySavedTheme(this);
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        currentTrackProgress = findViewById(R.id.currentTrackProgress);
        currentTrackElapsed = findViewById(R.id.currentTrackElapsed);
        currentTrackRemaining = findViewById(R.id.currentTrackRemaining);

        findViewById(R.id.toggleTheme).setOnClickListener(v -> {
            ThemeManager.toggleTheme(this);
        });

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main), (v, insets) -> {

                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );

        carregarEstatisticas();

        Button openRecap = findViewById(R.id.openRecap);

        openRecap.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecapActivity.class);
            startActivity(intent);
        });

        Button openRecords = findViewById(R.id.openRecords);

        openRecords.setOnClickListener(v -> {
            Intent intent = new Intent(
                    MainActivity.this,
                    RecordsActivity.class
            );

            startActivity(intent);
        });

        verificarPermissaoNotificacao();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!receiverRegistrado) {
            IntentFilter filter = new IntentFilter(
                    "com.example.freeaudiotime.TRACK_UPDATED"
            );

            ContextCompat.registerReceiver(
                    this,
                    trackReceiver,
                    filter,
                    ContextCompat.RECEIVER_NOT_EXPORTED
            );

            receiverRegistrado = true;
        }

        carregarMusicaAtual();
        progressHandler.removeCallbacks(progressRunnable);
        progressHandler.post(progressRunnable);
    }

    @Override
    protected void onStop() {
        progressHandler.removeCallbacks(progressRunnable);
        super.onStop();

        if (receiverRegistrado) {
            unregisterReceiver(trackReceiver);
            receiverRegistrado = false;
        }
    }

    private void carregarMusicaAtual() {

        android.content.SharedPreferences prefs =
                getSharedPreferences("current_track", MODE_PRIVATE);

        String title = prefs.getString("title", "Nenhuma música");
        String artist = prefs.getString("artist", "Artista desconhecido");

        TextView titleView = findViewById(R.id.currentTrackTitle);
        TextView artistView = findViewById(R.id.currentTrackArtist);
        ImageView artView = findViewById(R.id.currentTrackCover);

        titleView.setText(title);
        artistView.setText(artist);

        File imageFile = new File(getFilesDir(), "current_album_art.png");

        if (imageFile.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());

            if (bitmap != null) {
                artView.setImageBitmap(bitmap);
                return;
            }
        }

        artView.setImageResource(android.R.drawable.ic_media_play);
    }

    private void carregarEstatisticas() {

        JSONObject history = PlaybackStorage.load(this);
        Log.d("AudioTime", "Histórico carregado: " + history.toString());

        long totalTime = history.optLong("totalTime", 0);

        long weeklyTotal =
                PlaybackStatistics.getWeeklyTotal(history);

        long monthlyTotal =
                PlaybackStatistics.getMonthlyTotal(history);

        long yearlyTotal =
                PlaybackStatistics.getYearlyTotal(history);

        String mostPlayedArtist =
                PlaybackStatistics.getMostPlayedArtist(history);

        String mostPlayedTrack =
                PlaybackStatistics.getMostPlayedTrack(history);

        TextView totalTimeView = findViewById(R.id.totalTime);
        TextView weeklyTimeView = findViewById(R.id.weeklyTime);
        TextView monthlyTimeView = findViewById(R.id.monthlyTime);
        TextView yearlyTimeView = findViewById(R.id.yearlyTime);

        TextView artistView = findViewById(R.id.mostPlayedArtist);
        TextView trackView = findViewById(R.id.mostPlayedTrack);


        String topArtists =
                PlaybackStatistics.getTopArtists(history);

        String topTracks =
                PlaybackStatistics.getTopTracks(history);

        TextView topArtistsView = findViewById(R.id.topArtists);
        TextView topTracksView = findViewById(R.id.topTracks);

        topArtistsView.setText(topArtists);
        topTracksView.setText(topTracks);


        totalTimeView.setText(formatarTempo(totalTime));
        weeklyTimeView.setText(formatarTempo(weeklyTotal));
        monthlyTimeView.setText(formatarTempo(monthlyTotal));
        yearlyTimeView.setText(formatarTempo(yearlyTotal));

        artistView.setText(mostPlayedArtist);
        trackView.setText(mostPlayedTrack);

        Log.d("AudioTime", "Música mais tocada: " + mostPlayedTrack);
        Log.d("AudioTime", "Ranking de músicas: " + topTracks);
    }





    private String formatarTempo(long segundos) {

        long horas = segundos / 3600;
        long minutos = (segundos % 3600) / 60;
        long segundosRestantes = segundos % 60;

        if (horas > 0) {
            return horas + "h " + minutos + "min";
        }

        if (minutos > 0) {
            return minutos + "min";
        }

        return segundosRestantes + "s";
    }

    private void atualizarProgresso() {

        android.content.SharedPreferences prefs =
                getSharedPreferences("current_track", MODE_PRIVATE);

        long duration = prefs.getLong("duration", 0);
        long position = prefs.getLong("position", 0);
        long updatedAt = prefs.getLong("positionUpdatedAt", 0);

        boolean isPlaying = prefs.getBoolean("isPlaying", false);

        if (isPlaying && updatedAt > 0) {
            long elapsed = SystemClock.elapsedRealtime() - updatedAt;
            position += elapsed;
        }

        if (duration > 0) {
            position = Math.min(position, duration);
        } else {
            position = 0;
        }

        currentTrackProgress.setMax(1000);

        int progress = duration > 0
                ? (int) ((double) position / duration * 1000)
                : 0;

        currentTrackProgress.setProgress(progress);

        currentTrackElapsed.setText(formatarMinutosSegundos(position));

        long remaining = Math.max(0, duration - position);

        currentTrackRemaining.setText(
                "-" + formatarMinutosSegundos(remaining)
        );
    }

    private String formatarMinutosSegundos(long milissegundos) {

        long totalSegundos = milissegundos / 1000;

        long minutos = totalSegundos / 60;
        long segundos = totalSegundos % 60;

        return String.format(
                java.util.Locale.getDefault(),
                "%d:%02d",
                minutos,
                segundos
        );
    }

    private boolean temPermissaoNotificacao() {

        String listeners = Settings.Secure.getString(
                getContentResolver(),
                "enabled_notification_listeners"
        );

        if (listeners == null) {
            return false;
        }

        ComponentName component = new ComponentName(
                this,
                MediaNotificationListener.class
        );

        return listeners.contains(component.flattenToString());
    }

    private void verificarPermissaoNotificacao() {

        if (temPermissaoNotificacao()) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Permissão necessária")
                .setMessage(
                        "O FreeAudioTime precisa de acesso às notificações " +
                                "de mídia para acompanhar suas reproduções. " +
                                "Ative essa permissão nas configurações do Android."
                )
                .setPositiveButton("Ativar permissão", (dialog, which) -> {
                    Intent intent = new Intent(
                            Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
                    );

                    startActivity(intent);
                })
                .setNegativeButton("Agora não", null)
                .setCancelable(false)
                .show();
    }

}

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
import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import android.widget.Button;
import android.widget.Toast;


import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> folderPicker;

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

        Button refreshButton = findViewById(R.id.refreshButton);

        currentTrackProgress = findViewById(R.id.currentTrackProgress);
        currentTrackElapsed = findViewById(R.id.currentTrackElapsed);
        currentTrackRemaining = findViewById(R.id.currentTrackRemaining);

        folderPicker = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {

                    if (result.getResultCode() == RESULT_OK
                            && result.getData() != null) {

                        Uri uri = result.getData().getData();

                        if (uri != null) {
                            PlaybackStorage.setFolder(this, uri);

                            if (PlaybackStorage.hasFolder(this)) {
                                carregarEstatisticas();

                                Toast.makeText(
                                        this,
                                        "Armazenamento configurado!",
                                        Toast.LENGTH_SHORT
                                ).show();
                            } else {
                                Toast.makeText(
                                        this,
                                        "Não foi possível configurar a pasta.",
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }

                    } else {
                        carregarEstatisticas();
                    }

                    iniciarVerificacaoNotificacao();
                }
        );

        if (!PlaybackStorage.hasFolder(this)) {
            mostrarConfiguracaoArmazenamento();
        } else {
            carregarEstatisticas();
            iniciarVerificacaoNotificacao();
        }

        // Mantenha aqui os demais listeners e configurações.

        refreshButton.setOnClickListener(v -> {
            carregarEstatisticas();

            Toast.makeText(
                    this,
                    "Estatísticas atualizadas!",
                    Toast.LENGTH_SHORT
            ).show();
        });
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

    }

    private void iniciarVerificacaoNotificacao() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            verificarPermissaoNotificacao();
        }, 400);
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

        // Sequência e proteções
        long currentStreak =
                PlaybackStatistics.getCurrentStreak(history, this);

        long bestStreak =
                PlaybackStatistics.getBestStreak(history, this);

        int protections =
                GoalStorage.getProtectionsRemaining(this);

        TextView currentStreakView =
                findViewById(R.id.currentStreak);

        TextView bestStreakView =
                findViewById(R.id.bestStreak);

        TextView protectionsView =
                findViewById(R.id.streakProtections);

        currentStreakView.setText(currentStreak + " dias");

        bestStreakView.setText("Recorde: " + bestStreak + " dias");

        protectionsView.setText(
                "🛡️ Proteções restantes: " + protections
        );
        Log.d("AudioTime", "Histórico carregado: " + history.toString());

        long totalTime = history.optLong("totalTime", 0);

        long weeklyTotal =
                PlaybackStatistics.getWeeklyTotal(history);
        JSONArray sessions = history.optJSONArray("sessions");

        long sessionsTotal = 0;

        if (sessions != null) {
            for (int i = 0; i < sessions.length(); i++) {
                JSONObject session = sessions.optJSONObject(i);

                if (session != null) {
                    sessionsTotal += session.optLong("duration", 0);
                }
            }
        }

        Log.d("AudioTime", "Total geral: " + totalTime);
        Log.d("AudioTime", "Total das sessões: " + sessionsTotal);
        Log.d("AudioTime", "Quantidade de sessões: " +
                (sessions != null ? sessions.length() : 0));

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


    private void mostrarConfiguracaoArmazenamento() {

        new AlertDialog.Builder(this)
                .setTitle("Bem-vindo ao FreeAudioTime!")
                .setMessage(
                        "Antes de começar, precisamos configurar onde " +
                                "seu histórico de reprodução será salvo.\n\n" +

                                "📁 Local: Documentos/FreeAudioTime\n\n" +

                                "Se a pasta não existir, o aplicativo irá criá-la " +
                                "automaticamente.\n\n" +

                                "Se você trocar de celular ou reinstalar o aplicativo, " +
                                "poderá selecionar essa mesma pasta para recuperar " +
                                "suas estatísticas.\n\n" +

                                "Seus dados ficam armazenados no próprio dispositivo."
                )
                .setPositiveButton("Escolher pasta", (dialog, which) -> {
                    Intent intent = new Intent(
                            Intent.ACTION_OPEN_DOCUMENT_TREE
                    );

                    folderPicker.launch(intent);
                })
                .setNegativeButton("Agora não", (dialog, which) -> {
                    carregarEstatisticas();
                })
                .setCancelable(false)
                .show();
    }


}
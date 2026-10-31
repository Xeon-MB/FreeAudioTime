package com.example.freeaudiotime;

import android.os.Bundle;
import android.widget.TextView;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import androidx.core.content.ContextCompat;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

public class RecordsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_records);

        TextView backButton = findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        carregarRecordes();
    }

    private void carregarRecordes() {
        JSONObject history = PlaybackStorage.load(this);

        String bestDay = PlaybackStatistics.getBestDay(history);
        long bestDayTime = PlaybackStatistics.getBestDayTime(history);
        int longestStreak = PlaybackStatistics.getLongestStreak(history);
        long totalTime = history.optLong("totalTime", 0);
        carregarConquistas(totalTime);
        long nextMilestone = PlaybackStatistics.getNextMilestone(totalTime);
        String mostPlayedArtist = PlaybackStatistics.getMostPlayedArtist(history);
        String mostPlayedTrack = PlaybackStatistics.getMostPlayedTrack(history);

        TextView recordBestDayView = findViewById(R.id.recordBestDay);
        TextView recordBestDayTimeView = findViewById(R.id.recordBestDayTime);
        TextView recordStreakView = findViewById(R.id.recordStreak);
        TextView recordMilestoneView = findViewById(R.id.recordMilestone);
        TextView recordArtistView = findViewById(R.id.recordMostPlayedArtist);
        TextView recordTrackView = findViewById(R.id.recordMostPlayedTrack);

        if (recordBestDayView != null) {
            recordBestDayView.setText(formatarData(bestDay));
        }
        if (recordBestDayTimeView != null) {
            recordBestDayTimeView.setText(formatarTempo(bestDayTime));
        }
        if (recordStreakView != null) {
            recordStreakView.setText(longestStreak + (longestStreak == 1 ? " dia" : " dias"));
        }
        if (recordMilestoneView != null) {
            if (nextMilestone > 0) {
                recordMilestoneView.setText(formatarTempo(nextMilestone));
            } else {
                recordMilestoneView.setText("Concluído!");
            }
        }
        if (recordArtistView != null) {
            recordArtistView.setText(mostPlayedArtist);
        }
        if (recordTrackView != null) {
            recordTrackView.setText(mostPlayedTrack);
        }
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

    private String formatarData(String data) {

        try {
            LocalDate date = LocalDate.parse(data);

            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern(
                            "dd 'de' MMMM 'de' yyyy",
                            new Locale("pt", "BR")
                    );

            return date.format(formatter);

        } catch (Exception e) {
            return data;
        }
    }
    private void carregarConquistas(long totalTime) {

        LinearLayout container = findViewById(R.id.achievementsContainer);

        if (container == null) return;

        container.removeAllViews();

        String[] nomes = {
                "Primeiras 10 horas",
                "50 horas de música",
                "100 horas de música",
                "Lenda musical"
        };

        String[] descricoes = {
                "Você começou sua jornada musical!",
                "Já são 50 horas de reprodução.",
                "100 horas acompanhadas. Impressionante!",
                "500 horas de música. Uma verdadeira lenda!"
        };

        long[] metas = {
                10 * 3600L,
                50 * 3600L,
                100 * 3600L,
                500 * 3600L
        };

        String[] icones = {"🎧", "🌟", "💚", "👑"};

        for (int i = 0; i < metas.length; i++) {

            boolean desbloqueada = totalTime >= metas[i];

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(18), dp(15), dp(18), dp(15));

            GradientDrawable background = new GradientDrawable();
            background.setColor(
                    desbloqueada
                            ? cor(R.color.card_light)
                            : cor(R.color.card_yellow)
            );
            background.setCornerRadius(dp(16));

            card.setBackground(background);

            LinearLayout.LayoutParams cardParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );

            cardParams.bottomMargin = dp(10);
            card.setLayoutParams(cardParams);

            TextView titulo = new TextView(this);
            titulo.setText(icones[i] + "  " + nomes[i]);
            titulo.setTextSize(16);
            titulo.setTextColor(cor(R.color.text_primary));
            titulo.setTypeface(null, Typeface.BOLD);

            TextView descricao = new TextView(this);
            descricao.setText(descricoes[i]);
            descricao.setTextSize(12);
            descricao.setTextColor(cor(R.color.text_secondary));

            TextView status = new TextView(this);
            status.setText(
                    desbloqueada ? "✓ DESBLOQUEADA" : "🔒 BLOQUEADA"
            );
            status.setTextSize(10);
            status.setTypeface(null, Typeface.BOLD);
            status.setTextColor(
                    desbloqueada
                            ? cor(R.color.text_secondary)
                            : cor(R.color.text_yellow)
            );

            LinearLayout.LayoutParams statusParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );

            statusParams.topMargin = dp(8);

            card.addView(titulo);
            card.addView(descricao);
            card.addView(status, statusParams);

            container.addView(card);
        }
    }

    private int dp(int valor) {
        return (int) (valor * getResources().getDisplayMetrics().density);
    }
    private int cor(int resource) {
        return ContextCompat.getColor(this, resource);
    }
}

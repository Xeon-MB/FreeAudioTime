package com.example.freeaudiotime;

import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.widget.LinearLayout;
import androidx.core.content.ContextCompat;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;
import android.view.View;


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
        configurarMetas();
        configurarMenuMetas();
    }

    private void carregarRecordes() {
        JSONObject history = PlaybackStorage.load(this);
        carregarProgressoMetas(history);

        String bestDay = PlaybackStatistics.getBestDay(history);
        long bestDayTime = PlaybackStatistics.getBestDayTime(history);
        int longestStreak =
                PlaybackStatistics.getBestStreak(history, this);

        int protections =
                GoalStorage.getProtectionsRemaining(this);
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
        TextView recordProtectionsView =
                findViewById(R.id.recordProtections);

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
        if (recordProtectionsView != null) {
            recordProtectionsView.setText(
                    "🛡️ Proteções restantes: " + protections
            );
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

    private void configurarMetas() {

        EditText dailyInput = findViewById(R.id.dailyGoalInput);
        EditText monthlyInput = findViewById(R.id.monthlyGoalInput);
        EditText streakInput = findViewById(R.id.streakGoalInput);

        Button saveButton = findViewById(R.id.saveGoalsButton);

        // Carregar valores salvos
        dailyInput.setText(String.valueOf(
                GoalStorage.getDailyGoal(this) / 3600
        ));

        monthlyInput.setText(String.valueOf(
                GoalStorage.getMonthlyGoal(this) / 3600
        ));

        streakInput.setText(String.valueOf(
                GoalStorage.getStreakGoal(this)
        ));

        saveButton.setOnClickListener(v -> {

            String dailyText = dailyInput.getText().toString().trim();
            String monthlyText = monthlyInput.getText().toString().trim();
            String streakText = streakInput.getText().toString().trim();

            if (dailyText.isEmpty()
                    || monthlyText.isEmpty()
                    || streakText.isEmpty()) {

                Toast.makeText(
                        this,
                        "Preencha todas as metas.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            try {
                long dailyHours = Long.parseLong(dailyText);
                long monthlyHours = Long.parseLong(monthlyText);
                int streakDays = Integer.parseInt(streakText);

                if (dailyHours < 0 || monthlyHours < 0 || streakDays < 0) {
                    Toast.makeText(
                            this,
                            "Use valores iguais ou maiores que zero.",
                            Toast.LENGTH_SHORT
                    ).show();

                    return;
                }

                GoalStorage.saveDailyGoal(
                        this,
                        dailyHours * 3600
                );

                GoalStorage.saveMonthlyGoal(
                        this,
                        monthlyHours * 3600
                );

                GoalStorage.saveStreakGoal(
                        this,
                        streakDays
                );

                Toast.makeText(
                        this,
                        "Metas salvas com sucesso!",
                        Toast.LENGTH_SHORT
                ).show();

            } catch (NumberFormatException e) {

                Toast.makeText(
                        this,
                        "Digite valores válidos.",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void configurarMenuMetas() {

        LinearLayout goalsHeader = findViewById(R.id.goalsHeader);
        LinearLayout goalsContent = findViewById(R.id.goalsContent);
        TextView goalsArrow = findViewById(R.id.goalsArrow);

        goalsHeader.setOnClickListener(v -> {

            if (goalsContent.getVisibility() == View.GONE) {
                goalsContent.setVisibility(View.VISIBLE);
                goalsArrow.setText("▲");
            } else {
                goalsContent.setVisibility(View.GONE);
                goalsArrow.setText("▼");
            }
        });
    }


    private void carregarProgressoMetas(JSONObject history) {

        long dailyGoal = GoalStorage.getDailyGoal(this);
        long monthlyGoal = GoalStorage.getMonthlyGoal(this);
        int streakGoal = GoalStorage.getStreakGoal(this);

        long dailyRemaining =
                PlaybackStatistics.getDailyGoalRemaining(history, this);

        long monthlyRemaining =
                PlaybackStatistics.getMonthlyGoalRemaining(history, this);

        int currentStreak =
                PlaybackStatistics.getCurrentStreak(history, this);

        long dailyListened = Math.max(0, dailyGoal - dailyRemaining);
        long monthlyListened = Math.max(0, monthlyGoal - monthlyRemaining);

        int dailyProgress =
                PlaybackStatistics.getDailyGoalProgress(history, this);

        int monthlyProgress =
                PlaybackStatistics.getMonthlyGoalProgress(history, this);

        int streakProgress = streakGoal > 0
                ? (int) Math.min(100, currentStreak * 100.0 / streakGoal)
                : 0;

        ProgressBar dailyBar = findViewById(R.id.dailyProgressBar);
        ProgressBar monthlyBar = findViewById(R.id.monthlyProgressBar);
        ProgressBar streakBar = findViewById(R.id.streakProgressBar);

        TextView dailyText = findViewById(R.id.dailyProgressText);
        TextView monthlyText = findViewById(R.id.monthlyProgressText);
        TextView streakText = findViewById(R.id.streakProgressText);

        dailyBar.setProgress(dailyProgress);
        monthlyBar.setProgress(monthlyProgress);
        streakBar.setProgress(streakProgress);

        dailyText.setText(
                dailyGoal > 0
                        ? formatarTempo(dailyListened) + " / " + formatarTempo(dailyGoal)
                        : "Meta não definida"
        );

        monthlyText.setText(
                monthlyGoal > 0
                        ? formatarTempo(monthlyListened) + " / " + formatarTempo(monthlyGoal)
                        : "Meta não definida"
        );

        streakText.setText(
                streakGoal > 0
                        ? currentStreak + " / " + streakGoal + " dias"
                        : "Meta não definida"
        );
    }

}

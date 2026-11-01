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
import java.util.Date;
import java.text.SimpleDateFormat;

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
        AchievementManager.checkAchievements(this, history);
        carregarProgressoMetas(history);

        String bestDay = PlaybackStatistics.getBestDay(history);
        long bestDayTime = PlaybackStatistics.getBestDayTime(history);
        int longestStreak =
                PlaybackStatistics.getBestStreak(history, this);

        int protections =
                GoalStorage.getProtectionsRemaining(this);
        long totalTime = history.optLong("totalTime", 0);
        carregarConquistas();
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
    private void carregarConquistas() {

        LinearLayout container =
                findViewById(R.id.achievementsContainer);

        if (container == null) return;

        container.removeAllViews();

        JSONObject desbloqueadas = AchievementStorage.load(this);

        String[][] ids = {
                {
                        "first_10_hours", "hours_50", "hours_100",
                        "hours_500", "hours_1000"
                },
                {
                        "streak_3", "streak_7", "streak_30", "streak_100"
                },
                {
                        "artists_5", "artists_10", "artists_25",
                        "tracks_10", "tracks_50", "tracks_100"
                },
                {
                        "listening_days_7", "listening_days_30",
                        "listening_days_100"
                },
                {
                        "daily_goal_7_days", "monthly_goal"
                }
        };

        String[][] nomes = {
                {
                        "Primeiras 10 horas", "50 horas de música",
                        "100 horas de música", "Lenda musical",
                        "1.000 horas de música"
                },
                {
                        "3 dias de sequência", "7 dias de sequência",
                        "30 dias de sequência", "100 dias de sequência"
                },
                {
                        "Conhecendo artistas", "Explorador musical",
                        "Colecionador de artistas", "10 músicas diferentes",
                        "50 músicas diferentes", "100 músicas diferentes"
                },
                {
                        "Uma semana ouvindo", "30 dias de música",
                        "100 dias de música"
                },
                {
                        "7 dias cumprindo a meta",
                        "Meta mensal alcançada"
                }
        };

        String[][] descricoes = {
                {
                        "Você começou sua jornada musical!",
                        "Já são 50 horas de reprodução.",
                        "100 horas acompanhadas!",
                        "500 horas de música!",
                        "Você chegou a mil horas!"
                },
                {
                        "Sua primeira sequência!",
                        "Uma semana de dedicação!",
                        "Um mês mantendo a sequência!",
                        "Uma sequência impressionante!"
                },
                {
                        "Você ouviu 5 artistas diferentes.",
                        "Você conheceu 10 artistas.",
                        "Já ouviu 25 artistas diferentes.",
                        "Você ouviu 10 faixas diferentes.",
                        "Você ouviu 50 faixas diferentes.",
                        "Você ouviu 100 faixas diferentes."
                },
                {
                        "Você ouviu música em 7 dias diferentes.",
                        "Você ouviu música em 30 dias diferentes.",
                        "Você ouviu música em 100 dias diferentes."
                },
                {
                        "Você atingiu sua meta diária em 7 dias do mês.",
                        "Você alcançou sua meta mensal de reprodução."
                }
        };

        String[][] icones = {
                {"🎧", "🌟", "💚", "👑", "🚀"},
                {"🔥", "⚡", "🏆", "💎"},
                {"🎨", "🧭", "🌎", "🎵", "🎼", "🎹"},
                {"📅", "🗓️", "⏳"},
                {"🎯", "🏅"}
        };

        String[] categorias = {
                "TEMPO DE REPRODUÇÃO",
                "SEQUÊNCIAS",
                "DESCOBERTAS",
                "CONSISTÊNCIA",
                "METAS"
        };

        for (int c = 0; c < categorias.length; c++) {

            int totalDesbloqueadas = 0;

            for (String id : ids[c]) {
                if (desbloqueadas.has(id)) {
                    totalDesbloqueadas++;
                }
            }

            adicionarCategoria(
                    container,
                    categorias[c],
                    totalDesbloqueadas,
                    ids[c],
                    nomes[c],
                    descricoes[c],
                    icones[c],
                    desbloqueadas
            );
        }
    }

    private void adicionarCategoria(
            LinearLayout container,
            String nome,
            int desbloqueadas,
            String[] ids,
            String[] nomes,
            String[] descricoes,
            String[] icones,
            JSONObject dados
    ) {

        LinearLayout categoria = new LinearLayout(this);
        categoria.setOrientation(LinearLayout.VERTICAL);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(15), dp(16), dp(15));

        GradientDrawable headerBackground = new GradientDrawable();
        headerBackground.setColor(cor(R.color.card_yellow));
        headerBackground.setCornerRadius(dp(14));
        header.setBackground(headerBackground);

        TextView titulo = new TextView(this);
        titulo.setText(nome + "  (" + desbloqueadas + "/" + ids.length + ")");
        titulo.setTextSize(13);
        titulo.setTypeface(null, Typeface.BOLD);
        titulo.setTextColor(cor(R.color.text_primary));

        TextView seta = new TextView(this);
        seta.setText("▼");
        seta.setTextSize(14);
        seta.setTypeface(null, Typeface.BOLD);
        seta.setTextColor(cor(R.color.text_primary));

        header.addView(titulo, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));

        header.addView(seta);

        LinearLayout conteudo = new LinearLayout(this);
        conteudo.setOrientation(LinearLayout.VERTICAL);
        conteudo.setPadding(dp(8), dp(10), dp(8), dp(4));
        conteudo.setVisibility(View.GONE);

        for (int i = 0; i < ids.length; i++) {

            boolean liberada = dados.has(ids[i]);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(16), dp(14), dp(16), dp(14));

            GradientDrawable cardBackground = new GradientDrawable();
            cardBackground.setColor(cor(
                    liberada ? R.color.card_light : R.color.card_yellow
            ));
            cardBackground.setCornerRadius(dp(14));
            card.setBackground(cardBackground);

            LinearLayout.LayoutParams cardParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );

            cardParams.bottomMargin = dp(8);
            card.setLayoutParams(cardParams);

            TextView nomeConquista = new TextView(this);
            nomeConquista.setText(icones[i] + "  " + nomes[i]);
            nomeConquista.setTextSize(15);
            nomeConquista.setTypeface(null, Typeface.BOLD);
            nomeConquista.setTextColor(cor(R.color.text_primary));

            TextView descricao = new TextView(this);
            descricao.setText(descricoes[i]);
            descricao.setTextSize(12);
            descricao.setTextColor(cor(R.color.text_secondary));

            TextView status = new TextView(this);

            if (liberada) {
                long timestamp = dados.optLong(ids[i]);

                String data = new SimpleDateFormat(
                        "dd/MM/yyyy",
                        new Locale("pt", "BR")
                ).format(new Date(timestamp));

                status.setText("✓ DESBLOQUEADA EM " + data);
            } else {
                status.setText("🔒 BLOQUEADA");
            }

            status.setTextSize(11);
            status.setTypeface(null, Typeface.BOLD);
            status.setTextColor(cor(R.color.text_primary));
            status.setPadding(dp(10), dp(7), dp(10), dp(7));

            GradientDrawable statusBackground = new GradientDrawable();
            statusBackground.setColor(cor(R.color.card_yellow));
            statusBackground.setCornerRadius(dp(8));
            status.setBackground(statusBackground);

            LinearLayout.LayoutParams statusParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );

            statusParams.topMargin = dp(10);

            card.addView(nomeConquista);
            card.addView(descricao);
            card.addView(status, statusParams);

            conteudo.addView(card);
        }

        header.setOnClickListener(v -> {
            if (conteudo.getVisibility() == View.GONE) {
                conteudo.setVisibility(View.VISIBLE);
                seta.setText("▲");
            } else {
                conteudo.setVisibility(View.GONE);
                seta.setText("▼");
            }
        });

        LinearLayout.LayoutParams categoriaParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        categoriaParams.bottomMargin = dp(12);

        categoria.setLayoutParams(categoriaParams);
        categoria.addView(header);
        categoria.addView(conteudo);

        container.addView(categoria);
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

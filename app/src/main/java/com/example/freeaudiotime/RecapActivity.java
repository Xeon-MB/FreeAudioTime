
package com.example.freeaudiotime;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Button;
import android.content.res.ColorStateList;
import android.graphics.Color;
import androidx.core.content.ContextCompat;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.time.format.TextStyle;
import android.view.View;


public class RecapActivity extends AppCompatActivity {

    private static final int SEMANA = 0;
    private static final int MES = 1;
    private static final int ANO = 2;

    private JSONObject history;

    private Button weekButton;
    private Button monthButton;
    private Button yearButton;

    private TextView totalView;
    private TextView artistView;
    private TextView trackView;
    private TextView topArtistsView;
    private TextView topTracksView;
    private TextView periodView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recap);

        history = PlaybackStorage.load(this);

        TextView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        weekButton = findViewById(R.id.periodWeek);
        monthButton = findViewById(R.id.periodMonth);
        yearButton = findViewById(R.id.periodYear);

        totalView = findViewById(R.id.weeklyTotal);
        artistView = findViewById(R.id.weeklyArtist);
        trackView = findViewById(R.id.weeklyTrack);
        topArtistsView = findViewById(R.id.weeklyTopArtists);
        topTracksView = findViewById(R.id.weeklyTopTracks);
        periodView = findViewById(R.id.weekPeriod);
        DailyActivityChart chart = findViewById(R.id.dailyActivityChart);

        Map<String, Long> dailyTotals =
                PlaybackStatistics.getDailyTotals(history);

        chart.setData(dailyTotals);
        atualizarMelhorDia(dailyTotals);

        weekButton.setOnClickListener(v -> carregarPeriodo(SEMANA));
        monthButton.setOnClickListener(v -> carregarPeriodo(MES));
        yearButton.setOnClickListener(v -> carregarPeriodo(ANO));

        carregarPeriodo(SEMANA);
    }

    private void carregarPeriodo(int periodo) {

        long total;
        String artists;
        String tracks;

        if (periodo == SEMANA) {

            total = PlaybackStatistics.getWeeklyTotal(history);
            artists = PlaybackStatistics.getWeeklyTopArtists(history);
            tracks = PlaybackStatistics.getWeeklyTopTracks(history);

        } else if (periodo == MES) {

            total = PlaybackStatistics.getMonthlyTotal(history);
            artists = PlaybackStatistics.getMonthlyTopArtists(history);
            tracks = PlaybackStatistics.getMonthlyTopTracks(history);

        } else {

            total = PlaybackStatistics.getYearlyTotal(history);
            artists = PlaybackStatistics.getYearlyTopArtists(history);
            tracks = PlaybackStatistics.getYearlyTopTracks(history);
        }

        totalView.setText(formatarTempo(total));

        topArtistsView.setText(artists);
        topTracksView.setText(tracks);

        artistView.setText(primeiroItem(artists));
        trackView.setText(primeiroItem(tracks));

        atualizarPeriodo(periodo);
        atualizarBotoes(periodo);
        View comparison = findViewById(R.id.weeklyComparison);

        comparison.setVisibility(
                periodo == SEMANA ? View.VISIBLE : View.GONE
        );

        if (periodo == SEMANA) {
            atualizarComparacaoSemanal();
        }
    }


    private void atualizarBotoes(int selecionado) {

        Button[] botoes = {weekButton, monthButton, yearButton};

        int verde = ContextCompat.getColor(this, R.color.card_dark);
        int claro = ContextCompat.getColor(this, R.color.card_light);
        int textoClaro = ContextCompat.getColor(this, R.color.text_on_dark);
        int textoEscuro = ContextCompat.getColor(this, R.color.text_primary);

        for (int i = 0; i < botoes.length; i++) {

            boolean ativo = i == selecionado;

            botoes[i].setBackgroundTintList(
                    ColorStateList.valueOf(ativo ? verde : claro)
            );

            botoes[i].setTextColor(
                    ativo ? textoClaro : textoEscuro
            );
        }
    }

    private String primeiroItem(String ranking) {

        if (ranking == null || ranking.trim().isEmpty()
                || ranking.startsWith("Nenhum")) {
            return "Nenhum registro";
        }

        String[] linhas = ranking.split("\n");

        return linhas[0].trim();
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

    private void atualizarPeriodo(int periodo) {

        LocalDate hoje = LocalDate.now();

        Locale brasil = new Locale("pt", "BR");

        DateTimeFormatter formato =
                DateTimeFormatter.ofPattern("dd MMM", brasil);

        String texto;

        if (periodo == SEMANA) {

            LocalDate inicio = hoje.with(DayOfWeek.MONDAY);
            LocalDate fim = inicio.plusDays(6);

            texto = inicio.format(formato) + " — "
                    + fim.format(formato) + " " + fim.getYear();

        } else if (periodo == MES) {

            DateTimeFormatter formatoMes =
                    DateTimeFormatter.ofPattern("MMMM 'DE' yyyy", brasil);

            texto = hoje.format(formatoMes);

        } else {

            texto = String.valueOf(hoje.getYear());
        }

        periodView.setText(texto.toUpperCase(brasil));
    }

    private void atualizarMelhorDia(Map<String, Long> dailyTotals) {

        LocalDate hoje = LocalDate.now();

        LocalDate melhorData = null;
        long maiorTempo = 0;

        // Analisa os últimos sete dias
        for (int i = 6; i >= 0; i--) {

            LocalDate data = hoje.minusDays(i);

            long tempo = dailyTotals.getOrDefault(
                    data.toString(), 0L
            );

            if (tempo > maiorTempo) {
                maiorTempo = tempo;
                melhorData = data;
            }
        }

        TextView dayName = findViewById(R.id.bestDayName);
        TextView dayTime = findViewById(R.id.bestDayTime);
        TextView description = findViewById(R.id.bestDayDescription);

        if (melhorData == null) {

            dayName.setText("SEM REPRODUÇÃO");
            dayTime.setText("0min");
            description.setText(
                    "Ainda não há registros nesta semana."
            );

            return;
        }

        Locale brasil = new Locale("pt", "BR");

        String nomeDia = melhorData.getDayOfWeek()
                .getDisplayName(TextStyle.FULL, brasil);

        dayName.setText(nomeDia.toUpperCase(brasil));
        dayTime.setText(formatarTempo(maiorTempo));

        description.setText(
                "Seu dia com mais música nos últimos 7 dias."
        );
    }


    private void atualizarComparacaoSemanal() {

        View comparison = findViewById(R.id.weeklyComparison);

        if (comparison == null) return;

        // A comparação só aparece no modo semanal
        comparison.setVisibility(View.VISIBLE);

        Map<String, Long> dailyTotals =
                PlaybackStatistics.getDailyTotals(history);

        LocalDate hoje = LocalDate.now();

        LocalDate inicioSemanaAtual =
                hoje.with(DayOfWeek.MONDAY);

        LocalDate inicioSemanaAnterior =
                inicioSemanaAtual.minusWeeks(1);

        LocalDate fimSemanaAnterior = hoje.minusWeeks(1);

        long tempoAtual = somarPeriodo(
                dailyTotals,
                inicioSemanaAtual,
                hoje
        );

        long tempoAnterior = somarPeriodo(
                dailyTotals,
                inicioSemanaAnterior,
                fimSemanaAnterior
        );

        TextView differenceView =
                findViewById(R.id.weeklyDifference);

        TextView descriptionView =
                findViewById(R.id.weeklyDifferenceDescription);

        TextView currentView =
                findViewById(R.id.currentWeekTime);

        TextView previousView =
                findViewById(R.id.previousWeekTime);

        currentView.setText(formatarTempo(tempoAtual));
        previousView.setText(formatarTempo(tempoAnterior));

        if (tempoAnterior == 0 && tempoAtual == 0) {

            differenceView.setText("0%");
            descriptionView.setText(
                    "Ainda não há registros para comparar."
            );

        } else if (tempoAnterior == 0) {

            differenceView.setText("NOVO!");
            descriptionView.setText(
                    "Você começou a registrar seu tempo de música nesta semana."
            );

        } else {

            long diferenca = tempoAtual - tempoAnterior;

            long percentual = Math.round(
                    (diferenca * 100.0) / tempoAnterior
            );

            if (diferenca > 0) {

                differenceView.setText("+" + percentual + "%");
                descriptionView.setText(
                        "Você ouviu mais música nesta semana."
                );

            } else if (diferenca < 0) {

                differenceView.setText(percentual + "%");
                descriptionView.setText(
                        "Você ouviu menos música nesta semana."
                );

            } else {

                differenceView.setText("0%");
                descriptionView.setText(
                        "Seu tempo de música permaneceu igual."
                );
            }
        }
    }

    private long somarPeriodo(
            Map<String, Long> dailyTotals,
            LocalDate inicio,
            LocalDate fim
    ) {

        long total = 0;

        for (LocalDate data = inicio;
             !data.isAfter(fim);
             data = data.plusDays(1)) {

            total += dailyTotals.getOrDefault(
                    data.toString(), 0L
            );
        }

        return total;
    }


}

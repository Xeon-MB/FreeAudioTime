    
    package com.example.freeaudiotime;
    
    import android.app.DatePickerDialog;
    import android.os.Bundle;
    import android.util.Log;
    import android.widget.EditText;
    import android.widget.LinearLayout;
    import android.widget.TextView;
    import android.widget.Button;
    import android.content.res.ColorStateList;
    import android.graphics.Color;
    import androidx.core.content.ContextCompat;
    import android.graphics.Bitmap;
    import android.graphics.Canvas;
    import android.provider.MediaStore;
    import android.content.ContentValues;
    import android.net.Uri;
    import android.os.Environment;
    import android.widget.Toast;
    import java.io.OutputStream;
    import java.io.IOException;
    import android.view.LayoutInflater;
    import android.view.ViewGroup;
    import android.view.View;
    import android.graphics.BitmapFactory;
    import android.view.Gravity;
    import android.widget.ImageView;
    import android.text.TextUtils;
    import org.json.JSONArray;
    import org.json.JSONObject;
    import java.io.File;
    import androidx.palette.graphics.Palette;
    import android.graphics.drawable.ColorDrawable;
    import android.graphics.Typeface;
    import java.time.Instant;
    import java.time.ZoneId;

    
    
    
    import androidx.appcompat.app.AppCompatActivity;
    
    
    import java.time.DayOfWeek;
    import java.time.LocalDate;
    import java.time.format.DateTimeFormatter;
    import java.util.HashMap;
    import java.util.Locale;
    import java.util.Map;
    import java.time.format.TextStyle;
    
    
    public class RecapActivity extends AppCompatActivity {
    
        private static final int SEMANA = 0;
        private static final int MES = 1;
        private static final int ANO = 2;
        private static final int DIA = 3;
    
        private JSONObject history;
    
        private Button weekButton;
        private Button monthButton;
        private Button yearButton;
        private LocalDate dataSelecionada = LocalDate.now();
        private Button dayButton;
        private Button dayCalendarButton;
        private EditText dayDateInput;
        private LinearLayout dayDateSelector;
    
    
        private TextView totalView;
        private TextView artistView;
        private TextView trackView;
    
        private TextView periodTotalDescription;
        private TextView chartTitle;
        private TextView chartSubtitle;
        private TextView bestDayTitle;
        private TextView artistPeriodTitle;
        private TextView trackPeriodTitle;
        private TextView bestDayDescription;
        private TextView topArtistsView;
        private LinearLayout topTracksView;
        private TextView periodView;
    
        private String rankingTracksAtual = "Nenhuma música";
    
        private android.widget.ImageView weeklyArtistImage;
    
        private int corFundo = Color.rgb(16, 29, 23);
        private int corDestaque = Color.rgb(36, 69, 54);
        private int corTexto = Color.rgb(244, 242, 223);
    
        private boolean paletaPronta = false;
        private boolean exportacaoPendente = false;
    
        private int periodoAtual = SEMANA;
    
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            ThemeManager.applySavedTheme(this);
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_recap);
            Button exportButton = findViewById(R.id.exportRecapButton);
            periodTotalDescription = findViewById(R.id.periodTotalDescription);
            chartTitle = findViewById(R.id.chartTitle);
            chartSubtitle = findViewById(R.id.chartSubtitle);
            bestDayTitle = findViewById(R.id.bestDayTitle);
            artistPeriodTitle = findViewById(R.id.artistPeriodTitle);
            trackPeriodTitle = findViewById(R.id.trackPeriodTitle);
            bestDayDescription = findViewById(R.id.bestDayDescription);
            dayButton = findViewById(R.id.periodDay);
            dayCalendarButton = findViewById(R.id.dayCalendarButton);
            dayDateInput = findViewById(R.id.dayDateInput);
            dayDateSelector = findViewById(R.id.dayDateSelector);
    
            dayCalendarButton.setOnClickListener(v -> {
                DatePickerDialog dialog = new DatePickerDialog(
                        this,
                        (view, year, month, dayOfMonth) -> {
                            dataSelecionada = LocalDate.of(
                                    year, month + 1, dayOfMonth
                            );
    
                            dayDateInput.setText(
                                    dataSelecionada.format(
                                            DateTimeFormatter.ofPattern("dd/MM/yyyy")
                                    )
                            );
    
                            carregarPeriodo(DIA);
                        },
                        dataSelecionada.getYear(),
                        dataSelecionada.getMonthValue() - 1,
                        dataSelecionada.getDayOfMonth()
                );
    
                dialog.getDatePicker().setMaxDate(
                        System.currentTimeMillis()
                );
    
                dialog.show();
            });
    
            dayButton.setOnClickListener(v -> carregarPeriodo(DIA));
    
            dayDateInput.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                    try {
                        DateTimeFormatter formato = DateTimeFormatter
                                .ofPattern("dd/MM/uuuu")
                                .withResolverStyle(
                                        java.time.format.ResolverStyle.STRICT
                                );
    
                        LocalDate data = LocalDate.parse(
                                dayDateInput.getText().toString().trim(),
                                formato
                        );
    
                        if (data.isAfter(LocalDate.now())) {
                            dayDateInput.setError("Data futura não permitida.");
                            return true;
                        }
    
                        dataSelecionada = data;
                        carregarPeriodo(DIA);
    
                    } catch (java.time.DateTimeException e) {
                        dayDateInput.setError("Data inválida. Use DD/MM/AAAA.");
                    }
    
                    return true;
                }
    
                return false;
            });
    
            exportButton.setOnClickListener(v -> {
                if (paletaPronta) {
                    salvarRecapComoImagem();
                } else {
                    exportacaoPendente = true;
                }
            });
    
            history = PlaybackStorage.load(this);
    
            TextView backButton = findViewById(R.id.backButton);
            backButton.setOnClickListener(v -> finish());
    
            weekButton = findViewById(R.id.periodWeek);
            monthButton = findViewById(R.id.periodMonth);
            yearButton = findViewById(R.id.periodYear);
    
            totalView = findViewById(R.id.weeklyTotal);
            artistView = findViewById(R.id.weeklyArtist);
            weeklyArtistImage = findViewById(R.id.weeklyArtistImage);
            trackView = findViewById(R.id.weeklyTrack);
            topArtistsView = findViewById(R.id.weeklyTopArtists);
            topTracksView = findViewById(R.id.weeklyTopTracks);
            periodView = findViewById(R.id.weekPeriod);
            DailyActivityChart chart = findViewById(R.id.dailyActivityChart);
    
            weekButton.setOnClickListener(v -> carregarPeriodo(SEMANA));
            monthButton.setOnClickListener(v -> carregarPeriodo(MES));
            yearButton.setOnClickListener(v -> carregarPeriodo(ANO));
    
            carregarPeriodo(SEMANA);
        }
    
        private void carregarPeriodo(int periodo) {
            periodoAtual = periodo;
    
            // Configura os textos de cada período
            if (periodo == SEMANA) {
    
                periodTotalDescription.setText("DE MÚSICA NESTA SEMANA");
                chartTitle.setText("SEU RITMO NA SEMANA");
                chartSubtitle.setText("Minutos ouvidos por dia");
                bestDayTitle.setText("SEU DIA MAIS MUSICAL");
                bestDayDescription.setText("Seu dia com mais música nos últimos 7 dias.");
                artistPeriodTitle.setText("ARTISTA DA SEMANA");
                trackPeriodTitle.setText("MÚSICA DA SEMANA");
    
            } else if (periodo == MES) {
    
                periodTotalDescription.setText("DE MÚSICA NESTE MÊS");
                chartTitle.setText("SEU RITMO NO MÊS");
                chartSubtitle.setText("Minutos ouvidos por mês");
                bestDayTitle.setText("SEU DIA MAIS MUSICAL");
                bestDayDescription.setText("Seu dia com mais música neste mês.");
                artistPeriodTitle.setText("ARTISTA DO MÊS");
                trackPeriodTitle.setText("MÚSICA DO MÊS");
    
            } else if (periodo == DIA) {
    
                periodTotalDescription.setText("DE MÚSICA NESTE DIA");
                chartTitle.setText("SEU RITMO NO DIA");
                chartSubtitle.setText("Tempo de música na data selecionada");
                bestDayTitle.setText("MÚSICAS DIFERENTES OUVIDAS");
                bestDayDescription.setText("Confira sua atividade musical nesta data.");
                artistPeriodTitle.setText("ARTISTA DO DIA");
                trackPeriodTitle.setText("MÚSICA DO DIA");
    
            } else {
    
                periodTotalDescription.setText("DE MÚSICA NESTE ANO");
                chartTitle.setText("SEU RITMO NO ANO");
                chartSubtitle.setText("Minutos ouvidos por ano");
                bestDayTitle.setText("SEU MÊS MAIS MUSICAL");
                bestDayDescription.setText("Seu mês com mais música neste ano.");
                artistPeriodTitle.setText("ARTISTA DO ANO");
                trackPeriodTitle.setText("MÚSICA DO ANO");
            }
    
            // Carrega os dados do período
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
    
            } else if (periodo == DIA) {
    
                String data = dataSelecionada.toString();
    
                total = PlaybackStatistics.getDailyTotal(history, data);
                artists = PlaybackStatistics.getDailyTopArtists(history, data);
                tracks = PlaybackStatistics.getDailyTopTracks(history, data);
    
            } else {
    
                total = PlaybackStatistics.getYearlyTotal(history);
                artists = PlaybackStatistics.getYearlyTopArtists(history);
                tracks = PlaybackStatistics.getYearlyTopTracks(history);
            }
    
            // Configura o gráfico
            DailyActivityChart chart = findViewById(R.id.dailyActivityChart);
    
            if (periodo == SEMANA) {
    
                chart.setData(
                        PlaybackStatistics.getDailyTotals(history)
                );
    
            } else if (periodo == MES) {
    
                chart.setMonthlyData(
                        PlaybackStatistics.getMonthlyTotalsLastMonths(history, 7)
                );
    
            } else if (periodo == DIA) {
    
                chart.setDailyData(
                        dataSelecionada.toString(),
                        PlaybackStatistics.getDailyTotal(
                                history,
                                dataSelecionada.toString()
                        )
                );
    
            } else {
    
                chart.setYearlyData(
                        PlaybackStatistics.getYearlyTotalsLastYears(history, 5)
                );
            }
    
            // Atualiza os dados exibidos
            rankingTracksAtual = tracks;
    
            totalView.setText(formatarTempo(total));
    
            topArtistsView.setText(artists);
            mostrarRankingMusicas(tracks, history);
    
            artistView.setText(primeiroItem(artists));
    
            String artista = primeiroItem(artists);
    
            if (!artista.equals("Nenhum registro")) {
                artista = artista.replaceFirst("^\\d+\\.\\s*", "");
    
                int separador = artista.indexOf(" — ");
    
                if (separador != -1) {
                    artista = artista.substring(0, separador).trim();
                }
            }
    
            android.util.Log.d("RecapDebug", "Artista recebido: " + artista);
    
            if (!artista.equals("Nenhum registro")) {
    
                android.util.Log.d("RecapDebug", "Chamando DeezerService...");
    
                DeezerService.buscarFotoArtista(this, artista, imagem -> {
    
                    if (imagem != null) {
                        weeklyArtistImage.setImageBitmap(imagem);
                        extrairCores(imagem);
    
                    } else {
                        weeklyArtistImage.setImageResource(
                                R.drawable.ic_launcher_foreground
                        );
                    }
                });
    
            } else {
    
                weeklyArtistImage.setImageResource(
                        R.drawable.ic_launcher_foreground
                );
            }
    
            trackView.setText(primeiroItem(tracks));
    
            atualizarPeriodo(periodo);
    
            dayDateSelector.setVisibility(
                    periodo == DIA ? View.VISIBLE : View.GONE
            );
    
            // Atualiza o destaque do período
            Map<String, Long> dailyTotals =
                    PlaybackStatistics.getDailyTotals(history);
    
            if (periodo == SEMANA) {
    
                atualizarMelhorDia(dailyTotals);
    
            } else if (periodo == MES) {
    
                atualizarMelhorDiaDaSemanaNoMes(dailyTotals);
    
            } else if (periodo == ANO) {
    
                atualizarMelhorMes(dailyTotals);
    
            } else if (periodo == DIA) {
    
                int quantidade = PlaybackStatistics.getDailyUniqueTracks(
                        history,
                        dataSelecionada.toString()
                );
    
                TextView bestDayName = findViewById(R.id.bestDayName);
                TextView bestDayTime = findViewById(R.id.bestDayTime);
    
                bestDayName.setText(String.valueOf(quantidade));
    
                bestDayTime.setText(
                        quantidade == 1
                                ? "MÚSICA DIFERENTE"
                                : "MÚSICAS DIFERENTES"
                );
    
                bestDayDescription.setText(
                        quantidade > 0
                                ? "Quantidade de músicas diferentes ouvidas nesta data."
                                : "Nenhuma música registrada nesta data."
                );
            }
    
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
    
            Button[] botoes = {
                    weekButton,
                    monthButton,
                    yearButton,
                    dayButton
            };
    
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
    
            } else if (periodo == DIA) {
    
                texto = dataSelecionada.format(
                        DateTimeFormatter.ofPattern("dd MMM yyyy", brasil)
                );
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
    
        private void atualizarMelhorDiaDaSemanaNoMes(
                Map<String, Long> dailyTotals) {
    
            LocalDate hoje = LocalDate.now();
    
            Map<Integer, Long> totaisPorDia = new HashMap<>();
    
            for (Map.Entry<String, Long> entry : dailyTotals.entrySet()) {
    
                try {
                    LocalDate data = LocalDate.parse(entry.getKey());
    
                    if (data.getYear() == hoje.getYear()
                            && data.getMonthValue() == hoje.getMonthValue()) {
    
                        int diaSemana = data.getDayOfWeek().getValue();
    
                        totaisPorDia.put(
                                diaSemana,
                                totaisPorDia.getOrDefault(diaSemana, 0L)
                                        + entry.getValue()
                        );
                    }
    
                } catch (Exception ignored) {
                }
            }
    
            TextView dayName = findViewById(R.id.bestDayName);
            TextView dayTime = findViewById(R.id.bestDayTime);
            TextView description = findViewById(R.id.bestDayDescription);
    
            if (totaisPorDia.isEmpty()) {
                dayName.setText("SEM REPRODUÇÃO");
                dayTime.setText("0min");
                description.setText("Ainda não há registros neste mês.");
                return;
            }
    
            int melhorDia = 0;
            long maiorTempo = 0;
    
            for (Map.Entry<Integer, Long> entry : totaisPorDia.entrySet()) {
    
                if (entry.getValue() > maiorTempo) {
                    melhorDia = entry.getKey();
                    maiorTempo = entry.getValue();
                }
            }
    
            Locale brasil = new Locale("pt", "BR");
    
            String nomeDia = java.time.DayOfWeek.of(melhorDia)
                    .getDisplayName(TextStyle.FULL, brasil);
    
            dayName.setText(nomeDia.toUpperCase(brasil));
            dayTime.setText(formatarTempo(maiorTempo));
    
            description.setText(
                    "Dia da semana em que você mais ouviu música neste mês."
            );
        }
    
        private void atualizarMelhorMes(Map<String, Long> dailyTotals) {
    
            int anoAtual = LocalDate.now().getYear();
    
            Map<Integer, Long> totaisMensais = new HashMap<>();
    
            for (Map.Entry<String, Long> entry : dailyTotals.entrySet()) {
    
                String data = entry.getKey();
    
                if (data.length() < 10) continue;
    
                try {
                    LocalDate dataRegistro = LocalDate.parse(data);
    
                    if (dataRegistro.getYear() == anoAtual) {
                        int mes = dataRegistro.getMonthValue();
    
                        totaisMensais.put(
                                mes,
                                totaisMensais.getOrDefault(mes, 0L)
                                        + entry.getValue()
                        );
                    }
    
                } catch (Exception ignored) {
                }
            }
    
            TextView dayName = findViewById(R.id.bestDayName);
            TextView dayTime = findViewById(R.id.bestDayTime);
            TextView description = findViewById(R.id.bestDayDescription);
    
            if (totaisMensais.isEmpty()) {
                dayName.setText("SEM REPRODUÇÃO");
                dayTime.setText("0min");
                description.setText("Ainda não há registros neste ano.");
                return;
            }
    
            int melhorMes = 0;
            long maiorTempo = 0;
    
            for (Map.Entry<Integer, Long> entry : totaisMensais.entrySet()) {
                if (entry.getValue() > maiorTempo) {
                    melhorMes = entry.getKey();
                    maiorTempo = entry.getValue();
                }
            }
    
            Locale brasil = new Locale("pt", "BR");
    
            String nomeMes = java.time.Month.of(melhorMes)
                    .getDisplayName(TextStyle.FULL, brasil);
    
            dayName.setText(nomeMes.toUpperCase(brasil));
            dayTime.setText(formatarTempo(maiorTempo));
    
            description.setText("Seu mês com mais música neste ano.");
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
    
        private Bitmap criarImagemRecap() {
    
            View arte = LayoutInflater.from(this)
                    .inflate(R.layout.recap_share, null, false);
            aplicarPaleta(arte);
            View shareTimeCard = arte.findViewById(R.id.shareTimeCard);
    
            if (shareTimeCard != null) {
                shareTimeCard.setBackgroundColor(corDestaque);
            }
    
            // Período
    // Identificação do período na imagem
            String tituloPeriodo;
    
            if (periodoAtual == DIA) {
                tituloPeriodo = "RECAP DIÁRIA";
            } else if (periodoAtual == SEMANA) {
                tituloPeriodo = "RECAP SEMANAL";
            } else if (periodoAtual == MES) {
                tituloPeriodo = "RECAP MENSAL";
            } else {
                tituloPeriodo = "RECAP ANUAL";
            }
    
            ((TextView) arte.findViewById(R.id.sharePeriod))
                    .setText(tituloPeriodo + "\n" + periodView.getText());
    
            // Tempo total
            ((TextView) arte.findViewById(R.id.shareTotal))
                    .setText(totalView.getText().toString().toUpperCase());
    
            long totalSegundos = obterTotalPeriodo();
            ((TextView) arte.findViewById(R.id.shareTotalMinutes))
                    .setText("/// " + (totalSegundos / 60) + " MIN");
    
            // Artista favorito
            String rankingArtistas;
    
            if (periodoAtual == SEMANA) {
                rankingArtistas = PlaybackStatistics.getWeeklyTopArtists(history);
    
            } else if (periodoAtual == MES) {
                rankingArtistas = PlaybackStatistics.getMonthlyTopArtists(history);
    
            } else if (periodoAtual == DIA) {
                rankingArtistas = PlaybackStatistics.getDailyTopArtists(
                        history,
                        dataSelecionada.toString()
                );
    
            } else {
                rankingArtistas = PlaybackStatistics.getYearlyTopArtists(history);
            }
    
            String artistaCompleto = primeiroItem(rankingArtistas);
    
            String artista = artistaCompleto
                    .replaceFirst("^\\d+\\.\\s*", "");
    
            if (artista.contains(" — ")) {
                artista = artista.substring(0, artista.indexOf(" — ")).trim();
            }
    
            ((TextView) arte.findViewById(R.id.shareArtist))
                    .setText(artista.toUpperCase());
    
            // Foto do artista
            ImageView imagemArtista = arte.findViewById(R.id.shareArtistImage);
    
            if (weeklyArtistImage.getDrawable() != null) {
                imagemArtista.setImageDrawable(
                        weeklyArtistImage.getDrawable()
                );
            }
    
            // Tempo do artista favorito
            String tempoArtista = "—";
    
            if (artistaCompleto.contains(" — ")) {
                String[] partes = artistaCompleto.split(" — ", 2);
                tempoArtista = partes[partes.length - 1].trim();
            }
    
            ((TextView) arte.findViewById(R.id.shareArtistTime))
                    .setText(tempoArtista.toUpperCase());
    
            LinearLayout outrosArtistas =
                    arte.findViewById(R.id.shareOtherArtists);
    
            mostrarOutrosArtistasNaImagem(outrosArtistas, rankingArtistas);
    
    
    
            // Melhor dia
            TextView melhorDia = findViewById(R.id.bestDayName);
            TextView tempoMelhorDia = findViewById(R.id.bestDayTime);
    
            ((TextView) arte.findViewById(R.id.shareBestDay))
                    .setText(melhorDia.getText().toString().toUpperCase());
    
            ((TextView) arte.findViewById(R.id.shareBestDayTime))
                    .setText(tempoMelhorDia.getText().toString().toUpperCase());
    
            // Ranking de músicas
            LinearLayout shareTopTracks =
                    arte.findViewById(R.id.shareTopTracks);
    
            mostrarRankingNaImagem(shareTopTracks);
            carregarConquistasNaImagem(arte);
    
    // Aplica as cores dinâmicas do recap
            View shareCard = arte.findViewById(R.id.shareCard);
            shareCard.setBackgroundColor(corFundo);
    
            View timeCard = arte.findViewById(R.id.shareTimeCard);
            timeCard.setBackgroundColor(corDestaque);
    
            // Renderização
            int largura = dp(360);
    
            int larguraSpec = View.MeasureSpec.makeMeasureSpec(
                    largura, View.MeasureSpec.EXACTLY
            );
    
            int alturaSpec = View.MeasureSpec.makeMeasureSpec(
                    0, View.MeasureSpec.UNSPECIFIED
            );
    
            arte.measure(larguraSpec, alturaSpec);
    
            arte.layout(
                    0, 0,
                    arte.getMeasuredWidth(),
                    arte.getMeasuredHeight()
            );
    
            Bitmap bitmap = Bitmap.createBitmap(
                    arte.getMeasuredWidth(),
                    arte.getMeasuredHeight(),
                    Bitmap.Config.ARGB_8888
            );
    
            Canvas canvas = new Canvas(bitmap);
            arte.draw(canvas);
    
            return bitmap;
        }
    
        private void salvarRecapComoImagem() {
    
            if (android.os.Build.VERSION.SDK_INT < 29) {
                Toast.makeText(this,
                        "A exportação precisa do Android 10 ou superior.",
                        Toast.LENGTH_LONG).show();
                return;
            }
    
            Bitmap bitmap = criarImagemRecap();
    
            String nomeArquivo = "FreeAudioTime_Recap_"
                    + System.currentTimeMillis() + ".png";
    
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, nomeArquivo);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/FreeAudioTime");
    
            Uri uri = null;
    
            try {
                uri = getContentResolver().insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        values);
    
                if (uri == null) {
                    throw new IOException("Não foi possível criar o arquivo.");
                }
    
                try (OutputStream outputStream =
                             getContentResolver().openOutputStream(uri)) {
    
                    if (outputStream == null ||
                            !bitmap.compress(Bitmap.CompressFormat.PNG,
                                    100, outputStream)) {
                        throw new IOException("Falha ao gerar a imagem.");
                    }
                }
    
                Toast.makeText(this,
                        "Recap salvo na galeria!",
                        Toast.LENGTH_LONG).show();
    
            } catch (IOException | RuntimeException e) {
    
                if (uri != null) {
                    getContentResolver().delete(uri, null, null);
                }
    
                Toast.makeText(this,
                        "Erro ao salvar o recap.",
                        Toast.LENGTH_LONG).show();
    
            } finally {
                bitmap.recycle();
            }
        }
    
    
        private void mostrarRankingMusicas(String ranking, JSONObject history) {
    
            Log.e("AudioTime", "mostrarRankingMusicas foi chamado!");
    
            topTracksView.removeAllViews();
    
            if (ranking == null || ranking.equals("Nenhuma música")) {
                TextView vazio = new TextView(this);
                vazio.setText("Nenhuma música registrada");
                vazio.setTextSize(14);
                topTracksView.addView(vazio);
                return;
            }
    
            String[] linhas = ranking.split("\n");
    
            JSONArray musicas = history.optJSONArray("tracks");
            JSONArray sessoes = history.optJSONArray("sessions");
    
            for (String linha : linhas) {
    
                if (linha.trim().isEmpty()) continue;
    
                String[] partes = linha.split(" — ", 3);
    
                if (partes.length < 3) continue;
    
                String titulo = partes[0].replaceFirst("^\\d+\\.\\s*", "").trim();
                String artista = partes[1].trim();
                String duracao = partes[2].trim();
    
                // Procura a capa correspondente no histórico.
                String caminhoCapa = "";
    
                if (musicas != null) {
                    for (int i = 0; i < musicas.length(); i++) {
    
                        JSONObject musica = musicas.optJSONObject(i);
    
                        if (musica == null) continue;
    
                        if (titulo.equals(musica.optString("title"))
                                && artista.equals(musica.optString("artist"))) {
    
                            caminhoCapa = musica.optString("artwork", "");
                            break;
                        }
                    }
                }
    
                Log.e("AudioTime", "Música: " + titulo
                        + " | Artista: " + artista
                        + " | Capa: " + caminhoCapa);
    
                // Cria a linha visual.
                // Se não encontrou a capa em tracks, procura nas sessões.
                if (caminhoCapa.isEmpty() && sessoes != null) {
                    for (int i = 0; i < sessoes.length(); i++) {
    
                        JSONObject sessao = sessoes.optJSONObject(i);
    
                        if (sessao == null) continue;
    
                        if (titulo.equals(sessao.optString("title"))
                                && artista.equals(sessao.optString("artist"))) {
    
                            caminhoCapa = sessao.optString("artwork", "");
    
                            if (!caminhoCapa.isEmpty()) break;
                        }
                    }
                }
                LinearLayout linhaMusica = new LinearLayout(this);
                linhaMusica.setOrientation(LinearLayout.HORIZONTAL);
                linhaMusica.setGravity(Gravity.CENTER_VERTICAL);
    
                LinearLayout.LayoutParams linhaParams =
                        new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );
    
                linhaParams.bottomMargin = (int) (14 * getResources()
                        .getDisplayMetrics().density);
    
                linhaMusica.setLayoutParams(linhaParams);
    
                // Capa do álbum.
                // Capa do álbum.
                ImageView capa = new ImageView(this);
    
                int tamanho = (int) (64 * getResources()
                        .getDisplayMetrics().density);
    
                LinearLayout.LayoutParams capaParams =
                        new LinearLayout.LayoutParams(tamanho, tamanho);
    
                capaParams.rightMargin = (int) (12 * getResources()
                        .getDisplayMetrics().density);
    
                capa.setLayoutParams(capaParams);
                capa.setScaleType(ImageView.ScaleType.CENTER_CROP);
    
    // Imagem provisória.
                capa.setImageResource(R.drawable.ic_launcher_foreground);
    
    // Verifica se já existe uma capa salva.
                File arquivoCapa = caminhoCapa.isEmpty()
                        ? null
                        : new File(getFilesDir(), caminhoCapa);
    
                if (arquivoCapa != null && arquivoCapa.exists()) {
    
                    Bitmap bitmap = BitmapFactory.decodeFile(
                            arquivoCapa.getAbsolutePath()
                    );
    
                    if (bitmap != null) {
                        capa.setImageBitmap(bitmap);
                    }
    
                } else {
    
                    // Busca automaticamente a capa que está faltando.
                    DeezerService.buscarCapaMusica(
                            this,
                            titulo,
                            artista,
                            (bitmap, artworkPath) -> {
    
                                if (bitmap != null) {
                                    capa.setImageBitmap(bitmap);
    
                                    salvarCapaNoHistorico(titulo, artista, artworkPath);
    
                                    Log.d("AudioTime",
                                            "Capa carregada e registrada: " + titulo);
                                } else {
                                    Log.d("AudioTime",
                                            "Nenhuma capa encontrada: " + titulo);
                                }
                            }
                    );
                }
    
                linhaMusica.addView(capa);
    
                // Informações da música.
                LinearLayout informacoes = new LinearLayout(this);
                informacoes.setOrientation(LinearLayout.VERTICAL);
    
                informacoes.setLayoutParams(
                        new LinearLayout.LayoutParams(
                                0,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                1
                        )
                );
    
                TextView nome = new TextView(this);
                nome.setText(titulo);
                nome.setTextColor(getColor(R.color.text_primary));
                nome.setTextSize(14);
                nome.setTypeface(null, android.graphics.Typeface.BOLD);
    
                TextView nomeArtista = new TextView(this);
                nomeArtista.setText(artista);
                nomeArtista.setTextColor(getColor(R.color.text_secondary));
                nomeArtista.setTextSize(12);
    
                TextView tempo = new TextView(this);
                tempo.setText(duracao);
                tempo.setTextColor(getColor(R.color.text_secondary));
                tempo.setTextSize(12);
    
                informacoes.addView(nome);
                informacoes.addView(nomeArtista);
                informacoes.addView(tempo);
    
                linhaMusica.addView(informacoes);
                topTracksView.addView(linhaMusica);
            }
        }
    
        private void salvarCapaNoHistorico(
                String titulo,
                String artista,
                String artworkPath
        ) {
            if (artworkPath == null || artworkPath.isEmpty()) return;
    
            try {
                JSONArray musicas = history.optJSONArray("tracks");
    
                if (musicas == null) {
                    musicas = new JSONArray();
                    history.put("tracks", musicas);
                }
    
                boolean encontrada = false;
    
                for (int i = 0; i < musicas.length(); i++) {
                    JSONObject musica = musicas.optJSONObject(i);
    
                    if (musica == null) continue;
    
                    if (titulo.equals(musica.optString("title"))
                            && artista.equals(musica.optString("artist"))) {
    
                        musica.put("artwork", artworkPath);
                        encontrada = true;
                        break;
                    }
                }
    
                if (!encontrada) {
                    JSONObject novaMusica = new JSONObject();
                    novaMusica.put("title", titulo);
                    novaMusica.put("artist", artista);
                    novaMusica.put("duration", 0);
                    novaMusica.put("artwork", artworkPath);
                    musicas.put(novaMusica);
                }
    
                JSONArray sessoes = history.optJSONArray("sessions");
    
                if (sessoes != null) {
                    for (int i = 0; i < sessoes.length(); i++) {
                        JSONObject sessao = sessoes.optJSONObject(i);
    
                        if (sessao == null) continue;
    
                        if (titulo.equals(sessao.optString("title"))
                                && artista.equals(sessao.optString("artist"))) {
    
                            sessao.put("artwork", artworkPath);
                        }
                    }
                }
    
                PlaybackStorage.save(this, history);
    
                Log.d("AudioTime",
                        "Caminho da capa salvo no histórico: " + titulo);
    
            } catch (Exception e) {
                Log.e("AudioTime",
                        "Erro ao registrar capa no histórico", e);
            }
        }
    
        private void mostrarRankingNaImagem(LinearLayout container) {
            container.removeAllViews();
    
            String[] linhas = rankingTracksAtual.split("\n");
    
            for (String linha : linhas) {
                if (linha.trim().isEmpty()) continue;
    
                String[] partes = linha.split(" — ", 3);
                if (partes.length < 2) continue;
    
                String titulo = partes[0].replaceFirst("^\\d+\\.\\s*", "").trim();
                String artista = partes[1].trim();
    
                String caminhoCapa = "";
    
                JSONArray musicas = history.optJSONArray("tracks");
    
                if (musicas != null) {
                    for (int i = 0; i < musicas.length(); i++) {
                        JSONObject musica = musicas.optJSONObject(i);
    
                        if (musica == null) continue;
    
                        if (titulo.equals(musica.optString("title"))
                                && artista.equals(musica.optString("artist"))) {
                            caminhoCapa = musica.optString("artwork");
                            break;
                        }
                    }
                }
    
                LinearLayout linhaMusica = new LinearLayout(this);
                linhaMusica.setOrientation(LinearLayout.HORIZONTAL);
                linhaMusica.setGravity(Gravity.CENTER_VERTICAL);
    
                LinearLayout.LayoutParams linhaParams =
                        new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                        );
    
                linhaParams.bottomMargin = dp(10);
                linhaMusica.setLayoutParams(linhaParams);
    
                ImageView capa = new ImageView(this);
                capa.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));
                capa.setScaleType(ImageView.ScaleType.CENTER_CROP);
    
                if (!caminhoCapa.isEmpty()) {
                    File arquivo = new File(getFilesDir(), caminhoCapa);
    
                    if (arquivo.exists()) {
                        Bitmap bitmap = BitmapFactory.decodeFile(arquivo.getAbsolutePath());
    
                        if (bitmap != null) {
                            capa.setImageBitmap(bitmap);
                        }
                    }
                }
    
                linhaMusica.addView(capa);
    
                TextView informacoes = new TextView(this);
                informacoes.setText(titulo + "\n" + artista);
                informacoes.setTextColor(Color.WHITE);
                informacoes.setTextSize(12);
                informacoes.setMaxLines(2);
                informacoes.setEllipsize(TextUtils.TruncateAt.END);
    
                LinearLayout.LayoutParams textoParams =
                        new LinearLayout.LayoutParams(
                                0,
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                1
                        );
    
                textoParams.leftMargin = dp(12);
                informacoes.setLayoutParams(textoParams);
    
                linhaMusica.addView(informacoes);
                container.addView(linhaMusica);
            }
        }
    
        private int dp(float valor) {
            return (int) (valor * getResources().getDisplayMetrics().density + 0.5f);
        }
    
        private void extrairCores(Bitmap bitmap) {
            if (bitmap == null) {
                paletaPronta = true;
                verificarExportacaoPendente();
                return;
            }
    
            paletaPronta = false;
    
            Palette.from(bitmap).generate(palette -> {
                corFundo = palette.getDarkMutedColor(
                        Color.rgb(16, 29, 23)
                );
    
                corDestaque = palette.getVibrantColor(
                        palette.getDarkVibrantColor(
                                Color.rgb(36, 69, 54)
                        )
                );
    
                corTexto = Color.WHITE;
    
                paletaPronta = true;
                verificarExportacaoPendente();
            });
        }
    
        private void verificarExportacaoPendente() {
            if (exportacaoPendente && paletaPronta) {
                exportacaoPendente = false;
                salvarRecapComoImagem();
            }
        }
    
        private long obterTotalPeriodo() {
    
            if (periodoAtual == SEMANA) {
                return PlaybackStatistics.getWeeklyTotal(history);
            }
    
            if (periodoAtual == MES) {
                return PlaybackStatistics.getMonthlyTotal(history);
            }
    
            if (periodoAtual == DIA) {
                return PlaybackStatistics.getDailyTotal(
                        history,
                        dataSelecionada.toString()
                );
            }
    
            return PlaybackStatistics.getYearlyTotal(history);
            }
    
        private int[] contarArtistasEMusicas() {
    
            JSONArray sessoes = history.optJSONArray("sessions");
    
            java.util.Set<String> artistas = new java.util.HashSet<>();
            java.util.Set<String> musicas = new java.util.HashSet<>();
    
            if (sessoes == null) {
                return new int[]{0, 0};
            }
    
            LocalDate hoje = LocalDate.now();
    
            LocalDate inicio;
    
            if (periodoAtual == SEMANA) {
                inicio = hoje.with(DayOfWeek.MONDAY);
            } else if (periodoAtual == MES) {
                inicio = hoje.withDayOfMonth(1);
            } else {
                inicio = hoje.withDayOfYear(1);
            }
    
            for (int i = 0; i < sessoes.length(); i++) {
    
                JSONObject sessao = sessoes.optJSONObject(i);
    
                if (sessao == null) continue;
    
                String dataTexto = sessao.optString("date", "");
    
                try {
                    LocalDate data = LocalDate.parse(dataTexto);
    
                    if (data.isBefore(inicio) || data.isAfter(hoje)) {
                        continue;
                    }
    
                    String artista = sessao.optString("artist", "").trim();
                    String titulo = sessao.optString("title", "").trim();
    
                    if (!artista.isEmpty()) {
                        artistas.add(artista);
                    }
    
                    if (!titulo.isEmpty() && !artista.isEmpty()) {
                        musicas.add(titulo + "|" + artista);
                    }
    
                } catch (Exception e) {
                    Log.e("AudioTime", "Erro ao contar sessão", e);
                }
            }
    
            return new int[]{
                    artistas.size(),
                    musicas.size()
            };
        }
    
        private void aplicarPaleta(View view) {
    
            // Cores originais do XML
            int verdeOriginal = Color.rgb(183, 255, 0);   // #B7FF00
            int pretoOriginal = Color.rgb(8, 10, 8);      // #080A08
            int painelOriginal = Color.rgb(17, 21, 17);   // #111511
            int cremeOriginal = Color.rgb(240, 235, 221); // #F0EBDD
    
            // Substitui as cores de fundo
            if (view.getBackground() instanceof ColorDrawable) {
    
                ColorDrawable fundo = (ColorDrawable) view.getBackground();
                int corAtual = fundo.getColor();
    
                if (corAtual == verdeOriginal) {
                    view.setBackgroundColor(corDestaque);
    
                } else if (corAtual == pretoOriginal) {
                    view.setBackgroundColor(corFundo);
    
                } else if (corAtual == painelOriginal) {
                    view.setBackgroundColor(escurecer(corFundo, 0.75f));
                }
            }
    
            // Substitui as cores dos textos
            if (view instanceof TextView) {
    
                TextView texto = (TextView) view;
                int corTextoAtual = texto.getCurrentTextColor();
    
                if (corTextoAtual == verdeOriginal) {
                    texto.setTextColor(corDestaque);
    
                } else if (corTextoAtual == pretoOriginal) {
                    texto.setTextColor(corFundo);
    
                } else if (corTextoAtual == cremeOriginal) {
                    texto.setTextColor(corTexto);
                }
            }
    
            // Percorre os elementos internos do layout
            if (view instanceof ViewGroup) {
    
                ViewGroup grupo = (ViewGroup) view;
    
                for (int i = 0; i < grupo.getChildCount(); i++) {
                    aplicarPaleta(grupo.getChildAt(i));
                }
            }
        }
    
        private int escurecer(int cor, float fator) {
    
            return Color.rgb(
                    (int) (Color.red(cor) * fator),
                    (int) (Color.green(cor) * fator),
                    (int) (Color.blue(cor) * fator)
            );
        }
    
        private void mostrarOutrosArtistasNaImagem(
                LinearLayout container,
                String ranking) {
    
            container.removeAllViews();
    
            TextView titulo = new TextView(this);
            titulo.setText("ALSO IN ROTATION");
            titulo.setTextColor(corDestaque);
            titulo.setTextSize(11);
            titulo.setTypeface(null, Typeface.BOLD);
            titulo.setLetterSpacing(0.08f);
    
            container.addView(titulo);
    
            String[] linhas = ranking.split("\n");
    
            for (int i = 1; i < Math.min(linhas.length, 3); i++) {
    
                String conteudo = linhas[i].trim();
    
                if (conteudo.isEmpty()) continue;
    
                conteudo = conteudo.replaceFirst("^\\d+\\.\\s*", "");
    
                String artista = conteudo;
                String tempo = "";
    
                if (conteudo.contains(" — ")) {
                    String[] partes = conteudo.split(" — ", 2);
                    artista = partes[0].trim();
                    tempo = partes[1].trim();
                }
    
                LinearLayout linha = new LinearLayout(this);
                linha.setOrientation(LinearLayout.HORIZONTAL);
                linha.setGravity(Gravity.CENTER_VERTICAL);
                linha.setPadding(0, dp(3), 0, 0);
    
                TextView posicao = new TextView(this);
                posicao.setText(String.format("%02d", i + 1));
                posicao.setTextColor(corDestaque);
                posicao.setTextSize(15);
                posicao.setTypeface(null, Typeface.BOLD);
    
                LinearLayout textos = new LinearLayout(this);
                textos.setOrientation(LinearLayout.VERTICAL);
                textos.setPadding(dp(12), 0, 0, 0);
    
                TextView nome = new TextView(this);
                nome.setText(artista.toUpperCase());
                nome.setTextColor(corTexto);
                nome.setTextSize(11);
                nome.setTypeface(null, Typeface.BOLD);
    
                TextView duracao = new TextView(this);
                duracao.setText(tempo.toUpperCase());
                duracao.setTextColor(corTexto);
                duracao.setTextSize(9);
    
                textos.addView(nome);
                textos.addView(duracao);
    
                linha.addView(posicao);
                linha.addView(textos);
    
                container.addView(linha);
            }
        }

        private void mostrarConquistasNaImagem(
                LinearLayout container,
                LocalDate inicio,
                LocalDate fim
        ) {
            container.removeAllViews();

            JSONObject conquistas = AchievementStorage.load(this);

            String[] ids = {
                    "first_10_hours",
                    "hours_50",
                    "hours_100",
                    "hours_500",
                    "hours_1000",
                    "streak_3",
                    "streak_7",
                    "streak_30",
                    "streak_100",
                    "artists_5",
                    "artists_10",
                    "artists_25",
                    "tracks_10",
                    "tracks_50",
                    "tracks_100",
                    "listening_days_7",
                    "listening_days_30",
                    "listening_days_100",
                    "daily_goal_7_days",
                    "monthly_goal"
            };

            String[] nomes = {
                    "Primeiras 10 horas",
                    "50 horas de música",
                    "100 horas de música",
                    "500 horas de música",
                    "1.000 horas de música",
                    "Sequência de 3 dias",
                    "Sequência de 7 dias",
                    "Sequência de 30 dias",
                    "Sequência de 100 dias",
                    "5 artistas descobertos",
                    "10 artistas descobertos",
                    "25 artistas descobertos",
                    "10 músicas diferentes",
                    "50 músicas diferentes",
                    "100 músicas diferentes",
                    "7 dias de reprodução",
                    "30 dias de reprodução",
                    "100 dias de reprodução",
                    "Meta diária por 7 dias",
                    "Meta mensal alcançada"
            };

            int quantidade = 0;

            for (int i = 0; i < ids.length; i++) {

                if (!conquistas.has(ids[i])) continue;

                long timestamp = conquistas.optLong(ids[i], 0);

                if (timestamp <= 0) continue;

                LocalDate dataDesbloqueio = Instant
                        .ofEpochMilli(timestamp)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();

                if (dataDesbloqueio.isBefore(inicio)
                        || dataDesbloqueio.isAfter(fim)) {
                    continue;
                }

                LinearLayout linha = new LinearLayout(this);
                linha.setOrientation(LinearLayout.HORIZONTAL);
                linha.setGravity(Gravity.CENTER_VERTICAL);
                linha.setPadding(0, dp(5), 0, dp(5));

                TextView icone = new TextView(this);
                icone.setText("🏆");
                icone.setTextSize(20);

                LinearLayout informacoes = new LinearLayout(this);
                informacoes.setOrientation(LinearLayout.VERTICAL);
                informacoes.setPadding(dp(10), 0, 0, 0);

                TextView nome = new TextView(this);
                nome.setText(nomes[i]);
                nome.setTextColor(corTexto);
                nome.setTextSize(12);
                nome.setTypeface(null, Typeface.BOLD);

                TextView data = new TextView(this);
                data.setText(
                        dataDesbloqueio.format(
                                DateTimeFormatter.ofPattern("dd/MM/yyyy")
                        )
                );
                data.setTextColor(corDestaque);
                data.setTextSize(10);

                informacoes.addView(nome);
                informacoes.addView(data);

                linha.addView(icone);
                linha.addView(informacoes);

                container.addView(linha);

                quantidade++;
            }

            if (quantidade == 0) {
                TextView vazio = new TextView(this);
                vazio.setText("Nenhuma conquista desbloqueada neste período.");
                vazio.setTextColor(corTexto);
                vazio.setTextSize(11);

                container.addView(vazio);
            }
        }

        private void carregarConquistasNaImagem(View arte) {

            LinearLayout secao = arte.findViewById(
                    R.id.shareAchievementsSection
            );

            LinearLayout container = arte.findViewById(
                    R.id.shareAchievements
            );

            LocalDate hoje = LocalDate.now();

            LocalDate inicio;
            LocalDate fim = hoje;

            if (periodoAtual == DIA) {

                inicio = dataSelecionada;
                fim = dataSelecionada;

            } else if (periodoAtual == SEMANA) {

                inicio = hoje.with(DayOfWeek.MONDAY);

            } else if (periodoAtual == MES) {

                inicio = hoje.withDayOfMonth(1);

            } else {

                inicio = hoje.withDayOfYear(1);
            }

            mostrarConquistasNaImagem(container, inicio, fim);

            secao.setVisibility(View.VISIBLE);
        }
    
        }
    

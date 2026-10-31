
package com.example.freeaudiotime;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.Intent;
import android.widget.Button;


import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applySavedTheme(this);
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

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
    }

    private void carregarEstatisticas() {

        JSONObject history = PlaybackStorage.load(this);

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
}

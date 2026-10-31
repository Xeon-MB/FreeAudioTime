package com.example.freeaudiotime;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        org.json.JSONObject history = PlaybackStorage.load(this);

        java.util.Map<String, Long> daily =
                PlaybackStatistics.getDailyTotals(history);

        for (String day : daily.keySet()) {
            android.util.Log.d("AudioTime",
                    "Dia: " + day + " | Tempo: " +
                            daily.get(day) + " segundos");
        }
        long weeklyTotal = PlaybackStatistics.getWeeklyTotal(history);

        android.util.Log.d("AudioTime",
                "Tempo ouvido na semana: " + weeklyTotal + " segundos");
        long monthlyTotal = PlaybackStatistics.getMonthlyTotal(history);
        long yearlyTotal = PlaybackStatistics.getYearlyTotal(history);

        android.util.Log.d("AudioTime",
                "Tempo ouvido no mês: " + monthlyTotal + " segundos");

        android.util.Log.d("AudioTime",
                "Tempo ouvido no ano: " + yearlyTotal + " segundos");
        String mostPlayedArtist =
                PlaybackStatistics.getMostPlayedArtist(history);

        android.util.Log.d("AudioTime",
                "Artista mais ouvido: " + mostPlayedArtist);
        String mostPlayedTrack =
                PlaybackStatistics.getMostPlayedTrack(history);

        android.util.Log.d("AudioTime",
                "Música mais ouvida: " + mostPlayedTrack);
        String topArtists = PlaybackStatistics.getTopArtists(history);

        android.util.Log.d("AudioTime",
                "TOP 5 ARTISTAS:\n" + topArtists);
        String topTracks = PlaybackStatistics.getTopTracks(history);

        android.util.Log.d("AudioTime",
                "TOP 5 MÚSICAS:\n" + topTracks);
        String weeklyTopArtists =
                PlaybackStatistics.getWeeklyTopArtists(history);

        android.util.Log.d("AudioTime",
                "TOP 5 ARTISTAS DA SEMANA:\n" + weeklyTopArtists);
        String weeklyTopTracks =
                PlaybackStatistics.getWeeklyTopTracks(history);

        android.util.Log.d("AudioTime",
                "TOP 5 MÚSICAS DA SEMANA:\n" + weeklyTopTracks);
        String monthlyTopArtists =
                PlaybackStatistics.getMonthlyTopArtists(history);

        android.util.Log.d("AudioTime",
                "TOP 5 ARTISTAS DO MÊS:\n" + monthlyTopArtists);


        String monthlyTopTracks =
                PlaybackStatistics.getMonthlyTopTracks(history);

        android.util.Log.d("AudioTime",
                "TOP 5 MÚSICAS DO MÊS:\n" + monthlyTopTracks);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
package com.example.reactionchallenge;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.reactionchallenge.db.AppDbHelper;
import com.example.reactionchallenge.model.Difficulty;
import com.example.reactionchallenge.model.GameMode;
import com.example.reactionchallenge.model.GameRecord;
import com.example.reactionchallenge.model.PlayerStats;
import com.example.reactionchallenge.util.MusicManager;
import com.example.reactionchallenge.util.Prefs;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RankingsActivity extends AppCompatActivity {

    private MaterialButtonToggleGroup toggleGroupSection;
    private MaterialButton btnTabRankings;
    private MaterialButton btnTabStats;
    private MaterialButtonToggleGroup toggleGroupViewType;
    private MaterialButton btnGeneral;
    private MaterialButton btnPersonal;

    private Spinner spinnerMode;
    private Spinner spinnerDifficulty;
    private ListView listView;
    private TextView tvEmpty;
    private TextView tvCurrentPlayer;
    private TextView tvStatsSummary;

    private android.view.View rankingContainer;
    private android.view.View statsContainer;

    private MaterialButton btnNavSettings;
    private MaterialButton btnNavGame;
    private MaterialButton btnNavRankings;

    private AppDbHelper dbHelper;
    private String currentPlayerName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (TextUtils.isEmpty(Prefs.getPlayerName(this))) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_rankings);

        currentPlayerName = Prefs.getPlayerName(this);
        dbHelper = new AppDbHelper(this);

        toggleGroupSection = findViewById(R.id.toggleGroupSection);
        btnTabRankings = findViewById(R.id.btnTabRankings);
        btnTabStats = findViewById(R.id.btnTabStats);
        toggleGroupViewType = findViewById(R.id.toggleGroupViewType);
        btnGeneral = findViewById(R.id.btnGeneralRanking);
        btnPersonal = findViewById(R.id.btnPersonalRanking);
        spinnerMode = findViewById(R.id.spinnerModeFilter);
        spinnerDifficulty = findViewById(R.id.spinnerDifficultyFilter);
        listView = findViewById(R.id.listViewResults);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvCurrentPlayer = findViewById(R.id.tvCurrentPlayer);
        tvStatsSummary = findViewById(R.id.tvStatsSummary);
        rankingContainer = findViewById(R.id.rankingContainer);
        statsContainer = findViewById(R.id.statsContainer);

        btnNavSettings = findViewById(R.id.btnNavSettings);
        btnNavGame = findViewById(R.id.btnNavGame);
        btnNavRankings = findViewById(R.id.btnNavRankings);

        setupSpinners();
        setupToggle();
        setupBottomNav();

        findViewById(R.id.btnReload).setOnClickListener(v -> loadData());
        tvCurrentPlayer.setText("Jugador actual: " + currentPlayerName);

        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MusicManager.sync(this, Prefs.loadConfig(this).menuMusicEnabled);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MusicManager.stop();
    }


    private ArrayAdapter<String> buildColoredSpinnerAdapter(String[] items, int color) {
        return new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView v = (TextView) super.getView(position, convertView, parent);
                v.setTextColor(color);
                return v;
            }
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView v = (TextView) super.getDropDownView(position, convertView, parent);
                v.setTextColor(color);
                return v;
            }
        };
    }



    private void setupSpinners() {
        int accent = ContextCompat.getColor(this, R.color.accent);


        ArrayAdapter<String> modeAdapter = buildColoredSpinnerAdapter(new String[]{
                "Todos",
                GameMode.NORMAL.getLabel(),
                GameMode.INVERSE.getLabel()
        }, accent);
        spinnerMode.setAdapter(modeAdapter);
        spinnerMode.setBackgroundTintList(ColorStateList.valueOf(accent));

        ArrayAdapter<String> diffAdapter = buildColoredSpinnerAdapter(new String[]{
                "Todas",
                Difficulty.EASY.getLabel(),
                Difficulty.MEDIUM.getLabel(),
                Difficulty.HARD.getLabel(),
                Difficulty.DYNAMIC.getLabel()
        }, accent);
        spinnerDifficulty.setAdapter(diffAdapter);
        spinnerDifficulty.setBackgroundTintList(ColorStateList.valueOf(accent));
    }

    private void setupToggle() {
        toggleGroupSection.check(btnTabRankings.getId());
        toggleGroupViewType.check(btnGeneral.getId());

        toggleGroupSection.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                updateVisibleSection();
                loadData();
            }
        });

        toggleGroupViewType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked && isRankingsVisible()) {
                loadData();
            }
        });

        updateVisibleSection();
    }

    private void setupBottomNav() {
        highlightNav(btnNavRankings, true);
        btnNavGame.setOnClickListener(v -> startActivity(new Intent(this, MainMenuActivity.class)));
        btnNavSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        btnNavRankings.setOnClickListener(v -> Toast.makeText(this, "Ya estás en rankings.", Toast.LENGTH_SHORT).show());
    }

    private boolean isRankingsVisible() {
        return toggleGroupSection.getCheckedButtonId() == btnTabRankings.getId();
    }

    private void updateVisibleSection() {
        boolean showRankings = isRankingsVisible();
        rankingContainer.setVisibility(showRankings ? android.view.View.VISIBLE : android.view.View.GONE);
        statsContainer.setVisibility(showRankings ? android.view.View.GONE : android.view.View.VISIBLE);
    }

    private void loadData() {
        if (isRankingsVisible()) {
            loadRankings();
        } else {
            loadStats();
        }
    }

    private void loadRankings() {
        boolean generalView = toggleGroupViewType.getCheckedButtonId() == btnGeneral.getId();

        String modeFilter = selectedSpinnerText(spinnerMode);
        String difficultyFilter = selectedSpinnerText(spinnerDifficulty);

        List<GameRecord> all = dbHelper.getAllRecords();
        List<GameRecord> filtered = new ArrayList<>();
        for (GameRecord r : all) {
            if (r.training) continue;
            if (!matchesMode(r, modeFilter)) continue;
            if (!matchesDifficulty(r, difficultyFilter)) continue;
            filtered.add(r);
        }

        List<GameRecord> display = generalView ? bestPerPlayer(filtered) : topFiveForCurrentPlayer(filtered);

        List<String> lines = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        int index = 1;
        for (GameRecord r : display) {
            StringBuilder sb = new StringBuilder();
            sb.append(index++).append(") ").append(r.playerName).append(" · ").append(r.score).append(" pts");
            sb.append(r.completed ? " · Ganada" : " · Perdida");
            sb.append("\n");
            sb.append("Modo: ").append(r.mode).append(" | Dif.: ").append(r.difficulty).append("\n");
            sb.append("Aciertos: ").append(r.correctCount).append("  Errores: ").append(r.wrongCount)
                    .append("  Omisiones: ").append(r.missedCount).append("\n");
            sb.append("Prom. reacción: ").append(r.avgReactionMs).append(" ms");
            sb.append(" | Mejor: ").append(r.bestReactionMs).append(" ms");
            sb.append(" | Peor: ").append(r.worstReactionMs).append(" ms");
            sb.append(" | Tiempo máx.: ").append(formatMaxReaction(r.maxReactionMs)).append("\n");
            sb.append("Iteraciones: ").append(r.iterationsPerLevel);
            sb.append(" | Etapas: ").append(r.levelsCompleted).append("\n");
            sb.append("Fecha: ").append(sdf.format(new Date(r.createdAt)));
            if (r.ruleSummary != null && !r.ruleSummary.trim().isEmpty()) {
                sb.append("\nRegla: ").append(r.ruleSummary);
            }
            lines.add(sb.toString());
        }

        if (lines.isEmpty()) {
            tvEmpty.setText(generalView
                    ? "No hay resultados para mostrar con esos filtros."
                    : "Todavía no existen partidas del jugador actual con esos filtros.");
            tvEmpty.setVisibility(android.view.View.VISIBLE);
        } else {
            tvEmpty.setVisibility(android.view.View.GONE);
        }

        final int white = ContextCompat.getColor(this, R.color.text_primary);
        listView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, lines) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView v = (TextView) super.getView(position, convertView, parent);
                v.setTextColor(white);
                return v;
            }
        });
    }

    private void loadStats() {
        PlayerStats stats = dbHelper.getPlayerStats(currentPlayerName);
        tvStatsSummary.setText(buildStatsSummary(stats));
    }

    private String buildStatsSummary(PlayerStats stats) {
        if (stats == null || stats.gamesPlayed <= 0) {
            return "Todavía no hay partidas guardadas para este jugador.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Partidas jugadas: ").append(stats.gamesPlayed).append('\n');
        sb.append("Ganadas: ").append(stats.gamesWon).append("  Perdidas: ").append(stats.gamesLost).append('\n');
        sb.append("Win rate: ").append(stats.getWinRate()).append("%\n");
        sb.append("Puntaje total: ").append(stats.totalScore).append("\n");
        sb.append("Puntaje promedio: ").append(stats.getAverageScore()).append("\n");
        sb.append("Mejor puntaje: ").append(stats.bestScore).append("\n");
        sb.append("Aciertos acumulados: ").append(stats.totalCorrect).append("\n");
        sb.append("Errores acumulados: ").append(stats.totalWrong).append("\n");
        sb.append("Omisiones acumuladas: ").append(stats.totalMissed).append("\n");
        sb.append("Reacción promedio: ").append(stats.getAverageReactionMs()).append(" ms\n");
        sb.append("Mejor reacción: ").append(stats.bestReactionMs).append(" ms\n");
        sb.append("Peor reacción: ").append(stats.worstReactionMs).append(" ms\n");
        sb.append("Estímulos totales: ").append(stats.totalStimuli).append("\n");
        sb.append("Iteraciones totales: ").append(stats.totalIterations).append("\n");
        if (stats.lastPlayedAt > 0) {
            sb.append("Última partida: ").append(
                    new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                            .format(new Date(stats.lastPlayedAt)));
        }
        return sb.toString();
    }

    private String selectedSpinnerText(Spinner spinner) {
        Object value = spinner.getSelectedItem();
        return value == null ? "" : value.toString();
    }

    private String formatMaxReaction(int maxReactionMs) {
        if (maxReactionMs <= 0) return "-";
        if (maxReactionMs % 1000 == 0) return (maxReactionMs / 1000) + " s";
        return String.format(Locale.getDefault(), "%.1f s", maxReactionMs / 1000f);
    }

    private boolean matchesMode(GameRecord r, String filter) {
        if (filter == null || filter.isEmpty() || "Todos".equalsIgnoreCase(filter)) return true;
        return r.mode != null && r.mode.equalsIgnoreCase(filter);
    }

    private boolean matchesDifficulty(GameRecord r, String filter) {
        if (filter == null || filter.isEmpty() || "Todas".equalsIgnoreCase(filter)) return true;
        return r.difficulty != null && r.difficulty.equalsIgnoreCase(filter);
    }

    private List<GameRecord> bestPerPlayer(List<GameRecord> records) {
        Map<String, GameRecord> best = new LinkedHashMap<>();
        for (GameRecord r : records) {
            GameRecord current = best.get(r.playerName);
            if (current == null || recordComparator().compare(r, current) < 0) {
                best.put(r.playerName, r);
            }
        }
        List<GameRecord> out = new ArrayList<>(best.values());
        out.sort(recordComparator());
        if (out.size() > 10) out = new ArrayList<>(out.subList(0, 10));
        return out;
    }

    private List<GameRecord> topFiveForCurrentPlayer(List<GameRecord> records) {
        List<GameRecord> out = new ArrayList<>();
        for (GameRecord r : records) {
            if (currentPlayerName.equalsIgnoreCase(r.playerName)) out.add(r);
        }
        out.sort(recordComparator());
        if (out.size() > 5) out = new ArrayList<>(out.subList(0, 5));
        return out;
    }

    private Comparator<GameRecord> recordComparator() {
        return (a, b) -> {
            if (a.score != b.score) return Integer.compare(b.score, a.score);
            if (a.avgReactionMs != b.avgReactionMs) return Long.compare(a.avgReactionMs, b.avgReactionMs);
            return Long.compare(b.createdAt, a.createdAt);
        };
    }

    private void highlightNav(MaterialButton button, boolean active) {
        if (button == null) return;
        int surface = ContextCompat.getColor(this, R.color.surface);
        int accent = ContextCompat.getColor(this, R.color.accent);
        int textDark = ContextCompat.getColor(this, R.color.background);
        int textLight = ContextCompat.getColor(this, R.color.text_primary);
        if (active) {
            button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(accent));
            button.setTextColor(textDark);
            button.setStrokeWidth(0);
        } else {
            button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(surface));
            button.setTextColor(textLight);
            button.setStrokeWidth(2);
            button.setStrokeColor(android.content.res.ColorStateList.valueOf(accent));
        }
    }
}
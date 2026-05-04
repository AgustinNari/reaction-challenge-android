package com.example.reactionchallenge;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.reactionchallenge.model.Difficulty;
import com.example.reactionchallenge.model.GameConfig;
import com.example.reactionchallenge.model.GameMode;
import com.example.reactionchallenge.util.MusicManager;
import com.example.reactionchallenge.util.Prefs;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MainMenuActivity extends AppCompatActivity {

    private Spinner spinnerMode;
    private Spinner spinnerDifficulty;
    private NumberPicker pickerIterations;
    private NumberPicker pickerMaxTime;
    private CheckBox cbTraining;
    private CheckBox cbTrainingNoFail;
    private CheckBox cbTrainingNoTime;
    private CheckBox cbTrainingHelp;
    private View trainingPanel;
    private TextView tvCurrentPlayer;
    private MaterialButton btnHelpConfig;

    private boolean suppressUiEvents = false;
    private boolean suppressDifficultyAutoTime = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (TextUtils.isEmpty(Prefs.getPlayerName(this))) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main_menu);
        bindViews();
        suppressUiEvents = true;
        setupControls();
        applyAccentColors();
        loadPrefs();
        wireButtons();
        setupBottomNav();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MusicManager.sync(this, Prefs.loadConfig(this).menuMusicEnabled);
    }

    @Override
    protected void onPause() {
        saveCurrentConfig();
        super.onPause();
        MusicManager.stop();
    }

    private void bindViews() {
        spinnerMode = findViewById(R.id.spinnerMode);
        spinnerDifficulty = findViewById(R.id.spinnerDifficulty);
        pickerIterations = findViewById(R.id.pickerIterations);
        pickerMaxTime = findViewById(R.id.pickerMaxTime);
        cbTraining = findViewById(R.id.cbTraining);
        cbTrainingNoFail = findViewById(R.id.cbTrainingNoFail);
        cbTrainingNoTime = findViewById(R.id.cbTrainingNoTime);
        cbTrainingHelp = findViewById(R.id.cbTrainingHelp);
        trainingPanel = findViewById(R.id.trainingPanel);
        tvCurrentPlayer = findViewById(R.id.tvCurrentPlayer);
        btnHelpConfig = findViewById(R.id.btnHelpConfig);
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

    private void applyAccentColors() {
        int accent = ContextCompat.getColor(this, R.color.accent);

        styleSpinner(spinnerMode, accent);
        styleSpinner(spinnerDifficulty, accent);
        styleNumberPicker(pickerIterations, accent);
        styleNumberPicker(pickerMaxTime, accent);
    }

    private void styleSpinner(Spinner spinner, int color) {
        spinner.setBackgroundTintList(ColorStateList.valueOf(color));
    }

    private void styleNumberPicker(NumberPicker picker, int color) {
        picker.setTextColor(color);
        try {
            java.lang.reflect.Field field =
                    NumberPicker.class.getDeclaredField("mSelectionDivider");
            field.setAccessible(true);
            field.set(picker, new ColorDrawable(color));
        } catch (Exception ignored) {}
    }

    private void setupControls() {
        int accent = ContextCompat.getColor(this, R.color.accent);

        spinnerMode.setAdapter(buildColoredSpinnerAdapter(new String[]{
                GameMode.NORMAL.getLabel(),
                GameMode.INVERSE.getLabel()
        }, accent));

        spinnerDifficulty.setAdapter(buildColoredSpinnerAdapter(new String[]{
                Difficulty.EASY.getLabel(),
                Difficulty.MEDIUM.getLabel(),
                Difficulty.HARD.getLabel(),
                Difficulty.DYNAMIC.getLabel()
        }, accent));

        pickerIterations.setMinValue(1);
        pickerIterations.setMaxValue(100);

        pickerMaxTime.setMinValue(1);
        pickerMaxTime.setMaxValue(30);

        spinnerMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (suppressUiEvents) return;
                saveCurrentConfig();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerDifficulty.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (suppressUiEvents) return;

                Difficulty selectedDifficulty = getSelectedDifficulty();
                Difficulty storedDifficulty = Prefs.loadConfig(MainMenuActivity.this).difficulty;

                if (selectedDifficulty == storedDifficulty) {
                    saveCurrentConfig();
                    return;
                }

                suppressDifficultyAutoTime = true;
                pickerMaxTime.setValue(selectedDifficulty.getDefaultMaxSeconds());
                suppressDifficultyAutoTime = false;

                saveCurrentConfig();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        pickerIterations.setOnValueChangedListener((picker, oldVal, newVal) -> {
            if (suppressUiEvents) return;
            saveCurrentConfig();
        });

        pickerMaxTime.setOnValueChangedListener((picker, oldVal, newVal) -> {
            if (suppressUiEvents) return;
            if (suppressDifficultyAutoTime) return;
            saveCurrentConfig();
        });

        cbTraining.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (suppressUiEvents) return;
            trainingPanel.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            applyTrainingStateToUi(isChecked);
            saveCurrentConfig();
        });

        cbTrainingNoFail.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (suppressUiEvents) return;
            saveCurrentConfig();
        });

        cbTrainingNoTime.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (suppressUiEvents) return;
            saveCurrentConfig();
        });

        cbTrainingHelp.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (suppressUiEvents) return;
            saveCurrentConfig();
        });
    }

    private void loadPrefs() {
        suppressUiEvents = true;
        try {
            String playerName = Prefs.getPlayerName(this);
            tvCurrentPlayer.setText("Jugador actual: " + playerName);

            GameConfig config = Prefs.loadConfig(this);

            spinnerMode.setSelection(indexOfMode(config.mode));
            spinnerDifficulty.setSelection(indexOfDifficulty(config.difficulty));

            pickerIterations.setValue(Math.max(1, Math.min(100, config.iterationsPerLevel)));
            pickerMaxTime.setValue(Math.max(1, Math.min(30, config.maxReactionSeconds)));

            cbTraining.setChecked(config.trainingEnabled);
            cbTrainingNoFail.setChecked(config.trainingNoFail);
            cbTrainingNoTime.setChecked(config.trainingNoTimeLimit);
            cbTrainingHelp.setChecked(config.trainingShowHelp);

            trainingPanel.setVisibility(config.trainingEnabled ? View.VISIBLE : View.GONE);
            applyTrainingStateToUi(config.trainingEnabled);
        } finally {
            suppressUiEvents = false;
        }
    }

    private void applyTrainingStateToUi(boolean trainingEnabled) {
        cbTrainingNoFail.setEnabled(trainingEnabled);
        cbTrainingNoTime.setEnabled(trainingEnabled);
        cbTrainingHelp.setEnabled(trainingEnabled);
    }

    private void wireButtons() {
        MaterialButton btnStart = findViewById(R.id.btnStartGame);
        btnStart.setOnClickListener(v -> startGameFromCurrentConfig());

        if (btnHelpConfig != null) {
            btnHelpConfig.setOnClickListener(v -> showHelpDialog());
        }
    }

    private void showHelpDialog() {
        StringBuilder help = new StringBuilder();

        help.append("Cómo jugar:\n\n");

        help.append("• El juego presenta estímulos y una regla en cada etapa.\n");
        help.append("• Cada partida tiene 3 etapas, con dificultad creciente según la configuración.\n\n");

        help.append("Modos:\n");
        help.append("• Normal: respondé Sí o No según la regla.\n");
        help.append("• Reacción Inversa: en algunas rondas debés tocar y en otras dejar pasar el tiempo.\n\n");

        help.append("Dificultad:\n");
        help.append("• Fácil, Media, Difícil o Dinámica (aumenta progresivamente).\n");
        help.append("• Define la base del desafío.\n\n");

        help.append("Configuración:\n");
        help.append("• Iteraciones por etapa: cantidad de estímulos (1 a 100).\n");
        help.append("• Tiempo máximo: límite por estímulo (1 a 30 segundos).\n");
        help.append("• Cada dificultad tiene un tiempo por defecto ajustable.\n\n");

        help.append("Puntaje:\n");
        help.append("• Premia exactitud y rapidez.\n");
        help.append("• Penaliza errores y omisiones.\n");
        help.append("• Influyen vidas restantes, etapas completadas y total de estímulos.\n");
        help.append("• Dificultades altas aplican multiplicadores.\n");
        help.append("• El modo Reacción Inversa y la dificultad Dinámica tienen ajustes extra.\n");
        help.append("• Más tiempo máximo = menor puntaje potencial.\n");
        help.append("• Más iteraciones = pequeño bono.\n\n");

        help.append("Entrenamiento:\n");
        help.append("• No afecta rankings ni estadísticas.\n");
        help.append("• Permite desactivar vidas, quitar límites de tiempo y activar avisos breves ante errores.\n\n");

        help.append("Otros:\n");
        help.append("• Las partidas y estadísticas se guardan en el dispositivo.\n");
        help.append("• Revisá Rankings y Estadísticas para ver tu progreso.\n");

        new MaterialAlertDialogBuilder(this)
                .setTitle("Ayuda")
                .setMessage(help.toString())
                .setPositiveButton("Entendido", null)
                .show();
    }

    private void setupBottomNav() {
        MaterialButton btnNavSettings = findViewById(R.id.btnNavSettings);
        MaterialButton btnNavGame = findViewById(R.id.btnNavGame);
        MaterialButton btnNavRankings = findViewById(R.id.btnNavRankings);

        highlightNav(btnNavGame, true);
        btnNavGame.setOnClickListener(v -> startGameFromCurrentConfig());
        btnNavSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        btnNavRankings.setOnClickListener(v -> startActivity(new Intent(this, RankingsActivity.class)));
    }

    private void highlightNav(MaterialButton button, boolean active) {
        if (button == null) return;

        int surface = ContextCompat.getColor(this, R.color.surface);
        int accent = ContextCompat.getColor(this, R.color.accent);
        int textDark = ContextCompat.getColor(this, R.color.background);
        int textLight = ContextCompat.getColor(this, R.color.text_primary);

        if (active) {
            button.setBackgroundTintList(ColorStateList.valueOf(accent));
            button.setTextColor(textDark);
            button.setStrokeWidth(0);
        } else {
            button.setBackgroundTintList(ColorStateList.valueOf(surface));
            button.setTextColor(textLight);
            button.setStrokeWidth(2);
            button.setStrokeColor(ColorStateList.valueOf(accent));
        }
    }

    private void startGameFromCurrentConfig() {
        GameConfig config = buildConfig();
        Prefs.saveConfig(this, config);
        startActivity(new Intent(this, GameActivity.class));
    }

    private void saveCurrentConfig() {
        if (suppressUiEvents) return;
        Prefs.saveConfig(this, buildConfig());
    }

    private GameConfig buildConfig() {
        GameConfig c = Prefs.loadConfig(this);
        c.playerName = Prefs.getPlayerName(this);
        c.mode = getSelectedMode();
        c.difficulty = getSelectedDifficulty();
        c.iterationsPerLevel = pickerIterations.getValue();
        c.maxReactionSeconds = pickerMaxTime.getValue();
        c.trainingEnabled = cbTraining.isChecked();
        c.trainingNoFail = c.trainingEnabled && cbTrainingNoFail.isChecked();
        c.trainingNoTimeLimit = c.trainingEnabled && cbTrainingNoTime.isChecked();
        c.trainingShowHelp = c.trainingEnabled && cbTrainingHelp.isChecked();
        return c;
    }

    private GameMode getSelectedMode() {
        return spinnerMode.getSelectedItemPosition() == 1 ? GameMode.INVERSE : GameMode.NORMAL;
    }

    private Difficulty getSelectedDifficulty() {
        switch (spinnerDifficulty.getSelectedItemPosition()) {
            case 1:
                return Difficulty.MEDIUM;
            case 2:
                return Difficulty.HARD;
            case 3:
                return Difficulty.DYNAMIC;
            default:
                return Difficulty.EASY;
        }
    }

    private int indexOfMode(GameMode mode) {
        return mode == GameMode.INVERSE ? 1 : 0;
    }

    private int indexOfDifficulty(Difficulty d) {
        if (d == Difficulty.MEDIUM) return 1;
        if (d == Difficulty.HARD) return 2;
        if (d == Difficulty.DYNAMIC) return 3;
        return 0;
    }
}
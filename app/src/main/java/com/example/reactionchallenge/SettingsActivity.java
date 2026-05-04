package com.example.reactionchallenge;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.reactionchallenge.model.GameConfig;
import com.example.reactionchallenge.util.MusicManager;
import com.example.reactionchallenge.util.Prefs;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsActivity extends AppCompatActivity {

    private SwitchMaterial swSound;
    private SwitchMaterial swVibration;
    private SwitchMaterial swMenuMusic;
    private SwitchMaterial swGameMusic;
    private TextView tvCurrentPlayer;
    private boolean suppressSave = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (TextUtils.isEmpty(Prefs.getPlayerName(this))) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_settings);
        bindViews();
        loadData();
        wireActions();
        setupBottomNav();
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

    private void bindViews() {
        tvCurrentPlayer = findViewById(R.id.tvCurrentPlayer);
        swSound = findViewById(R.id.swSound);
        swVibration = findViewById(R.id.swVibration);
        swMenuMusic = findViewById(R.id.swMenuMusic);
        swGameMusic = findViewById(R.id.swGameMusic);
    }

    private void loadData() {
        String playerName = Prefs.getPlayerName(this);
        tvCurrentPlayer.setText("Jugador actual: " + playerName);

        GameConfig config = Prefs.loadConfig(this);
        suppressSave = true;
        swSound.setChecked(config.soundEnabled);
        swVibration.setChecked(config.vibrationEnabled);
        swMenuMusic.setChecked(config.menuMusicEnabled);
        swGameMusic.setChecked(config.gameMusicEnabled);
        suppressSave = false;
    }

    private void wireActions() {
        swSound.setOnCheckedChangeListener((buttonView, isChecked) -> saveNow());
        swVibration.setOnCheckedChangeListener((buttonView, isChecked) -> saveNow());
        swMenuMusic.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveNow();
            MusicManager.sync(this, isChecked);
        });
        swGameMusic.setOnCheckedChangeListener((buttonView, isChecked) -> saveNow());

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            Prefs.clearPlayerName(this);
            MusicManager.stop();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void saveNow() {
        if (suppressSave) return;
        GameConfig config = Prefs.loadConfig(this);
        config.soundEnabled = swSound.isChecked();
        config.vibrationEnabled = swVibration.isChecked();
        config.menuMusicEnabled = swMenuMusic.isChecked();
        config.gameMusicEnabled = swGameMusic.isChecked();
        Prefs.saveConfig(this, config);
    }

    private void setupBottomNav() {
        MaterialButton btnNavSettings = findViewById(R.id.btnNavSettings);
        MaterialButton btnNavGame = findViewById(R.id.btnNavGame);
        MaterialButton btnNavRankings = findViewById(R.id.btnNavRankings);

        highlightNav(btnNavSettings, true);
        btnNavGame.setOnClickListener(v -> startActivity(new Intent(this, MainMenuActivity.class)));
        btnNavRankings.setOnClickListener(v -> startActivity(new Intent(this, RankingsActivity.class)));
        btnNavSettings.setOnClickListener(v -> Toast.makeText(this, "Ya estás en configuración.", Toast.LENGTH_SHORT).show());
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

package com.example.reactionchallenge;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.reactionchallenge.db.AppDbHelper;
import com.example.reactionchallenge.model.Difficulty;
import com.example.reactionchallenge.model.GameConfig;
import com.example.reactionchallenge.model.GameRecord;
import com.example.reactionchallenge.model.GameSessionResult;
import com.example.reactionchallenge.model.GameMode;
import com.example.reactionchallenge.model.StagePlan;
import com.example.reactionchallenge.model.Stimulus;
import com.example.reactionchallenge.util.MusicManager;
import com.example.reactionchallenge.util.Prefs;
import com.example.reactionchallenge.util.RuleFactory;
import com.example.reactionchallenge.util.ScoreCalculator;
import com.example.reactionchallenge.util.SoundManager;
import com.example.reactionchallenge.util.StimulusFactory;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class GameActivity extends AppCompatActivity {

    private TextView tvMode;
    private TextView tvDifficulty;
    private TextView tvLives;
    private TextView tvScore;
    private TextView tvStageInfo;
    private TextView tvRule;
    private TextView tvStimulus;
    private TextView tvFeedback;
    private ProgressBar progressTime;
    private MaterialButton btnYes;
    private MaterialButton btnNo;
    private MaterialButton btnAction;
    private MaterialButton btnNavSettings;
    private MaterialButton btnNavGame;
    private MaterialButton btnNavRankings;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final List<Long> reactionTimes = new ArrayList<>();
    private final List<StagePlan> stages = new ArrayList<>();

    private AppDbHelper dbHelper;
    private SoundManager soundManager;
    private static final int INVERSE_TRAINING_NO_TIME_MS = 45000;
    private GameConfig config;
    private StagePlan currentStage;
    private Stimulus currentStimulus;
    private CountDownTimer timer;

    private int currentStageIndex;
    private int currentStimulusIndex;
    private int livesLeft;
    private int startingLives;
    private int score;
    private int correctCount;
    private int wrongCount;
    private int missedCount;
    private int totalStimuli;
    private int levelsCompleted;
    private long bestReactionMs;
    private long worstReactionMs;
    private long stimulusStartMs;
    private boolean awaitingResponse;
    private boolean responded;
    private boolean currentShouldReact;
    private boolean gameFinished;

    private final Runnable advanceRunnable = this::showNextStimulus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (TextUtils.isEmpty(Prefs.getPlayerName(this))) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_game);
        bindViews();

        dbHelper = new AppDbHelper(this);
        soundManager = new SoundManager(this);
        config = Prefs.loadConfig(this);

        tvMode.setText("Modo: " + config.mode.getLabel());
        tvDifficulty.setText("Dificultad: " + config.difficulty.getLabel() + (config.trainingEnabled ? " · Entrenamiento" : ""));

        setupButtons();
        setupBottomNav();
        buildStages();
        startGame();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MusicManager.sync(this, Prefs.loadConfig(this).gameMusicEnabled);
    }

    @Override
    protected void onPause() {
        super.onPause();
        cancelTimer();
        handler.removeCallbacks(advanceRunnable);
        MusicManager.stop();
    }

    private void bindViews() {
        tvMode = findViewById(R.id.tvMode);
        tvDifficulty = findViewById(R.id.tvDifficulty);
        tvLives = findViewById(R.id.tvLives);
        tvScore = findViewById(R.id.tvScore);
        tvStageInfo = findViewById(R.id.tvStageInfo);
        tvRule = findViewById(R.id.tvRule);
        tvStimulus = findViewById(R.id.tvStimulus);
        tvFeedback = findViewById(R.id.tvFeedback);
        progressTime = findViewById(R.id.progressTime);
        btnYes = findViewById(R.id.btnYes);
        btnNo = findViewById(R.id.btnNo);
        btnAction = findViewById(R.id.btnAction);
        btnNavSettings = findViewById(R.id.btnNavSettings);
        btnNavGame = findViewById(R.id.btnNavGame);
        btnNavRankings = findViewById(R.id.btnNavRankings);
    }

    private void setupButtons() {
        btnYes.setOnClickListener(v -> registerChoice(true));
        btnNo.setOnClickListener(v -> registerChoice(false));
        btnAction.setOnClickListener(v -> registerChoice(true));
    }

    private void setupBottomNav() {
        highlightNav(btnNavGame, true);
        btnNavSettings.setOnClickListener(v -> confirmLeaveCurrentGame(() -> {
            startActivity(new Intent(this, SettingsActivity.class));
            finish();
        }));
        btnNavRankings.setOnClickListener(v -> confirmLeaveCurrentGame(() -> {
            startActivity(new Intent(this, RankingsActivity.class));
            finish();
        }));
        btnNavGame.setOnClickListener(v -> Toast.makeText(this, "Ya estás en la partida.", Toast.LENGTH_SHORT).show());
    }

    private void buildStages() {
        stages.clear();
        boolean trainingNoTime = config.trainingEnabled && config.trainingNoTimeLimit;

        stages.addAll(RuleFactory.createStagePlans(
                config.mode,
                config.difficulty,
                config.iterationsPerLevel,
                config.maxReactionSeconds,
                trainingNoTime
        ));

        Difficulty baseDifficulty = config.difficulty == null ? Difficulty.EASY : config.difficulty;
        startingLives = baseDifficulty.getStartingLives();
        livesLeft = startingLives;

        tvLives.setText("Vidas: " + livesLeft);
        refreshScore();
    }

    private void startGame() {
        score = 0;
        correctCount = 0;
        wrongCount = 0;
        missedCount = 0;
        totalStimuli = 0;
        levelsCompleted = 0;
        bestReactionMs = Long.MAX_VALUE;
        worstReactionMs = 0L;
        currentStageIndex = 0;
        currentStimulusIndex = 0;
        reactionTimes.clear();
        gameFinished = false;
        startStage(currentStageIndex);
    }

    private void startStage(int index) {
        if (index < 0 || index >= stages.size()) {
            finishGame(true);
            return;
        }

        currentStage = stages.get(index);
        currentStimulusIndex = 0;
        currentShouldReact = false;
        awaitingResponse = false;
        responded = false;
        tvRule.setVisibility(currentStage.isHideRuleDuringPlay() ? android.view.View.GONE : android.view.View.VISIBLE);
        tvRule.setText(currentStage.getRule().getDescription());
        tvStageInfo.setText(String.format(Locale.getDefault(), "Etapa %d de %d · %s", index + 1, stages.size(), currentStage.getIntroText()));

        soundManager.level(config.soundEnabled, config.vibrationEnabled);

        StringBuilder message = new StringBuilder();
        message.append(currentStage.getIntroText())
                .append("\n\nRegla: ").append(currentStage.getRule().getDescription())
                .append(config.isInverse()
                        ? "\n\nModo reacción inversa: tocá solo cuando corresponda. Sino, dejá pasar el tiempo."
                        : "\n\nModo normal: usá SI o NO según corresponda.\n\nSi aparece un tipo de estímulo no tratado en la regla, usá NO.");

        if (config.trainingEnabled && config.trainingNoTimeLimit) {
            if (config.isInverse()) {
                message.append("\n\nEntrenamiento con tiempo extendido: en reacción inversa no se usa tiempo infinito, sino que se aplica un límite amplio para mantener la partida jugable.");
            } else {
                message.append("\n\nEntrenamiento sin tiempo práctico: no habrá cuenta regresiva y deberás responder para avanzar.");
            }
        }

        if (config.trainingEnabled && config.trainingNoFail) {
            message.append("\n\nEntrenamiento sin vidas: no perderás vidas por errores.");
        }

        if (config.trainingEnabled && config.trainingShowHelp) {
            message.append("\n\nLa ayuda aparecerá cuando te equivoques.");
        }

        new AlertDialog.Builder(this)
                .setTitle("Comenzar etapa " + (index + 1))
                .setMessage(message.toString())
                .setCancelable(false)
                .setPositiveButton("Comenzar", (dialog, which) -> beginStagePlay())
                .show();
    }

    private void beginStagePlay() {
        if (currentStage.isHideRuleDuringPlay()) {
            tvRule.setVisibility(android.view.View.GONE);
        } else {
            tvRule.setVisibility(android.view.View.VISIBLE);
        }
        showNextStimulus();
    }

    private void showNextStimulus() {
        cancelTimer();

        if (livesLeft <= 0 && !isTrainingWithoutLives()) {
            finishGame(false);
            return;
        }

        if (currentStimulusIndex >= currentStage.getIterations()) {
            levelsCompleted++;
            currentStageIndex++;
            if (currentStageIndex < stages.size()) {
                startStage(currentStageIndex);
            } else {
                finishGame(true);
            }
            return;
        }

        currentStimulusIndex++;
        totalStimuli++;

        currentStimulus = StimulusFactory.randomFromAllowedTypes(random, currentStage.getAllowedTypes());
        currentShouldReact = currentStage.getRule().matches(currentStimulus);
        awaitingResponse = true;
        responded = false;
        stimulusStartMs = SystemClock.elapsedRealtime();

        tvStageInfo.setText(String.format(Locale.getDefault(), "Etapa %d de %d · Estímulo %d/%d", currentStageIndex + 1, stages.size(), currentStimulusIndex, currentStage.getIterations()));

        tvFeedback.setText("");
        tvStimulus.setText(currentStimulus.details());
        tvStimulus.setBackgroundColor(currentStimulus.getBackgroundColor());
        tvStimulus.setTextColor(isDarkColor(currentStimulus.getBackgroundColor()) ? Color.WHITE : Color.BLACK);

        updateModeButtons();
        tvLives.setText("Vidas: " + livesLeft);
        refreshScore();
        startTimerIfNeeded();
    }

    private boolean isTrainingWithoutLives() {
        return config.trainingEnabled && config.trainingNoFail;
    }

    private void updateModeButtons() {
        if (config.isInverse()) {
            btnAction.setVisibility(android.view.View.VISIBLE);
            btnYes.setVisibility(android.view.View.GONE);
            btnNo.setVisibility(android.view.View.GONE);
            btnAction.setText("TOCAR");
        } else {
            btnAction.setVisibility(android.view.View.GONE);
            btnYes.setVisibility(android.view.View.VISIBLE);
            btnNo.setVisibility(android.view.View.VISIBLE);
        }
    }

    private void startTimerIfNeeded() {
        cancelTimer();

        int duration = currentStage.getMaxReactionMs();
        boolean trainingNoCountdown = config.trainingEnabled && config.trainingNoTimeLimit && !config.isInverse();
        boolean inverseTrainingExtendedTime = config.trainingEnabled && config.trainingNoTimeLimit && config.isInverse();

        if (trainingNoCountdown) {
            progressTime.setProgress(0);
            progressTime.setVisibility(android.view.View.GONE);
            return;
        }

        if (inverseTrainingExtendedTime) {
            duration = Math.max(duration, INVERSE_TRAINING_NO_TIME_MS);
        }

        progressTime.setVisibility(android.view.View.VISIBLE);
        progressTime.setMax(duration);
        progressTime.setProgress(duration);

        timer = new CountDownTimer(duration, 50) {
            @Override
            public void onTick(long millisUntilFinished) {
                progressTime.setProgress((int) millisUntilFinished);
            }

            @Override
            public void onFinish() {
                progressTime.setProgress(0);
                handleTimerExpiry();
            }
        }.start();
    }

    private void handleTimerExpiry() {
        if (!awaitingResponse || responded) {
            return;
        }

        if (currentShouldReact) {
            handleMiss(true);
        } else {
            handleCorrect(false, true);
        }
    }

    private void registerChoice(boolean action) {
        if (!awaitingResponse || responded) {
            return;
        }

        boolean correct = action == currentShouldReact;
        if (correct) {
            handleCorrect(action, false);
        } else {
            handleWrong(action);
        }
    }

    private void handleCorrect(boolean reacted, boolean timedOut) {
        responded = true;
        awaitingResponse = false;
        cancelTimer();

        long reaction = timedOut ? currentStage.getMaxReactionMs() : Math.max(0L, SystemClock.elapsedRealtime() - stimulusStartMs);
        reactionTimes.add(reaction);
        if (reaction > 0) {
            bestReactionMs = Math.min(bestReactionMs, reaction);
            worstReactionMs = Math.max(worstReactionMs, reaction);
        }

        correctCount++;
        soundManager.success(config.soundEnabled, config.vibrationEnabled);
        tvFeedback.setText(currentShouldReact
                ? (timedOut ? "Correcto: esperabas la señal." : "Correcto: reaccionaste bien con SI.")
                : "Correcto: reaccionaste bien con NO.");
        if (config.mode.isInverse()){
            tvFeedback.setText("Correcto: " + (currentShouldReact ? "reaccionaste ante el estímulo" : "esperaste a que se termine el tiempo, sin reaccionar"));
        }
        refreshScore();
        delayNext();
    }

    private void handleWrong(boolean actionPressed) {
        responded = true;
        awaitingResponse = false;
        cancelTimer();

        wrongCount++;
        if (!isTrainingWithoutLives()) {
            livesLeft--;
        }

        soundManager.error(config.soundEnabled, config.vibrationEnabled);

        String help = currentShouldReact
                    ? "Debías presionar SI."
                    : "Debías presionar NO.";

        if (config.mode.isInverse()) {
            help = currentShouldReact
                    ? "Debías reaccionar antes de terminar el tiempo."
                    : "Debías esperar a que termine el tiempo, sin reaccionar.";
        }

        tvFeedback.setText("Incorrecto. " + help);

        if (config.trainingEnabled && config.trainingShowHelp) {
            Toast.makeText(this, help, Toast.LENGTH_LONG).show();
        }

        refreshScore();

        if (livesLeft <= 0 && !isTrainingWithoutLives()) {
            finishGame(false);
            return;
        }
        delayNext();
    }

    private void handleMiss(boolean dueToTimeout) {
        responded = true;
        awaitingResponse = false;
        cancelTimer();

        missedCount++;
        if (!isTrainingWithoutLives()) {
            livesLeft--;
        }

        soundManager.error(config.soundEnabled, config.vibrationEnabled);
        String help = "Debías reaccionar antes de que se terminara el tiempo.";
        tvFeedback.setText("Tiempo agotado. " + help);

        if (config.trainingEnabled && config.trainingShowHelp) {
            Toast.makeText(this, help, Toast.LENGTH_LONG).show();
        }

        refreshScore();

        if (livesLeft <= 0 && !isTrainingWithoutLives()) {
            finishGame(false);
            return;
        }
        delayNext();
    }

    private void delayNext() {
        handler.removeCallbacks(advanceRunnable);
        handler.postDelayed(advanceRunnable, 650);
    }

    private void refreshScore() {
        GameSessionResult result = buildSessionResult();
        score = ScoreCalculator.calculate(result, config);
        tvScore.setText("Puntaje: " + score);
        tvLives.setText("Vidas: " + livesLeft);
    }

    private GameSessionResult buildSessionResult() {
        GameSessionResult result = new GameSessionResult();
        result.correctCount = correctCount;
        result.wrongCount = wrongCount;
        result.missedCount = missedCount;
        result.totalStimuli = totalStimuli;
        result.levelsCompleted = levelsCompleted;
        result.livesStart = startingLives;
        result.livesLeft = livesLeft;
        result.maxReactionMs = currentStage != null ? currentStage.getMaxReactionMs() : (config.maxReactionSeconds * 1000);

        long sum = 0L;
        long best = Long.MAX_VALUE;
        long worst = 0L;
        for (Long value : reactionTimes) {
            if (value == null) continue;
            sum += value;
            best = Math.min(best, value);
            worst = Math.max(worst, value);
        }

        if (!reactionTimes.isEmpty()) {
            result.avgReactionMs = sum / reactionTimes.size();
            result.bestReactionMs = best == Long.MAX_VALUE ? 0 : best;
            result.worstReactionMs = worst;
        } else {
            result.avgReactionMs = 0;
            result.bestReactionMs = 0;
            result.worstReactionMs = 0;
        }
        return result;
    }

    private void finishGame(boolean completed) {
        if (gameFinished) {
            return;
        }
        gameFinished = true;
        cancelTimer();
        handler.removeCallbacks(advanceRunnable);

        GameSessionResult result = buildSessionResult();
        score = ScoreCalculator.calculate(result, config);

        if (!config.trainingEnabled) {
            saveRecord(result, completed);
        }

        String title = completed ? "¡Partida completada!" : "Partida finalizada";
        StringBuilder summary = new StringBuilder();
        summary.append("Puntaje: ").append(score)
                .append("\nAciertos: ").append(correctCount)
                .append("\nErrores: ").append(wrongCount)
                .append("\nOmisiones: ").append(missedCount)
                .append("\nPromedio reacción: ").append(result.avgReactionMs).append(" ms")
                .append("\nMejor reacción: ").append(result.bestReactionMs).append(" ms")
                .append("\nPeor reacción: ").append(result.worstReactionMs).append(" ms")
                .append("\nEstímulos: ").append(totalStimuli)
                .append("\nEtapas completadas: ").append(levelsCompleted);

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(summary.toString() + (config.trainingEnabled ? "\n\nModo entrenamiento: no se guardó ranking ni estadísticas." : ""))
                .setCancelable(false)
                .setPositiveButton("Jugar de nuevo", (dialog, which) -> {
                    startActivity(new Intent(this, GameActivity.class));
                    finish();
                })
                .setNegativeButton("Volver al menú", (dialog, which) -> {
                    startActivity(new Intent(this, MainMenuActivity.class));
                    finish();
                })
                .show();
    }

    private void saveRecord(GameSessionResult result, boolean completed) {
        GameRecord record = new GameRecord();
        record.playerName = config.playerName;
        record.mode = config.mode == null ? GameMode.NORMAL.getLabel() : config.mode.getLabel();
        record.difficulty = config.difficulty == null ? Difficulty.EASY.getLabel() : config.difficulty.getLabel();
        record.training = false;
        record.completed = completed;
        record.score = score;
        record.correctCount = result.correctCount;
        record.wrongCount = result.wrongCount;
        record.missedCount = result.missedCount;
        record.avgReactionMs = result.avgReactionMs;
        record.bestReactionMs = result.bestReactionMs;
        record.worstReactionMs = result.worstReactionMs;
        record.iterationsPerLevel = config.iterationsPerLevel;
        record.totalStimuli = result.totalStimuli;
        record.levelsCompleted = result.levelsCompleted;
        record.maxReactionMs = result.maxReactionMs;
        record.livesStart = result.livesStart;
        record.livesLeft = result.livesLeft;
        record.createdAt = System.currentTimeMillis();
        record.ruleSummary = buildRuleSummary();
        dbHelper.insertRecord(record);
    }

    private String buildRuleSummary() {
        if (stages.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < stages.size(); i++) {
            StagePlan stage = stages.get(i);
            if (i > 0) sb.append(" | ");
            sb.append(stage.getDifficulty().getLabel()).append(": ").append(stage.getRule().getDescription());
        }
        return sb.toString();
    }

    private void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void confirmLeaveCurrentGame(Runnable action) {
        new AlertDialog.Builder(this)
                .setTitle("Salir de la partida")
                .setMessage("La partida actual se cerrará y el progreso en curso se perderá.")
                .setPositiveButton("Salir", (dialog, which) -> {
                    cancelTimer();
                    handler.removeCallbacks(advanceRunnable);
                    action.run();
                })
                .setNegativeButton("Cancelar", null)
                .show();
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

    private boolean isDarkColor(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness >= 0.55;
    }
}

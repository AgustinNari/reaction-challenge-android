package com.example.reactionchallenge.util;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

public class SoundManager {
    private final ToneGenerator toneGenerator;
    private final Context context;

    public SoundManager(Context context) {
        this.context = context.getApplicationContext();
        this.toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 90);
    }

    public void success(boolean soundEnabled, boolean vibrationEnabled) {
        if (soundEnabled) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 120);
        }
        vibrate(vibrationEnabled, 40);
    }

    public void error(boolean soundEnabled, boolean vibrationEnabled) {
        if (soundEnabled) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_NACK, 180);
        }
        vibrate(vibrationEnabled, 80);
    }

    public void level(boolean soundEnabled, boolean vibrationEnabled) {
        if (soundEnabled) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 120);
        }
        vibrate(vibrationEnabled, 25);
    }

    private void vibrate(boolean enabled, long millis) {
        if (!enabled) return;
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(millis);
        }
    }
}

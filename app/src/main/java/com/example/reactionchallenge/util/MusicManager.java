
package com.example.reactionchallenge.util;

import android.content.Context;
import android.media.MediaPlayer;

import com.example.reactionchallenge.R;

public final class MusicManager {
    private static MediaPlayer mediaPlayer;

    private MusicManager() {
    }

    public static synchronized void sync(Context context, boolean enabled) {
        Context app = context.getApplicationContext();
        if (!enabled) {
            stop();
            return;
        }
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(app, R.raw.bg_music);
            if (mediaPlayer != null) {
                mediaPlayer.setLooping(true);
                mediaPlayer.setVolume(0.35f, 0.35f);
            }
        }
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            try {
                mediaPlayer.start();
            } catch (IllegalStateException ignored) {
            }
        }
    }

    public static synchronized void stop() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            } catch (IllegalStateException ignored) {
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}

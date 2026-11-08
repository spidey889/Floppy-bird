package com.flappybro.skyline;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

public class MainActivity extends Activity {
    private GameView game;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);
        game = new GameView(this);
        setContentView(game);
    }
    @Override protected void onPause() { super.onPause(); game.suspend(); }
    @Override protected void onResume() { super.onResume(); if (game != null) game.resumeFrames(); }
    @Override protected void onDestroy() { game.close(); super.onDestroy(); }
    @Override public void onBackPressed() { if (!game.back()) super.onBackPressed(); }
}

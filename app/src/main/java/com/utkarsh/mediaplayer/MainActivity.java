package com.utkarsh.mediaplayer;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.MediaController;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private MediaPlayer mediaPlayer;
    private MediaController mediaController;
    private SurfaceView videoSurface;
    private SurfaceHolder videoHolder;
    private String currentFilePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI elements
        videoSurface = findViewById(R.id.videoSurface);
        videoHolder = videoSurface.getHolder();

        Button btnPlay = findViewById(R.id.btnPlay);
        Button btnSelect = findViewById(R.id.btnSelectFile);

        // Setup video surface
        videoHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (currentFilePath != null) {
                    playMedia(currentFilePath);
                }
            }
            // ... other required SurfaceHolder methods
        });

        btnSelect.setOnClickListener(v -> selectMediaFile());
        btnPlay.setOnClickListener(v -> togglePlayPause());
    }

    private void selectMediaFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            currentFilePath = getPathFromUri(uri);
            playMedia(currentFilePath);
        }
    }

    private void playMedia(String filePath) {
        try {
            releaseMediaPlayer();

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(filePath);

            if (filePath.endsWith(".mp4") || filePath.endsWith(".3gp")) {
                // Video setup
                mediaPlayer.setDisplay(videoHolder);
                setupMediaControls();
            }

            mediaPlayer.prepare();
            mediaPlayer.start();

          Button play =  findViewById(R.id.btnPlay);
                  play.setText("⏸");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void togglePlayPause() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                ((Button)findViewById(R.id.btnPlay)).setText("▶");
            } else {
                mediaPlayer.start();
                ((Button)findViewById(R.id.btnPlay)).setText("⏸");
            }
        }
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMediaPlayer();
    }
}
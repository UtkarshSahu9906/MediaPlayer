package com.utkarsh.mediaplayer;

import static com.utkarsh.mediaplayer.FileUtils.getPathFromUri;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.Toast;

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

        if (requestCode == FILE_SELECT_CODE && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();

                try {
                    // Get file path from URI
                    currentFilePath = FileUtils.getPathFromUri(this, uri);

                    if (currentFilePath != null) {
                        // Verify file exists and is readable
                        File mediaFile = new File(currentFilePath);
                        if (mediaFile.exists() && mediaFile.canRead()) {
                            playMedia(currentFilePath);
                        } else {
                            showToast("Cannot access selected file");
                        }
                    } else {
                        showToast("Unsupported file source");
                    }
                } catch (SecurityException e) {
                    showToast("Permission denied for file access");
                    Log.e("MediaPlayer", "Security Exception: " + e.getMessage());
                } catch (Exception e) {
                    showToast("Error loading media file");
                    Log.e("MediaPlayer", "File Error: " + e.getMessage());
                }
            } else {
                showToast("No file selected");
            }
        }
    }

    // Helper method to show toast messages
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    private void playMedia(Uri uri) {
        String filePath = FileUtils.getPathFromUri(this, uri);

        if (filePath != null) {
            try {
                mediaPlayer.setDataSource(filePath);
                mediaPlayer.prepare();
                // ... rest of your playback logic
            } catch (IOException e) {
                Toast.makeText(this, "Error loading file", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Invalid file path", Toast.LENGTH_SHORT).show();
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
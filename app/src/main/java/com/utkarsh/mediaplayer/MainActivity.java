package com.utkarsh.mediaplayer;




import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private static final int FILE_SELECT_CODE = 1;
    private static final int STORAGE_PERMISSION_CODE = 101;

    private MediaPlayer mediaPlayer;
    private SurfaceView videoSurface;
    private SurfaceHolder videoHolder;
    private String currentFilePath;
    private SeekBar seekBar;
    private Handler updateHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI
        videoSurface = findViewById(R.id.videoSurface);
        videoHolder = videoSurface.getHolder();
        videoHolder.addCallback(this);

        Button btnSelectFile = findViewById(R.id.btnSelectFile);
        btnSelectFile.setOnClickListener(v -> selectMediaFile());

        setupMediaControls();
    }

    private void selectMediaFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, FILE_SELECT_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_SELECT_CODE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            currentFilePath = FileUtils.getPathFromUri(this, uri);
            if (currentFilePath != null) {
                playMedia(currentFilePath);
            } else {
                showToast("Failed to get file path");
            }
        }
    }

    private void playMedia(String filePath) {
        try {
            releaseMediaPlayer();
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(filePath);

            if (filePath.endsWith(".mp4") || filePath.endsWith(".mkv")) {
                mediaPlayer.setDisplay(videoHolder);
            }

            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                updatePlayPauseButton();
                setupSeekBar();
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                updatePlayPauseButton();
                showToast("Playback completed");
            });

        } catch (IOException e) {
            showToast("Error loading media file");
            e.printStackTrace();
        }
    }

    private void setupMediaControls() {
        Button btnPlay = findViewById(R.id.btnPlay);
        btnPlay.setOnClickListener(v -> togglePlayPause());

        Button btnPrevious = findViewById(R.id.btnPrevious);
        btnPrevious.setOnClickListener(v -> seekRelative(-10000)); // -10 seconds

        Button btnNext = findViewById(R.id.btnNext);
        btnNext.setOnClickListener(v -> seekRelative(10000)); // +10 seconds

        seekBar = findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void setupSeekBar() {
        if (mediaPlayer == null) return;

        seekBar.setMax(mediaPlayer.getDuration());
        updateTimeText();

        updateHandler.postDelayed(updateSeekBar, 1000);
    }

    private Runnable updateSeekBar = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                seekBar.setProgress(mediaPlayer.getCurrentPosition());
                updateTimeText();
                updateHandler.postDelayed(this, 1000);
            }
        }
    };

    private void updateTimeText() {
        TextView tvCurrentTime = findViewById(R.id.tvCurrentTime);
        TextView tvTotalTime = findViewById(R.id.tvTotalTime);

        if (mediaPlayer != null) {
            tvCurrentTime.setText(formatTime(mediaPlayer.getCurrentPosition()));
            tvTotalTime.setText(formatTime(mediaPlayer.getDuration()));
        }
    }

    private String formatTime(int milliseconds) {
        int seconds = (milliseconds / 1000) % 60;
        int minutes = (milliseconds / (1000 * 60)) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void togglePlayPause() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
            } else {
                mediaPlayer.start();
                setupSeekBar();
            }
            updatePlayPauseButton();
        }
    }

    private void updatePlayPauseButton() {
        Button btnPlay = findViewById(R.id.btnPlay);
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            btnPlay.setText("⏸");
        } else {
            btnPlay.setText("▶");
        }
    }

    private void seekRelative(int milliseconds) {
        if (mediaPlayer != null) {
            int newPosition = mediaPlayer.getCurrentPosition() + milliseconds;
            mediaPlayer.seekTo(Math.max(0, Math.min(newPosition, mediaPlayer.getDuration())));
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (currentFilePath != null) {
            playMedia(currentFilePath);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {}

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
            updateHandler.removeCallbacks(updateSeekBar);
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMediaPlayer();
        updateHandler.removeCallbacksAndMessages(null);
    }
}
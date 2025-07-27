package com.utkarsh.mediaplayer;

import static android.view.View.VISIBLE;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private static final int FILE_SELECT_CODE = 1;
    private static final int STORAGE_PERMISSION_CODE = 101;
    private static final String TAG = "MediaPlayer";

    private MediaPlayer mediaPlayer;
    private SurfaceView videoSurface;
    private SurfaceHolder videoHolder;
    private String currentFilePath;
    private SeekBar seekBar;
    private Handler updateHandler = new Handler();
    private boolean surfaceReady = false;
    private boolean isVideo = false;
    private boolean isPrepared = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI
        videoSurface = findViewById(R.id.videoSurface);
        videoHolder = videoSurface.getHolder();
        videoHolder.addCallback(this);
        videoHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        Button btnSelectFile = findViewById(R.id.btnSelectFile);
        btnSelectFile.setOnClickListener(v -> selectMediaFile());

        setupMediaControls();
    }

    private void selectMediaFile() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_CODE);
        } else {
            openFilePicker();
        }
    }

    private void openFilePicker() {
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
            if (uri != null) {
                try {
                    currentFilePath = FileUtils.getPathFromUri(this, uri);
                    if (currentFilePath != null) {
                        isVideo = currentFilePath.endsWith(".mp4") || currentFilePath.endsWith(".mkv");
                        playMedia(currentFilePath);
                    } else {
                        playMedia(uri);
                    }
                } catch (Exception e) {
                    showToast("Error processing file");
                    Log.e(TAG, "File error", e);
                }
            }
        }
    }

    private void playMedia(Uri uri) {
        try {
            releaseMediaPlayer();
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(getApplicationContext(), uri);
            setupMediaPlayer();
        } catch (IOException e) {
            showToast("Error loading media");
            Log.e(TAG, "Media error", e);
        }
    }

    private void playMedia(String filePath) {
        try {
            releaseMediaPlayer();
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(filePath);
            setupMediaPlayer();
        } catch (IOException e) {
            showToast("Error loading media");
            Log.e(TAG, "Media error", e);
        }
    }

    private void setupMediaPlayer() {
        if (mediaPlayer == null) return;

        // Video-specific setup
        if (isVideo) {
            videoSurface.setVisibility(VISIBLE);
            if (surfaceReady) {
                mediaPlayer.setDisplay(videoHolder);
            }

            mediaPlayer.setOnVideoSizeChangedListener((mp, width, height) -> {
                // Adjust surface view aspect ratio
                ViewGroup.LayoutParams lp = videoSurface.getLayoutParams();
                if (width > 0 && height > 0) {
                    float videoRatio = width / (float) height;
                    float viewRatio = videoSurface.getWidth() / (float) videoSurface.getHeight();
                    if (videoRatio > viewRatio) {
                        lp.width = videoSurface.getWidth();
                        lp.height = (int) (videoSurface.getWidth() / videoRatio);
                    } else {
                        lp.height = videoSurface.getHeight();
                        lp.width = (int) (videoSurface.getHeight() * videoRatio);
                    }
                    videoSurface.setLayoutParams(lp);
                }
            });
        } else {
            videoSurface.setVisibility(View.GONE);
        }

        mediaPlayer.setScreenOnWhilePlaying(true);
        mediaPlayer.prepareAsync();

        mediaPlayer.setOnPreparedListener(mp -> {
            isPrepared = true;
            if (isVideo && !surfaceReady) {
                videoHolder.addCallback(this);
            }
            startPlayback();
        });

        mediaPlayer.setOnCompletionListener(mp -> {
            updatePlayPauseButton();
            showToast("Playback completed");
        });

        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            showToast("Playback error: " + what);
            return true;
        });

        mediaPlayer.setOnInfoListener((mp, what, extra) -> {
            if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                Log.d(TAG, "Video rendering started");
            }
            return true;
        });
    }

    private void setupMediaControls() {
        Button btnPlay = findViewById(R.id.btnPlay);
        btnPlay.setOnClickListener(v -> togglePlayPause());

        Button btnPrevious = findViewById(R.id.btnPrevious);
        btnPrevious.setOnClickListener(v -> seekRelative(-10000));

        Button btnNext = findViewById(R.id.btnNext);
        btnNext.setOnClickListener(v -> seekRelative(10000));

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

    private void startPlayback() {
        if (mediaPlayer != null && isPrepared) {
            mediaPlayer.start();
            updatePlayPauseButton();
            setupSeekBar();
        }
    }

    private void setupSeekBar() {
        if (mediaPlayer == null) return;

        seekBar.setMax(mediaPlayer.getDuration());
        updateTimeText();

        updateHandler.postDelayed(updateSeekBar, 1000);
    }

    private final Runnable updateSeekBar = new Runnable() {
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
        if (mediaPlayer != null && isPrepared) {
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
        if (mediaPlayer != null && isPrepared) {
            int newPosition = mediaPlayer.getCurrentPosition() + milliseconds;
            mediaPlayer.seekTo(Math.max(0, Math.min(newPosition, mediaPlayer.getDuration())));
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceReady = true;
        if (mediaPlayer != null && isVideo) {
            mediaPlayer.setDisplay(holder);
            if (isPrepared && !mediaPlayer.isPlaying()) {
                mediaPlayer.start();
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Handle surface size changes if needed
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceReady = false;
        if (mediaPlayer != null) {
            mediaPlayer.setDisplay(null);
        }
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
            isPrepared = false;
            updateHandler.removeCallbacks(updateSeekBar);
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mediaPlayer != null && !mediaPlayer.isPlaying() && isPrepared && surfaceReady) {
            mediaPlayer.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMediaPlayer();
        updateHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openFilePicker();
            } else {
                showToast("Permission denied");
            }
        }
    }
}
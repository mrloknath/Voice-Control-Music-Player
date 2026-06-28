package com.giriloknath4.musicplayer;


import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
//
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;

//import com.chibde.visualizer.BarVisualizer;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.jgabrielfreitas.core.BlurImageView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
//import java.util.Locale;
import java.util.Locale;
import java.util.Objects;

import java.io.IOException;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.StorageService;
import org.vosk.android.SpeechService;
import org.vosk.android.RecognitionListener;
import de.hdodenhof.circleimageview.CircleImageView;
import jp.wasabeef.recyclerview.adapters.ScaleInAnimationAdapter;

public class MainActivity extends AppCompatActivity {
    // members
    RecyclerView recyclerView;
    TabLayout tabLayout;
    SongAdapter songAdapter;
    List<Song> allSongs = new ArrayList<>();
    String currentSelectedTab = "All";
    String currentSearchQuery = "";
    ActivityResultLauncher<String> storagePermissionLauncher;
    ActivityResultLauncher<String> notificationPermissionLauncher;
    String permission = Manifest.permission.READ_EXTERNAL_STORAGE;
    //--------------------------------player activity----------------------------
    ExoPlayer player;
    ConstraintLayout playerView;
    TextView playerCloseBtn;
    //controls
    TextView songNameView, skipPreviousBtn, skipNextBtn, playPauseBtn, repeatModeBtn, playListBtn;
    TextView homeSongNameView, homeSkipPreviousBtn, homeSkipNextBtn, homePlayPauseBtn;
    //wrappers
    ConstraintLayout homeControlWrapper, headWrapper, artworkWrapper, seekbarWrapper, controlWrapper;// audioVisualizerWrapper;
    // artwork
    CircleImageView artworkView, homeArtworkView;
    //seek bar
    SeekBar seekbar;
    TextView progressView,durationView;
    // audio visualizer
    BlurImageView blurImageView;
    // status bar & navigation color
    int defaultStatusColor;
    //repeat mode
    int repeatMode = 1; //repeat All = 1, repeat one = 2, shuffle all = 3;
    //  is the act. bound ?
    boolean isBound = false;

//---------------------------------------wake up word detection and Speech recognizer---------------------------------------
    private Model voskModel = null;
    private SpeechService voskSpeechService = null;
    Button btnStart , btnStop;
    TextView textView;
    //--------------------------------------------Text to speech------------------------------------------------------
    TextToSpeech textToSpeech;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initStatusAndNavigationBar();
        initToolbar();
        initViews();
        setupWindowInsets();
        initPermissions();
        initServiceBinding();
        initButtons();
        initTextToSpeech();
        initVoskModel();
    }

    private void setupWindowInsets() {
        View rootView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());

            View appbar = findViewById(R.id.appbar);
            if (appbar != null) {
                appbar.setPadding(
                        appbar.getPaddingLeft(),
                        insets.top,
                        appbar.getPaddingRight(),
                        appbar.getPaddingBottom()
                );
            }

            View homeControlWrapper = findViewById(R.id.homeControlWrapper);
            if (homeControlWrapper != null) {
                homeControlWrapper.setPadding(
                        homeControlWrapper.getPaddingLeft(),
                        homeControlWrapper.getPaddingTop(),
                        homeControlWrapper.getPaddingRight(),
                        insets.bottom
                );
            }

            View headWrapper = findViewById(R.id.headWrapper);
            if (headWrapper != null) {
                headWrapper.setPadding(
                        headWrapper.getPaddingLeft(),
                        insets.top,
                        headWrapper.getPaddingRight(),
                        headWrapper.getPaddingBottom()
                );
            }

            View commandLayout = findViewById(R.id.commandLayout);
            if (commandLayout != null) {
                commandLayout.setPadding(
                        commandLayout.getPaddingLeft(),
                        commandLayout.getPaddingTop(),
                        commandLayout.getPaddingRight(),
                        insets.bottom
                );
            }

            return windowInsets;
        });
    }

    private void initStatusAndNavigationBar() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
        } else {
            View decor = window.getDecorView();
            decor.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            );
        }

        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);

        WindowInsetsControllerCompat controllerCompat =
                new WindowInsetsControllerCompat(window, window.getDecorView());
        controllerCompat.setAppearanceLightStatusBars(false);
        controllerCompat.setAppearanceLightNavigationBars(false);
    }


    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar())
                .setTitle(getResources().getString(R.string.app_name));
    }
    private void initViews() {

        recyclerView = findViewById(R.id.recyclerview);
        tabLayout = findViewById(R.id.tabLayout);

        playerView = findViewById(R.id.playerView);
        playerCloseBtn = findViewById(R.id.playerCloseBtn);
        songNameView = findViewById(R.id.songNameView);
        skipPreviousBtn = findViewById(R.id.skipPreviousBtn);
        skipNextBtn = findViewById(R.id.skipNextBtn);
        playPauseBtn = findViewById(R.id.playPauseBtn);
        repeatModeBtn = findViewById(R.id.repeatModeBtn);
        playListBtn = findViewById(R.id.playlistBtn);

        homeSongNameView = findViewById(R.id.homeSongNameView);
        homeSkipNextBtn = findViewById(R.id.homeSkipNextBtn);
        homeSkipPreviousBtn = findViewById(R.id.homeSkipPreviousBtn);
        homePlayPauseBtn = findViewById(R.id.homePlayPauseBtn);

        homeControlWrapper = findViewById(R.id.homeControlWrapper);
        headWrapper = findViewById(R.id.headWrapper);
        artworkWrapper = findViewById(R.id.artworkWrapper);
        seekbarWrapper = findViewById(R.id.seekbarWrapper);
        controlWrapper = findViewById(R.id.controlWrapper);

        artworkView = findViewById(R.id.artworkView);
        homeArtworkView = findViewById(R.id.homeArtworkView);
        blurImageView = findViewById(R.id.blurImageView);

        seekbar = findViewById(R.id.seekbar);
        progressView = findViewById(R.id.progressView);
        durationView = findViewById(R.id.durationView);

        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
        textView = findViewById(R.id.textView);

        btnStart.setVisibility(View.VISIBLE);
        btnStop.setVisibility(View.GONE);
    }
    private void initPermissions() {

        // 1. Decide permission based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_AUDIO;

            // Initialize notification permission launcher for Android 13+
            notificationPermissionLauncher =
                    registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                        // handled notification permission result
                    });

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        // 2. Initialize launcher
        storagePermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                    if (granted) {
                        fetchSongs();   // load songs
                    } else {
                        userResponse(); // show dialog / toast
                    }
                });

        // 3. Ask permission only if not already granted
        if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED) {

            // request
            storagePermissionLauncher.launch(permission);

        } else {
            // already granted
            fetchSongs();
        }
    }


    private void initServiceBinding() {
    Intent playerServiceIntent = new Intent(this,PlayerService.class);
        bindService(playerServiceIntent,playerServiceConnection, Context.BIND_AUTO_CREATE);
    }
    private void initButtons() {

        btnStart.setOnClickListener(v -> {
            if (!hasRecordPermission()) {
                requestRecordPermission();
            } else {
                btnStart.setVisibility(View.GONE);
                btnStop.setVisibility(View.VISIBLE);
                startVosk();
                Toast.makeText(this, "Start", Toast.LENGTH_SHORT).show();
            }
        });

        btnStop.setOnClickListener(v -> {
            btnStart.setVisibility(View.VISIBLE);
            btnStop.setVisibility(View.GONE);
            stopVosk();
            Toast.makeText(this, "Stop", Toast.LENGTH_SHORT).show();
        });
    }
    private void initTextToSpeech() {
        textToSpeech = new TextToSpeech(this, i -> {
            if (i != TextToSpeech.ERROR) {
                textToSpeech.setLanguage(Locale.US);
                textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        if ("LISTENING_UTTERANCE".equals(utteranceId)) {
                            runOnUiThread(() -> SpeakNow());
                        }
                    }

                    @Override
                    public void onError(String utteranceId) {
                        if ("LISTENING_UTTERANCE".equals(utteranceId)) {
                            runOnUiThread(() -> SpeakNow());
                        }
                    }
                });
            }
        });
    }


    ServiceConnection playerServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            // get the service instance
            PlayerService.ServiceBinder binder = (PlayerService.ServiceBinder) iBinder;
            player = binder.getPlayerService().player;
            isBound = true;
            // ready to show songs
            storagePermissionLauncher.launch(permission);
            // call player control method
            playerControls();
            syncPlayerUI();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    @Override
    public void onBackPressed() {
        // we say if the player view is visible , close it
        if(playerView.getVisibility() == View.VISIBLE) {
            exitPlayerView();
        }
        else {
            super.onBackPressed();
        }


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
        stopVosk();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isBound && player != null) {
            syncPlayerUI();
        }
    }

    private void syncPlayerUI() {
        if (player == null || player.getCurrentMediaItem() == null) {
            return;
        }
        com.google.android.exoplayer2.MediaItem mediaItem = player.getCurrentMediaItem();
        if (mediaItem != null && mediaItem.mediaMetadata.title != null) {
            songNameView.setText(mediaItem.mediaMetadata.title);
            homeSongNameView.setText(mediaItem.mediaMetadata.title);
        }
        progressView.setText(getReadableTime((int) player.getCurrentPosition()));
        durationView.setText(getReadableTime((int) player.getDuration()));
        seekbar.setMax((int) player.getDuration());
        seekbar.setProgress((int) player.getCurrentPosition());

        if (player.isPlaying()) {
            playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause_outline, 0, 0, 0);
            homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause, 0, 0, 0);
            artworkView.startAnimation(loadRotation());
        } else {
            playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play_outline, 0, 0, 0);
            homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play, 0, 0, 0);
            artworkView.clearAnimation();
        }

        showCurrentArtwork();
        updatePlayerPositionProgress();
        updatePlayerColors();
    }

    private void doUnbindService() {
        if(isBound){
            unbindService(playerServiceConnection);
            isBound = false;
        }
    }

    private void playerControls() {
        // song name marquee
        songNameView.setSelected(true);
        homeSongNameView.setSelected(true);
        //exit the player view
        playerCloseBtn.setOnClickListener(view -> exitPlayerView());
        playListBtn.setOnClickListener(view -> exitPlayerView());
        //open player view on home control wrapper click
        homeControlWrapper.setOnClickListener(view -> showPlayerView());

        //player listener
        player.addListener(new Player.Listener() {
            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                Player.Listener.super.onMediaItemTransition(mediaItem, reason);
                //show the playing song title
                assert  mediaItem != null;
                songNameView.setText(mediaItem.mediaMetadata.title);
                homeSongNameView.setText(mediaItem.mediaMetadata.title);

                progressView.setText(getReadableTime((int) player.getCurrentPosition()));
                seekbar.setProgress((int) player.getCurrentPosition());
                seekbar.setMax((int) player.getDuration());
                durationView.setText(getReadableTime((int) player.getDuration()));
                playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause_outline,0,0,0);
                homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause_outline,0,0,0);

                // show the current art work
                showCurrentArtwork();
                // update the progress position of a current playing song
                updatePlayerPositionProgress();
                //load the art work animation
                artworkView.setAnimation(loadRotation());
                //update player view colors
                updatePlayerColors();

                if (!player.isPlaying()){
                    player.play();
                }

            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                Player.Listener.super.onPlaybackStateChanged(playbackState);
                if(playbackState == ExoPlayer.STATE_READY){
                    //set values to player views
                    songNameView.setText(Objects.requireNonNull(player.getCurrentMediaItem()).mediaMetadata.title);
                    homeSongNameView.setText(player.getCurrentMediaItem().mediaMetadata.title);
                    progressView.setText(getReadableTime((int) player.getCurrentPosition()));
                    durationView.setText(getReadableTime((int) player.getDuration()));
                    seekbar.setMax((int) player.getDuration());
                    seekbar.setProgress((int) player.getCurrentPosition());
                    playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause_outline,0,0,0);
                    homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause_outline,0,0,0);


                    // show the current art work
                    showCurrentArtwork();
                    // update the progress position of a current playing song
                    updatePlayerPositionProgress();
                    //load the art work animation
                    artworkView.setAnimation(loadRotation());
                    //update player view colors
                    updatePlayerColors();
                }
                else {
                    playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play_outline,0,0,0);
                    homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play,0,0,0);

                }
            }
        });

        // skip to next step
        skipNextBtn.setOnClickListener(view -> skipToNextSong());
        homeSkipNextBtn.setOnClickListener(view -> skipToNextSong());

        //skip to previous track
        skipPreviousBtn.setOnClickListener(view -> skipToPreviousSong());
        homeSkipPreviousBtn.setOnClickListener(view -> skipToPreviousSong());

        // play or pause the player
        playPauseBtn.setOnClickListener(view -> playOrPausePlayer());
        homePlayPauseBtn.setOnClickListener(view -> playOrPausePlayer());

        // seekbar bar listener
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressValue = 0;
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                progressValue = seekBar.getProgress();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if(player.getPlaybackState() == ExoPlayer.STATE_READY){
                    seekBar.setProgress(progressValue);
                    progressView.setText(getReadableTime(progressValue));
                    player.seekTo(progressValue);
                }
            }
        });

        // repeat mode
        repeatModeBtn.setOnClickListener(view -> {
            if(repeatMode == 1){
                // repeat one
                player.setRepeatMode(ExoPlayer.REPEAT_MODE_ONE);
                repeatMode = 2;
                repeatModeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_repeat_one,0,0,0);
            } else if (repeatMode == 2) {
                // shuffle all
                player.setShuffleModeEnabled(true);
                player.setRepeatMode(ExoPlayer.REPEAT_MODE_ALL);
                repeatMode = 3;
                repeatModeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_shuffle,0,0,0);
            } else if (repeatMode == 3) {
                // repeat all
                player.setRepeatMode(ExoPlayer.REPEAT_MODE_ALL);
                player.setShuffleModeEnabled(false);
                repeatMode = 1;
                repeatModeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_repeat_all,0,0,0);
            }
            // update colors
            updatePlayerColors();
        });


    }

    private void playOrPausePlayer() {
        if(player.isPlaying()){
            player.pause();
            playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play_outline,0,0,0);
            homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play,0,0,0);
            artworkView.clearAnimation();
        }
        else {
            player.play();
            playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause_outline,0,0,0);
            homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause,0,0,0);
            artworkView.startAnimation(loadRotation());
        }

        // update player color
        updatePlayerColors();
    }

    private void skipToPreviousSong() {
        if(player.hasPreviousMediaItem()){
            player.seekToPrevious();
        }
    }

    private void skipToNextSong() {
        if(player.hasNextMediaItem()){
            player.seekToNext();
        }
    }

    private Animation loadRotation() {
        RotateAnimation rotateAnimation = new RotateAnimation(0,360,Animation.RELATIVE_TO_SELF,0.5f,Animation.RELATIVE_TO_SELF,0.5f);
        rotateAnimation.setInterpolator(new LinearInterpolator());
        rotateAnimation.setDuration(10000);
        rotateAnimation.setRepeatCount(Animation.INFINITE);
        return rotateAnimation;
    }

    private void updatePlayerPositionProgress() {
        new Handler().postDelayed(() -> {
            if (player.isPlaying()){
                progressView.setText(getReadableTime((int) player.getCurrentPosition()));
                seekbar.setProgress((int) player.getCurrentPosition());
            }

            //repeat calling the method
            updatePlayerPositionProgress();
        }, 1000);
    }

    private void showCurrentArtwork() {
        if (player != null && player.getCurrentMediaItem() != null && player.getCurrentMediaItem().mediaMetadata.artworkUri != null) {
            artworkView.setImageURI(player.getCurrentMediaItem().mediaMetadata.artworkUri);
            if (homeArtworkView != null) {
                homeArtworkView.setImageURI(player.getCurrentMediaItem().mediaMetadata.artworkUri);
            }
        }

        if (artworkView.getDrawable() == null){
            artworkView.setImageResource(R.drawable.default_artwork);
        }
        if (homeArtworkView != null && homeArtworkView.getDrawable() == null){
            homeArtworkView.setImageResource(R.drawable.default_artwork);
        }
    }

    String getReadableTime(int duration) {
        String time;
         int hrs =duration/(1000*60*60);
         int min = (duration%(1000*60*60))/(1000*60);
         int secs = ((duration%(1000*60*60))%(1000*60))/1000;


         if(hrs<1){     time = min +":"+secs;   }
         else {     time = hrs +":"+ min + ":"  +secs;     }

         return time;
    }

    private void updatePlayerColors() {
        // only player view is visible
        if(playerView.getVisibility() == View.GONE)
            return;

        BitmapDrawable bitmapDrawable = (BitmapDrawable) artworkView.getDrawable();
        if(bitmapDrawable == null){
            bitmapDrawable = (BitmapDrawable) ContextCompat.getDrawable(this,R.drawable.default_artwork);
        }

        assert bitmapDrawable != null;
        Bitmap bmp = bitmapDrawable.getBitmap();

        // set bitmap to blur image view
        blurImageView.setImageBitmap(bmp);
        blurImageView.setBlur(4);

        // player control colors
        Palette.from(bmp).generate(palette -> {

            if(palette != null){
                Palette.Swatch swatch = palette.getDarkVibrantSwatch();
                if(swatch == null){
                    swatch = palette.getMutedSwatch();
                    if(swatch == null){
                        swatch = palette.getDominantSwatch();
                    }
                }

                // extracts text colors
                assert swatch != null;
                int titleTextColor = swatch.getTitleTextColor();
                int bodyTextColor = swatch.getBodyTextColor();
                int rbgColor = swatch.getRgb();

                // set color to player views
                // status & navigation bar color
                getWindow().setStatusBarColor(Color.TRANSPARENT);
                getWindow().setNavigationBarColor(Color.TRANSPARENT);

                // more view colors
                songNameView.setTextColor(titleTextColor);
                playerCloseBtn.getCompoundDrawables()[0].setTint(titleTextColor);
                progressView.setTextColor(bodyTextColor);
                durationView.setTextColor(bodyTextColor);

                repeatModeBtn.getCompoundDrawables()[0].setTint(bodyTextColor);
                skipPreviousBtn.getCompoundDrawables()[0].setTint(bodyTextColor);
                skipNextBtn.getCompoundDrawables()[0].setTint(bodyTextColor);
                playPauseBtn.getCompoundDrawables()[0].setTint(titleTextColor);
                playListBtn.getCompoundDrawables()[0].setTint(bodyTextColor);
            }

        });
    }
    private void showPlayerView() {
        playerView.setVisibility(View.VISIBLE);
        updatePlayerColors();
    }



    private void exitPlayerView() {
        playerView.setVisibility(View.GONE);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
    }


    private void userResponse() {
        if(ContextCompat.checkSelfPermission(this,permission) == PackageManager.PERMISSION_GRANTED){
            // fetch the songs
            fetchSongs();
        }
        else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                if(shouldShowRequestPermissionRationale(permission)){
                    // alert dialog: show a  message (Education UI) to user explaining why app need this permission
                    new AlertDialog.Builder(this)
                            .setTitle("Requesting Permission")
                            .setMessage("Allow to fetch songs on your device")
                            .setPositiveButton("allow", (dialogInterface, i) -> {
                                //request permission
                                storagePermissionLauncher.launch(permission);
                            })
                            .setNegativeButton("cancel", (dialogInterface, i) -> {
                                Toast.makeText(getApplicationContext(), "You denied app to show songs", Toast.LENGTH_SHORT).show();
                                dialogInterface.dismiss();
                            })
                            .show();
                }
        }
        else {
            Toast.makeText(this, "You canceled to show message", Toast.LENGTH_SHORT).show();
        }

    }

    private void fetchSongs() {

        List<Song> songs = new ArrayList<>();
        Uri mediaStoreUri;

        // Android Q and above uses volume-based URIs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mediaStoreUri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        } else {
            mediaStoreUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }

        // Projections = columns you want to fetch
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.IS_MUSIC,
                MediaStore.Audio.Media.IS_NOTIFICATION,
                MediaStore.Audio.Media.IS_RINGTONE,
                MediaStore.Audio.Media.IS_ALARM,
                MediaStore.Audio.Media.IS_PODCAST,
                MediaStore.Audio.Media.DATA
        };

        // ⭐ BEST FILTER → RETURNS ALL AUDIO FILES WITH SIZE > 0
        String selection = MediaStore.Audio.Media.SIZE + " > 0";

        // Sort by recently added
        String sortOrder = MediaStore.Audio.Media.DATE_ADDED + " DESC";

        try (Cursor cursor = getContentResolver().query(
                mediaStoreUri,
                projection,
                selection,
                null,
                sortOrder
        )) {
            if (cursor == null) return;

            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
            int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
            int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE);
            int albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);

            int isMusicCol = cursor.getColumnIndex(MediaStore.Audio.Media.IS_MUSIC);
            int isNotifCol = cursor.getColumnIndex(MediaStore.Audio.Media.IS_NOTIFICATION);
            int isRingCol = cursor.getColumnIndex(MediaStore.Audio.Media.IS_RINGTONE);
            int isAlarmCol = cursor.getColumnIndex(MediaStore.Audio.Media.IS_ALARM);
            int isPodcastCol = cursor.getColumnIndex(MediaStore.Audio.Media.IS_PODCAST);
            int dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);

            while (cursor.moveToNext()) {

                long id = cursor.getLong(idColumn);
                String name = cursor.getString(nameColumn);
                int duration = cursor.getInt(durationColumn);
                long size = cursor.getLong(sizeColumn);
                long albumId = cursor.getLong(albumIdColumn);

                int isMusic = isMusicCol != -1 ? cursor.getInt(isMusicCol) : 0;
                int isNotif = isNotifCol != -1 ? cursor.getInt(isNotifCol) : 0;
                int isRing = isRingCol != -1 ? cursor.getInt(isRingCol) : 0;
                int isAlarm = isAlarmCol != -1 ? cursor.getInt(isAlarmCol) : 0;
                int isPodcast = isPodcastCol != -1 ? cursor.getInt(isPodcastCol) : 0;
                String filePath = dataCol != -1 ? cursor.getString(dataCol) : "";

                // file Uri
                Uri songUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                );

                // album artwork Uri
                Uri artworkUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"), albumId
                );

                String rawName = name;
                // Remove extension like .mp3 or .m4a safely
                if (name != null && name.contains(".")) {
                    name = name.substring(0, name.lastIndexOf("."));
                }

                String lowerPath = filePath != null ? filePath.toLowerCase() : "";
                String lowerName = rawName != null ? rawName.toLowerCase() : "";

                String audioType = "Songs";
                if (lowerPath.contains("recording") || lowerPath.contains("recorder") || lowerPath.contains("callrec") || lowerPath.contains("voice") || lowerName.startsWith("rec_") || lowerName.contains("recording")) {
                    audioType = "Recordings";
                } else if (isNotif == 1 || lowerPath.contains("notification")) {
                    audioType = "Notifications";
                } else if (isAlarm == 1 || lowerPath.contains("alarm")) {
                    audioType = "Alarms";
                } else if (isRing == 1 || lowerPath.contains("ringtone")) {
                    audioType = "Ringtones";
                } else if (isPodcast == 1 || lowerPath.contains("podcast")) {
                    audioType = "Podcasts";
                } else if (isMusic == 1) {
                    audioType = "Songs";
                }

                songs.add(new Song(name, songUri, artworkUri, size, duration, audioType));
            }

            showSongs(songs);
        }
    }


    private void showSongs(List<Song> songs) {

        if(songs.isEmpty()){
            Toast.makeText(this, "No Songs", Toast.LENGTH_SHORT).show();
            return;
        }

        //save songs
        allSongs.clear();
        allSongs.addAll(songs);

        //Setup dynamic tabs
        setupDynamicTabs(songs);

        // layout manager
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        //songs adapter
        songAdapter = new SongAdapter(this, songs, player, playerView);

        //recycler view animator methods
        ScaleInAnimationAdapter scaleInAnimationAdapter = new ScaleInAnimationAdapter(songAdapter);
        scaleInAnimationAdapter.setDuration(900);
        scaleInAnimationAdapter.setInterpolator(new OvershootInterpolator());
        scaleInAnimationAdapter.setFirstOnly(false);
        recyclerView.setAdapter(scaleInAnimationAdapter);

        applyCombinedFilters();
    }

    private void setupDynamicTabs(List<Song> songs) {
        if (tabLayout == null) return;

        Map<String, Integer> categoryCounts = new LinkedHashMap<>();
        categoryCounts.put("All", songs.size());
        categoryCounts.put("Songs", 0);
        categoryCounts.put("Recordings", 0);
        categoryCounts.put("Notifications", 0);
        categoryCounts.put("Alarms", 0);
        categoryCounts.put("Ringtones", 0);
        categoryCounts.put("Podcasts", 0);

        for (Song song : songs) {
            String type = song.getAudioType();
            if (categoryCounts.containsKey(type)) {
                categoryCounts.put(type, categoryCounts.get(type) + 1);
            } else {
                categoryCounts.put(type, 1);
            }
        }

        tabLayout.removeAllTabs();
        tabLayout.clearOnTabSelectedListeners();

        int selectedTabIndex = 0;
        int index = 0;

        for (Map.Entry<String, Integer> entry : categoryCounts.entrySet()) {
            String category = entry.getKey();
            int count = entry.getValue();

            // Only add tab if it has songs or is "All"
            if (count > 0 || category.equals("All")) {
                TabLayout.Tab tab = tabLayout.newTab();
                tab.setText(category + " (" + count + ")");
                tab.setTag(category);
                tabLayout.addTab(tab);

                if (category.equalsIgnoreCase(currentSelectedTab)) {
                    selectedTabIndex = index;
                }
                index++;
            }
        }

        if (tabLayout.getTabCount() > selectedTabIndex) {
            TabLayout.Tab tab = tabLayout.getTabAt(selectedTabIndex);
            if (tab != null) {
                tab.select();
            }
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getTag() != null) {
                    currentSelectedTab = (String) tab.getTag();
                    applyCombinedFilters();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    //setting the menu / search button
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.search_btn,menu);
        //search button item
        MenuItem menuItem= menu.findItem(R.id.searchBtn);
        SearchView searchView = (SearchView) menuItem.getActionView();
        // call search songs
        SearchSong(searchView);

        return super.onCreateOptionsMenu(menu);
    }

    private void SearchSong(SearchView searchView) {
        // search view listener
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                //filter songs
                filterSongs(newText.toLowerCase());
                return true;
            }
        });

    }


    private void filterSongs(String query) {
        currentSearchQuery = query;
        applyCombinedFilters();
    }

    private void applyCombinedFilters() {
        List<Song> filteredList = new ArrayList<>();

        if (allSongs.size() > 0) {
            for (Song song : allSongs) {
                boolean matchesTab = currentSelectedTab.equals("All") || song.getAudioType().equalsIgnoreCase(currentSelectedTab);
                boolean matchesQuery = currentSearchQuery.isEmpty() || song.getTitle().toLowerCase().contains(currentSearchQuery.toLowerCase());

                if (matchesTab && matchesQuery) {
                    filteredList.add(song);
                }
            }

            if (songAdapter != null) {
                songAdapter.filterSongs(filteredList);
            }

            String title = getResources().getString(R.string.app_name) + " (" + filteredList.size() + " Songs)";
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(title);
            }
        }
    }

    //------------------------------control through command-----------------------------------
    private void PlayerCommandControl(String totalCommand){

        if (totalCommand.contains("pause")) {
            text_to_speech("Okay, pause song");
            playOrPausePlayer();
        } else if (totalCommand.contains("next")) {
            text_to_speech("Okay, playing next song");
            skipToNextSong();
        } else if (totalCommand.contains("previous")) {
            text_to_speech("Okay, playing previous song");
            // to set seekbar at start position
            if(player.isPlaying())
                skipToPreviousSong();
            skipToPreviousSong();
        }else if(totalCommand.contains("play")) {
            text_to_speech("Okay,playing song");
            playOrPausePlayer();
        }
         else {
            text_to_speech("Sorry dear, This is invalid command , please try again");
            Toast.makeText(this, "Invalid Command", Toast.LENGTH_SHORT).show();
        }

    }


    //--------------------read command (If wake up word detected)------------------------------
    private  void SpeakNow(){
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"AI Listening .....");
        startActivityForResult(intent,111);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 111) {
            // Re-start Vosk if we are still in listening mode (Stop button visible)
            if (btnStop.getVisibility() == View.VISIBLE) {
                startVosk();
            }
            if (resultCode == RESULT_OK && data != null) {
                try {
                    PlayerCommandControl(data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).get(0).toLowerCase());
                } catch (NullPointerException e) {
                    Toast.makeText(this, "Null Pointer Exception " + e, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    // -----------------------------wakeup word detection---------------

    private void initVoskModel() {
        StorageService.unpack(this, "model-en-us", "model",
                model -> {
                    voskModel = model;
                    Toast.makeText(MainActivity.this, "Speech recognition model loaded", Toast.LENGTH_SHORT).show();
                    // If start button is already clicked (or permission already active and btnStop is visible), start it
                    if (btnStop.getVisibility() == View.VISIBLE && hasRecordPermission()) {
                        startVosk();
                    }
                },
                exception -> Toast.makeText(MainActivity.this, "Failed to unpack model: " + exception.getMessage(), Toast.LENGTH_LONG).show()
        );
    }

    private void startVosk() {
        if (voskModel == null) {
            Toast.makeText(this, "Vosk model is still loading, please wait...", Toast.LENGTH_SHORT).show();
            return;
        }

        if (voskSpeechService != null) {
            // Already running
            return;
        }

        try {
            // Restrict vocabulary to "computer" and unknown noise "[unk]" to act as KWS
            String grammar = "[\"computer\", \"[unk]\"]";
            Recognizer recognizer = new Recognizer(voskModel, 16000.0f, grammar);
            voskSpeechService = new SpeechService(recognizer, 16000.0f);
            voskSpeechService.startListening(voskListener);
        } catch (IOException e) {
            Toast.makeText(this, "Failed to start Vosk: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void stopVosk() {
        if (voskSpeechService != null) {
            voskSpeechService.stop();
            voskSpeechService.shutdown();
            voskSpeechService = null;
        }
    }

    private final RecognitionListener voskListener = new RecognitionListener() {
        @Override
        public void onResult(String hypothesis) {
            handleHypothesis(hypothesis);
        }

        @Override
        public void onPartialResult(String hypothesis) {
            handleHypothesis(hypothesis);
        }

        @Override
        public void onFinalResult(String hypothesis) {
            handleHypothesis(hypothesis);
        }

        @Override
        public void onError(Exception exception) {
            Toast.makeText(MainActivity.this, "Vosk Error: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onTimeout() {
            // Resume/restart listening if it times out
            if (btnStop.getVisibility() == View.VISIBLE) {
                stopVosk();
                startVosk();
            }
        }
    };

    private void handleHypothesis(String hypothesis) {
        if (hypothesis != null && hypothesis.contains("\"computer\"")) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Pause Vosk to release the microphone for SpeechRecognizer dialog
                    stopVosk();
                    text_to_speech("Listening", "LISTENING_UTTERANCE");
                }
            });
        }
    }

    private boolean hasRecordPermission() {
        return ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED;
    }

    private void requestRecordPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 0);
    }


    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length == 0 || grantResults[0] == PackageManager.PERMISSION_DENIED) {
            Toast.makeText(this, "Microphone permission is required for this demo", Toast.LENGTH_SHORT).show();
        } else {
            startVosk();
        }
    }

    private void text_to_speech(String s){
        if (textToSpeech != null) {
            textToSpeech.speak(s, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void text_to_speech(String s, String utteranceId){
        if (textToSpeech != null) {
            int result = textToSpeech.speak(s, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
            if (result == TextToSpeech.ERROR) {
                if ("LISTENING_UTTERANCE".equals(utteranceId)) {
                    SpeakNow();
                }
            }
        } else {
            if ("LISTENING_UTTERANCE".equals(utteranceId)) {
                SpeakNow();
            }
        }
    }

}

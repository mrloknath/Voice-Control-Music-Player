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
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

//import com.chibde.visualizer.BarVisualizer;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.jgabrielfreitas.core.BlurImageView;

import java.util.ArrayList;
import java.util.List;
//import java.util.Locale;
import java.util.Locale;
import java.util.Objects;

import ai.picovoice.porcupine.PorcupineActivationException;
import ai.picovoice.porcupine.PorcupineActivationLimitException;
import ai.picovoice.porcupine.PorcupineActivationRefusedException;
import ai.picovoice.porcupine.PorcupineActivationThrottledException;
import ai.picovoice.porcupine.PorcupineException;
import ai.picovoice.porcupine.PorcupineInvalidArgumentException;
import ai.picovoice.porcupine.PorcupineManager;
import ai.picovoice.porcupine.PorcupineManagerCallback;
import de.hdodenhof.circleimageview.CircleImageView;
import jp.wasabeef.recyclerview.adapters.ScaleInAnimationAdapter;

public class MainActivity extends AppCompatActivity {
    // members
    RecyclerView recyclerView;
    SongAdapter songAdapter;
    List<Song> allSongs = new ArrayList<>();
    ActivityResultLauncher<String> storagePermissionLauncher;
    final String permission = Manifest.permission.READ_EXTERNAL_STORAGE;
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
    CircleImageView artworkView;
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
private static final String ACCESS_KEY = "RMrVGt9VecjKf+qDziomneZlkGP92Yxeospv/5iN6tC+eBHZ6r8VBA==";
    private PorcupineManager porcupineManager = null;
    Button btnStart , btnStop;
    TextView textView;
    //--------------------------------------------Text to speech------------------------------------------------------
    TextToSpeech textToSpeech;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // save the status color
        defaultStatusColor = getWindow().getStatusBarColor();
        //set the navigation color
        getWindow().setNavigationBarColor(ColorUtils.setAlphaComponent(defaultStatusColor,199)); // 0 & 255
        
        // set the tool bar , and app title
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle(getResources().getString(R.string.app_name));
        
        // recyclerview
        recyclerView = findViewById(R.id.recyclerview);
        storagePermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted->{
            if(granted){
                fetchSongs();

            }
            else {
                userResponse();
            }
        });

        // Here launch storage permission
        storagePermissionLauncher.launch(permission);


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
        seekbar = findViewById(R.id.seekbar);
        progressView = findViewById(R.id.progressView);
        durationView = findViewById(R.id.durationView);

        //blur image view as the background of playing screen
        blurImageView = findViewById(R.id.blurImageView);

        // Bind Player Service and do every thing after the binding
        doBindService();

        //-----------------------initialize button and visibility---------------------------
        btnStart = findViewById(R.id.btnStart);
        btnStart.setVisibility(View.VISIBLE);
        btnStop  = findViewById(R.id.btnStop);
        btnStop.setVisibility(View.GONE);

        textView = findViewById(R.id.textView);

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!hasRecordPermission()){
                    requestRecordPermission();
                }
                else {
                    btnStart.setVisibility(View.GONE);
                    btnStop.setVisibility(View.VISIBLE);
                    startPorcupine();
                    Toast.makeText(MainActivity.this, "Start", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(porcupineManager != null) {
                    btnStart.setVisibility(View.VISIBLE);
                    btnStop.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "Stop", Toast.LENGTH_SHORT).show();
                    stopPorcupine();
                }
            }
        });

        //------------------------text to speech------------------------------------------
        textToSpeech=new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if (i != TextToSpeech.ERROR)
                {
                    textToSpeech.setLanguage(Locale.CHINESE);
                }

            }
        });

        
    }

    private void doBindService() {
        Intent playerServiceIntent = new Intent(this,PlayerService.class);
        bindService(playerServiceIntent,playerServiceConnection, Context.BIND_AUTO_CREATE);
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
        artworkView.setImageURI(Objects.requireNonNull(player.getCurrentMediaItem()).mediaMetadata.artworkUri);

        if (artworkView.getDrawable() ==null){
            artworkView.setImageResource(R.drawable.default_artwork);
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
                getWindow().setStatusBarColor(rbgColor);
                getWindow().setNavigationBarColor(rbgColor);

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
        getWindow().setStatusBarColor(defaultStatusColor);
        getWindow().setNavigationBarColor(ColorUtils.setAlphaComponent(defaultStatusColor,199));
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
        // define a list to carry songs
        List<Song> songs = new ArrayList<>();
        Uri mediaStoreUri;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            mediaStoreUri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        }
        else{
            mediaStoreUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }

        //define projection
        String[] projection = new String[]{
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.ALBUM_ID,
        };

        //Order
        String sortOrder = MediaStore.Audio.Media.DATE_ADDED + " DESC";

        // get the songs
        try(Cursor cursor = getContentResolver().query(mediaStoreUri,projection,null,null,sortOrder)){
            // cache cursor indices
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
            int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
            int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE);
            int albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);

            //clear the previous loaded before adding loading again
            while(cursor.moveToNext()){
                //get the values of a column for a given audio file
                long id = cursor.getLong(idColumn);
                String name = cursor.getString(nameColumn);
                int duration = cursor.getInt(durationColumn);
                int size = cursor.getInt(sizeColumn);
                long albumId = cursor.getLong(albumIdColumn);

                // song uri
                Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,id);

                //album art work uri
                Uri albumArtworkUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"),albumId);

                // remove .mp3 extension from the songs name
                name = name.substring(0,name.lastIndexOf("."));

                // song item
                Song song = new Song(name,uri,albumArtworkUri,size,duration);

                // add songs item to songs list
                songs.add(song);
            }

            //display songs
            showSongs(songs);

        }
    }

    private void showSongs(List<Song> songs) {

        if(songs.size() == 0){
            Toast.makeText(this, "No Songs", Toast.LENGTH_SHORT).show();
            return;
        }

        //save songs
        allSongs.clear();
        allSongs.addAll(songs);

        //Update the tool bar title
        String title = getResources().getString(R.string.app_name) + " ("+songs.size() +" Songs) ";
        Objects.requireNonNull(getSupportActionBar()).setTitle(title);

        // layout manager
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        //songs adapter
        songAdapter = new SongAdapter(this,songs,player,playerView);

        // set the adapter to recyclerview


        //recycler view animator methods
        ScaleInAnimationAdapter scaleInAnimationAdapter = new ScaleInAnimationAdapter(songAdapter);
        scaleInAnimationAdapter.setDuration(900);
        scaleInAnimationAdapter.setInterpolator(new OvershootInterpolator());
        scaleInAnimationAdapter.setFirstOnly(false);
        recyclerView.setAdapter(scaleInAnimationAdapter);
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
    List<Song> filteredList = new ArrayList<>();

    if(allSongs.size() >0){
        for (Song song : allSongs){
            if (song.getTitle().toLowerCase().contains(query)){
                filteredList.add(song);
            }
        }

        if (songAdapter != null){
            songAdapter.filterSongs(filteredList);
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

        if(requestCode == 111 && resultCode == RESULT_OK){
            try {
                //textView.setText(data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).get(0));
                PlayerCommandControl(data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).get(0).toLowerCase());

            }catch (NullPointerException e){
                Toast.makeText(this, "Null Pointer Exception "+e, Toast.LENGTH_SHORT).show();
            }
        }
    }

    // -----------------------------wakeup word detection---------------

    private void startPorcupine() {
        try {
            final String keywordName ="computer";

            String keyword = keywordName.toLowerCase().replace(" ", "_") + ".ppn";
            porcupineManager = new PorcupineManager.Builder()
                    .setAccessKey(ACCESS_KEY)
                    .setKeywordPath(keyword)
                    .setSensitivity(0.7f)
                    .build(getApplicationContext(), porcupineManagerCallback);
            porcupineManager.start();
        } catch (PorcupineInvalidArgumentException e) {
            String s = String.format("%s\nEnsure your accessKey '%s' is a valid access key.", e.getMessage(), ACCESS_KEY) ;
            Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
        } catch (PorcupineActivationException e) {
            Toast.makeText(this, "AccessKey activation error", Toast.LENGTH_SHORT).show();
        } catch (PorcupineActivationLimitException e) {
            Toast.makeText(this, "AccessKey reached its device limit", Toast.LENGTH_SHORT).show();
        } catch (PorcupineActivationRefusedException e) {
            Toast.makeText(this, "AccessKey refused", Toast.LENGTH_SHORT).show();
        } catch (PorcupineActivationThrottledException e) {
            Toast.makeText(this, "AccessKey has been throttled", Toast.LENGTH_SHORT).show();
        } catch (PorcupineException e) {
            Toast.makeText(this, "Failed to initialize Porcupine " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    private void stopPorcupine() {
        if (porcupineManager != null) {
            try {
                porcupineManager.stop();
                porcupineManager.delete();
            } catch (PorcupineException e) {
                Toast.makeText(this, " Failed to stop Porcupine.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private final PorcupineManagerCallback porcupineManagerCallback = new PorcupineManagerCallback() {
        @Override
        public void invoke(int keywordIndex) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Toast.makeText(MainActivity.this, " Detected ", Toast.LENGTH_SHORT).show();
                    text_to_speech("Listening");
                    SpeakNow();
                }
            });
        }
    };


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
            startPorcupine();
        }
    }

    private void text_to_speech(String s){
        textToSpeech.speak(s,TextToSpeech.QUEUE_FLUSH,null);
    }

}

package com.giriloknath4.musicplayer;

//import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
//import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
//import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.ExoPlayer;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    //members
    Context context;
    List<Song> songs;
    ExoPlayer player;
    ConstraintLayout playerView;

    // constructor
    public SongAdapter(Context context, List<Song> songs, ExoPlayer player, ConstraintLayout playerView) {
        this.context = context;
        this.songs = songs;
        this.player = player;
        this.playerView = playerView;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        //inflate song row items layout
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.song_row_item, parent,false);


        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        //current song and view holder
        Song song = songs.get(position);
        SongViewHolder viewHolder = (SongViewHolder) holder;

        //set values to views
        viewHolder.titleHolder.setText(song.getTitle());
        viewHolder.durationHolder.setText(Helper.getDuration(song.getDuration()));
        viewHolder.sizeHolder.setText(Helper.getSize(song.getSize()));

        //artwork
        Uri artworkUri = song.getArtworkUri();

        if(artworkUri != null){
            //set the uri to image view
            viewHolder.artWorkHolder.setImageURI(artworkUri);

            // make sure that the uri has an artwork
            if(viewHolder.artWorkHolder.getDrawable() == null){
                viewHolder.artWorkHolder.setImageResource(R.drawable.default_artwork);

            }
        }

        // play the song on item click
        viewHolder.itemView.setOnClickListener(view -> {

            // Start PlayerService safely on all Android versions
            Intent serviceIntent = new Intent(context, PlayerService.class);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }

            // Show player view
            playerView.setVisibility(View.VISIBLE);

            // Handle item click playback
            if (!player.isPlaying()) {
                player.setMediaItems(Helper.getMediaItems(songs), position, 0);
            } else {
                player.pause();
                player.seekTo(position, 0);
            }

            player.prepare();
            player.play();

            Toast.makeText(context, song.getTitle(), Toast.LENGTH_SHORT).show();
        });

    }


    @Override
    public int getItemCount() {

        return songs.size();
    }

    //filter songs search results
    @SuppressLint("NotifyDataSetChanged")
    public void filterSongs(List<Song> filteredList){
        songs = filteredList;
        notifyDataSetChanged();

    }

}

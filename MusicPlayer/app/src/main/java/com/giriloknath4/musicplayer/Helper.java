package com.giriloknath4.musicplayer;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.MediaMetadata;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class Helper {



    public static List<MediaItem> getMediaItems(List<Song> songs) {
        // define a list of media items
        List<MediaItem> mediaItems = new ArrayList<>();

        for (Song song: songs) {
            MediaItem mediaItem = new MediaItem.Builder()
                    .setUri(song.getUri())
                    .setMediaMetadata(getMetadata(song))
                    .build();
            //add the media item to media items list
            mediaItems.add(mediaItem);
        }

        return  mediaItems;
    }

    public static MediaMetadata getMetadata(Song song) {

        return new MediaMetadata.Builder()
                .setTitle(song.getTitle())
                .setArtworkUri(song.getArtworkUri())
                .build();
    }

    // duration in hours minutes second
    public static String getDuration(int totalDuration){

        String totalDurationText;

        int hrs = totalDuration/(1000*60*60);
        int min = (totalDuration%(1000*60*60))/(1000*60);
        int secs = (((totalDuration%(1000*60*60))%(1000*60)))/1000;

        if(hrs < 1){
            totalDurationText =  " " +min+":"+secs;  //String.format("%02d:%02d",min,secs);
        }
        else {
            totalDurationText =  " " + hrs + ":" + min +":" + secs; // String.format("%02d:%02d:%02d",hrs,min,secs);
        }

        return totalDurationText;
    }

    //size in MB
    public static String getSize(long bytes){
        String hrSize;

        double k = bytes/1024.0;
        double m = ((bytes/1024.0)/1024.0);
        double g = (((bytes/1024.0)/1024.0)/1024.0);
        double t = ((((bytes/1024.0)/1024.0)/1024.0)/1024.0);

        //decimal format
        DecimalFormat dec = new DecimalFormat("0.00");

        if(t>1){
            hrSize = dec.format(t).concat(" TB");
        }
        else if(g>1){
            hrSize = dec.format(g).concat(" GB");
        }
        else if(m>1){
            hrSize = dec.format(m).concat(" MB");
        }
        else if(k>1){
            hrSize = dec.format(k).concat(" KB");
        }
        else{
            hrSize = dec.format(g).concat(" Bytes");
        }

        return  hrSize;

    }
}

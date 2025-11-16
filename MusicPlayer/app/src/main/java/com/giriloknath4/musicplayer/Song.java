package com.giriloknath4.musicplayer;

import android.net.Uri;

public class Song {
    //members
    String title;
    Uri uri;
    Uri artworkUri;
    long size;
    int duration;

    //constructor

    public Song(String title, Uri uri, Uri artworkUri, long size, int duration) {
        this.title = title;
        this.uri = uri;
        this.artworkUri = artworkUri;
        this.size = size;
        this.duration = duration;
    }

    //getters

    public String getTitle() {
        return title;
    }

    public Uri getUri() {
        return uri;
    }

    public Uri getArtworkUri() {
        return artworkUri;
    }

    public long getSize() {
        return size;
    }

    public int getDuration() {
        return duration;
    }

    //setters

    public void setTitle(String title) {
        this.title=title;
    }

    public void setUri(Uri uri) {
        this.uri=uri;
    }

    public void setArtworkUri(Uri artworkUri) {
        this.artworkUri=artworkUri;
    }

    public void setSize(long size) {
        this.size=size;
    }

    public void setDuration(int duration) {
        this.duration=duration;
    }
}

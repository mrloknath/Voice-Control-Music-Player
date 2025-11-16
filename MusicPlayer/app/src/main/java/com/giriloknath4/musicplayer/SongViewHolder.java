package com.giriloknath4.musicplayer;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

//view holder class
public class SongViewHolder extends RecyclerView.ViewHolder{

    //members
    ImageView artWorkHolder;
    TextView titleHolder,durationHolder,sizeHolder;


    public SongViewHolder(@NonNull View itemView) {
        super(itemView);

        artWorkHolder = itemView.findViewById(R.id.artworkView);
        titleHolder = itemView.findViewById(R.id.titleView);
        durationHolder = itemView.findViewById(R.id.durationView);
        sizeHolder = itemView.findViewById(R.id.sizeView);
    }
}

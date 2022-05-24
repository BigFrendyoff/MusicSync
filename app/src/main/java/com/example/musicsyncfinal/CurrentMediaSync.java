package com.example.musicsyncfinal;

import android.graphics.Bitmap;
import android.media.Image;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.net.Uri;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.net.URI;

public class CurrentMediaSync{
    private MediaController mediaController;
    public CurrentMediaSync(MediaController mediaController){
        this.mediaController = mediaController;
    }

    public void Play(){
        mediaController.getTransportControls().play();
    }
    public void Pause(){
        mediaController.getTransportControls().pause();
    }
    public String getTrack(){
        if (mediaController != null) {
            return mediaController.getMetadata().getString(MediaMetadata.METADATA_KEY_TITLE);
        }
        else{
            return "Сейчас ничего не играет";
        }
    }
    public Bitmap getTrackIcon(){
        return mediaController.getMetadata().getBitmap(MediaMetadata.METADATA_KEY_ART);
    }
    public String getArtist(){
        return mediaController.getMetadata().getString(MediaMetadata.METADATA_KEY_ARTIST);
    }
   public String getTrackPosition(){
      return String.valueOf(mediaController.getPlaybackState().getPosition());
   }

}

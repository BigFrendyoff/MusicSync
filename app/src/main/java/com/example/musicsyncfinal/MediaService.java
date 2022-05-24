package com.example.musicsyncfinal;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.session.MediaController;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.media.MediaBrowserCompat;
import android.util.Log;

import androidx.annotation.Nullable;

public class MediaService extends Service {
    MediaPlayer localMediaPlayer;
    Uri track_uri;
    IBinder binder = new LocalBinder();


    public class LocalBinder extends Binder{
        MediaService getService() {
            return MediaService.this;
        }
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        track_uri = Uri.parse(intent.getStringExtra("track uri"));

        Log.d("CRT-D", "binder created" + track_uri.toString());
        localMediaPlayer = MediaPlayer.create(this, track_uri);

        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        localMediaPlayer.stop();
    }
    public void setTrack_uri(Uri uri){
        track_uri = uri;
    }

    public void Play(){
        localMediaPlayer.start();
    }
    public void Pause(){
        localMediaPlayer.pause();
    }
    public void setPosition(int pos){
        localMediaPlayer.seekTo(pos);
    }


}

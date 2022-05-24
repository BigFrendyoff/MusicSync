package com.example.musicsyncfinal.ui.home;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.example.musicsyncfinal.NotificationListener;

import java.util.List;

import javax.crypto.Cipher;

public class MediaInfo {
    private final String TAG = "MSessions";


    NotificationListener notificationListener = new NotificationListener();
    Context context = null;
    MediaSessionManager mediaSessionManager;
    ComponentName nListener;
    MediaSessionManager.OnActiveSessionsChangedListener onActiveSessionsChangedListener
            = new MediaSessionListener(notificationListener);

    MediaInfo(Context c){
        this.context = c;
    }
    class MediaSessionListener implements MediaSessionManager.OnActiveSessionsChangedListener{

        NotificationListener notificationListener;

        MediaSessionListener(NotificationListener notificationListener) {
            this.notificationListener = notificationListener;
        }


        @Override
        public void onActiveSessionsChanged(@Nullable List<MediaController> controllers) {
            Log.d(TAG, "Sessions changed in Media info");
        }
    }

    public void initialize(){
        nListener = new ComponentName(context, NotificationListener.class);
        mediaSessionManager = (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);
    }

    public List<MediaController> getSessions(){
        return mediaSessionManager.getActiveSessions(nListener);
    }



}

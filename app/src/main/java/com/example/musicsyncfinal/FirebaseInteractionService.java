package com.example.musicsyncfinal;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.security.Provider;

public class FirebaseInteractionService extends Service {
    IBinder binder = new FirebaseLocalBinder();

    public class FirebaseLocalBinder extends Binder{
        FirebaseInteractionService getService(){return FirebaseInteractionService.this;}
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {


        return binder;
    }

    public void startPlaybackControllerListener(String connectedUser, String role,
                                                MediaService mService,
                                                FirebaseDatabase database,
                                                CurrentMediaSync currentMediaSync){
        ValueEventListener plbControlListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getValue().toString().equals("play")){
                    Log.d("CRT", "This is play");
                    if (role == "host") {
                        currentMediaSync.Play();
                    }
                    if (role == "client"){
                        mService.Play();

                    }
                }
                if (snapshot.getValue().toString().equals("pause")){
                    if (role == "host"){
                        currentMediaSync.Pause();
                    }
                    if (role == "client") {
                        mService.Pause();
                    }

                }
            }


            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        database.getReference().child("users").child(connectedUser).child("last command")
                .addValueEventListener(plbControlListener);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }
}

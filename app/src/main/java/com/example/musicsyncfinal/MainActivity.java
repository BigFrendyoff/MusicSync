package com.example.musicsyncfinal;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.session.MediaController;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.StrictMode;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.example.musicsyncfinal.ui.home.MediaAdapter;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.musicsyncfinal.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private final String TAG = "MSessions";
    private List<MediaController> mediaControllers;
    private MediaAdapter mediaAdapter;
    private CurrentMediaSync currentMediaSync = null;
    private FirebaseUser user;
    private boolean isConnectionEstablished = false;
    private boolean isSyncComplete = false;
    private boolean isInviteSended = false;
    private boolean firstEncounter = true;
    private boolean mBound = false;
    private boolean fBound = false;
    private String currentConnectedUser;
    private FirebaseDatabase database = FirebaseDatabase.getInstance("https://musicsyncfinal-1651651889018-default-rtdb.europe-west1.firebasedatabase.app/");
    private DatabaseRedactor redactor = new DatabaseRedactor(database);;
    private FirebaseStorage storage = FirebaseStorage.getInstance("gs://musicsyncfinal-1651651889018.appspot.com");
    private String role = "no roles";
    private MediaService mService;
    private FirebaseInteractionService fbInService;
    private int trackPosition;
    private String trackName = "Ничего не играет";


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!isNotificationAllowed()) {
            startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
        }
        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.MEDIA_CONTENT_CONTROL}, 200);

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());



        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                 R.id.navigation_connection, R.id.navigation_session, R.id.navigation_music)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
        currentConnectedUser = "NaU";

        BroadcastReceiver inviteReciever = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ValueEventListener acceptionListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Log.d("CRT", "Data changed");
                        if (Objects.requireNonNull(snapshot.getValue()).toString().equals("accepted")){
                            Toast.makeText(getApplicationContext(), "Приглашение принято", Toast.LENGTH_SHORT).show();
                            role = "host";
                            isConnectionEstablished = true;
                            currentConnectedUser = intent.getExtras().getString("user");
                            CompleteSyncInfoExchange(intent.getExtras().getString("user"));

                        }
                        else if (snapshot.getValue().toString().equals("declined")){
                            Toast.makeText(getApplicationContext(), "Приглашение отклонено", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                };
                database.getReference().child("users").child(intent.getExtras().getString("user"))
                        .child("invite_acception")
                        .addValueEventListener(acceptionListener);
                currentConnectedUser = intent.getExtras().getString("user");
            }
        };
        registerReceiver(inviteReciever, new IntentFilter("invitation"));

        BroadcastReceiver startCommandReciever = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                startFirebaseInteractionService();
            }
        };
        registerReceiver(startCommandReciever, new IntentFilter("start controller"));



//        mediaInfo.mediaSessionManager.addOnActiveSessionsChangedListener(mediaInfo.onActiveSessionsChangedListener, mediaInfo.nListener);



    }

    @Override
    protected void onResume() {
        super.onResume();
        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user!=null && firstEncounter){
            Map<String, Object> toPush = new HashMap<>();
            toPush.put("email", user.getEmail());
            toPush.put("name", user.getDisplayName());
                toPush.put("sync", "not synchronized");
                toPush.put("connected user", "none");
                toPush.put("track position", "none");
                toPush.put("last command", "none");
                toPush.put("invite_acception", "not invited");
                toPush.put("invited by", "none");
                toPush.put("track in storage", "none");
            redactor.PushData("users", user.getDisplayName(), toPush);
            firstEncounter = false;
            ValueEventListener invitesListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Log.d("CRT", "incoming invite");
                    if (!snapshot.getValue().toString().equals("none")){
                        InvitationDialogBuilder(snapshot.getValue().toString()).show();
                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };
            database.getReference().child("users").child(user.getDisplayName()).child("invited by").addValueEventListener(invitesListener);



        }
        if (isInviteSended){
            Log.d("CRT", "Invite sended and on listen");

        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("CRT", "Paused");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("CRT", "Started again");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        user = FirebaseAuth.getInstance().getCurrentUser();
        redactor.DeleteData("users", user.getDisplayName());
        Log.d("CRT-D", "Destroyed");
    }

    private boolean isNotificationAllowed(){
        ContentResolver contentResolver = getContentResolver();
        String enabledNotificationListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners");
        String packageName = getPackageName();
        return enabledNotificationListeners != null && enabledNotificationListeners.contains(packageName);
    }

    public CurrentMediaSync getCurrentMediaSync(){
        Log.d("CRT", "Method invoked");
        return currentMediaSync;
    }
    public void setCurrentMedia(CurrentMediaSync media){
        this.currentMediaSync = media;
    }

    public Dialog InvitationDialogBuilder(String invName){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(invName + " предлагает синхронизироваться")
                .setPositiveButton("Принять", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        redactor.RedactFields("users", user.getDisplayName(), "invite_acception", "accepted");
                        redactor.RedactFields("users", user.getDisplayName(), "connected user", invName);
                        isConnectionEstablished = true;
                        currentConnectedUser = invName;
                        role = "client";
                        StartFileExchange(invName);

                    }
                })
                .setNegativeButton("Отклонить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        redactor.RedactFields("users", user.getDisplayName(), "invite_acception", "declined");
                    }
                });
        return builder.create();
    }
    public boolean getIsConnectionEstablished(){
        return isConnectionEstablished;
    }
    public String getCurrentConnectedUser(){
        return currentConnectedUser;
    }

    public void setIsInviteSended(String user){
        isInviteSended = true;
        currentConnectedUser = user;
    }
    public void StartFileExchange(String name){
        ValueEventListener mediFileListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.getValue().toString().equals("none")){
                    trackName = snapshot.getValue().toString();
                    Log.d("CRT-D", "Found file");
                    Toast.makeText(getApplicationContext(), "Загружаем файл...",
                            Toast.LENGTH_SHORT).show();
                    StorageReference mediaRef = storage.getReference().child(name + "/")
                            .child(snapshot.getValue().toString());
                    database.getReference().child("users").child(name)
                            .child("track position").get().addOnCompleteListener(task->{
                       trackPosition = Integer.parseInt(task.getResult().getValue().toString());
                    });
                    try {
                        File localFile = File.createTempFile(snapshot.getValue().toString(), ".mp3");
                        mediaRef.getFile(localFile)
                                .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {

                                Log.d("CRT-D", "File downloaded");
                                Toast.makeText(getApplicationContext(), "Файл загружен",
                                        Toast.LENGTH_SHORT).show();
                                Uri musicUri = Uri.fromFile(localFile);

                                startMediaService(musicUri);
                                startFirebaseInteractionService();
                                isSyncComplete = true;
                                redactor.RedactFields("users", user.getDisplayName(), "sync", "synced");
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        database.getReference().child("users").child(name).child("track in storage")
                .addValueEventListener(mediFileListener);

    }
    public void startMediaService(Uri trackUri){
        Log.d("CRT-D", "Starting media service");
        Intent mediaServiceActivation = new Intent(this, MediaService.class);
        mediaServiceActivation.putExtra("track uri", trackUri.toString());
        ServiceConnection connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.d("CRT-D", "Service connected");
                MediaService.LocalBinder binder = (MediaService.LocalBinder) service;
                mService = binder.getService();
                mService.Play();
                mService.setPosition(trackPosition);
                mService.Pause();
                mBound = true;

            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mBound = false;
            }
        };

        bindService(mediaServiceActivation, connection, BIND_AUTO_CREATE);

    }
    public void startFirebaseInteractionService(){
        Intent firebaseInteractionServiceActivation = new Intent(this, FirebaseInteractionService.class);
        ServiceConnection connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                FirebaseInteractionService.FirebaseLocalBinder binder = (FirebaseInteractionService.FirebaseLocalBinder) service;
                fbInService = binder.getService();
                fbInService.startPlaybackControllerListener(currentConnectedUser, role, mService, database, currentMediaSync);
                fBound = true;
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                fBound = false;
            }
        };

        bindService(firebaseInteractionServiceActivation, connection, BIND_AUTO_CREATE);
    }

    public void CompleteSyncInfoExchange(String name){
        ValueEventListener syncEvent = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getValue().toString().equals("synced")){
                    isSyncComplete = true;
                    Toast.makeText(getApplicationContext(), "Синхронизация завершена",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        database.getReference().child("users").child(name).child("sync")
                .addValueEventListener(syncEvent);
    }

    public boolean isSyncComplete(){
        return isSyncComplete;
    }

    public MediaService getMService(){
        return mService;
    }

    public String getTrackName(){
        return trackName;
    }


}
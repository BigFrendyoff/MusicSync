package com.example.musicsyncfinal.ui.home;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.media.session.MediaController;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicsyncfinal.CurrentMediaSync;
import com.example.musicsyncfinal.DatabaseRedactor;
import com.example.musicsyncfinal.MainActivity;
import com.example.musicsyncfinal.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

public class MediaAdapter extends RecyclerView.Adapter<MediaAdapter.MediaInfoViewHolder> {
    OnViewClickListener listener;

    private List<MediaController> controllers;
    private Context context;
    private FirebaseDatabase database = FirebaseDatabase.getInstance("https://musicsyncfinal-1651651889018-default-rtdb.europe-west1.firebasedatabase.app/");
    private DatabaseRedactor redactor = new DatabaseRedactor(database);
    private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    private FirebaseStorage storage = FirebaseStorage.getInstance("gs://musicsyncfinal-1651651889018.appspot.com");
    private StorageReference reference = storage.getReference();
    private MediaStore.Audio.Media mediaFile = new MediaStore.Audio.Media();

    public MediaAdapter(List<MediaController> controllers,Context context, OnViewClickListener listener){
        this.controllers = controllers;
        this.listener = listener;
        this.context = context;
    }

    @NonNull
    @Override
    public MediaInfoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.media_element, parent, false);
        return new MediaInfoViewHolder(view);

    }

    @Override
    public void onBindViewHolder(@NonNull MediaInfoViewHolder holder, int position) {
        String appName;
        PackageManager packageManager = context.getPackageManager();
        ApplicationInfo appInfo;
        holder.insideController = controllers.get(position);
        try {
            appInfo = packageManager.getApplicationInfo(controllers.get(position).getPackageName(), 0);
            appName = (String) packageManager.getApplicationLabel(appInfo);
            holder.sessionName.setText(appName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }


        try {
            Drawable icon = packageManager.getApplicationIcon(controllers.get(position).getPackageName());
            holder.musicImage.setImageDrawable(icon);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }


    }

    @Override
    public int getItemCount() {
        return controllers.size();
    }

    public class MediaInfoViewHolder extends RecyclerView.ViewHolder {
        ImageView musicImage;
        TextView sessionName;
        MediaController insideController;

        public MediaInfoViewHolder(@NonNull View itemView) {
            super(itemView);
            musicImage = itemView.findViewById(R.id.session_image);
            sessionName = itemView.findViewById(R.id.session_name);
            FloatingActionButton syncButton = itemView.findViewById(R.id.sync_btn);
            if (FirebaseAuth.getInstance().getCurrentUser() != null)
            syncButton.setOnClickListener(v -> {
                CurrentMediaSync currentMediaSync = new CurrentMediaSync(insideController);
                currentMediaSync.Pause();
                redactor.RedactFields("users", user.getDisplayName(), "sync", "synced");
                Uri file = Uri.fromFile(new File(getMusicUri(currentMediaSync.getArtist(),
                        currentMediaSync.getTrack())));
                UploadTask uploadTask = storage.getReference().child(user.getDisplayName() + "/")
                        .child(currentMediaSync.getTrack()).putFile(file);
                uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        redactor.RedactFields("users", user.getDisplayName(), "track position",
                                currentMediaSync.getTrackPosition());
                        redactor.RedactFields("users", user.getDisplayName(),
                                "track in storage", currentMediaSync.getTrack());
                    }
                });

                listener.OnViewClicked(currentMediaSync);
                Intent toStartController = new Intent("start controller");
                context.sendBroadcast(toStartController);
            });
        }
    }

    interface OnViewClickListener{
        void OnViewClicked(CurrentMediaSync currentMediaSync);
    }
    public String getMusicUri(String artist, String title){
        Uri collection;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            collection= MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        }
        else{
            collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }
        String[] projection = new String[]{
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA
        };
        String selection = MediaStore.Audio.Media.TITLE + " like ?";

        String[] selectionArgs = new String[]{ title };

        Cursor c = context.getContentResolver().
                query(collection, projection, null,null, null);
        Log.d("CRT", title);
        if (c != null){
            while (c.moveToNext()){
//                Log.d("CRT", "ID: " + c.getString(0));
//                Log.d("CRT", "ARTIST: " + c.getString(1));
//                Log.d("CRT", "TITLE: " + c.getString(2));
                if ((c.getString(2)).equals(title)){
                    Log.d("CRT", "Трек найден: " + c.getString(3));
                    Log.d("CRT", "URI: " + Uri.fromFile(new File(c.getString(3))));
                    return c.getString(3);
                }
            }
        }
        return "not found";
    }

}

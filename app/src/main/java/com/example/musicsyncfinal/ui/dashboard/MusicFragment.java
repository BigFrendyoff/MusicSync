package com.example.musicsyncfinal.ui.dashboard;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.musicsyncfinal.CurrentMediaSync;
import com.example.musicsyncfinal.DatabaseRedactor;
import com.example.musicsyncfinal.MainActivity;
import com.example.musicsyncfinal.MediaService;
import com.example.musicsyncfinal.R;
import com.example.musicsyncfinal.databinding.FragmentMusicBinding;
import com.example.musicsyncfinal.databinding.FragmentMusicBinding;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MusicFragment extends Fragment {

    private FragmentMusicBinding binding;
    private CurrentMediaSync currentMediaControl;
    private String trackName = "Пока ничего не играет";
    private FirebaseDatabase database = FirebaseDatabase
            .getInstance("https://musicsyncfinal-1651651889018-default-rtdb.europe-west1.firebasedatabase.app/");
    private DatabaseRedactor redactor = new DatabaseRedactor(database);
    private MediaService mediaService;
    private FloatingActionButton playButton;
    private FloatingActionButton pauseButton;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentMusicBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        Log.d("CRT", "Created");
        final TextView track = root.findViewById(R.id.trackName);
        final ImageView albumIcon = root.findViewById(R.id.track_icon);
        playButton = root.findViewById(R.id.play_btn);
        pauseButton = root.findViewById(R.id.pause_btn);
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        MainActivity activity = (MainActivity) getActivity();
        currentMediaControl = activity.getCurrentMediaSync();
        if (activity.getIsConnectionEstablished()){
            ValueEventListener controlViewListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.getValue().equals("play")){
                        playButton.setEnabled(false);
                        pauseButton.setEnabled(true);
                        playButton.setVisibility(View.INVISIBLE);
                        pauseButton.setVisibility(View.VISIBLE);
                    }
                    if (snapshot.getValue().equals("pause")){
                        playButton.setEnabled(true);
                        pauseButton.setEnabled(false);
                        playButton.setVisibility(View.VISIBLE);
                        pauseButton.setVisibility(View.INVISIBLE);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };
            database.getReference().child("users").child(activity.getCurrentConnectedUser())
                    .child("last command").addValueEventListener(controlViewListener);
        }
        if (currentMediaControl == null){
            TextView trackName = root.findViewById(R.id.trackName);
            trackName.setText(((MainActivity) getActivity()).getTrackName());
            mediaService = ((MainActivity) getActivity()).getMService();

            playButton.setOnClickListener(v->{
                if (activity.isSyncComplete()) {
                    redactor.RedactFields("users", user.getDisplayName(), "last command", "play");
                    mediaService.Play();
                    playButton.setEnabled(false);
                    pauseButton.setEnabled(true);
                    playButton.setVisibility(View.INVISIBLE);
                    pauseButton.setVisibility(View.VISIBLE);
                }
                else{
                    Toast.makeText(activity, "Синхронизация не заверешена", Toast.LENGTH_SHORT).show();
                }
            });
            pauseButton.setOnClickListener(v->{
                if (activity.isSyncComplete()) {
                    redactor.RedactFields("users", user.getDisplayName(), "last command", "pause");
                    mediaService.Pause();
                    playButton.setEnabled(true);
                    pauseButton.setEnabled(false);
                    playButton.setVisibility(View.VISIBLE);
                    pauseButton.setVisibility(View.INVISIBLE);
                }
                else{
                    Toast.makeText(activity, "Синхронизация не завершена", Toast.LENGTH_SHORT).show();
                }
                });


        }
        if(currentMediaControl != null) {
            Log.d("CRT", "Control not null");
            track.setText(currentMediaControl.getTrack());
            albumIcon.setImageBitmap(currentMediaControl.getTrackIcon());
            playButton.setOnClickListener(v->{
                if (activity.isSyncComplete()){
                    currentMediaControl.Play();
                    redactor.RedactFields("users", user.getDisplayName(), "last command", "play");
                    playButton.setEnabled(false);
                    pauseButton.setEnabled(true);
                    playButton.setVisibility(View.INVISIBLE);
                    pauseButton.setVisibility(View.VISIBLE);
                }
                else{
                    Toast.makeText(activity, "Синхронизация не завершена", Toast.LENGTH_SHORT).show();
                }

            });
            pauseButton.setOnClickListener(v->{
                if (activity.isSyncComplete()){
                    currentMediaControl.Pause();
                    redactor.RedactFields("users", user.getDisplayName(), "last command", "pause");
                    playButton.setEnabled(true);
                    pauseButton.setEnabled(false);
                    playButton.setVisibility(View.VISIBLE);
                    pauseButton.setVisibility(View.INVISIBLE);
                }
                else{
                    Toast.makeText(activity, "Синхронизация не завершена", Toast.LENGTH_SHORT).show();
                }
            });
        }



        return root;
    }



    @Override
    public void onResume() {
        super.onResume();
        if (((MainActivity) getActivity()).getIsConnectionEstablished()){
            database.getReference().child("users").child(((MainActivity) getActivity()).getCurrentConnectedUser())
                    .child("last command").get().addOnSuccessListener(task->{
                if (task.getValue().equals("play")){
                    playButton.setEnabled(false);
                    pauseButton.setEnabled(true);
                    playButton.setVisibility(View.INVISIBLE);
                    pauseButton.setVisibility(View.VISIBLE);
                }
                if (task.getValue().equals("pause")){
                    playButton.setEnabled(true);
                    pauseButton.setEnabled(false);
                    playButton.setVisibility(View.VISIBLE);
                    pauseButton.setVisibility(View.INVISIBLE);
                }
            });
        }

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d("CRT", "Destroyed");
        binding = null;
    }
}
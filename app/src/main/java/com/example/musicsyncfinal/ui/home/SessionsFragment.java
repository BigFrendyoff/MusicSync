package com.example.musicsyncfinal.ui.home;

import android.media.session.MediaController;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicsyncfinal.CurrentMediaSync;
import com.example.musicsyncfinal.MainActivity;
import com.example.musicsyncfinal.R;
import com.example.musicsyncfinal.databinding.FragmentSessionsBinding;
import com.example.musicsyncfinal.databinding.FragmentSessionsBinding;

import java.util.List;

public class SessionsFragment extends Fragment implements MediaAdapter.OnViewClickListener {

    private FragmentSessionsBinding binding;
    private List<MediaController> mediaControllers;
    private MediaAdapter mediaAdapter;
    private CurrentMediaSync currentMediaSync = null;
    MediaInfo mediaInfo;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentSessionsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        final TextView alert = root.findViewById(R.id.connectionMessage);

        setHasOptionsMenu(true);

        MediaInfo mediaInfo = new MediaInfo(root.getContext());
        mediaInfo.initialize();

        mediaControllers = mediaInfo.getSessions();
        if (mediaControllers.size() > 0){
            if (((MainActivity) getActivity()).getIsConnectionEstablished()){
                if (((MainActivity) getActivity()).isSyncComplete()){
                    alert.setText("Вы уже синхронизированы");
                }
                else{
                    root.findViewById(R.id.media_view).setVisibility(View.VISIBLE);
                    root.findViewById(R.id.connectionMessage).setVisibility(View.INVISIBLE);
                    initRecyclerView(mediaControllers, root);
                }

            }
        }
        else {
            alert.setText("Нет активных сессий");
        }


        return root;

    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.sessions_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.refresh_btn) {
            Log.d("REFR", "Refreshed!");
        }
        return super.onOptionsItemSelected(item);


    }

    private void initRecyclerView(List<MediaController> mediaControllerList, View view){
        RecyclerView recyclerView = view.findViewById(R.id.media_view);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        mediaAdapter = new MediaAdapter(mediaControllerList, getContext(), this);
        recyclerView.setAdapter(mediaAdapter);
    }


    @Override
    public void OnViewClicked(CurrentMediaSync currentMediaSync) {
        Log.d("CRT", "Clicked and transfered");
        ((MainActivity) getActivity()).setCurrentMedia(currentMediaSync);
}
}
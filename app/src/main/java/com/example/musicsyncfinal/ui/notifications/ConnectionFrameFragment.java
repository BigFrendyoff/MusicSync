package com.example.musicsyncfinal.ui.notifications;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.example.musicsyncfinal.DatabaseRedactor;
import com.example.musicsyncfinal.FireBaseUIActivity;
import com.example.musicsyncfinal.MainActivity;
import com.example.musicsyncfinal.R;
import com.example.musicsyncfinal.databinding.FragmentConnectionFrameBinding;
import com.example.musicsyncfinal.databinding.FragmentConnectionFrameBinding;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ConnectionFrameFragment extends Fragment {

    private FragmentConnectionFrameBinding binding;
    FirebaseDatabase database = FirebaseDatabase
            .getInstance("https://musicsyncfinal-1651651889018-default-rtdb.europe-west1.firebasedatabase.app/");
    DatabaseRedactor redactor = new DatabaseRedactor(database);
    private boolean output;
    private Context context;
    private boolean isInviteSended = false;
    private String userToConnect;



    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        binding = FragmentConnectionFrameBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        context = root.getContext();

        final Button signInButton = binding.getRoot().findViewById(R.id.sign_in_btn);
        final EditText userSearch = root.findViewById(R.id.editTextSearch);
        final FloatingActionButton searchButton = root.findViewById(R.id.search_button);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null){
            signInButton.setVisibility(View.INVISIBLE);
            signInButton.setEnabled(false);
            userSearch.setVisibility(View.VISIBLE);
            userSearch.setEnabled(true);
            searchButton.setVisibility(View.VISIBLE);
            searchButton.setEnabled(true);
            searchButton.setOnClickListener(v-> {
                if(!userSearch.getText().toString().equals(user.getDisplayName())) {
                    userToConnect = userSearch.getText().toString();
                    database.getReference().child("users").get().addOnCompleteListener(task -> {
                        if (!task.getResult().hasChild(userToConnect)){
                            Toast.makeText(root.getContext(), "Пользователь не найден",
                                    Toast.LENGTH_SHORT).show();
                        }
                        else {
                            database.getReference().child("users").child(userToConnect)
                                    .child("connected user").get().addOnCompleteListener(task1 -> {
                                if(task1.getResult().getValue().equals("none")){
                                    DialogBuilder(userSearch.getText().toString(),
                                            user.getDisplayName()).show();
                                    if (isInviteSended) {
                                        userToConnect = userSearch.getText().toString();
                                    }
                                }
                                else{
                                    Toast.makeText(context, "Пользователь уже с кем-то связан",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });

                        }
                    });


                }
                else{
                    Toast.makeText(root.getContext(), "Вы ищите себя...",
                            Toast.LENGTH_SHORT).show();
                }

            });



        }
        signInButton.setOnClickListener(v -> {
            Intent toSignIn = new Intent(root.getContext(), FireBaseUIActivity.class);
            startActivity(toSignIn);

        });

        return root;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.friend_list_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.sign_out_btn){
            AuthUI.getInstance().signOut(this.getContext());
            binding.getRoot().findViewById(R.id.sign_in_btn).setVisibility(View.VISIBLE);
            binding.getRoot().findViewById(R.id.sign_in_btn).setEnabled(true);
            binding.getRoot().findViewById(R.id.search_button).setVisibility(View.INVISIBLE);
            binding.getRoot().findViewById(R.id.search_button).setEnabled(false);
            binding.getRoot().findViewById(R.id.editTextSearch).setVisibility(View.INVISIBLE);
            binding.getRoot().findViewById(R.id.editTextSearch).setEnabled(false);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null){
            binding.getRoot().findViewById(R.id.sign_in_btn).setVisibility(View.INVISIBLE);
            binding.getRoot().findViewById(R.id.sign_in_btn).setEnabled(false);


        }

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public Dialog DialogBuilder(String designationUser, String sender){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Пользователь найден!")
                .setPositiveButton("Пригласить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        redactor.RedactFields("users", designationUser,
                                "invited by", sender);
                        isInviteSended = true;
                        Intent invite = new Intent("invitation");
                        invite.putExtra("user", designationUser);
                        context.sendBroadcast(invite);
                    }
                })
                .setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        return builder.create();
    }
}
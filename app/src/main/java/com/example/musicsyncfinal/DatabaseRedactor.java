package com.example.musicsyncfinal;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class DatabaseRedactor {
    private FirebaseDatabase database;
    boolean searchOutput;
    private String getOutput;

    public DatabaseRedactor(FirebaseDatabase database){
        this.database = database;
    }
    public void PushData(String childInDb, String userInDb, Map<String, Object> data){
        database.getReference().child(childInDb).child(userInDb).setValue(data);
    }
    public void DeleteData(String childinDb, String userInDb){
        database.getReference().child(childinDb).child(userInDb).setValue(null);
    }

    public void RedactFields(String childInDb, String userInDb, String field, String value){
        database.getReference().child(childInDb).child(userInDb).child(field).setValue(value);
    }



}

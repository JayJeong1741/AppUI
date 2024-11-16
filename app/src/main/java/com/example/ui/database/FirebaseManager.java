package com.example.ui.database;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
public class FirebaseManager {
    private static final String TAG = "FirebaseManager";
    private final DatabaseReference databaseReference;

    public FirebaseManager(){
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        this.databaseReference = database.getReference();
    }

    //데이터 쓰기
    public void writeData(String path, Object data) {
        databaseReference.child(path).setValue(data)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "데이터 쓰기 성공 경로 : " + path))
                .addOnFailureListener(e -> Log.e("데이터 쓰기 실패: " + path, String.valueOf(e)));
    }

    //데이터 읽기
    public void readData(String path, final FirebaseCallback callback) {
        databaseReference.child(path).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Object data = snapshot.getValue();
                callback.onSuccess(data);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailure(error.toException());
            }
        });
    }

    public interface FirebaseCallback {
        void onSuccess(Object data);
        void onFailure(Exception e);
    }
}

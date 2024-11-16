package com.example.ui.database;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;

public class FirebaseTest {
    private Context context;

    public FirebaseTest(Context context) {
        this.context = context;
    }

    public void testFirebaseDataInsertion() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("test_connection");

        Map<String, Object> testData = new HashMap<>();
        testData.put("timestamp", ServerValue.TIMESTAMP);
        testData.put("message", "Firebase 연결 테스트");

        myRef.setValue(testData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirebaseTest", "데이터 삽입 성공");
                    Toast.makeText(context, "데이터 삽입 성공", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseTest", "데이터 삽입 실패: " + e.getMessage());
                    Toast.makeText(context, "데이터 삽입 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}

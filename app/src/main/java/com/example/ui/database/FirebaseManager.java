package com.example.ui.database;
import android.icu.text.SimpleDateFormat;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    public void readDataById(String parentPath, String targetId, FirebaseCallback callback) {
        databaseReference.child(parentPath).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Integer> hourlyCountMap = new HashMap<>(); // 시간별 카운트 저장
                Map<String, Integer> dailyCountMap = new HashMap<>();  // 날짜별 카운트 저장

                // 주간 데이터 추출 기준일 계산 (현재 날짜 - 7일)
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_MONTH, -7);
                Date sevenDaysAgo = calendar.getTime();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String weekStartDate = dateFormat.format(sevenDaysAgo);

                int totalWeeklyCount = 0;

                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    Object data = childSnapshot.getValue();
                    if (data instanceof Map) {
                        Map<String, Object> dataMap = (Map<String, Object>) data;
                        Object idValue = dataMap.get("id");
                        Object timestampValue = dataMap.get("timestamp");
                        Log.d(TAG, "DataSnapshot: " + childSnapshot.getValue());
                        if (targetId.equals(idValue) && timestampValue instanceof String) {
                            try {
                                // "yyyy-MM-dd HH:mm:ss" 포맷으로 파싱
                                String fullTimestamp = (String) timestampValue;
                                SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                                Date parsedDate = timestampFormat.parse(fullTimestamp);

                                // 날짜 및 시간 분리
                                SimpleDateFormat dateOnlyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                SimpleDateFormat hourOnlyFormat = new SimpleDateFormat("HH", Locale.getDefault());

                                String date = dateOnlyFormat.format(parsedDate); // 날짜
                                String hour = hourOnlyFormat.format(parsedDate); // 시간

                                // 오늘 날짜의 경우 시간별로 카운트
                                if (isToday(date)) {
                                    hourlyCountMap.put(hour, hourlyCountMap.getOrDefault(hour, 0) + 1);
                                }

                                // 주간 데이터 추출
                                if (date.compareTo(weekStartDate) >= 0) {
                                    dailyCountMap.put(date, dailyCountMap.getOrDefault(date, 0) + 1);
                                    totalWeeklyCount++;
                                }
                                Log.d(TAG, "hourlyCountMap: " + hourlyCountMap + ", dailyCountMap: " + dailyCountMap);
                            } catch (Exception e) {
                                Log.e(TAG, "날짜 파싱 오류: " + timestampValue, e);
                            }
                        }
                    }
                }

                // 일간 평균 계산 (주간 총 감지 횟수 / 7일)
                double averageDailyCount = totalWeeklyCount / 7.0;

                // Callback에 결과 전달
                callback.onSuccess(new Object[]{hourlyCountMap, dailyCountMap, averageDailyCount});
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailure(error.toException());
            }
        });
    }

    private boolean isToday(String date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = sdf.format(new Date());
        return date.equals(today);
    }

    public interface FirebaseCallback {
        void onSuccess(Object data);
        void onFailure(Exception e);
    }
}
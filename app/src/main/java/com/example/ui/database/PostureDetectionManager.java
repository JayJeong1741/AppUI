package com.example.ui.database;

import android.util.Log;

import androidx.arch.core.executor.ArchTaskExecutor;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
public class PostureDetectionManager extends FirebaseManager {
    private static final String TAG = "PostureDetectionManager";

    // 데이터 저장 메서드
    public void savePostureData(String userId) {
        // Firebase 경로 설정
        String path = "turtleneck"; // Firebase 데이터 저장 경로

        // 현재 시간 가져오기
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());

        // 9시간 추가 (한국 표준시)
        calendar.add(Calendar.HOUR, 9);

        // 새로운 시간 포맷팅
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(calendar.getTime());

        // 저장할 데이터 맵 생성
        Map<String, Object> data = new HashMap<>();
        data.put("timestamp", timestamp); // 현재 시간
        data.put("id", userId); // 사용자 ID 추가

        // 고유 ID 생성 (timestamp 기반)
        String uniqueId = String.valueOf(System.currentTimeMillis());

        // Firebase에 데이터 저장
        writeData(path + "/" + uniqueId, data);
    }
}
package com.example.ui.database;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class PostureDetectionManager extends FirebaseManager {
    private static final String TAG = "PostureDetectionManager";

    // 데이터 저장 메서드
    public void savePostureData() {
        // Firebase 경로 설정
        String path = "tutleneck"; // tutleneck 목록

        // 현재 시간 가져오기
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        // 저장할 데이터 맵 생성
        Map<String, Object> data = new HashMap<>();
        data.put("timestamp", timestamp); // 현재 시간
//        data.put("id", userId); // 사용자 ID
//        data.put("high_value_count", count); // 카운트 값

        // 고유 ID를 생성 (timestamp를 사용하여 고유하게 만들 수 있음)
        String uniqueId = String.valueOf(System.currentTimeMillis()); // 예시로 현재 시간을 사용

        // Firebase에 데이터 저장
        writeData(path + "/" + uniqueId, data);
    }
}
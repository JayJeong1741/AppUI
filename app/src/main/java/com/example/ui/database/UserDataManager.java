package com.example.ui.database;

import java.util.HashMap;
import java.util.Map;

public class UserDataManager {
    private final FirebaseManager firebaseManager;

    public UserDataManager() {
        this.firebaseManager = new FirebaseManager();
    }

    // 사용자 데이터 저장
    public void saveUserData(String userId, String name, String id, String password) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("id", name);
        userData.put("password", name);


        firebaseManager.writeData("users/" + userId, userData);
    }

    // 사용자 데이터 읽기
    public void getUserData(String userId, FirebaseManager.FirebaseCallback callback) {
        firebaseManager.readData("users/" + userId, callback);
    }
}

package com.example.ui.database;

import java.util.HashMap;
import java.util.Map;

public class UserDataManager {
    private final FirebaseManager firebaseManager;

    public UserDataManager() {
        this.firebaseManager = new FirebaseManager();
    }

    // 사용자 데이터 저장
    public void saveUserData(String id, String name, String password) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("password", name);

        firebaseManager.writeData("users/" + id, userData);
    }

    // 사용자 데이터 읽기
    public void getUserData(String id, FirebaseManager.FirebaseCallback callback) {
        firebaseManager.readData("users/" + id, callback);
    }
}

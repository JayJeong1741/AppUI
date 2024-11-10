package com.example.ui.ui.user; // 패키지 이름에 맞게 조정하세요.

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ui.MainActivity;
import com.example.ui.R;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextId;
    private EditText editTextPassword;
    private Button buttonLogin;
    private Button buttonSignup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_login); // 로그인 XML 레이아웃 설정

        editTextId = findViewById(R.id.login_id);
        editTextPassword = findViewById(R.id.login_pw);
        buttonLogin = findViewById(R.id.btn_login);
        buttonSignup = findViewById(R.id.btn_signup); // 회원가입 버튼 초기화

        buttonLogin.setOnClickListener(v -> {
            String id = editTextId.getText().toString();
            String password = editTextPassword.getText().toString();

            if (id.equals("a") && password.equals("1234")) {
                // 로그인 성공 시 메인 액티비티로 이동
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish(); // 현재 액티비티 종료
            } else {
                // 로그인 실패 처리
                Toast.makeText(this, "아이디 또는 비밀번호를 확인해주세요ㄴ.", Toast.LENGTH_SHORT).show();
            }
        });

        buttonSignup.setOnClickListener(v -> {
            // 회원가입 화면으로 이동
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });
    }
}
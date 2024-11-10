package com.example.ui.ui.user;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ui.MainActivity;
import com.example.ui.R;

public class SignupActivity extends AppCompatActivity {

    private EditText editName, editId, editPw, editPwCorrect;
    private Button buttonSignup, buttonCheck, buttonBack; // 뒤로가기 버튼 추가

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_signup); // 레이아웃 설정

        // UI 요소 초기화
        editName = findViewById(R.id.edit_name);
        editId = findViewById(R.id.edit_id);
        editPw = findViewById(R.id.edit_pw);
        editPwCorrect = findViewById(R.id.edit_pw_correct);
        buttonSignup = findViewById(R.id.btn_create);
        buttonCheck = findViewById(R.id.btn_check); // 중복확인 버튼
        buttonBack = findViewById(R.id.button_back); // 뒤로가기 버튼

        // 중복확인 버튼 클릭 이벤트
        buttonCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String id = editId.getText().toString().trim(); // 입력된 ID 가져오기
                if (id.isEmpty()) {
                    Toast.makeText(SignupActivity.this, "아이디를 입력하세요", Toast.LENGTH_SHORT).show();
                } else {
                    if (id.equals("a")) { // 예시: ID 'a'가 이미 존재한다고 가정
                        Toast.makeText(SignupActivity.this, "이미 존재하는 아이디입니다", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(SignupActivity.this, "사용 가능한 아이디입니다", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        // 회원가입 버튼 클릭 이벤트
        buttonSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = editName.getText().toString().trim();
                String id = editId.getText().toString().trim();
                String password = editPw.getText().toString().trim();
                String passwordConfirm = editPwCorrect.getText().toString().trim();

                if (name.isEmpty() || id.isEmpty() || password.isEmpty() || passwordConfirm.isEmpty()) {
                    Toast.makeText(SignupActivity.this, "모든 필드를 입력하세요", Toast.LENGTH_SHORT).show();
                } else if (!password.equals(passwordConfirm)) {
                    Toast.makeText(SignupActivity.this, "비밀번호가 일치하지 않습니다", Toast.LENGTH_SHORT).show();
                } else {
                    if (id.equals("a") && password.equals("1234")) {
                        Toast.makeText(SignupActivity.this, "회원가입 성공", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(SignupActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(SignupActivity.this, "회원가입 실패: ID 또는 비밀번호 오류", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        // 뒤로가기 버튼 클릭 이벤트
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 로그인 액티비티로 이동
                Intent intent = new Intent(SignupActivity.this, LoginActivity.class); // LoginActivity로 변경
                startActivity(intent);
                finish(); // 현재 액티비티 종료
            }
        });
    }
}
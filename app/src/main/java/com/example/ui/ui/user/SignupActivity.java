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
import com.example.ui.database.FirebaseManager;
import com.example.ui.database.UserDataManager;
import com.google.firebase.database.FirebaseDatabase;

public class SignupActivity extends AppCompatActivity {

    private EditText editName, editId, editPw, editPwCorrect;
    private Button buttonSignup, buttonCheck, buttonBack; // 뒤로가기 버튼 추가
    private UserDataManager userDataManager;
    private boolean isIdChecked = false;    // 중복 체크 여부를 확인하는 플레그 변수
    private String idCheck;     // 중복 체크할 때 아이디
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

        userDataManager = new UserDataManager();

// 중복확인 버튼 클릭 이벤트
        buttonCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String id = editId.getText().toString().trim();     // 입력된 ID 가져오기
                // 공란일 경우
                if(id.isEmpty()){
                    Toast.makeText(SignupActivity.this, "아이디를 입력하세요", Toast.LENGTH_SHORT).show();
                } else {
                    // Firebase에 ID 존재 여부 확인
                    userDataManager.getUserData(id, new FirebaseManager.FirebaseCallback() {
                        @Override
                        public void onSuccess(Object data) {
                            if (data != null) {     // 중복 ID가 존재함
                                Toast.makeText(SignupActivity.this, "아이디가 이미 존재합니다", Toast.LENGTH_SHORT).show();
                                isIdChecked = false;
                            } else {    // 중복 ID가 존재하지 않음
                                Toast.makeText(SignupActivity.this, "사용 가능한 아이디입니다", Toast.LENGTH_SHORT).show();
                                isIdChecked = true;
                                idCheck = id;
                            }
                        }
                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(SignupActivity.this, "데이터베이스 오류: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            isIdChecked = false;
                        }
                    });

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
                // 필드가 한 개 이상 공란일 때
                if (name.isEmpty() || id.isEmpty() || password.isEmpty() || passwordConfirm.isEmpty()) {
                    Toast.makeText(SignupActivity.this, "모든 필드를 입력하세요", Toast.LENGTH_SHORT).show();
                    // 비밀번호와 재입력의 일치 확인
                } else if (!password.equals(passwordConfirm)) {
                    Toast.makeText(SignupActivity.this, "비밀번호가 일치하지 않습니다", Toast.LENGTH_SHORT).show();
                    // 모든 필드를 채우고, 비밀번호 일치할 때
                } else if (!isIdChecked) {
                    Toast.makeText(SignupActivity.this, "아이디 중복 체크를 해주세요", Toast.LENGTH_SHORT).show();
                } else if (idCheck.equals(id)){
                    try {
                        userDataManager.saveUserData(id, name, password);
                        Toast.makeText(SignupActivity.this, "회원가입 성공", Toast.LENGTH_SHORT).show();
                        finish();
                    } catch (Exception e){
                        Toast.makeText(SignupActivity.this, "회원가입 실패: ID 생성 실패", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(SignupActivity.this, "아이디 중복 체크를 해주세요", Toast.LENGTH_SHORT).show();
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
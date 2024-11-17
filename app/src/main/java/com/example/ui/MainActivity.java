package com.example.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.widget.Toast;


import com.example.ui.ui.user.LoginActivity;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ui.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private NavController navController; // NavController를 전역 변수로 선언

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // NavController 초기화
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);

        // Toolbar 설정
        setSupportActionBar(binding.appBarMain.toolbar);
        binding.appBarMain.toolbar.setOnClickListener(view -> {
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null)
                    .setAnchorView(R.id.toolbar).show();
        });

        // Navigation Drawer 설정
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.item_home, R.id.item_myinfo, R.id.item_stretch, R.id.item_todo, R.id.item_monitoring)
                .setOpenableLayout(drawer)
                .build();

        // Navigation Drawer와 NavController 연결
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // NavigationView의 항목 클릭 리스너 설정
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.item_logout) {
                // 로그아웃 처리
                logout(); // 로그아웃 메소드 호출
            } else {
                navController.navigate(id); // ID에 해당하는 프래그먼트로 이동
                binding.drawerLayout.closeDrawers(); // Drawer 닫기
            } return true;
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    public void logout() {
        // 로그아웃 알림 표시
        Toast.makeText(this, "로그아웃 되었습니다", Toast.LENGTH_SHORT).show();

        // 로그인 액티비티로 이동
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish(); // 현재 액티비티 종료
    }
    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }
}
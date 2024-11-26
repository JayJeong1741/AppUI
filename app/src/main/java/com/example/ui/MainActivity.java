package com.example.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
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
    private NavController navController;

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

        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // NavigationView 헤더의 TextView 업데이트
        updateNavigationViewHeader(navigationView);

        // NavigationView의 항목 클릭 리스너 설정
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.item_logout) {
                logout();
            } else {
                navController.navigate(id);
                binding.drawerLayout.closeDrawers();
            }
            return true;
        });
    }

    private void updateNavigationViewHeader(NavigationView navigationView) {
        // SharedPreferences에서 userId 가져오기
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userId = sharedPreferences.getString("USER_ID", "Unknown User");

        // 헤더 뷰 가져오기
        View headerView = navigationView.getHeaderView(0);

        // TextView 찾아서 userId 설정
        TextView userNameTextView = headerView.findViewById(R.id.textView_user_id); // XML에서 TextView ID 확인
        if (userNameTextView != null) {
            userNameTextView.setText(userId);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public void logout() {
        Toast.makeText(this, "로그아웃 되었습니다", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }
}

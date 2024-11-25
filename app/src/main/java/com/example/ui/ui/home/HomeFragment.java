package com.example.ui.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.ui.MainActivity;
import com.example.ui.R;
import com.example.ui.databinding.FragmentHomeBinding;

import org.webrtc.SurfaceViewRenderer;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private NavController navController; // NavController를 선언합니다.

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // NavController 초기화
        navController = Navigation.findNavController(view);


        // 버튼 초기화
        Button btnMonitoring = view.findViewById(R.id.Monitoring);
        Button btnMyInfo = view.findViewById(R.id.myinfo);
        Button btnStretchMarket = view.findViewById(R.id.stretchMarket);
        Button btnTodo = view.findViewById(R.id.todo);
        Button btnLogout = view.findViewById(R.id.logout);
        // 버튼 클릭 리스너 설정
        btnMonitoring.setOnClickListener(v -> {
            // MonitoringFragment로 네비게이션
            navController.navigate(R.id.item_monitoring);
        });

        btnStretchMarket.setOnClickListener(v -> {
            // StretchMarketFragment로 네비게이션
            navController.navigate(R.id.item_stretch);
        });

        btnTodo.setOnClickListener(v -> {
            // TodoFragment로 네비게이션
            navController.navigate(R.id.item_todo);
        });

        btnMyInfo.setOnClickListener(v -> {
            // MyInfoFragment로 네비게이션
            navController.navigate(R.id.item_myinfo);
        });
        btnLogout.setOnClickListener(v -> {
            // MainActivity의 logout() 메소드를 호출
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).logout();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}
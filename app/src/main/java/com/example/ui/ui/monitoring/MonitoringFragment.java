package com.example.ui.ui.monitoring;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import org.webrtc.*;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.ui.R;

public class MonitoringFragment extends Fragment {
    private TextView predict;
    private SurfaceViewRenderer remoteVideoView;
    private WebRTCService webRTCService;
    private boolean isBound = false;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_monitoring, container, false);

        predict = view.findViewById(R.id.predict);
        remoteVideoView = view.findViewById(R.id.remote_video_view);
        Button btnStop = view.findViewById(R.id.button);

        btnStop.setOnClickListener(v -> {
            if (isBound) {
                requireActivity().unbindService(serviceConnection);
                isBound = false;
            }
            requireActivity().stopService(new Intent(requireActivity(), WebRTCService.class));
            if (remoteVideoView != null) {
                remoteVideoView.release();
            }
            NavController navController = Navigation.findNavController(requireView());
            navController.popBackStack();
        });

        return view;
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (isBound) {
            requireActivity().unbindService(serviceConnection);
            isBound = false;
        }
        if (remoteVideoView != null) {
            remoteVideoView.release();
        }
    }
    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(requireActivity(), WebRTCService.class);
        requireActivity().startService(intent);
        requireActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            WebRTCService.LocalBinder binder = (WebRTCService.LocalBinder) service;
            webRTCService = binder.getService();
            isBound = true;

            // Initialize video view with service's EglBase context
            remoteVideoView.init(webRTCService.getEglBaseContext(), null);
            remoteVideoView.setMirror(false);
            remoteVideoView.setEnableHardwareScaler(true);

            // Set video sink and data channel observer
            webRTCService.setRemoteVideoSink(remoteVideoView);
            webRTCService.setMessageListener(new WebRTCMessageListener() {
                @SuppressLint("SetTextI18n")
                @Override
                public void onMessageReceived(String message) {
                    // 여기서 메시지를 처리합니다
                    if(isAdded() && getActivity() != null){
                        requireActivity().runOnUiThread(()->{
                            predict.setText("거북목 예측도" + message);
                        });
                    }
                }
            });

        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            webRTCService = null;
            isBound = false;
        }
    };
}

package com.example.ui.ui.monitoring;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.webrtc.SurfaceViewRenderer;

public class SharedViewModel extends ViewModel {
    private final MutableLiveData<SurfaceViewRenderer> surfaceViewRenderer = new MutableLiveData<>();

    // 객체 설정
    public void setSurfaceViewRenderer(SurfaceViewRenderer renderer) {
        surfaceViewRenderer.setValue(renderer);
    }

    // 객체 가져오기
    public LiveData<SurfaceViewRenderer> getSurfaceViewRenderer() {
        return surfaceViewRenderer;
    }
}

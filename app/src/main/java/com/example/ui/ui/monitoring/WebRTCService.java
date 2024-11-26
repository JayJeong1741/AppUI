package com.example.ui.ui.monitoring;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;


import com.example.ui.database.PostureDetectionManager;

import io.socket.client.IO;
import io.socket.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

import org.webrtc.*;

public class WebRTCService extends Service {
    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnection;
    private Socket mSocket;
    private static final String TAG = "WebRTC_SERVICE";
    private static final String SOCKET_URL = "http://172.171.240.32:3000";
    private EglBase eglBase;
    private final IBinder binder = new LocalBinder();
    private VideoSink remoteVideoSink, currentRemoteVideoSink;
    private DataChannel.Observer dataChannelObserver;
    private WebRTCMessageListener messageListener;
    // 클래스 필드에 Count 및 TARGET_COUNT 선언
    private int Count = 0; // 카운트를 저장하는 변수
    private static final int TARGET_COUNT = 5; // 목표 카운트 값
    PostureDetectionManager manager = new PostureDetectionManager();
    private String userId; // SharedPreferences에서 가져온 사용자 ID
    // 리스너 설정 메소드
    public void setMessageListener(WebRTCMessageListener listener) {
        this.messageListener = listener;
    }

    // Socket 관련 변수들
    private boolean isSocketConnected = false;
    private static final int RECONNECT_DELAY = 5000; // 5초
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private int reconnectAttempts = 0;
    private final Handler reconnectHandler = new Handler(Looper.getMainLooper());
    private VideoTrack remoteVideoTrack;

    private final Runnable reconnectRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isSocketConnected && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                Log.d(TAG, "Attempting to reconnect... Attempt: " + (reconnectAttempts + 1));
                initSocket();
                reconnectAttempts++;
                reconnectHandler.postDelayed(this, RECONNECT_DELAY);
            }
        }
    };

    public class LocalBinder extends Binder {
        WebRTCService getService() {
            return WebRTCService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Service bound");

        return binder;
    }

    public void setRemoteVideoSink(VideoSink sink) {
        this.remoteVideoSink = sink;
        Log.d(TAG, "Remote video sink set");
        updateVideoSink();
    }

    private void updateVideoSink() {
        if (remoteVideoTrack != null && remoteVideoSink != null && remoteVideoSink != currentRemoteVideoSink) {
            if (currentRemoteVideoSink != null) {
                remoteVideoTrack.removeSink(currentRemoteVideoSink);
            }
            remoteVideoTrack.addSink(remoteVideoSink);
            currentRemoteVideoSink = remoteVideoSink;
        }
    }

    public void createNewStream() {
        MediaStream newStream = peerConnectionFactory.createLocalMediaStream("newStream");
        // 필요한 트랙 추가
        // newStream.addTrack(...);
        peerConnection.addStream(newStream);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // SharedPreferences에서 로그인 상태 확인
        SharedPreferences sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userId = sharedPref.getString("USER_ID", null);
        Log.d(TAG, "Service created");
        eglBase = EglBase.create();
        initWebRTC();
        initSocket();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");
        return START_STICKY;
    }

    private void initWebRTC() {
        Log.d(TAG, "Initializing WebRTC");
        PeerConnectionFactory.InitializationOptions initializationOptions =
                PeerConnectionFactory.InitializationOptions.builder(this)
                        .setEnableInternalTracer(true)
                        .createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(new DefaultVideoEncoderFactory(eglBase.getEglBaseContext(), true, true))
                .setVideoDecoderFactory(new DefaultVideoDecoderFactory(eglBase.getEglBaseContext()))
                .createPeerConnectionFactory();

        createPeerConnection();
    }

    private void createPeerConnection() {
        Log.d(TAG, "Creating peer connection");
        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(new ArrayList<>());
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;

        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, new PeerConnection.Observer() {
            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {
                Log.d(TAG, "onSignalingChange: " + signalingState);
            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                Log.d(TAG, "onIceConnectionChange: " + iceConnectionState);
            }

            @Override
            public void onIceConnectionReceivingChange(boolean b) {
                Log.d(TAG, "onIceConnectionReceivingChange: " + b);
            }

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
                Log.d(TAG, "onIceGatheringChange: " + iceGatheringState);
            }

            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                Log.d(TAG, "onIceCandidate: " + iceCandidate.sdp);
                handleIceCandidate(iceCandidate);
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                Log.d(TAG, "onAddStream: " + mediaStream.toString());
                if (!mediaStream.videoTracks.isEmpty()) {
                    remoteVideoTrack = mediaStream.videoTracks.get(0);
                    updateVideoSink();
                }
            }
            @Override
            public void onRemoveStream(MediaStream mediaStream) {
                Log.d(TAG, "onRemoveStream");
            }

            @Override
            public void onDataChannel(DataChannel dataChannel) {
                Log.d(TAG, "onDataChannel");
                dataChannel.registerObserver(dataChannelObserver);
                dataChannel.registerObserver(new DataChannel.Observer() {
                    @Override
                    public void onBufferedAmountChange(long previousAmount) {
                        Log.d(TAG, "Buffered amount changed: " + previousAmount);
                    }

                    @Override
                    public void onStateChange() {

                    }

                    @Override
                    public void onMessage(DataChannel.Buffer buffer) {
                        try {
                            if (!buffer.binary) {
                                ByteBuffer data = buffer.data;
                                byte[] bytes = new byte[data.remaining()];
                                data.get(bytes);
                                final String text = new String(bytes, StandardCharsets.UTF_8);
                                Log.d(TAG, "Received text message: " + text);
                                // 문자열을 소수점 숫자로 변환
                                double value = Double.parseDouble(text); // 소수점 포함 숫자 처리
                                if (value >= 90) {
                                    Count++;
                                    Log.d(TAG, "Count 증가: " + Count);

                                    // 목표 도달 시 데이터 저장 및 초기화
                                    if (Count >= TARGET_COUNT) {
                                        manager.savePostureData(userId);
                                        Log.d(TAG, "Posture data saved");
                                        Count = 0; // 카운트 초기화
                                    }
                                } else {
                                    // 90 이하의 데이터가 들어오면 카운트 초기화
                                    Count = 0;
                                    Log.d(TAG, "Count 초기화: " + Count);
                                }
                                if (messageListener != null) {
                                    // UI 스레드에서 콜백 실행
                                    new Handler(Looper.getMainLooper()).post(() -> {
                                        messageListener.onMessageReceived(text);
                                    });
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing message: " + e.getMessage());
                        }
                    }

                });

            }

            @Override
            public void onRenegotiationNeeded() {
                Log.d(TAG, "onRenegotiationNeeded");
            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
                Log.d(TAG, "onIceCandidatesRemoved");
            }


        });
    }
    private void initSocket() {
        Log.d(TAG, "Initializing socket connection to: " + SOCKET_URL);
        try {
            if (mSocket != null) {
                mSocket.disconnect();
                mSocket.off();
            }

            IO.Options options = IO.Options.builder()
                    .setForceNew(true)
                    .setReconnection(true)
                    .setReconnectionAttempts(3)
                    .setReconnectionDelay(1000)
                    .setReconnectionDelayMax(5000)
                    .setTransports(new String[]{"websocket"})
                    .build();

            mSocket = IO.socket(SOCKET_URL, options);
            setupSocketListeners();
            mSocket.connect();
        } catch (URISyntaxException e) {
            Log.e(TAG, "Socket initialization error: " + e.getMessage());
            startReconnectTimer();
        }
    }

    private void startReconnectTimer() {

        reconnectHandler.removeCallbacks(reconnectRunnable);
        reconnectAttempts = 0;
        reconnectHandler.postDelayed(reconnectRunnable, RECONNECT_DELAY);
    }

    private void setupSocketListeners() {
        mSocket.on(Socket.EVENT_CONNECT, args -> {
            Log.d(TAG, "Socket connected successfully");
            isSocketConnected = true;
            reconnectAttempts = 0;
            reconnectHandler.removeCallbacks(reconnectRunnable);
            mSocket.emit("join_room", "1234");
        });

        mSocket.on(Socket.EVENT_DISCONNECT, args -> {
            Log.d(TAG, "Socket disconnected");
            isSocketConnected = false;
            startReconnectTimer();
        });

        mSocket.on(Socket.EVENT_CONNECT_ERROR, args -> {
            Log.e(TAG, "Socket connection error: " + Arrays.toString(args));
            isSocketConnected = false;
            startReconnectTimer();
        });

        mSocket.on("offer", args -> {
            Log.d(TAG, "Received offer");
            if (args.length > 0 && args[0] != null) {
                JSONObject offer = (JSONObject) args[0];
                try {
                    handleOffer(offer);
                } catch (JSONException e) {
                    Log.e(TAG, "Error handling offer: " + e.getMessage());
                }
            }
        });

        mSocket.on("ice", args -> {
            Log.d(TAG, "Received ICE candidate");
            if (args.length > 0 && args[0] != null) {
                try {
                    JSONObject iceCandidateJson = (JSONObject) args[0];
                    IceCandidate iceCandidate = new IceCandidate(
                            iceCandidateJson.getString("sdpMid"),
                            iceCandidateJson.getInt("sdpMLineIndex"),
                            iceCandidateJson.getString("candidate")
                    );
                    peerConnection.addIceCandidate(iceCandidate);
                } catch (JSONException e) {
                    Log.e(TAG, "Error handling ICE candidate: " + e.getMessage());
                }
            }
        });
    }

    private void handleOffer(JSONObject offerJson) throws JSONException {
        Log.d(TAG, "Handling offer: " + offerJson.toString());
        String sdpString = offerJson.getString("sdp");
        SessionDescription sessionDescription = new SessionDescription(
                SessionDescription.Type.OFFER,
                sdpString
        );

        peerConnection.setRemoteDescription(new SimpleSdpObserver() {
            @Override
            public void onSetSuccess() {
                super.onSetSuccess();
                createAnswer();
            }

            @Override
            public void onSetFailure(String s) {
                Log.e(TAG, "Failed to set remote description: " + s);
            }
        }, sessionDescription);
    }

    private void createAnswer() {
        Log.d(TAG, "Creating answer");
        MediaConstraints mediaConstraints = new MediaConstraints();
        mediaConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        mediaConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"));

        peerConnection.createAnswer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);
                Log.d(TAG, "Answer created successfully");
                peerConnection.setLocalDescription(new SimpleSdpObserver() {
                    @Override
                    public void onSetSuccess() {
                        Log.d(TAG, "Local description set successfully");
                    }

                    @Override
                    public void onSetFailure(String s) {
                        Log.e(TAG, "Failed to set local description: " + s);
                    }
                }, sessionDescription);
                try {
                    sendSDPAnswer(sessionDescription);
                } catch (JSONException e) {
                    Log.e(TAG, "Error sending answer: " + e.getMessage());
                }
            }

            @Override
            public void onCreateFailure(String s) {
                Log.e(TAG, "Failed to create answer: " + s);
            }
        }, mediaConstraints);
    }

    private void sendSDPAnswer(SessionDescription sessionDescription) throws JSONException {
        Log.d(TAG, "Sending SDP answer");
        JSONObject sdpJson = new JSONObject();
        sdpJson.put("type", sessionDescription.type.canonicalForm());
        sdpJson.put("sdp", sessionDescription.description);
        mSocket.emit("answer", sdpJson, "1234");
    }

    private void handleIceCandidate(IceCandidate iceCandidate) {
        try {
            Log.d(TAG, "Handling ICE candidate: " + iceCandidate.sdp);
            JSONObject candidateData = new JSONObject();
            candidateData.put("sdpMid", iceCandidate.sdpMid);
            candidateData.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);
            candidateData.put("candidate", iceCandidate.sdp);
            candidateData.put("roomName", "1234");
            mSocket.emit("ice", candidateData, "1234");
        } catch (JSONException e) {
            Log.e(TAG, "Error sending ICE candidate: " + e.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service being destroyed");
        super.onDestroy();
        reconnectHandler.removeCallbacks(reconnectRunnable);

        if (peerConnection != null) {
            peerConnection.dispose();
        }
        if (peerConnectionFactory != null) {
            peerConnectionFactory.dispose();
        }
        if (eglBase != null) {
            eglBase.release();
        }
        if (mSocket != null) {
            mSocket.disconnect();
            mSocket.off();
        }
    }

    public EglBase.Context getEglBaseContext() {
        return eglBase.getEglBaseContext();
    }

    private static class SimpleSdpObserver implements SdpObserver {
        @Override
        public void onCreateSuccess(SessionDescription sessionDescription) {}

        @Override
        public void onSetSuccess() {}

        @Override
        public void onCreateFailure(String s) {
            Log.e(TAG, "SDP onCreateFailure: " + s);
        }

        @Override
        public void onSetFailure(String s) {
            Log.e(TAG, "SDP onSetFailure: " + s);
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopSelf();
    }

}
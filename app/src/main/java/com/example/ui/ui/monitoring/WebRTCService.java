package com.example.ui.ui.monitoring;

import static android.content.ContentValues.TAG;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ServiceCompat;

import io.socket.client.IO;
import io.socket.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;

import org.webrtc.*;

public class WebRTCService extends Service {
    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnection;
    private Socket mSocket;
    private static final String TAG = "WebRTC_SERVICE";
    private static final String SOCKET_URL = "http://192.168.35.206:3000";
    private EglBase eglBase;
    private final IBinder binder = new LocalBinder();
    private VideoSink remoteVideoSink;
    private DataChannel.Observer dataChannelObserver;

    // Socket 관련 변수들
    private boolean isSocketConnected = false;
    private static final int RECONNECT_DELAY = 5000; // 5초
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private int reconnectAttempts = 0;
    private final Handler reconnectHandler = new Handler(Looper.getMainLooper());

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
    }

    public void setDataChannelObserver(DataChannel.Observer observer) {
        this.dataChannelObserver = observer;
        Log.d(TAG, "Data channel observer set");
    }

    @Override
    public void onCreate() {
        super.onCreate();
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
                if (!mediaStream.videoTracks.isEmpty() && remoteVideoSink != null) {
                    VideoTrack remoteVideoTrack = mediaStream.videoTracks.get(0);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        remoteVideoTrack.addSink(remoteVideoSink);
                    });
                }
            }

            @Override
            public void onRemoveStream(MediaStream mediaStream) {
                Log.d(TAG, "onRemoveStream");
            }

            @Override
            public void onDataChannel(DataChannel dataChannel) {
                Log.d(TAG, "onDataChannel");
                if (dataChannelObserver != null) {
                    dataChannel.registerObserver(dataChannelObserver);
                }
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
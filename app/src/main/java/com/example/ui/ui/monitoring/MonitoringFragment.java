package com.example.ui.ui.monitoring;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;
import android.widget.TextView;

import io.socket.client.IO;
import io.socket.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import org.webrtc.*;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.ui.R;

public class MonitoringFragment extends Fragment {
    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnection;
    private Socket mSocket;
    private static final String TAG = "WebRTC_LOCAL";
    private TextView statusText, predict;
    private static final String SOCKET_URL = "http://192.168.35.37:3000"; // 로컬 서버 주소
    private EglBase eglBase;
    private SurfaceViewRenderer remoteVideoView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_monitoring, container, false);

        predict = view.findViewById(R.id.predict);
        remoteVideoView = view.findViewById(R.id.remote_video_view);
        statusText = view.findViewById(R.id.statusText);
        eglBase = EglBase.create();
        initViews();
        initWebRTC();
        initSocket();
        return view;

    }
    private void initViews() {
        remoteVideoView.init(eglBase.getEglBaseContext(), null);
        remoteVideoView.setMirror(false);  // 로컬 테스트에서는 미러링 불필요
        remoteVideoView.setEnableHardwareScaler(true);
    }
    private void initWebRTC() {
        // WebRTC 초기화
        PeerConnectionFactory.InitializationOptions initializationOptions =
                PeerConnectionFactory.InitializationOptions.builder(requireContext())
                        .setEnableInternalTracer(true)
                        .createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);

        // PeerConnectionFactory 생성
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(new DefaultVideoEncoderFactory(eglBase.getEglBaseContext(), true, true))
                .setVideoDecoderFactory(new DefaultVideoDecoderFactory(eglBase.getEglBaseContext()))
                .createPeerConnectionFactory();

        createPeerConnection();
    }
    private void createPeerConnection() {
        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(new ArrayList<>());
        // 로컬 네트워크용 설정
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;

        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, new PeerConnection.Observer() {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                Log.d(TAG, "onIceCandidate: " + iceCandidate.sdp);
                handleIceCandidate(iceCandidate);
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                Log.d(TAG, "onAddStream: " + mediaStream.toString());

                requireActivity().runOnUiThread(() -> {
                    // UI 업데이트 코드 작성
                    if (!mediaStream.videoTracks.isEmpty()) {
                        VideoTrack remoteVideoTrack = mediaStream.videoTracks.get(0);
                        remoteVideoTrack.addSink(remoteVideoView);
                    }
                });
            }
            // 기타 필수 콜백 메서드들
            @Override
            public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
                Log.d(TAG, "onIceCandidatesRemoved");
            }

            @Override
            public void onRemoveStream(MediaStream mediaStream) {
                Log.d(TAG, "onRemoveStream");
            }

            @Override
            public void onDataChannel(DataChannel dataChannel) {
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
                                requireActivity().runOnUiThread(() -> {
                                    // UI 업데이트 코드 작성
                                    predict.setText("거북목 예측도 : " + text);
                                });
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
        });
    }

    private void initSocket() {
        try {
            IO.Options options = IO.Options.builder()
                    .setForceNew(true)
                    .setReconnection(true)
                    .setTransports(new String[] { "websocket" })
                    .build();

            mSocket = IO.socket(SOCKET_URL, options);
            setupSocketListeners();
            mSocket.connect();
        } catch (URISyntaxException e) {
            Log.e(TAG, "Socket initialization error: " + e.getMessage());
        }
    }

    private void setupSocketListeners() {
        mSocket.on(Socket.EVENT_CONNECT, args -> {
            Log.d(TAG, "Connected to server");

            requireActivity().runOnUiThread(() -> {
                // UI 업데이트 코드 작성
                statusText.setText("Connected to server");
            });
            // 연결 즉시 방 참여
            mSocket.emit("join_room", "1234");
        });

        mSocket.on(Socket.EVENT_DISCONNECT, args -> {
            Log.d(TAG, "Disconnected from server");
        });

        mSocket.on("offer", args -> {
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
            if (args.length > 0 && args[0] != null) {
                try {
                    JSONObject iceCandidateJson = (JSONObject) args[0];
                    IceCandidate iceCandidate = new IceCandidate(
                            iceCandidateJson.getString("sdpMid"),
                            iceCandidateJson.getInt("sdpMLineIndex"),
                            iceCandidateJson.getString("candidate")
                    );
                    peerConnection.addIceCandidate(iceCandidate);
                    Log.d(TAG, "Added remote ICE candidate: " + iceCandidate.sdp);
                } catch (JSONException e) {
                    Log.e(TAG, "Error handling ICE candidate: " + e.getMessage());
                }
            }
        });
    }

    private void handleOffer(JSONObject offerJson) throws JSONException {
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
        }, sessionDescription);
    }

    private void createAnswer() {
        MediaConstraints mediaConstraints = new MediaConstraints();
        mediaConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        mediaConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));

        peerConnection.createAnswer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);
                peerConnection.setLocalDescription(new SimpleSdpObserver(), sessionDescription);
                try {
                    sendSDPAnswer(sessionDescription);
                } catch (JSONException e) {
                    Log.e(TAG, "Error sending answer: " + e.getMessage());
                }
            }
        }, mediaConstraints);
    }

    private void sendSDPAnswer(SessionDescription sessionDescription) throws JSONException {
        JSONObject sdpJson = new JSONObject();
        sdpJson.put("type", sessionDescription.type.canonicalForm());
        sdpJson.put("sdp", sessionDescription.description);
        mSocket.emit("answer", sdpJson, "1234");
    }

    private void handleIceCandidate(IceCandidate iceCandidate) {
        try {
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

    private static class SimpleSdpObserver implements SdpObserver {
        @Override
        public void onCreateSuccess(SessionDescription sessionDescription) {}
        @Override
        public void onSetSuccess() {}
        @Override
        public void onCreateFailure(String s) {}
        @Override
        public void onSetFailure(String s) {}
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (remoteVideoView != null) {
            remoteVideoView.release();
        }
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
}

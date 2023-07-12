package com.example.poc_spandan_sdk_flutter_java;

import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import in.sunfox.healthcare.commons.android.sericom.SeriCom;
import in.sunfox.healthcare.commons.android.spandan_sdk.OnInitializationCompleteListener;
import in.sunfox.healthcare.commons.android.spandan_sdk.SpandanSDK;
import in.sunfox.healthcare.commons.android.spandan_sdk.collection.EcgTest;
import in.sunfox.healthcare.commons.android.spandan_sdk.collection.EcgTestCallback;
import in.sunfox.healthcare.commons.android.spandan_sdk.connection.OnDeviceConnectionStateChangeListener;
import in.sunfox.healthcare.commons.android.spandan_sdk.enums.DeviceConnectionState;
import in.sunfox.healthcare.commons.android.spandan_sdk.enums.EcgPosition;
import in.sunfox.healthcare.commons.android.spandan_sdk.enums.EcgTestType;
import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodChannel;

public class MainActivity extends FlutterActivity {
    private String TAG = "MainActivity.TAG";
    private String CHANNEL = "com.example.poc_spandan_sdk_flutter_java/sericom";
    private String CHANNEL_EVENT = "com.example.poc_spandan_sdk_flutter_java/sericom_event";

    private MethodChannel methodChannel = null;
    private EventChannel eventChannel = null;

    private SpandanSDK span;
    private String token = "";
    private EcgTest ecgTest = null;
    private final Map<EcgPosition, ArrayList<Double>> hashMap = new HashMap<>();

    private final MutableLiveData<ShareLeadData> shareResultData = new MutableLiveData<>();

    private int currentEcgIndex = 0;

    private ArrayList<Double> tempPointList = new ArrayList<>();

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);

        methodChannel = new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL);
        methodChannel.setMethodCallHandler(
                (call, result) -> {
                    switch (call.method) {
                        case "setUpConnection": {
                            setUpConnection();
                            result.success(true);
                        }
                        break;

                        case "sendCommand": {
                            String argument = call.argument("command");
                            if (argument.equals("0")) {
                                SeriCom.sendCommand(argument);
                                result.success(true);
                            } else if (argument.equals("1")) {
                                EcgPosition[] ecgPositionArray = {EcgPosition.LEAD_2};
                                performLeadTest(EcgTestType.LEAD_TWO, ecgPositionArray, 0, ecgPositionArray.length);
                            } else if (argument.equals("2")) {
                                EcgPosition[] ecgPositionArray = {EcgPosition.V1, EcgPosition.V2,
                                        EcgPosition.V3, EcgPosition.V4, EcgPosition.V5, EcgPosition.V6, EcgPosition.LEAD_1, EcgPosition.LEAD_2};
                                performLeadTest(EcgTestType.TWELVE_LEAD, ecgPositionArray, 0, ecgPositionArray.length);
                            } else {
                                SeriCom.sendCommand(argument);
                            }

                            result.success(true);
                        }
                        break;

                        default: {
                            result.notImplemented();
                        }
                    }
                }
        );

        eventChannel = new EventChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL_EVENT);
        eventChannel.setStreamHandler(new EventChannel.StreamHandler() {
            @Override
            public void onListen(Object arguments, EventChannel.EventSink events) {

                shareResultData.observe(MainActivity.this, shareLeadData -> {
                    Log.w("TEST_TAG", "2. onListen: " + " Key --> " + shareResultData.getValue().getResultString() + "\nEcgPoints Size --> " + tempPointList.size());
                    HashMap<String, ArrayList<Double>> temp = new HashMap<>();
                    temp.put(shareResultData.getValue().getResultString(), tempPointList);
                    events.success(temp);
                });
            }

            @Override
            public void onCancel(Object arguments) {

            }
        });
    }

    private void performLeadTest(EcgTestType testType, EcgPosition[] ecgPositionArray, int start, int end) {
        Log.d(TAG, "performLeadTest: EcgTestType --> " + testType + " EcgPositionArray -->" + Arrays.toString(ecgPositionArray));

//        final int[] currentEcgIndex = {start};
        currentEcgIndex = start;
        int lastEcgIndex = end;

        span = SpandanSDK.getInstance();
        if (currentEcgIndex < lastEcgIndex) {
            EcgPosition ecgPositionName = ecgPositionArray[currentEcgIndex];

            ecgTest = span.createTest(testType, new EcgTestCallback() {
                @Override
                public void onTestFailed(int statusCode) {
                    Log.e(TAG, "onTestFailed: " + statusCode);
                    //shareResultData.postValue(new ShareLeadData("Error in Test " + statusCode, sampleHashMap));
                }

                @Override
                public void onTestStarted(@NonNull EcgPosition ecgPosition) {
                    Log.d(TAG, "onTestStarted: --> " + ecgPosition);
                    //shareResultData.postValue(new ShareLeadData("Started : " + ecgPosition.name(), new ArrayList<>()));
                }

                @Override
                public void onElapsedTimeChanged(long elapsedTime, long remainingTime) {
                    Log.d(TAG, "onElapsedTimeChanged: Test Name : " + ecgPositionName.name() + "\nRemaining " + remainingTime + ": from " + elapsedTime);
                }

                @Override
                public void onReceivedData(@NonNull String data) {
                    Log.d(TAG, "onReceivedData: --> " + data);
                }

                @Override
                public void onPositionRecordingComplete(@NonNull EcgPosition ecgPosition, @Nullable ArrayList<Double> ecgPoints) {
                    String TAG = "TEST_TAG";
                    //Log.d(TAG, "onPositionRecordingComplete: EcgPosition --> " + ecgPosition + " : EcgPoints --> " + ecgPoints);

                    //put all the ecgPoints in hashmap to generate report
                    hashMap.put(ecgPosition, ecgPoints);

                    tempPointList.clear();
                    assert ecgPoints != null;
                    tempPointList.addAll(ecgPoints);

                    Log.w(TAG, "1. onPositionRecordingComplete: EcgPosition Name : " + ecgPosition.name() + " Points Length : " + ecgPoints.size());

                    shareResultData.postValue(new ShareLeadData(ecgPosition.name(), ecgPoints));

                    //generate report if currentTest is lastTest
                    if (currentEcgIndex == lastEcgIndex - 1) {
                        currentEcgIndex = 0;
                        Toast.makeText(MainActivity.this, "Report Generation work started...", Toast.LENGTH_SHORT).show();
                        Log.w("TEST_TAG", "onPositionRecordingComplete: Report Generation work started...");
                    } else if (currentEcgIndex < lastEcgIndex) {
                        currentEcgIndex++;
                        //start another task
                        performLeadTest(testType, ecgPositionArray, currentEcgIndex, lastEcgIndex);
                    }
                }
            }, token);

            ecgTest.start(ecgPositionName);
        }

    }

    private void setUpConnection() {
        SpandanSDK.initialize(getApplication(),
                "iDOSFfTw712TOGIu", new OnInitializationCompleteListener() {
                    @Override
                    public void onInitializationSuccess(@NonNull String authenticationToken) {
                        token = authenticationToken;
                        Log.d(TAG, "onInitializationSuccess: --> " + authenticationToken);
                        span = SpandanSDK.getInstance();
                        span.setOnDeviceConnectionStateChangedListener(new OnDeviceConnectionStateChangeListener() {
                            @Override
                            public void onDeviceConnectionStateChanged(@NonNull DeviceConnectionState deviceConnectionState) {
                                Log.d(TAG, "onDeviceConnectionStateChanged: --> " + deviceConnectionState);
                            }

                            @Override
                            public void onDeviceTypeChange(@NonNull String s) {

                            }

                            @Override
                            public void onDeviceVerified() {
                                Log.d(TAG, "onDeviceVerified:");
                            }
                        });
                    }

                    @Override
                    public void onInitializationFailed(@NonNull String s) {

                    }
                });
    }

}

package com.example.poc_spandan_sdk_flutter_java;

import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

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

public class MainActivity extends FlutterActivity {
    private String TAG = "MainActivity.TAG";

    private SpandanSDK span;
    private String token;
    private EcgTest ecgTest;

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);

        setUpConnection();

//        sendCommand("0");
//        sendCommand("1");
//        sendCommand("2");
    }

    private void sendCommand(String command) {
        switch (command) {
            case "0":
                SeriCom.sendCommand("0");
                break;

            case "1": {
                EcgPosition[] ecgPositionArray = {EcgPosition.LEAD_2};
                performLeadTest(EcgTestType.LEAD_TWO, ecgPositionArray, 0, ecgPositionArray.length);
            }
            break;

            case "2": {
                EcgPosition[] ecgPositionArray = {EcgPosition.V1,
                        EcgPosition.V2,
                        EcgPosition.V3,
                        EcgPosition.V4,
                        EcgPosition.V5,
                        EcgPosition.V6,
                        EcgPosition.LEAD_1,
                        EcgPosition.LEAD_2};
                performLeadTest(EcgTestType.TWELVE_LEAD, ecgPositionArray, 0, ecgPositionArray.length);
            }
            break;

            default: {
                SeriCom.sendCommand(command);
            }
            break;
        }
    }

    private void performLeadTest(EcgTestType testType, EcgPosition[] ecgPositionArray, int start, int end) {
        Log.d(TAG, "performLeadTest --> " + testType + " : " + ecgPositionArray);

        //do lead test
        final int[] currentEcgIndex = {start};
        int lastEcgIndex = end;
        span = SpandanSDK.getInstance();

        if (currentEcgIndex[0] < lastEcgIndex) {
            EcgPosition ecgPositionName = ecgPositionArray[currentEcgIndex[0]];

            ecgTest = span.createTest(testType, new EcgTestCallback() {
                @Override
                public void onTestFailed(int statusCode) {
                    Log.e(TAG, "onTestFailed: " + statusCode);
                }

                @Override
                public void onTestStarted(@NonNull EcgPosition ecgPosition) {
                    Log.d(TAG, "onTestStarted: EcgPosition -> " + ecgPosition);
                }

                @Override
                public void onElapsedTimeChanged(long elapsedTime, long remainingTime) {
                    Log.d(TAG, "onElapsedTimeChanged: " + elapsedTime + " : " + remainingTime);
                }

                @Override
                public void onReceivedData(@NonNull String data) {
                    Log.d(TAG, "onReceivedData: " + data);
                }

                @Override
                public void onPositionRecordingComplete(@NonNull EcgPosition ecgPosition, @Nullable ArrayList<Double> ecgPoints) {
                    if (ecgPoints != null) {
                        Log.d(TAG, "onPositionRecordingComplete: EcgPosition --> " + ecgPosition + " EcgPoints --> " + ecgPoints.size());
                    }

                    //generate report if currentTest is lastTest
                    if (currentEcgIndex[0] == lastEcgIndex - 1) {
                        Toast.makeText(
                                MainActivity.this,
                                "Report Generation work started...",
                                Toast.LENGTH_SHORT
                        ).show();

                        //generate report
                        /*span.generateReport(32, hashMap, token,
                            object : OnReportGenerationStateListener {
                                override fun onReportGenerationSuccess(ecgReport: EcgReport) {
                                    if (testType == EcgTestType.LEAD_TWO) {
                                        val conclusion = ecgReport.conclusion as LeadTwoConclusion
                                        val characteristics = ecgReport.ecgCharacteristics
                                        Log.d(TAG, "onReportGenerationSuccess:  Conclusion --> $conclusion : Characteristics --> $characteristics")
                                        runOnUiThread {
                                            resultData.value =
                                                "Detection --> ${conclusion.detection}\n" +
                                                        "EcgType --> ${conclusion.ecgType}\n" +
                                                        "BaseLine Wandering --> ${conclusion.baselineWandering}\n" +
                                                        "pWave Type --> ${conclusion.pWaveType}\n" +
                                                        "QRS Type --> ${conclusion.qrsType}\n" +
                                                        "PowerLine Interference --> ${conclusion.powerLineInterference}"+
                                                        "ECG Data --> ${ecgReport.ecgData}"

                                            Toast.makeText(this@MainActivity, "$ecgPositionName : Lead two report successful...${resultData.value}", Toast.LENGTH_SHORT).show()
                                        }
                                    }

                                    if (testType == EcgTestType.TWELVE_LEAD) {
                                        val conclusion = ecgReport.conclusion as TwelveLeadConclusion
                                        val characteristics = ecgReport.ecgCharacteristics
                                        Log.d(TAG, "onReportGenerationSuccess:  Conclusion --> $conclusion : Characteristics --> $characteristics")
                                        runOnUiThread {
                                            resultData.value =
                                                "Detection --> ${conclusion.detection}\n" +
                                                        "EcgType --> ${conclusion.ecgType}\n" +
                                                        "Anomalies --> ${conclusion.anomalies}\n" +
                                                        "Risk --> ${conclusion.risk}\n" +
                                                        "Recommendation --> ${conclusion.recommendation}\n"+
                                                        "ECG Data --> ${ecgReport.ecgData}"
                                            Toast.makeText(this@MainActivity, "$ecgPositionName : Twelve Lead report successful...${resultData.value}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }

                                override fun onReportGenerationFailed(
                                    errorCode: Int,
                                    errorMsg: String
                                ) {
                                    runOnUiThread {
                                        Log.e(TAG, "onReportGenerationFailed: $errorMsg")
                                        Toast.makeText(
                                            this@MainActivity,
                                            errorMsg,
                                            Toast.LENGTH_SHORT
                                        )
                                            .show()
                                    }
                                }
                            })*/
                    } else if (currentEcgIndex[0] < lastEcgIndex) {//0 < 1
                        currentEcgIndex[0]++;
                        //start another task
                        performLeadTest(testType, ecgPositionArray, currentEcgIndex[0], lastEcgIndex);
                    }
                }
            }, token);

            ecgTest.start(ecgPositionName);
        }
    }

    private void setUpConnection() {
        SpandanSDK.initialize(getApplication(), "master_key", new OnInitializationCompleteListener() {
            @Override
            public void onInitializationSuccess(@NonNull String authenticationToken) {
                token = authenticationToken;

                Log.d(TAG, "onInitializationSuccess: " + authenticationToken);
                span = SpandanSDK.getInstance();

                span.setOnDeviceConnectionStateChangedListener(new OnDeviceConnectionStateChangeListener() {
                    @Override
                    public void onDeviceConnectionStateChanged(@NonNull DeviceConnectionState deviceConnectionState) {
                        Log.d(TAG, "onDeviceConnectionStateChanged: " + deviceConnectionState);
                    }

                    @Override
                    public void onDeviceTypeChange(@NonNull String s) {

                    }

                    @Override
                    public void onDeviceVerified() {
                        Log.d(TAG, "onDeviceConnectionStateChanged: Device Verified");
                    }
                });
            }

            @Override
            public void onInitializationFailed(@NonNull String s) {

            }
        });
    }

}

package com.example.poc_spandan_sdk_flutter_java;

import java.util.ArrayList;

public class ShareLeadData {
    private String resultString;
    private ArrayList<Double> ecgPoints;

    public ShareLeadData(String resultString, ArrayList<Double> ecgPoints) {
        this.resultString = resultString;
        this.ecgPoints = ecgPoints;
    }

    public String getResultString() {
        return resultString;
    }

    public ArrayList<Double> getEcgPoints() {
        return ecgPoints;
    }

}

package com.rahul.moneywallet.service.syncworkers.devicesync;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

public class SyncDeviceProtocol {
    public static final Gson gson = new Gson();

    @NonNull
    public static String getDataToBeSent(ProtocolData data) {
        return gson.toJson(data);
        //return Strinng.join(";", result);
    }

    @NonNull
    public static ProtocolData getSeparatedData(String dataString) {
        return gson.fromJson(dataString, ProtocolData.class);
        //return dataString.split(";", -1);// Negative limit to include empty values in last columns
    }
}

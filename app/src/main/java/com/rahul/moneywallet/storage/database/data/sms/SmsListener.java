package com.rahul.moneywallet.storage.database.data.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

public class SmsListener extends BroadcastReceiver {
    SMSHandler smsHandler = new SMSHandler();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("SmsListenerNew", "Received");
        if (intent.getAction().equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
            Log.d("SmsListenerNew", "SMS Received");
            Bundle bundle = intent.getExtras();

            if (bundle != null) {
                Log.d("SmsListenerNew", "SMS Received Not null");
                try {
                    String format = bundle.getString("format");
                    byte[][] pdus = (byte[][]) bundle.get("pdus");
                    for (byte[] bytes : pdus) {
                        SmsMessage sms = SmsMessage.createFromPdu(bytes, format);
                        String messageBody = sms.getDisplayMessageBody();
                        String originatingAddress = sms.getOriginatingAddress();
                        String displayOriginatingAddress = sms.getDisplayOriginatingAddress();
                        long timestampMillis = sms.getTimestampMillis();
                        // Toast.makeText(context, originatingAddress + " " + displayOriginatingAddress + " " + messageBody, Toast.LENGTH_LONG)
                        // .show();
                        Log.d("SmsListenerNew", "SMS Received Toast Done");
                        smsHandler.handleSMS(context, originatingAddress, displayOriginatingAddress, messageBody, timestampMillis, true);
                    }
                } catch (Exception e) {
                    Log.d("SmsListenerNew", e.getMessage());
                    Log.d("SmsListenerNew", "SMS Received Toast Done");
                }
            }
        }
    }
}
package com.oriondev.moneywallet.storage.database.data.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

public class SmsListener extends BroadcastReceiver {
    public static final String SMS_INTENT_ACTION = "android.provider.Telephony.SMS_RECEIVED";
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
                    byte[][] pdus = (byte[][]) bundle.get("pdus");
                    for (int i = 0; i < pdus.length; i++) {
                        SmsMessage sms = SmsMessage.createFromPdu(pdus[i]);
                        String messageBody = sms.getDisplayMessageBody();
                        String originatingAddress = sms.getOriginatingAddress();
                        String displayOriginatingAddress = sms.getDisplayOriginatingAddress();
                        long timestampMillis = sms.getTimestampMillis();
                        Toast.makeText(context, originatingAddress + " " + displayOriginatingAddress + " " + messageBody, Toast.LENGTH_LONG).show();
                        Log.d("SmsListenerNew", "SMS Received Toast DOne");
                        smsHandler.handleSMS(context, originatingAddress, displayOriginatingAddress, messageBody, timestampMillis);
                    }
                } catch (Exception e) {
                    Log.d("SmsListenerNew", e.getMessage());
                    Log.d("SmsListenerNew", "SMS Received Toast Done");
                }
            }
        }
    }
}
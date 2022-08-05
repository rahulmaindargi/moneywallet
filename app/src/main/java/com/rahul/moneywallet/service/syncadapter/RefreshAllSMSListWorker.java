package com.rahul.moneywallet.service.syncadapter;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.Telephony;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.rahul.moneywallet.storage.database.data.sms.SMSHandler;

import java.net.URL;
import java.time.format.DateTimeFormatter;

public class RefreshAllSMSListWorker extends Worker {
    ContentResolver contentResolver;

    public RefreshAllSMSListWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        contentResolver = context.getContentResolver();
    }


    @NonNull
    @Override
    public Result doWork() {
        Log.d("SMSLoader", "Get doWork");
        URL url = null;
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:ss");
        SMSHandler handler = new SMSHandler();
        String[] projections = new String[]{Telephony.Sms.Inbox.DATE, Telephony.Sms.Inbox.ADDRESS, Telephony.Sms.Inbox.BODY,
                Telephony.Sms.Inbox.DATE_SENT};
        try (Cursor cursor = contentResolver.query(Telephony.Sms.Inbox.CONTENT_URI, projections, null, null,
                Telephony.Sms.Inbox.DEFAULT_SORT_ORDER)) {
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        String date = cursor.getString(0);
                        String address = cursor.getString(1);
                        String body = cursor.getString(2);
                        String date_sent = cursor.getString(3);
                        Log.d("RefreshAllSMSListWorker", "date : " + date);
                        Log.d("RefreshAllSMSListWorker", "date_sent : " + date_sent);
                        Log.d("RefreshAllSMSListWorker", "address : " + address);
                        Log.d("RefreshAllSMSListWorker", "body : " + body);
//                        Calendar calendar = Calendar.getInstance();
//                        calendar.setTimeInMillis(Long.parseLong(date));
                        //calendar.getTimeInMillis()
                        long dateVal = Long.parseLong(date);
                        dateVal = (dateVal / 1000) * 1000;

                        dateVal = Long.parseLong(date_sent);
                        dateVal = (dateVal / 1000) * 1000;
                        handler.handleSMS(getApplicationContext(), address, address, body, dateVal);
                        cursor.moveToNext();
                    } while (!cursor.isAfterLast());
                }
            }
        }

        return Result.success();
    }
}
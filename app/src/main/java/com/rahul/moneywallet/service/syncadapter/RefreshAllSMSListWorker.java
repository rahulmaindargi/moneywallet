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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

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

        SMSHandler handler = new SMSHandler();
        String[] projections = new String[]{Telephony.Sms.Inbox.DATE, Telephony.Sms.Inbox.ADDRESS, Telephony.Sms.Inbox.BODY,
                Telephony.Sms.Inbox.DATE_SENT};
        try (Cursor cursor = contentResolver.query(Telephony.Sms.Inbox.CONTENT_URI, projections, null, null, null)) {
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
                        if (checkSenderIsValid(address)) {
                            long dateVal = Long.parseLong(date_sent);
                            dateVal = (dateVal / 1000) * 1000;
                            handler.handleSMS(getApplicationContext(), address, address, body, dateVal);
                        }
                    } while (cursor.moveToNext());
                }
            }
        } catch (Throwable t) {
            try {
                StringWriter sw = new StringWriter();
                t.printStackTrace(new PrintWriter(sw));
                Files.write(new File(getApplicationContext().getExternalFilesDir(null), "RefreshSMSList.log").toPath(),
                        ("\n" + sw).getBytes(),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("SMSHandler", "Failed to Log exception", e);
            }
            throw t;
        }

        return Result.success();
    }

    private boolean checkSenderIsValid(String sender) {

        return (sender.trim().contains("+918586980859")
                || sender.contains("08586980869")
                || sender.contains("085869")
                || sender.contains("ICICIB")
                || sender.contains("HDFCBK")
                || sender.contains("SBIINB")
                || sender.contains("SBMSMS")
                || sender.contains("SCISMS")
                || sender.contains("CBSSBI")
                || sender.contains("SBIPSG")
                || sender.contains("SBIUPI")
                || sender.contains("SBICRD")
                || sender.contains("ATMSBI")
                || sender.contains("QPMYAMEX")
                || sender.contains("IDFCFB")
                || sender.contains("UCOBNK")
                || sender.contains("CANBNK")
                || sender.contains("BOIIND")
                || sender.contains("AXISBK")
                || sender.contains("PAYTMB")
                || sender.contains("UnionB")
                || sender.contains("INDBNK")
                || sender.contains("KOTAKB")
                || sender.contains("CENTBK")
                || sender.contains("SCBANK")
                || sender.contains("PNBSMS")
                || sender.contains("DOPBNK")
                || sender.contains("YESBNK")
                || sender.contains("IDBIBK")
                || sender.contains("ALBANK")
                || sender.contains("CITIBK")
                || sender.contains("ANDBNK")
                || sender.contains("BOBTXN")
                || sender.contains("IOBCHN")
                || sender.contains("MAHABK")
                || sender.contains("OBCBNK")
                || sender.contains("RBLBNK")
                || sender.contains("RBLCRD")
                || sender.contains("SPRCRD")
                || sender.contains("HSBCBK")
                || sender.contains("HSBCIN")
                || sender.contains("INDUSB")
                || sender.contains("CITIBA"));
    }
}
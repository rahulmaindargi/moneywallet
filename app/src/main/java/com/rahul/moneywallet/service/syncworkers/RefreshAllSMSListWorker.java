package com.rahul.moneywallet.service.syncworkers;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.Telephony;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.rahul.moneywallet.storage.database.Contract;
import com.rahul.moneywallet.storage.database.SyncContentProvider;
import com.rahul.moneywallet.storage.database.data.sms.SMSHandler;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
        ExecutorService executorService = null;
        try {

            executorService = Executors.newSingleThreadExecutor();
            List<Future<?>> futureList = new ArrayList<>();
            try {
                Files.write(new File(getApplicationContext().getExternalFilesDir(null), "no_format_matched.log").toPath(),
                        ("Refresh SMS Started at " + LocalDateTime.now()).getBytes(),
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("RefreshAllSMSListWorker", "Failed to Reset No format matched file", e);
            }

            ContentResolver contentResolver = getApplicationContext().getContentResolver();
            List<String> knownSenders = new ArrayList<>();
            try (Cursor cursor = contentResolver.query(SyncContentProvider.CONTENT_SMS_FORMAT, new String[]{"DISTINCT " + Contract.SMSFormat.SENDER}, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        knownSenders.add(cursor.getString(cursor.getColumnIndexOrThrow(Contract.SMSFormat.SENDER)));
                    } while (cursor.moveToNext());
                }
            }

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
                            if (checkSenderIsValid(knownSenders, address)) {
                                long dateVal = Long.parseLong(date_sent);
                                Runnable runnable = () -> {
                                    long finalDate = (dateVal / 1000) * 1000;
                                    handler.handleSMS(getApplicationContext(), address, address, body, finalDate, false);
                                };
                                futureList.add(executorService.submit(runnable));
                                // handler.handleSMS(getApplicationContext(), address, address, body, dateVal);
                            }
                        } while (cursor.moveToNext());
                        while (futureList.size() > 0) {
                            futureList.removeIf(Future::isDone);
                        }
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
                    Log.e("RefreshAllSMSListWorker", "Failed to Log exception", e);
                }
                throw t;
            }
        } finally {
            if (executorService != null) {
                executorService.shutdown();
            }
        }
        try {
            Files.write(new File(getApplicationContext().getExternalFilesDir(null), "no_format_matched.log").toPath(),
                    ("Refresh SMS Finished at " + LocalDateTime.now()).getBytes(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("RefreshAllSMSListWorker", "Failed to No format matched", e);
        }
        return Result.success();
    }

    private boolean checkSenderIsValid(List<String> knownSenders, String sender) {
        if (!knownSenders.isEmpty()) {
            return knownSenders.stream().anyMatch(sender::contains);
        }
        // If above working then remove
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
                || sender.contains("CITIBA")
                || sender.contains("CITIBN"));
    }
}
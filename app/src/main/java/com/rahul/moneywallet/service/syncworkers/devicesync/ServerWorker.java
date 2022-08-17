package com.rahul.moneywallet.service.syncworkers.devicesync;

import static com.rahul.moneywallet.storage.database.SyncContentProvider.CONTENT_TRANSACTION;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.rahul.moneywallet.model.CurrencyUnit;
import com.rahul.moneywallet.storage.database.Contract;
import com.rahul.moneywallet.storage.database.data.AbstractDataImporter;
import com.rahul.moneywallet.utils.CurrencyManager;
import com.rahul.moneywallet.utils.DateUtils;
import com.rahul.moneywallet.utils.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

public class ServerWorker extends Worker {

    private final int port;

    public ServerWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        port = workerParams.getInputData().getInt("port", 0);
    }

    @NonNull
    @Override
    public Result doWork() {

        Log.d("SyncDeviceActivityListener", "Listening on  port " + port);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            try (Socket accept = serverSocket.accept();
                 PrintStream printWriter = new PrintStream(accept.getOutputStream(), true);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(accept.getInputStream()))
            ) {

                printWriter.println(Utils.getDeviceID(getApplicationContext()));
                DataImporter importer = new DataImporter(getApplicationContext());
                Log.d("SyncDeviceActivityListener", "Waiting for Data");
                for (String dataString = reader.readLine(); dataString != null && !dataString.equalsIgnoreCase(SyncDeviceHelper.END_OF_DATA); dataString = reader.readLine()) {
                    Log.d("SyncDeviceActivityListener", "Received Data" + dataString);

                    ProtocolData data = SyncDeviceProtocol.getSeparatedData(dataString);
                    String selector = Contract.Transaction.DEVICE_SOURCE_ID + " =? AND " + Contract.Transaction.SYNC_SIDE_ID + "=?";
                    try (Cursor query = getContentResolver().query(CONTENT_TRANSACTION, new String[]{Contract.Transaction.ID}, selector, new String[]{data.deviceSourceId, data.id}, null)) {
                        if (query == null || !query.moveToFirst()) {
                            // No Result return..
                            // Insert the transaction
                            importer.insertTransaction(data.walletName, CurrencyManager.getCurrency(data.currency), data.category,
                                    DateUtils.getDateFromSQLDateTimeString(data.date), Long.parseLong(data.money),
                                    Integer.parseInt(data.direction), "(S)" + data.description, data.event, data.place, data.peopleNames, data.note,
                                    data.deviceSourceId, data.id, null);
                            printWriter.println("ID " + data.id + " Received");
                            Log.d("SyncDeviceActivityListener", "Received Record");
                        } else {
                            Log.i("SyncDeviceActivityListener", "Received Record Already exists");
                            printWriter.println("ID " + data.id + " Received");
                        }
                    }
                }
            } catch (IOException e) {
                Log.e("SyncDeviceActivityListener", "Socket connection failed", e);
                return Result.failure();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("SyncDeviceActivityListener", "Server Socket failed", e);
            return Result.failure();
        }

        return Result.success();
    }

    private ContentResolver getContentResolver() {
        return getApplicationContext().getContentResolver();
    }


    public static class DataImporter extends AbstractDataImporter {
        public DataImporter(Context context) {
            super(context);
        }

        public Uri insertTransaction(String wallet, CurrencyUnit currencyUnit, String category, Date datetime, Long money, int direction,
                                     String description, String event, String place, String people, String note, String deviceSourceId, String syncedSideId,
                                     String syncedWithList) {
            return super.insertTransaction(wallet, currencyUnit, category, datetime, money, direction, description, event, place, people, note, deviceSourceId, syncedSideId, syncedWithList);
        }

        @Override
        public void importData() {

        }

        @Override
        public void close() throws IOException {

        }
    }
}

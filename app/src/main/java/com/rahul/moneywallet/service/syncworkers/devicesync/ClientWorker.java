package com.rahul.moneywallet.service.syncworkers.devicesync;

import static com.rahul.moneywallet.storage.database.SyncContentProvider.CONTENT_TRANSACTION;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.rahul.moneywallet.storage.database.Contract;
import com.rahul.moneywallet.storage.database.DataContentProvider;
import com.rahul.moneywallet.storage.database.SyncContentProvider;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.StringJoiner;

public class ClientWorker extends Worker {

    private final String host;
    private final int port;

    public ClientWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        host = workerParams.getInputData().getString("host");
        port = workerParams.getInputData().getInt("port", 0);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("SyncDeviceActivityWriter", "Connecting Port " + port);
        PrintStream printWriter = null;

        try (Socket socket = new Socket(host, port);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            printWriter = new PrintStream(socket.getOutputStream());

            Log.d("SyncDeviceActivityWriter", "Connected Port " + port);
            String receiverId = reader.readLine();
            String selector = "( " + Contract.Transaction.SYNCED_WITH_LIST + " not like ? OR " + Contract.Transaction.SYNCED_WITH_LIST + " is null ) AND " + Contract.Transaction.SYNC_SIDE_ID + " is null";
            try (Cursor query = getContentResolver().query(DataContentProvider.CONTENT_TRANSACTIONS, null, selector, new String[]{"%" + receiverId + "%"}, null)) {
                if (query != null && query.moveToFirst()) {
                    do {
                        String rowId = query.getString(query.getColumnIndexOrThrow(Contract.Transaction.ID));
                        String peopleNames = null;
                        String peopleIds = query.getString(query.getColumnIndexOrThrow(Contract.Transaction.PEOPLE_IDS));
                        if (!StringUtils.isEmpty(peopleIds)) {
                            peopleIds = peopleIds.replaceAll("<", "");
                            peopleIds = peopleIds.replaceAll(">", "");
                            Log.d("SyncDeviceActivityWriter", "PeopleIDS : (" + peopleIds + ")");
                            String select = Contract.Person.ID + " in (" + peopleIds + ")";
                            try (Cursor peopleCur = getContentResolver().query(SyncContentProvider.CONTENT_PEOPLE, new String[]{Contract.Person.NAME}, select, null, null)) {
                                if (peopleCur != null && peopleCur.moveToFirst()) {
                                    StringJoiner joiner = new StringJoiner(",");
                                    do {
                                        joiner.add(peopleCur.getString(peopleCur.getColumnIndexOrThrow(Contract.Person.NAME)));
                                    } while (peopleCur.moveToNext());
                                    peopleNames = joiner.toString();
                                }
                            }
                        }
                        ProtocolData data = new ProtocolData(
                                query.getString(query.getColumnIndexOrThrow(Contract.Transaction.DEVICE_SOURCE_ID)),
                                rowId,
                                query.getString(query.getColumnIndexOrThrow(Contract.Transaction.WALLET_NAME)),
                                query.getString(query.getColumnIndexOrThrow(Contract.Transaction.WALLET_CURRENCY)),
                                query.getString(query.getColumnIndexOrThrow(Contract.Transaction.CATEGORY_NAME)),
                                query.getString(query.getColumnIndexOrThrow(Contract.Transaction.DATE)),
                                query.getString(query.getColumnIndexOrThrow(Contract.Transaction.MONEY)),
                                query.getString(query.getColumnIndexOrThrow(Contract.Transaction.DIRECTION)),
                                query.getString(query.getColumnIndexOrThrow(Contract.Transaction.DESCRIPTION)),
                                query.getString(query.getColumnIndexOrThrow(Contract.Transaction.EVENT_NAME)),
                                query.getString(query.getColumnIndexOrThrow(Contract.Transaction.PLACE_NAME)),
                                peopleNames,
                                query.getString(query.getColumnIndexOrThrow(Contract.Transaction.NOTE))
                        );
                        String result = SyncDeviceProtocol.getDataToBeSent(data);
                        printWriter.println(result);

                        Log.d("SyncDeviceActivityWriter", "Sent Record");
                        if (reader.readLine().contains(rowId)) {
                            String syncedWithList = query.getString(query.getColumnIndexOrThrow(Contract.Transaction.SYNCED_WITH_LIST));
                            syncedWithList = String.join(",", syncedWithList, receiverId);
                            ContentValues values = new ContentValues();
                            values.put(Contract.Transaction.SYNCED_WITH_LIST, syncedWithList);
                            String select = Contract.Transaction.ID + " = ?";
                            int updResult = getContentResolver().update(CONTENT_TRANSACTION, values, select, new String[]{rowId});
                            Log.d("SyncDeviceActivityWriter", "Sent Record Successful " + updResult);
                        } else {
                            Log.d("SyncDeviceActivityWriter", "Sent Record Not Successful ");
                            break;
                        }
                    } while (query.moveToNext());
                }
            }
            printWriter.flush();

        } catch (IOException e) {
            e.printStackTrace();
            Log.e("SyncDeviceActivityWriter", "Exception", e);
            return Result.failure();
        } finally {
            if (printWriter != null) {
                printWriter.println(SyncDeviceHelper.END_OF_DATA);
                Log.d("SyncDeviceActivityWriter", "EOD SENT");
                printWriter.close();
            }
        }
        return Result.success();
    }


    private ContentResolver getContentResolver() {
        return getApplicationContext().getContentResolver();
    }
}

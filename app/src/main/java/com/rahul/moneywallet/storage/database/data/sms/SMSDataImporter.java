package com.rahul.moneywallet.storage.database.data.sms;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.rahul.moneywallet.model.CurrencyUnit;
import com.rahul.moneywallet.storage.database.Contract;
import com.rahul.moneywallet.storage.database.DataContentProvider;
import com.rahul.moneywallet.storage.database.SQLDatabaseImporter;
import com.rahul.moneywallet.storage.database.SyncContentProvider;
import com.rahul.moneywallet.storage.database.data.AbstractDataImporter;
import com.rahul.moneywallet.storage.database.model.SMSMessage;

import java.util.Date;


public class SMSDataImporter extends AbstractDataImporter {
    public SMSDataImporter(Context context) {
        super(context);
    }

    @Override
    public Uri insertTransaction(String wallet, CurrencyUnit currencyUnit, String category, Date datetime, Long money, int direction,
                                 String description, String event, String place, String people, String note, String deviceSourceId, String syncedSideId, String syncedWithList) {
        Uri uri = DataContentProvider.CONTENT_TRANSACTIONS;
        String[] projection = new String[]{Contract.Transaction.CATEGORY_NAME};
        String selection = Contract.Transaction.DESCRIPTION + " =? AND " + Contract.Transaction.DIRECTION + " =? ";
        String[] selectionArgs = new String[]{description, String.valueOf(direction)};
        String sortOrder = Contract.Transaction.DATE + " DESC";
        try (Cursor cursor = getContext().getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder)) {
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    // Use existing category based on description if found. Otherwise use passed category
                    category = cursor.getString(cursor.getColumnIndexOrThrow(Contract.Transaction.CATEGORY_NAME));
                }
            }
        }
        uri = super.insertTransaction(wallet, currencyUnit, category, datetime, money, direction, description, event, place, people, note, deviceSourceId, syncedSideId, syncedWithList);

        Log.d("SMSDataImporter", "Transaction Inserted ");
        return uri;
    }

    @Override
    protected long getOrCreateWallet(ContentResolver contentResolver, String name, CurrencyUnit currencyUnit) {
        Uri uri = DataContentProvider.CONTENT_WALLETS;
        String[] projection = new String[]{Contract.Wallet.ID};
        String selection =
                "( " + Contract.Wallet.NOTE + " LIKE '%'|| ?||'%' OR " + Contract.Wallet.NAME + " = ? ) AND " + Contract.Wallet.CURRENCY +
                        " = ?";
        String[] selectionArgs = new String[]{name, name, currencyUnit.getIso()};
        String sortOrder = Contract.Wallet.ID + " DESC";
        Cursor cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getLong(cursor.getColumnIndexOrThrow(Contract.Wallet.ID));
                }
            } finally {
                cursor.close();
            }
        }
        // if we reached this line, the wallet does not exists and we have to create it
        ContentValues contentValues = new ContentValues();
        contentValues.put(Contract.Wallet.NAME, name);
        contentValues.put(Contract.Wallet.ICON, generateRandomIcon(name));
        contentValues.put(Contract.Wallet.CURRENCY, currencyUnit.getIso());
        contentValues.put(Contract.Wallet.COUNT_IN_TOTAL, true);
        contentValues.put(Contract.Wallet.START_MONEY, 0L);
        contentValues.put(Contract.Wallet.ARCHIVED, false);
        Uri result = contentResolver.insert(uri, contentValues);
        if (result == null) {
            throw new RuntimeException("Failed to create the new wallet");
        }
        return ContentUris.parseId(result);
    }

    @Override
    public void importData() {

    }

    @Override
    public void close() {

    }

    public boolean insertSMS(String id, String message) {
        ContentResolver contentResolver = getContext().getContentResolver();
        String selection = Contract.SMSMessage.DELETED + " = 0 ";
        selection += "AND " + Contract.SMSMessage.ID + " = ?";
        try (Cursor cursor = contentResolver.query(SyncContentProvider.CONTENT_SMS_MESSAGE, null, selection, new String[]{id}, null)) {
            if (cursor != null) {
                cursor.moveToFirst();
                if (!cursor.isAfterLast()) {
                    // Message with ID already exists
                    Log.d("SMSDataImporter", "Message id already exists. Id:- " + id);
                    return false;
                }
            }
        }
        SMSMessage smsMessage = new SMSMessage();
        smsMessage.mUUID = id;
        smsMessage.mMessage = message;
        smsMessage.mLastEdit = System.currentTimeMillis();
        SQLDatabaseImporter.insert(contentResolver, smsMessage);
        Log.d("SMSDataImporter", "Message Inserted id:- " + id);
        return true;
    }
}
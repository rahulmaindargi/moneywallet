package com.rahul.moneywallet.storage.database.data.csv;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;

import androidx.documentfile.provider.DocumentFile;

import com.opencsv.CSVWriter;
import com.rahul.moneywallet.model.CurrencyUnit;
import com.rahul.moneywallet.model.Wallet;
import com.rahul.moneywallet.storage.database.Contract;
import com.rahul.moneywallet.storage.database.data.AbstractDataExporter;
import com.rahul.moneywallet.utils.CurrencyManager;
import com.rahul.moneywallet.utils.MoneyFormatter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by andrea on 21/12/18.
 */
public class CSVDataExporter extends AbstractDataExporter {

    private final CSVWriter mWriter;
    private final MoneyFormatter mMoneyFormatter;
    private final Uri mFileUri;

    private boolean mShouldLoadPeople = false;

    public CSVDataExporter(Context context, Uri folder) throws IOException {
        super(context);
        DocumentFile documentFile = DocumentFile.fromTreeUri(context, folder);
        DocumentFile file = documentFile.createFile("text/csv", getDefaultFileName(".csv"));
        mFileUri = file.getUri();
        ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(mFileUri, "w");
        FileOutputStream fileOutputStream = new ParcelFileDescriptor.AutoCloseOutputStream(pfd);
        // FileOutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());

        mWriter = new CSVWriter(new OutputStreamWriter(fileOutputStream));
        mMoneyFormatter = MoneyFormatter.getInstance();
        mMoneyFormatter.setCurrencyEnabled(false);
        mMoneyFormatter.setRoundDecimalsEnabled(false);
        mMoneyFormatter.setGroupDigitEnabled(false);
    }

    @Override
    public boolean isMultiWalletSupported() {
        // in a csv file we cannot create different sections for the transactions of
        // each wallet so we have to list all the transactions inside the same file
        return false;
    }

    @Override
    public String[] getColumns(boolean uniqueWallet, String[] optionalColumns) {
        List<String> contractColumns = new ArrayList<>();
        contractColumns.add(Constants.COLUMN_WALLET);
        contractColumns.add(Constants.COLUMN_CURRENCY);
        contractColumns.add(Constants.COLUMN_CATEGORY);
        contractColumns.add(Constants.COLUMN_DATETIME);
        contractColumns.add(Constants.COLUMN_MONEY);
        contractColumns.add(Constants.COLUMN_DESCRIPTION);
        contractColumns.add(Constants.COLUMN_DEVICE_SOURCE_ID);
        contractColumns.add(Constants.COLUMN_SYNCED_SIDE_ID);
        contractColumns.add(Constants.COLUMN_SYNCED_WITH_LIST);
        if (optionalColumns != null) {
            for (String column : optionalColumns) {
                switch (column) {
                    case COLUMN_EVENT:
                        contractColumns.add(Constants.COLUMN_EVENT);
                        break;
                    case COLUMN_PEOPLE:
                        contractColumns.add(Constants.COLUMN_PEOPLE);
                        mShouldLoadPeople = true;
                        break;
                    case COLUMN_PLACE:
                        contractColumns.add(Constants.COLUMN_PLACE);
                        break;
                    case COLUMN_NOTE:
                        contractColumns.add(Constants.COLUMN_NOTE);
                        break;
                }
            }
        }
        return contractColumns.toArray(new String[0]);
    }

    @Override
    public boolean shouldLoadPeople() {
        return mShouldLoadPeople;
    }

    @Override
    public void exportData(Cursor cursor, String[] columns, Wallet... wallets) {
        // initialize the header line
        mWriter.writeNext(columns);
        // export all the rows
        while (cursor.moveToNext()) {
            // for each row, we need to export all the fields as string
            String[] csvRow = new String[columns.length];
            for (int i = 0; i < columns.length; i++) {
                switch (columns[i]) {
                    case Constants.COLUMN_WALLET:
                        csvRow[i] = cursor.getString(cursor.getColumnIndexOrThrow(Contract.Transaction.WALLET_NAME));
                        break;
                    case Constants.COLUMN_CURRENCY:
                        csvRow[i] = cursor.getString(cursor.getColumnIndexOrThrow(Contract.Transaction.WALLET_CURRENCY));
                        break;
                    case Constants.COLUMN_CATEGORY:
                        csvRow[i] = cursor.getString(cursor.getColumnIndexOrThrow(Contract.Transaction.CATEGORY_NAME));
                        break;
                    case Constants.COLUMN_DATETIME:
                        csvRow[i] = cursor.getString(cursor.getColumnIndexOrThrow(Contract.Transaction.DATE));
                        break;
                    case Constants.COLUMN_MONEY:
                        CurrencyUnit currencyUnit = CurrencyManager.getCurrency(cursor.getString(cursor.getColumnIndexOrThrow(Contract.Transaction.WALLET_CURRENCY)));
                        long money = cursor.getLong(cursor.getColumnIndexOrThrow(Contract.Transaction.MONEY));
                        int direction = cursor.getInt(cursor.getColumnIndexOrThrow(Contract.Transaction.DIRECTION));
                        if (direction == Contract.Direction.EXPENSE) {
                            money *= -1;
                        }
                        csvRow[i] = mMoneyFormatter.getNotTintedString(currencyUnit, money);
                        break;
                    case Constants.COLUMN_DESCRIPTION:
                        csvRow[i] = cursor.getString(cursor.getColumnIndexOrThrow(Contract.Transaction.DESCRIPTION));
                        break;
                    case Constants.COLUMN_EVENT:
                        csvRow[i] = cursor.getString(cursor.getColumnIndexOrThrow(Contract.Transaction.EVENT_NAME));
                        break;
                    case Constants.COLUMN_PEOPLE:
                        List<Long> peopleIds = Contract.parseObjectIds(cursor.getString(cursor.getColumnIndexOrThrow(Contract.Transaction.PEOPLE_IDS)));
                        if (peopleIds != null && !peopleIds.isEmpty()) {
                            StringBuilder builder = new StringBuilder();
                            for (Long personId : peopleIds) {
                                String name = getPersonName(personId);
                                if (!TextUtils.isEmpty(name)) {
                                    if (builder.length() > 0) {
                                        builder.append(",");
                                    }
                                    builder.append(name);
                                }
                            }
                            csvRow[i] = builder.toString();
                        } else {
                            csvRow[i] = null;
                        }
                        break;
                    case Constants.COLUMN_PLACE:
                        csvRow[i] = cursor.getString(cursor.getColumnIndexOrThrow(Contract.Transaction.PLACE_NAME));
                        break;
                    case Constants.COLUMN_NOTE:
                        csvRow[i] = cursor.getString(cursor.getColumnIndexOrThrow(Contract.Transaction.NOTE));
                        break;
                    case Constants.COLUMN_DEVICE_SOURCE_ID:
                        csvRow[i] = cursor.getString(cursor.getColumnIndexOrThrow(Contract.Transaction.DEVICE_SOURCE_ID));
                        break;
                    case Constants.COLUMN_SYNCED_SIDE_ID:
                        csvRow[i] = cursor.getString(cursor.getColumnIndexOrThrow(Contract.Transaction.SYNC_SIDE_ID));
                        break;
                    case Constants.COLUMN_SYNCED_WITH_LIST:
                        csvRow[i] = cursor.getString(cursor.getColumnIndexOrThrow(Contract.Transaction.SYNCED_WITH_LIST));
                        if (csvRow[i] != null) {
                            csvRow[i] = csvRow[i].replaceAll(",", ";");
                        }
                        break;
                }
            }
            mWriter.writeNext(csvRow);
        }
    }

    @Override
    public void close() throws IOException {
        mWriter.close();
    }

    @Override
    public Uri getOutputFileUri() {
        return mFileUri;
    }

    @Override
    public String getResultType() {
        return "application/vnd.ms-excel";
    }
}
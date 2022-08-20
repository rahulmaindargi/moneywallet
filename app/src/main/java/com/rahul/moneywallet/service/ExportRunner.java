package com.rahul.moneywallet.service;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.rahul.moneywallet.broadcast.LocalAction;
import com.rahul.moneywallet.model.DataFormat;
import com.rahul.moneywallet.model.Wallet;
import com.rahul.moneywallet.storage.database.Contract;
import com.rahul.moneywallet.storage.database.DataContentProvider;
import com.rahul.moneywallet.storage.database.data.AbstractDataExporter;
import com.rahul.moneywallet.storage.database.data.csv.CSVDataExporter;
import com.rahul.moneywallet.storage.database.data.pdf.PDFDataExporter;
import com.rahul.moneywallet.storage.database.data.xls.XLSDataExporter;
import com.rahul.moneywallet.utils.DateUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ExportRunner implements Runnable {
    public static final String RESULT_FILE_URI = "ExportRunner::Results::FileUri";
    public static final String RESULT_FILE_TYPE = "ExportRunner::Results::FileType";
    public static final String EXCEPTION = "ExportRunner::Results::Exception";
    private final LocalBroadcastManager mBroadcastManager;
    Context context;
    DataFormat dataFormat;
    Date startDate;
    Date endDate;
    Wallet[] wallets;
    Uri folderUi;
    boolean uniqueWallet;
    String[] optionalColumns;

    public ExportRunner(Context context, @NonNull DataFormat dataFormat, Date startDate, Date endDate, @NonNull Wallet[] wallets, @NonNull Uri folderUi, boolean uniqueWallet, String[] optionalColumns) {
        this.context = context;
        this.dataFormat = dataFormat;
        this.startDate = startDate;
        this.endDate = endDate;
        this.wallets = wallets;
        this.folderUi = folderUi;
        this.uniqueWallet = uniqueWallet;
        this.optionalColumns = optionalColumns;
        mBroadcastManager = LocalBroadcastManager.getInstance(context);
    }

    @Override
    public void run() {
        try {
            notifyTaskStarted();
            // check necessary parameters
            if (dataFormat == null) {
                throw new IllegalArgumentException("parameter is null [FORMAT]");
            }
            if (wallets == null || wallets.length == 0) {
                throw new IllegalArgumentException("parameter is null or empty [WALLETS]");
            }
            if (folderUi == null) {
                throw new IllegalArgumentException("parameter is null or not a directory [FOLDER]");
            }
            // initialize the correct data exporter
            AbstractDataExporter dataExporter = getDataExporter(dataFormat, folderUi);
            ContentResolver contentResolver = context.getContentResolver();
            Uri uri = DataContentProvider.CONTENT_TRANSACTIONS;
            // initialize the selection builder with common variables
            StringBuilder selectionBuilder = new StringBuilder();
            List<String> selectionArguments = new ArrayList<>();
            // append rule to limit to the end date or to the current date
            selectionBuilder.append("DATE (" + Contract.Transaction.DATE + ") <= DATE(?)");
            selectionArguments.add(DateUtils.getSQLDateString(getFixedEndDate(endDate)));
            // if provided, apply a rule to the start date
            if (startDate != null) {
                selectionBuilder.append(" AND DATE (" + Contract.Transaction.DATE + ") >= DATE(?)");
                selectionArguments.add(DateUtils.getSQLDateString(startDate));
            }
            String sortOrder = Contract.Transaction.DATE + " DESC";
            // check if we should create a unique wallet or if we can export each wallet
            // in a separate way
            boolean multiWallet = wallets.length > 1 && dataExporter.isMultiWalletSupported() && !uniqueWallet;
            String[] columns = dataExporter.getColumns(!multiWallet, optionalColumns);
            // before starting with the export logic, check if the exporter should
            // store the people names into his internal cache to speedup the procedure
            if (dataExporter.shouldLoadPeople()) {
                Cursor cursor = contentResolver.query(DataContentProvider.CONTENT_PEOPLE, null, null, null, null);
                if (cursor != null) {
                    dataExporter.cachePeople(cursor);
                    cursor.close();
                }
            }
            // handle the export logic differently
            if (multiWallet) {
                // execute a query for each wallet: we should clone the original builder
                // to avoid mistakes during each successive query
                for (Wallet wallet : wallets) {
                    String selection = selectionBuilder + " AND " + Contract.Transaction.WALLET_ID + " = ?";
                    String[] arguments = selectionArguments.toArray(new String[selectionArguments.size() + 1]);
                    arguments[arguments.length - 1] = String.valueOf(wallet.getId());
                    Cursor cursor = contentResolver.query(uri, null, selection, arguments, sortOrder);
                    if (cursor != null) {
                        dataExporter.exportData(cursor, columns, wallet);
                        cursor.close();
                    }
                }
            } else {
                // execute only a large query: we can modify the original builder here
                selectionBuilder.append(" AND (");
                for (int i = 0; i < wallets.length; i++) {
                    if (i != 0) {
                        selectionBuilder.append(" OR ");
                    }
                    selectionBuilder.append(Contract.Transaction.WALLET_ID + " = ?");
                    selectionArguments.add(String.valueOf(wallets[i].getId()));
                }
                selectionBuilder.append(")");
                Cursor cursor = contentResolver.query(uri, null, selectionBuilder.toString(), selectionArguments.toArray(new String[0]), sortOrder);
                if (cursor != null) {
                    dataExporter.exportData(cursor, columns, wallets);
                    cursor.close();
                }
            }
            // close the exporter to flush and close all the open streams
            dataExporter.close();
            // if no exception has been thrown so far, we can ask the exporter
            // for the output file: we can pass the uri of this file inside the intent
            // Uri resultUri = Uri.fromFile(dataExporter.getOutputFile());


            Uri resultUri = dataExporter.getOutputFileUri();
            String resultType = dataExporter.getResultType();
            // send a successful intent to inform the receivers that the operation succeed
            notifyTaskFinished(resultUri, resultType);
        } catch (Exception e) {
            e.printStackTrace();
            notifyTaskFailed(e);
        }

    }

    private void notifyTaskStarted() {
        Intent intent = new Intent(LocalAction.ACTION_EXPORT_SERVICE_STARTED);
        mBroadcastManager.sendBroadcast(intent);
    }

    private void notifyTaskFinished(Uri resultUri, String resultType) {
        Intent intent = new Intent(LocalAction.ACTION_EXPORT_SERVICE_FINISHED);
        intent.putExtra(RESULT_FILE_URI, resultUri);
        intent.putExtra(RESULT_FILE_TYPE, resultType);
        mBroadcastManager.sendBroadcast(intent);
    }

    private void notifyTaskFailed(Exception exception) {
        Intent intent = new Intent(LocalAction.ACTION_EXPORT_SERVICE_FAILED);
        intent.putExtra(EXCEPTION, exception);
        mBroadcastManager.sendBroadcast(intent);
    }

    private AbstractDataExporter getDataExporter(DataFormat dataFormat, Uri uri) throws IOException {
        switch (dataFormat) {
            case CSV:
                return new CSVDataExporter(context, uri);
            case XLS:
                return new XLSDataExporter(context, uri);
            case PDF:
                return new PDFDataExporter(context, uri);
            default:
                throw new RuntimeException("DataFormat not supported");
        }
    }

    private Date getFixedEndDate(Date endDate) {
        Date now = new Date();
        if (endDate != null) {
            long minMillis = Math.min(now.getTime(), endDate.getTime());
            return new Date(minMillis);
        }
        return now;
    }
}

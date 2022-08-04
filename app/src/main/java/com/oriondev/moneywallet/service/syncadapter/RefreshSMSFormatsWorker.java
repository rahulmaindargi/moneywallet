package com.oriondev.moneywallet.service.syncadapter;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.oriondev.moneywallet.storage.database.ImportException;
import com.oriondev.moneywallet.storage.database.SyncContentProvider;
import com.oriondev.moneywallet.storage.database.json.JSONDatabaseImporter;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class RefreshSMSFormatsWorker extends Worker {
    ContentResolver contentResolver;
    public RefreshSMSFormatsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
         contentResolver = context.getContentResolver();
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    @NonNull
    @Override
    public Result doWork() {
        Log.d("SMSWorker", "Get doWork");
        URL url = null;
        StringBuilder stringBuilder = new StringBuilder();
        try {

            url = new URL("https://drive.google.com/uc?export=download&id=1Zbtn6RudgxKykXaXaxDKAQbZ6oeecha9");
           // url= new URL("https://drive.google.com/file/d/1Zbtn6RudgxKykXaXaxDKAQbZ6oeecha9/view?usp=sharing");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            String line;
            while ((line = bufferedReader.readLine()) != null)
            {
                stringBuilder.append(line);
            }
            bufferedReader.close();
        } catch (IOException e) {
            Log.e("SMSFormats", "Exception "+e.getMessage());
            e.printStackTrace();
            return Result.failure();
        }
        String smsFormats = stringBuilder.toString();
        Log.d("SMSFormats", smsFormats);

        JSONObject obj= null;
        try {
            //obj = new JSONObject(smsFormats);
            //JSONArray smsFormat = obj.getJSONArray("smsFormat");
            ContentResolver contentResolver = getApplicationContext().getContentResolver();
            Uri uri = SyncContentProvider.CONTENT_SMS_FORMAT;
            contentResolver.delete(uri,new Bundle());
            Log.d("SMSFormats", "Data Deleted");
            JSONDatabaseImporter importer= new JSONDatabaseImporter(new ByteArrayInputStream(smsFormats.getBytes()));
            importer.importSMSFormats(contentResolver);
            Log.d("SMSFormats", "Data Inserted");
        } catch ( ImportException e) {
            Log.e("SMSFormats", "Exception "+e.getMessage());
            e.printStackTrace();
            return Result.failure();
        }

        return Result.success();
    }
}
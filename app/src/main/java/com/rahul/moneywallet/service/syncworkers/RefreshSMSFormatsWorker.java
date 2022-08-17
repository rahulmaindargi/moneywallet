package com.rahul.moneywallet.service.syncworkers;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rahul.moneywallet.storage.database.ImportException;
import com.rahul.moneywallet.storage.database.SyncContentProvider;
import com.rahul.moneywallet.storage.database.json.JSONDatabaseImporter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class RefreshSMSFormatsWorker extends Worker {
    ContentResolver contentResolver;

    public RefreshSMSFormatsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        contentResolver = context.getContentResolver();
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("SMSFormatWorker", "doWork");
        URL url;
        StringBuilder stringBuilder = new StringBuilder();
        try {

            //url = new URL("https://drive.google.com/uc?export=download&id=1Zbtn6RudgxKykXaXaxDKAQbZ6oeecha9");
            //url = new URL("https://raw.githubusercontent.com/rahulmaindargi/moneywallet/master/app/src/main/assets/resources/SMS_Transaction_Format.json");

            url = new URL("https://raw.githubusercontent.com/rahulmaindargi/moneywallet/master/app/src/main/assets/resources/sms_formats/Supported_Bank_List.json");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            bufferedReader.close();

        } catch (IOException e) {
            Log.e("SMSFormats", "Exception " + e.getMessage());
            e.printStackTrace();
            return Result.failure();
        }

        JsonObject jsonObject = new Gson().fromJson(stringBuilder.toString(), JsonObject.class);
        JsonArray bankList = jsonObject.getAsJsonArray("bankList");
        List<String> formatLinks = new ArrayList<>();
        for (JsonElement bankElem : bankList) {
            JsonObject bank = bankElem.getAsJsonObject();
            String formatsLink = bank.get("formatsLink").getAsString();
            formatLinks.add(formatsLink);
        }
        ContentResolver contentResolver = getApplicationContext().getContentResolver();
        Uri uri = SyncContentProvider.CONTENT_SMS_FORMAT;
        contentResolver.delete(uri, new Bundle());
        Log.d("SMSFormats", "Data Deleted");
        JSONDatabaseImporter.resetFormatId();
        formatLinks.parallelStream().forEach(link -> handleBankFormatsLink(contentResolver, link));
        return Result.success();
    }

    private void handleBankFormatsLink(ContentResolver contentResolver, String link) {
        StringBuilder builder = new StringBuilder();
        try {
            URL formatUrl = new URL(link);

            HttpURLConnection conn = (HttpURLConnection) formatUrl.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                builder.append(line);
            }
            bufferedReader.close();

        } catch (IOException e) {
            e.printStackTrace();
            Log.e("SMSFormats", "SMSFormats Reading failed for " + link, e);
            throw new RuntimeException(e);
        }

        String smsFormats = builder.toString();
        Log.d("SMSFormats", smsFormats);
        try {
            JSONDatabaseImporter importer = new JSONDatabaseImporter(new ByteArrayInputStream(smsFormats.getBytes()));
            importer.importSMSFormats(contentResolver);
            Log.d("SMSFormats", "Data Inserted for " + link);
        } catch (ImportException e) {
            Log.e("SMSFormats", "Exception " + e.getMessage() + " for link " + link, e);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
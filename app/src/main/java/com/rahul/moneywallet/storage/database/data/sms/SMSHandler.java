package com.rahul.moneywallet.storage.database.data.sms;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.rahul.moneywallet.R;
import com.rahul.moneywallet.broadcast.LocalAction;
import com.rahul.moneywallet.broadcast.Message;
import com.rahul.moneywallet.model.CurrencyUnit;
import com.rahul.moneywallet.storage.database.Contract;
import com.rahul.moneywallet.storage.database.SyncContentProvider;
import com.rahul.moneywallet.utils.CurrencyManager;
import com.rahul.moneywallet.utils.Utils;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.sql.Date;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SMSHandler {

    public static final String TRANSACTION_ADDED = "TRANSACTION_ADDED";

    private void createNotificationChannel(Context context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        CharSequence name = "Transaction Notification Channel";
        String description = "Notification about transaction added";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(TRANSACTION_ADDED, name, importance);
        channel.setDescription(description);
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    public void handleSMS(Context context, String originatingAddress, String dispOriginatingAddress, String message, long timestampMillis, boolean showNotification) {
        Log.d("SMSHandler", "handleSMS");
        ContentResolver contentResolver = context.getContentResolver();
        ParsedDetails details = getParsedDetails(SyncContentProvider.CONTENT_SMS_FORMAT, contentResolver, originatingAddress,
                dispOriginatingAddress, message, timestampMillis, context);
        if (details == null) return;
        // System.out.println(details);
        String id =
                details.account + details.amount + details.otherParty + details.dateTime +
                        details.type + originatingAddress + dispOriginatingAddress;
        Log.d("SMSHandler", "SMSID:= " + id);
        SMSDataImporter dataImporter = new SMSDataImporter(context);

        if (dataImporter.insertSMS(id, message)) {
            Log.d("SMSHandler", "SMS Inserted ");
            CurrencyUnit currencyUnit = CurrencyManager.getDefaultCurrency();
            BigDecimal decimalMultiply = BigDecimal.valueOf(Math.pow(10, currencyUnit.getDecimals()));
            BigDecimal moneyDecimal = decimalMultiply.multiply(new BigDecimal(details.amount));
            long money = moneyDecimal.longValue();
            Uri insertedUri = dataImporter.insertTransaction(details.account, currencyUnit, "Misc",
                    Date.from(details.dateTime.atZone(ZoneId.systemDefault()).toInstant())
                    , money, "debit".equalsIgnoreCase(details.type) ? 0 : 1, details.otherParty, null,
                    null, null, message, Utils.getDeviceID(context), null, null);
            if (showNotification) {
                createNotificationChannel(context);
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, TRANSACTION_ADDED)
                        .setSmallIcon(R.mipmap.ic_launcher) // notification icon
                        .setContentTitle("Transaction Added!") // title for notification
                        .setContentText(String.format("%s %s %s %s", details.type, details.amount, "debit".equalsIgnoreCase(details.type) ? "to" : "from", details.otherParty)) // message for notification
                        .setAutoCancel(true) // clear notification after click
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                //Intent intent = new Intent(context, MainActivity.class);
                long insertedId = Long.parseLong(insertedUri.getLastPathSegment());
                Intent intent = getIntent(context, insertedId);
                PendingIntent pi = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
                mBuilder.setContentIntent(pi);
                NotificationManagerCompat manager = NotificationManagerCompat.from(context);
                manager.notify(200, mBuilder.build());
//                NotificationManager mNotificationManager =
//                        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//                mNotificationManager.notify(200, mBuilder.build());
            }
        }
    }

    private Intent getIntent(Context context, long id) {
        Intent intent = new Intent(LocalAction.ACTION_ITEM_CLICK);
        intent.putExtra(Message.ITEM_ID, id);
        intent.putExtra(Message.ITEM_TYPE, Message.TYPE_TRANSACTION);
        return intent;
    }

    @Nullable
    protected ParsedDetails getParsedDetails(Uri contentSmsFormat, ContentResolver contentResolver, String originatingAddress,
                                             String dispOriginatingAddress,
                                             String message, long timestampMillis, Context context) {

        String[] projection = new String[]{
                Contract.SMSFormat.ID,
                Contract.SMSFormat.TYPE,
                Contract.SMSFormat.SENDER,
                Contract.SMSFormat.REGEX
        };
        //String selection = Contract.SMSFormat.SENDER + " In ( ?, ? )";
        String selection = "? like '%' ||" + Contract.SMSFormat.SENDER + "|| '%' OR ? like '%' ||" + Contract.SMSFormat.SENDER + "|| '%'";
        String[] selectionArgs = new String[]{originatingAddress, dispOriginatingAddress};
        Log.d("SMSHandler", "Searching DB");
        // Set<String> formats=new HashSet<>();
        try (Cursor cursor = contentResolver.query(contentSmsFormat, projection, selection, selectionArgs, null)) {
            if (cursor != null) {
                Log.d("SMSHandler", "Cursor not null");
                if (cursor.moveToFirst()) {
                    Log.d("SMSHandler", "Cursor First");
                    do {
                        String regex = cursor.getString(cursor.getColumnIndexOrThrow(Contract.SMSFormat.REGEX));
                        String type = cursor.getString(cursor.getColumnIndexOrThrow(Contract.SMSFormat.TYPE));
                        String sender = cursor.getString(cursor.getColumnIndexOrThrow(Contract.SMSFormat.SENDER));

                        regex = regex.replaceAll("\\[\\[account]]", "(?<account>(?:[a-z]|[0-9]|\\\\*)+)");
                        regex = regex.replaceAll("\\[\\[amount]]", "(?<amount>(?:[0-9]|,)*.?[0-9]{2})");
                        regex = regex.replaceAll("\\[\\[date]]", "(?<date>(?:[1-9]|[0][1-9]|[1-2][0-9]|3[0-1])[-|\\/](?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC|0[1-9]|1[0-2])[-|\\/](?:[0-9]+))");
                        regex = regex.replaceAll("\\[\\[time]]", "(?<time>(?:[0-1][0-9]|2[0-3]):(?:[0-5][0-9])(?::[0-5][0-9])?)");
                        regex = regex.replaceAll("\\[\\[to]]", "(?<to>(?:[a-z]|[0-9]|_|@|-| |\\\\*|\\\\.)+?)");
                        //   formats.add(regex);
                        Pattern pattern = Pattern.compile(regex);
                        Matcher matcher = pattern.matcher(message);
                        Log.d("SMSHandler", "Matcher Check");
                        if (matcher.find()) {
                            Log.d("SMSHandler", "Matcher Find");
                            String account;
                            try {
                                account = matcher.group("account");
                            } catch (IllegalArgumentException iae) {
                                account = sender;
                            }
                            String amount = matcher.group("amount");
                            String date;
                            try {
                                date = matcher.group("date");
                            } catch (IllegalArgumentException iae) {
                                date = null;
                            }
                            String time;
                            try {
                                time = matcher.group("time");
                            } catch (IllegalArgumentException iae) {
                                time = null;
                            }
                            String to;
                            try {
                                to = matcher.group("to");
                            } catch (IllegalArgumentException iae) {
                                to = null;
                            }
                            if (account != null && amount != null) {
                                Log.d("SMSHandler", "account: " + account);
                                Log.d("SMSHandler", "amount: " + amount);
                                Log.d("SMSHandler", "to: " + to);
                                Log.d("SMSHandler", "date: " + date);
                                Log.d("SMSHandler", "time: " + time);
                                try {
                                    ParsedDetails details = new ParsedDetails();
                                    details.setAccount(account);
                                    // TODO: Find a way to allow , if its decimal separator for locale
                                    //amount.replaceAll(",","");
                                    Number parsedAmount = NumberFormat.getInstance().parse(amount);
                                    details.setAmount(parsedAmount.doubleValue());
                                    details.setOtherParty(StringUtils.isEmpty(to) ? "Unknown" : to);
                                    if (date != null && time != null) {
                                        DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_TIME;
                                        LocalDate lDate = getLocalDate(date, timestampMillis);
                                        LocalTime lTime = LocalTime.parse(time, timeFormatter);
                                        details.setDateTime(LocalDateTime.of(lDate, lTime));
                                    } else if (date != null) {
                                        LocalDate lDate = getLocalDate(date, timestampMillis);
                                        details.setDateTime(LocalDateTime.of(lDate,
                                                Instant.ofEpochMilli(timestampMillis).atZone(ZoneId.systemDefault()).toLocalTime()));
                                    } else if (time != null) {
                                        DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_TIME;
                                        LocalTime lTime = LocalTime.parse(time, timeFormatter);
                                        details.setDateTime(LocalDateTime.of(Instant.ofEpochMilli(timestampMillis).atZone(ZoneId.systemDefault()).toLocalDate(), lTime));
                                    } else {
                                        // Both Date time null
                                        if (timestampMillis != 0) {
                                            details.setDateTime(Instant.ofEpochMilli(timestampMillis).atZone(ZoneId.systemDefault()).toLocalDateTime());
                                        } else {
                                            details.setDateTime(LocalDateTime.now());
                                        }
                                    }
                                    details.setType(type);
                                    return details;
                                } catch (ParseException | NullPointerException parseException) {
                                    Log.e("SMSHandler", "Parse exception", parseException);
                                    Log.e("SMSHandler", "Parse exception For SMS " + message);
                                    try (PrintWriter pw = new PrintWriter(new FileOutputStream(context.getExternalFilesDir("SMSHandler.log")))) {
                                        parseException.printStackTrace(pw);
                                    } catch (FileNotFoundException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    } while (cursor.moveToNext());
                }
            }
        } catch (Throwable t) {
            try {
                StringWriter sw = new StringWriter();
                t.printStackTrace(new PrintWriter(sw));
                Files.write(new File(context.getExternalFilesDir(null), "SMSHandler.log").toPath(),
                        ("\n" + sw).getBytes(),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("SMSHandler", "Failed to Log exception", e);
            }
            throw t;
        }
        Log.w("SMSHandler", "No Formatter Matched for Sender " + originatingAddress + " " + dispOriginatingAddress + " Message " + message);

        try {
            Files.write(new File(context.getExternalFilesDir(null), "no_format_matched.log").toPath(),
                    ("\nNo Formatter Matched for Sender " + originatingAddress + " " + dispOriginatingAddress + " Message " + message).getBytes(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("SMSHandler", "Failed to No format matched", e);
        }

        return null;
    }


    private LocalDate getLocalDate(String date, long timestampMillis) {
        List<String> formats = Stream.of("dd-MMM-yyyy", "dd-MMM-yy", "dd/MM/yy", "dd/MM/yyyy").collect(Collectors.toList());
        return parseWithFormat(date, formats, 0, timestampMillis);
    }


    private LocalDate parseWithFormat(String date, List<String> formats, int index, long timestampMillis) {
        if (index >= formats.size()) {
            Log.e("SMSParsing", "Handle Date format : <" + date + ">");
            return Instant.ofEpochMilli(timestampMillis).atZone(ZoneId.systemDefault()).toLocalDate();
        }
        try {
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(formats.get(index));
            return LocalDate.parse(date, dateFormatter);
        } catch (DateTimeParseException dtps) {
            return parseWithFormat(date, formats, index + 1, timestampMillis);
        }
    }


    public static class ParsedDetails {
        LocalDateTime dateTime;
        double amount;
        String account;
        String otherParty;
        String type;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public LocalDateTime getDateTime() {
            return dateTime;
        }

        public void setDateTime(LocalDateTime dateTime) {
            this.dateTime = dateTime;
        }

        public double getAmount() {
            return amount;
        }

        public void setAmount(double amount) {
            this.amount = amount;
        }

        public String getAccount() {
            return account;
        }

        public void setAccount(String account) {
            this.account = account;
        }

        public String getOtherParty() {
            return otherParty;
        }

        public void setOtherParty(String otherParty) {
            this.otherParty = otherParty;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ParsedDetails)) return false;
            ParsedDetails that = (ParsedDetails) o;
            return Double.compare(that.amount, amount) == 0 && Objects.equals(dateTime, that.dateTime) && Objects.equals(account, that.account) && Objects.equals(otherParty, that.otherParty);
        }

        @NonNull
        @Override
        public String toString() {
            return "ParsedDetails{" +
                    "dateTime=" + dateTime +
                    ", amount=" + amount +
                    ", account='" + account + '\'' +
                    ", otherParty='" + otherParty + '\'' +
                    '}';
        }

        @Override
        public int hashCode() {
            return Objects.hash(dateTime, amount, account, otherParty);
        }
    }


}
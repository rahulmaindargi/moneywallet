package com.oriondev.moneywallet.storage.database.data.sms;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.oriondev.moneywallet.model.CurrencyUnit;
import com.oriondev.moneywallet.storage.database.Contract;
import com.oriondev.moneywallet.storage.database.SyncContentProvider;
import com.oriondev.moneywallet.utils.CurrencyManager;

import java.math.BigDecimal;
import java.sql.Date;
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

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void handleSMS(Context context, String originatingAddress, String dispOriginatingAddress, String message, long timestampMillis) {
        Log.d("SMSHandler", "handleSMS");
        ContentResolver contentResolver = context.getContentResolver();
        ParsedDetails details = getParsedDetails(contentResolver, originatingAddress, dispOriginatingAddress, message, timestampMillis);
        if (details == null) return;
        System.out.println(details);
        String id =
                details.account + details.amount + details.otherParty + details.dateTime +
                        details.type + originatingAddress + dispOriginatingAddress;
        Log.d("SMSHandler", "SMSID:= " + id);
        SMSDataImporter dataImporter = new SMSDataImporter(context);
        if (dataImporter.insertSMS(id, message)) {
            Log.d("SMSHandler", "SMS Inserted ");
            CurrencyUnit currencyUnit = CurrencyManager.getCurrency("INR");
            BigDecimal decimalMultiply = new BigDecimal(Math.pow(10, currencyUnit.getDecimals()));
            BigDecimal moneyDecimal = decimalMultiply.multiply(new BigDecimal(details.amount));
            long money = moneyDecimal.longValue();
            dataImporter.insertTransaction(details.account, currencyUnit, "Misc",
                    Date.from(details.dateTime.atZone(ZoneId.systemDefault()).toInstant())
                    , money, "Expense".equalsIgnoreCase(details.type) ? 1 : 0, details.otherParty, null,
                    null, null, message);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Nullable
    protected ParsedDetails getParsedDetails(ContentResolver contentResolver, String originatingAddress, String dispOriginatingAddress,
                                             String message, long timestampMillis) {
        Uri contentSmsFormat = SyncContentProvider.CONTENT_SMS_FORMAT;
        String[] projection = new String[]{
                Contract.SMSFormat.ID,
                Contract.SMSFormat.TYPE,
                Contract.SMSFormat.SENDER,
                Contract.SMSFormat.REGEX
        };
        String selection = Contract.SMSFormat.SENDER + " In ( ?, ? )";
        String[] selectionArgs = new String[]{originatingAddress, dispOriginatingAddress};
        Log.d("SMSHandler", "Searching DB");
        try (Cursor cursor = contentResolver.query(contentSmsFormat, projection, selection, selectionArgs, null)) {
            if (cursor != null) {
                Log.d("SMSHandler", "Cursor not null");
                cursor.moveToFirst();
                if (!cursor.isAfterLast()) {
                    Log.d("SMSHandler", "Cursor after last");
                    do {
                        String regex = cursor.getString(cursor.getColumnIndexOrThrow(Contract.SMSFormat.REGEX));
                        String type = cursor.getString(cursor.getColumnIndexOrThrow(Contract.SMSFormat.TYPE));
                        Pattern pattern = Pattern.compile(regex);
                        Matcher matcher = pattern.matcher(message);
                        Log.d("SMSHandler", "Matcher Check");
                        if (matcher.find()) {
                            Log.d("SMSHandler", "Matcher Find");
                            String account = matcher.group("account");
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
                            String to = matcher.group("to");

                            if (account != null && amount != null && to != null) {
                                Log.d("SMSHandler", "account: " + account);
                                Log.d("SMSHandler", "amount: " + amount);
                                Log.d("SMSHandler", "to: " + to);
                                Log.d("SMSHandler", "date: " + date);
                                Log.d("SMSHandler", "time: " + time);
                                ParsedDetails details = new ParsedDetails();
                                details.setAccount(account);
                                details.setAmount(Double.parseDouble(amount));
                                details.setOtherParty(to);
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
                            }
                        }
                    } while (cursor.moveToNext());
                }
            }
        }
        Log.d("SMS Handler", "No Formatter Matched for Sender " + originatingAddress + " " + dispOriginatingAddress + " Message " + message);
//        if (checkSenderIsValid(originatingAddress) || checkSenderIsValid(dispOriginatingAddress)) {
//            ParsedDetails details = new ParsedDetails();
//
//            Pattern amountP = Pattern.compile("(?i)(?:(?:RS|INR|MRP)\\.?\\s?)(\\d+(:?\\,\\d+)?(\\,\\d+)?(\\.\\d{1,2})?)");
//            Matcher amountM = amountP.matcher(message);
//            if (amountM.find()) {
//                String amount = amountM.group(1);
//                Log.d("SMSHandler Amount", amount);
//                details.setAmount(Double.parseDouble(amount));
//            } else {
//                return null;
//            }
//            Pattern accP = Pattern.compile("(?i)(?:\\smade on|ur|made a\\s|in\\*)([A-Za-z]*\\s?-?\\s[A-Za-z]*\\s?-?\\s[A-Za-z]*\\s?-?)");
//            Matcher accM = accP.matcher(message);
//            if (accM.find()) {
//                String acc = accM.group(1);
//                Log.d("SMSHandler acc", acc);
//                details.setAccount(acc);
//            } else {
//                return null;
//            }
//
//            Pattern merchantP = Pattern.compile("(?i)(?:\\sat\\s|in\\*)([A-Za-z0-9]*\\s?-?\\s?[A-Za-z0-9]*\\s?-?\\.?)");
//            Matcher merchantM = merchantP.matcher(message);
//            if (merchantM.find()) {
//                String merchant = merchantM.group(1);
//                Log.d("SMSHandler merchant", merchant);
//                details.setOtherParty(merchant);
//            } else {
//                return null;
//            }
//            return details;
//        }
        return null;
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private LocalDate getLocalDate(String date, long timestampMillis) {
        List<String> formats = Stream.of("dd-MMM-yyyy", "dd-MMM-yy").collect(Collectors.toList());
        return parseWithFormat(date, formats, 0, timestampMillis);
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private LocalDate parseWithFormat(String date, List<String> formats, int index, long timestampMillis) {
        if (index >= formats.size()) {
            Log.e("SMSParsing", "Handle Date format : " + date);
            return Instant.ofEpochMilli(timestampMillis).atZone(ZoneId.systemDefault()).toLocalDate();
        }
        try {
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(formats.get(index));
            return LocalDate.parse(date, dateFormatter);
        } catch (DateTimeParseException dtps) {
            return parseWithFormat(date, formats, index + 1, timestampMillis);
        }
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
                || sender.contains("TM-CITIBA"));
    }

    public class ParsedDetails {
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
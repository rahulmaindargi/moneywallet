package com.rahul.moneywallet.storage.database.data.sms;

import static org.mockito.ArgumentMatchers.any;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.rahul.moneywallet.storage.database.Contract;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ExtendWith(MockitoExtension.class)
class SMSHandlerTest {
    ContentResolver contentResolver;
    Cursor cursor;
    Uri uri;

    @BeforeEach
    void beforeEach() {
        contentResolver = Mockito.mock(ContentResolver.class);
        cursor = Mockito.mock(Cursor.class);
        uri = Mockito.mock(Uri.class);
    }


    @Test
    void testFormat() {
        String preRegex = "(?i)Payment of Rs.[[amount]] received for card [[account]] on [[date]]..*";
        String regex = getRegex(preRegex);
        System.out.println(regex);
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher("Payment of Rs.18719.40 received for card ************2147 on 05/08/22. Limit available=Rs.1257010.00. Download CitiMobile app to track spends.");

        Assertions.assertTrue(matcher.find());

        preRegex = "(?i)Your [[account]] A\\/c has been debited with INR [[amount]] on [[date]] at [[time]] and account [[to]] has been credited..*";
        regex = getRegex(preRegex);
        System.out.println(regex);
        pattern = Pattern.compile(regex);
        matcher = pattern.matcher("Your Citibank A/c has been debited with INR 100.25 on 03-06-1988 at 07:00 and account Testing.upi@to has been credited.");
        Assertions.assertTrue(matcher.find());
        Assertions.assertEquals("Testing.upi@to", matcher.group("to"));

        preRegex = "(?i)Dear Customer, ICICI Bank Account [[account]] is debited with INR [[amount]] on [[date]]. Info: [[to]]. The Available.*";
        regex = getRegex(preRegex);
        System.out.println(regex);
        pattern = Pattern.compile(regex);
        matcher = pattern.matcher("Dear Customer, ICICI Bank Account XX097 is debited with INR 477.00 on 04-Aug-22. Info: INF*IWISH Con. The Available account");
        Assertions.assertTrue(matcher.find());
        Assertions.assertEquals("INF*IWISH Con", matcher.group("to"));

        preRegex = "(?i)Your Citi A\\/c [[account]] is debited for Rs.[[amount]] on [[date]] [[time]] for fund transfer request to [[to]] \\(Ref. No.*";

        regex = getRegex(preRegex);
        System.out.println(regex);
        pattern = Pattern.compile(regex);
        matcher = pattern.matcher("Your Citi A/c 0703 is debited for Rs.136,000.00 on 01-AUG-2022 16:36:18 for fund transfer request to RAHUL VIVEK MAINDARGI (Ref. No.070070253855764). A/c balance=Rs.69,754.32");
        Assertions.assertTrue(matcher.find());
        // Assertions.assertEquals("INF*IWISH Con", matcher.group("to"));

        preRegex = "(?i)ICICI Bank Acct [[account]] debited (?:for|with) (?:Rs|INR) [[amount]] on [[date]](;|\\.| &) [[to]] credited..*";
        regex = getRegex(preRegex);
        System.out.println(regex);
        pattern = Pattern.compile(regex);
        matcher = pattern.matcher("ICICI Bank Acct XX591 debited with Rs 49,000.00 on 25-Aug-22 & Acct XX704 credited.IMPS:");
        Assertions.assertTrue(matcher.find());

    }

    @NonNull
    private String getRegex(String regex) {
        regex = regex.replaceAll("\\[\\[account]]", "(?<account>(?:[a-z]|[0-9]|\\\\*)+)");
        regex = regex.replaceAll("\\[\\[amount]]", "(?<amount>(?:[0-9]|,)*.?[0-9]{2})");
        regex = regex.replaceAll("\\[\\[date]]", "(?<date>(?:[1-9]|[0][1-9]|[1-2][0-9]|3[0-1])[-|\\/](?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC|0[1-9]|1[0-2])[-|\\/](?:[0-9]+))");
        regex = regex.replaceAll("\\[\\[time]]", "(?<time>(?:[0-1][0-9]|2[0-3]):(?:[0-5][0-9])(?::[0-5][0-9])?)");
        regex = regex.replaceAll("\\[\\[to]]", "(?<to>(?:[a-z]|[0-9]|_|@|-| |\\\\*|\\\\.)+?)");
        return regex;
    }

    @Test
    void getParsedDetails() {
        SMSHandler smsHandler = new SMSHandler();

        Mockito.lenient().when(contentResolver.query(any(), any(), any(), any(), any())).thenReturn(cursor);
        Mockito.lenient().when(cursor.moveToFirst()).thenReturn(true);
        Mockito.lenient().when(cursor.moveToNext()).thenReturn(false);
        Mockito.lenient().when(cursor.isAfterLast()).thenReturn(false);
        Mockito.lenient().when(cursor.getString(3)).thenReturn("(?i)INR [[amount]] spent on ICICI Bank Card [[account]] on [[date]] " +
                "at [[to]]..*");
        Mockito.lenient().when(cursor.getString(1)).thenReturn("debit");
        Mockito.lenient().when(cursor.getColumnIndexOrThrow(Contract.SMSFormat.REGEX)).thenReturn(3);
        Mockito.lenient().when(cursor.getColumnIndexOrThrow(Contract.SMSFormat.TYPE)).thenReturn(1);
        String message = "INR 649.00 spent on ICICI Bank Card XX0006 on 31-Jul-22 at Netflix Test. Avl lmt: INR 8,96,534.47. To dispute,call " +
                "18002662/SMS Block 0006 to 9215676766";
        try (MockedStatic<Uri> mockedUriStatic = Mockito.mockStatic(Uri.class);
             MockedStatic<Log> mockedLogStatic = Mockito.mockStatic(Log.class)) {
            mockedUriStatic.when(() -> Uri.parse(any())).thenReturn(uri);
            //Mockito.lenient().when(Uri.parse(any())).thenReturn(null);
            mockedLogStatic.when(() -> Log.d(any(), any())).thenReturn(1);
            SMSHandler.ParsedDetails parsedDetails = smsHandler.getParsedDetails(uri, contentResolver,
                    "address", "address", message,
                    System.currentTimeMillis(), null);

            Assertions.assertNotNull(parsedDetails);
            Assertions.assertNotNull(parsedDetails.getAccount());
            Assertions.assertNotEquals(0, parsedDetails.getAmount());
            Assertions.assertNotNull(parsedDetails.getOtherParty());

            Mockito.lenient().when(cursor.getString(3)).thenReturn("(?i)Your [[account]] A\\/c has been debited with INR [[amount]] on [[date]] at [[time]] and account [[to]](?<= has) been credited..*");

            message = "Your Citibank A/c has been debited with INR 18719.40 on 04-AUG-2022 at 21:40 and account billdesk@hdfcbank has been credited" +
                    ". UPI Ref no. 221632857699";
            parsedDetails = smsHandler.getParsedDetails(uri, contentResolver,
                    "address", "address", message,
                    System.currentTimeMillis(), null);

            Assertions.assertNotNull(parsedDetails);
            Assertions.assertNotNull(parsedDetails.getAccount());
            Assertions.assertNotEquals(0, parsedDetails.getAmount());
            Assertions.assertNotNull(parsedDetails.getOtherParty());
        }

    }
}
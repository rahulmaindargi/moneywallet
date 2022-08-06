package com.rahul.moneywallet.storage.database.data.sms;

import static org.mockito.ArgumentMatchers.any;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.rahul.moneywallet.storage.database.Contract;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

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
    void getParsedDetails() {
        SMSHandler smsHandler = new SMSHandler();

        Mockito.lenient().when(contentResolver.query(any(), any(), any(), any(), any())).thenReturn(cursor);
        Mockito.lenient().when(cursor.moveToFirst()).thenReturn(true);
        Mockito.lenient().when(cursor.moveToNext()).thenReturn(false);
        Mockito.lenient().when(cursor.isAfterLast()).thenReturn(false);
        Mockito.lenient().when(cursor.getString(3)).thenReturn("(?i)INR (?<amount>(?:[0-9]|,)*.?[0-9]{2}) spent on ICICI Bank Card (?<account>" +
                "(?:[a-z]|[A-Z]|[0-9])+) on (?<date>(?:[0][1-9]|[1-2][0-9]|3[0-1])-(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)-" +
                "(?:[0-9]+)) " +
                "at (?<to>(?:[a-z]|[0-9]|_|@|-| )+). Avl Lmt: INR (?:(?:[0-9]|,)*.?[0-9]{2})..*");
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
                    System.currentTimeMillis());

            Assertions.assertNotNull(parsedDetails);

            Mockito.lenient().when(cursor.getString(3)).thenReturn("(?i)Your (?<account>(?:[a-z]|[A-Z]|[0-9])+) A/c has been debited with INR " +
                    "(?<amount>(?:[0-9]|,)*.?[0-9]{2}) on (?<date>(?:[0][1-9]|[1-2][0-9]|3[0-1])-" +
                    "(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)-(?:[0-9]+)) at (?<time>(?:[0-1][0-9]|2[0-3]):(?:[0-5][0-9])) and account " +
                    "(?<to>(?:[a-z]|[0-9]|_|@|-| )+) has been credited. UPI Ref no. (?:[0-9]+)");

            message = "Your Citibank A/c has been debited with INR 18719.40 on 04-AUG-2022 at 21:40 and account billdesk@hdfcbank has been credited" +
                    ". UPI Ref no. 221632857699";
            parsedDetails = smsHandler.getParsedDetails(uri, contentResolver,
                    "address", "address", message,
                    System.currentTimeMillis());
            Assertions.assertNotNull(parsedDetails);
        }

    }
}
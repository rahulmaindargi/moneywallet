package com.rahul.moneywallet.storage.database.data.csv;

import android.content.Context;
import android.text.TextUtils;

import com.opencsv.CSVReaderHeaderAware;
import com.rahul.moneywallet.model.CurrencyUnit;
import com.rahul.moneywallet.storage.database.Contract;
import com.rahul.moneywallet.storage.database.data.AbstractDataImporter;
import com.rahul.moneywallet.utils.CurrencyManager;
import com.rahul.moneywallet.utils.DateUtils;

import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

/**
 * Created by andrea on 23/12/18.
 */
public class CSVDataImporter extends AbstractDataImporter implements Closeable {

    private final CSVReaderHeaderAware mReader;

    public CSVDataImporter(Context context, File file) throws IOException {
        super(context);
        mReader = new CSVReaderHeaderAware(new FileReader(file));
    }

    public CSVDataImporter(Context context, Reader file) throws IOException {
        super(context);
        mReader = new CSVReaderHeaderAware(file);
    }

    @Override
    public void importData() throws IOException {
        Map<String, String> lineMap = mReader.readMap();
        while (lineMap != null) {
            System.out.println("Importing line");
            // extract required information from the csv file
            String wallet = getTrimmedString(lineMap.get(Constants.COLUMN_WALLET));
            String currency = getTrimmedString(lineMap.get(Constants.COLUMN_CURRENCY));
            String category = getTrimmedString(lineMap.get(Constants.COLUMN_CATEGORY));
            String datetimeString = getTrimmedString(lineMap.get(Constants.COLUMN_DATETIME));
            String moneyString = getTrimmedString(lineMap.get(Constants.COLUMN_MONEY));
            // if one of this information is missing, we should stop the import
            // process because the file is not valid
            if (TextUtils.isEmpty(wallet) || TextUtils.isEmpty(currency) || TextUtils.isEmpty(category) || TextUtils.isEmpty(datetimeString) || TextUtils.isEmpty(moneyString)) {
                throw new RuntimeException("Invalid csv file: one or more required columns are missing");
            }

            String deviceSourceID = getTrimmedString(lineMap.get(Constants.COLUMN_DEVICE_SOURCE_ID));
            String syncSideID = getTrimmedString(lineMap.get(Constants.COLUMN_SYNCED_SIDE_ID));
            String syncedWithList = getTrimmedString(lineMap.get(Constants.COLUMN_SYNCED_WITH_LIST));
            // extract the optional information from the csv file
            String description = getTrimmedString(lineMap.get(Constants.COLUMN_DESCRIPTION));
            String event = getTrimmedString(lineMap.get(Constants.COLUMN_EVENT));
            String people = getTrimmedString(lineMap.get(Constants.COLUMN_PEOPLE));
            String place = getTrimmedString(lineMap.get(Constants.COLUMN_PLACE));
            String note = getTrimmedString(lineMap.get(Constants.COLUMN_NOTE));
            // try to build the internal transaction state starting from strings
            CurrencyUnit currencyUnit = CurrencyManager.getCurrency(currency);
            if (currencyUnit == null) {
                throw new RuntimeException("Unknown currency unit (" + currency + ")");
            }
            BigDecimal moneyDecimal;
            try {
                moneyDecimal = new BigDecimal(moneyString.replaceAll(",", "."));
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid money amount (" + e.getMessage() + ")");
            }
            BigDecimal decimalMultiply = new BigDecimal(Math.pow(10, currencyUnit.getDecimals()));
            moneyDecimal = moneyDecimal.multiply(decimalMultiply);
            long money = moneyDecimal.longValue();
            int direction = money < 0 ? Contract.Direction.EXPENSE : Contract.Direction.INCOME;
            Date datetime = DateUtils.getDateFromSQLDateTimeString(datetimeString);
            insertTransaction(wallet, currencyUnit, category, datetime, Math.abs(money), direction, description, event, place, people, note, deviceSourceID, syncSideID, syncedWithList);
            lineMap = mReader.readMap();
        }
    }

    private String getTrimmedString(String source) {
        if (source != null) {
            return source.trim();
        }
        return null;
    }

    @Override
    public void close() throws IOException {
        mReader.close();
    }
}
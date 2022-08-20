package com.rahul.moneywallet.ui.activity;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.rahul.moneywallet.R;
import com.rahul.moneywallet.broadcast.LocalAction;
import com.rahul.moneywallet.model.DataFormat;
import com.rahul.moneywallet.model.Wallet;
import com.rahul.moneywallet.picker.DateTimePicker;
import com.rahul.moneywallet.picker.ExportColumnsPicker;
import com.rahul.moneywallet.picker.ExportFormatPicker;
import com.rahul.moneywallet.picker.WalletPicker;
import com.rahul.moneywallet.service.ExportRunner;
import com.rahul.moneywallet.ui.activity.base.SinglePanelActivity;
import com.rahul.moneywallet.ui.fragment.dialog.GenericProgressDialog;
import com.rahul.moneywallet.ui.view.text.MaterialEditText;
import com.rahul.moneywallet.ui.view.text.Validator;
import com.rahul.moneywallet.ui.view.theme.ThemedDialog;
import com.rahul.moneywallet.utils.DateFormatter;

import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by andrea on 19/12/18.
 */
public class ExportActivity extends SinglePanelActivity implements ExportFormatPicker.Controller, DateTimePicker.Controller, WalletPicker.MultiWalletController, ExportColumnsPicker.Controller {


    private static final String TAG_DATA_FORMAT_PICKER = "ImportExportActivity::Tag::DataFormatPicker";
    private static final String TAG_START_DATE_TIME_PICKER = "ImportExportActivity::Tag::StartDateTimePicker";
    private static final String TAG_END_DATE_TIME_PICKER = "ImportExportActivity::Tag::EndDateTimePicker";
    private static final String TAG_WALLET_PICKER = "ImportExportActivity::Tag::WalletPicker";
    private static final String TAG_COLUMNS_PICKER = "ImportExportActivity::Tag::ColumnsPicker";
    private static final String TAG_PROGRESS_DIALOG = "ImportExportActivity::tag::GenericProgressDialog";


    private MaterialEditText mExportFormatEditText;
    private MaterialEditText mStartDateEditText;
    private MaterialEditText mEndDateEditText;
    private MaterialEditText mWalletsEditText;
    private MaterialEditText mExportColumnsEditText;
    private CheckBox mUniqueWalletCheckbox;

    private final BroadcastReceiver mLocalBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, final Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {

                    case LocalAction.ACTION_EXPORT_SERVICE_STARTED:
                        if (mProgressDialog == null) {
                            mProgressDialog = GenericProgressDialog.newInstance(R.string.title_data_exporting, R.string.message_data_export_running, true);
                        }
                        mProgressDialog.show(getSupportFragmentManager(), TAG_PROGRESS_DIALOG);
                        break;
                    case LocalAction.ACTION_EXPORT_SERVICE_FINISHED:
                        if (mProgressDialog != null) {
                            mProgressDialog.dismissAllowingStateLoss();
                            mProgressDialog = null;
                        }
                        ThemedDialog.buildMaterialDialog(ExportActivity.this)
                                .title(R.string.title_success)
                                .content(R.string.message_data_export_success)
                                .positiveText(android.R.string.ok)
                                .negativeText(android.R.string.cancel)
                                .onPositive((dialog, which) -> {
                                    Uri uri = intent.getParcelableExtra(ExportRunner.RESULT_FILE_URI);
                                    String type = intent.getStringExtra(ExportRunner.RESULT_FILE_TYPE);
                                    if (uri != null) {
                                        Intent target = new Intent(Intent.ACTION_VIEW);
                                        target.setDataAndType(uri, type);
                                        target.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                        target.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                        Intent intent1 = Intent.createChooser(target, getString(R.string.action_open));
                                        try {
                                            startActivity(intent1);
                                        } catch (ActivityNotFoundException ignore) {
                                            // no activity to handle this type of file
                                        }
                                    }
                                })
                                .show();
                        break;
                    case LocalAction.ACTION_EXPORT_SERVICE_FAILED:
                        if (mProgressDialog != null) {
                            mProgressDialog.dismissAllowingStateLoss();
                            mProgressDialog = null;
                        }
                        Exception exception = (Exception) intent.getSerializableExtra(ExportRunner.EXCEPTION);
                        ThemedDialog.buildMaterialDialog(ExportActivity.this)
                                .title(R.string.title_failed)
                                .content(R.string.message_data_export_failed, exception.getMessage())
                                .positiveText(android.R.string.ok)
                                .show();
                        break;
                }
            }
        }

    };
    private DateTimePicker mStartDateTimePicker;
    private DateTimePicker mEndDateTimePicker;
    private WalletPicker mWalletPicker;

    private ExportColumnsPicker mExportColumnsPicker;

    private GenericProgressDialog mProgressDialog;
    private LocalBroadcastManager mLocalBroadcastManager;
    Executor executor;
    private ExportFormatPicker mDataFormatPicker;
    private ActivityResultLauncher<Uri> exportLocationSelector;

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mLocalBroadcastManager != null) {
            mLocalBroadcastManager.unregisterReceiver(mLocalBroadcastReceiver);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LocalAction.ACTION_EXPORT_SERVICE_STARTED);
        intentFilter.addAction(LocalAction.ACTION_EXPORT_SERVICE_FINISHED);
        intentFilter.addAction(LocalAction.ACTION_EXPORT_SERVICE_FAILED);
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mLocalBroadcastManager.registerReceiver(mLocalBroadcastReceiver, intentFilter);
        executor = Executors.newSingleThreadExecutor();
    }

    @Override
    protected void onCreatePanelView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_panel_import_export, parent, true);

        mExportFormatEditText = view.findViewById(R.id.export_format_edit_text);
        mStartDateEditText = view.findViewById(R.id.start_date_edit_text);
        mEndDateEditText = view.findViewById(R.id.end_date_edit_text);
        mWalletsEditText = view.findViewById(R.id.wallets_edit_text);

        mExportColumnsEditText = view.findViewById(R.id.export_optional_columns_edit_text);
        mUniqueWalletCheckbox = view.findViewById(R.id.export_unique_wallet_checkbox);
        // check activity mode and update ui

        mExportFormatEditText.setVisibility(View.VISIBLE);
        mStartDateEditText.setVisibility(View.VISIBLE);
        mEndDateEditText.setVisibility(View.VISIBLE);
        mWalletsEditText.setVisibility(View.VISIBLE);

        mExportColumnsEditText.setVisibility(View.VISIBLE);
        mUniqueWalletCheckbox.setVisibility(View.VISIBLE);
        // attach listeners to views
        mExportFormatEditText.setOnClickListener(v -> {
            DataFormat[] dataFormats = new DataFormat[]{
                    DataFormat.CSV,
                    DataFormat.XLS,
                    DataFormat.PDF
            };
            mDataFormatPicker.showPicker(dataFormats);
        });
        mStartDateEditText.setOnClickListener(v -> mStartDateTimePicker.showDatePicker());
        mStartDateEditText.setOnCancelButtonClickListener(materialEditText -> {
            mStartDateTimePicker.setCurrentDateTime(null);
            return false;
        });
        mEndDateEditText.setOnClickListener(v -> mEndDateTimePicker.showDatePicker());
        mEndDateEditText.setOnCancelButtonClickListener(materialEditText -> {
            mEndDateTimePicker.setCurrentDateTime(null);
            return false;
        });
        mWalletsEditText.setOnClickListener(v -> mWalletPicker.showMultiWalletPicker());

        // disable edit texts
        mExportFormatEditText.setTextViewMode(true);
        mStartDateEditText.setTextViewMode(true);
        mEndDateEditText.setTextViewMode(true);
        mWalletsEditText.setTextViewMode(true);

        mExportColumnsEditText.setTextViewMode(true);
        // attach validators

        mExportFormatEditText.addValidator(new Validator() {

            @NonNull
            @Override
            public String getErrorMessage() {
                return getString(R.string.error_input_missing_format);
            }

            @Override
            public boolean isValid(@NonNull CharSequence charSequence) {
                return mDataFormatPicker.isSelected();
            }

            @Override
            public boolean autoValidate() {
                return false;
            }

        });
        mWalletsEditText.addValidator(new Validator() {

            @NonNull
            @Override
            public String getErrorMessage() {
                return getString(R.string.error_input_missing_multiple_wallets);
            }

            @Override
            public boolean isValid(@NonNull CharSequence charSequence) {
                return mWalletPicker.isSelected();
            }

            @Override
            public boolean autoValidate() {
                return false;
            }

        });

        mExportColumnsEditText.setOnClickListener(v -> mExportColumnsPicker.showPicker());
        // initialize pickers
        FragmentManager fragmentManager = getSupportFragmentManager();
        mDataFormatPicker = ExportFormatPicker.createPicker(fragmentManager, TAG_DATA_FORMAT_PICKER);
        mStartDateTimePicker = DateTimePicker.createPicker(fragmentManager, TAG_START_DATE_TIME_PICKER, null);
        mEndDateTimePicker = DateTimePicker.createPicker(fragmentManager, TAG_END_DATE_TIME_PICKER, null);
        mWalletPicker = WalletPicker.createPicker(fragmentManager, TAG_WALLET_PICKER, (Wallet[]) null);
        mExportColumnsPicker = ExportColumnsPicker.createPicker(fragmentManager, TAG_COLUMNS_PICKER);
        mProgressDialog = (GenericProgressDialog) fragmentManager.findFragmentByTag(TAG_PROGRESS_DIALOG);
        exportLocationSelector = registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), result -> {
            if (result == null) {
                return;
            }
            exportData(result);
        });
    }

    @Override
    @MenuRes
    protected int onInflateMenu() {
        return R.menu.menu_import_export;
    }

    @Override
    protected int getActivityTitleRes() {
        return R.string.title_activity_export_data;
    }

    @Override
    protected void onMenuCreated(Menu menu) {
        menu.findItem(R.id.action_export_data).setVisible(true);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.action_export_data) {
            if (mExportFormatEditText.validate() && mWalletsEditText.validate()) {
                exportLocationSelector.launch(null);
            }
        }
        return false;
    }

    @Override
    protected boolean isFloatingActionButtonEnabled() {
        // the floating action button is not required here
        return false;
    }

    private void exportData(Uri folder) {
        ExportRunner exportRunner = new ExportRunner(this, mDataFormatPicker.getCurrentFormat(),
                mStartDateTimePicker.getCurrentDateTime(),
                mEndDateTimePicker.getCurrentDateTime(),
                mWalletPicker.getCurrentWallets(),
                folder,
                mUniqueWalletCheckbox.isChecked(),
                mExportColumnsPicker.getCurrentServiceColumns());
        executor.execute(exportRunner);
    }

    @Override
    public void onDateTimeChanged(String tag, Date date) {
        switch (tag) {
            case TAG_START_DATE_TIME_PICKER:
                if (date != null) {
                    DateFormatter.applyDate(mStartDateEditText, date);
                } else {
                    mStartDateEditText.setText(null);
                }
                break;
            case TAG_END_DATE_TIME_PICKER:
                if (date != null) {
                    DateFormatter.applyDate(mEndDateEditText, date);
                } else {
                    mEndDateEditText.setText(null);
                }
                break;
        }
    }

    @Override
    public void onWalletListChanged(String tag, Wallet[] wallets) {
        if (wallets != null && wallets.length > 0) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < wallets.length; i++) {
                if (i != 0) {
                    builder.append(", ");
                }
                builder.append(wallets[i].getName());
            }
            mWalletsEditText.setText(builder);
        } else {
            mWalletsEditText.setText(null);
        }
        onFormatOrWalletChanged();
    }

    @Override
    public void onFormatChanged(String tag, DataFormat format) {
        if (format != null) {
            switch (format) {
                case CSV:
                    mExportFormatEditText.setText(R.string.hint_data_format_csv);
                    mExportColumnsEditText.setVisibility(View.VISIBLE);
                    break;
                case XLS:
                    mExportFormatEditText.setText(R.string.hint_data_format_xls);

                    mExportColumnsEditText.setVisibility(View.VISIBLE);

                    break;
                case PDF:
                    mExportFormatEditText.setText(R.string.hint_data_format_pdf);
                    mExportColumnsEditText.setVisibility(View.VISIBLE);
                    break;
            }
        } else {
            mExportFormatEditText.setText(null);
            mExportColumnsEditText.setVisibility(View.GONE);
        }
        onFormatOrWalletChanged();
    }

    @Override
    public void onExportColumnsChanged(String tag, String[] columns) {
        if (columns != null && columns.length > 0) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < columns.length; i++) {
                if (i != 0) {
                    builder.append(", ");
                }
                builder.append(columns[i]);
            }
            mExportColumnsEditText.setText(builder);
        } else {
            mExportColumnsEditText.setText(null);
        }
    }

    private void onFormatOrWalletChanged() {

        if (mWalletPicker.isSelected()) {
            Wallet[] wallets = mWalletPicker.getCurrentWallets();
            if (wallets != null && wallets.length > 1) {
                DataFormat dataFormat = mDataFormatPicker.getCurrentFormat();
                if (dataFormat != null) {
                    switch (dataFormat) {
                        case CSV:
                            mUniqueWalletCheckbox.setVisibility(View.GONE);
                            break;
                        case XLS:
                        case PDF:
                            mUniqueWalletCheckbox.setVisibility(View.VISIBLE);
                            break;
                    }
                    return;
                }
            }
        }

        mUniqueWalletCheckbox.setVisibility(View.GONE);
    }
}
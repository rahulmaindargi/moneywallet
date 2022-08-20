/*
 * Copyright (c) 2018.
 *
 * This file is part of MoneyWallet.
 *
 * MoneyWallet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MoneyWallet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MoneyWallet.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.rahul.moneywallet.ui.fragment.secondary;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.rahul.moneywallet.R;
import com.rahul.moneywallet.broadcast.LocalAction;
import com.rahul.moneywallet.storage.database.data.csv.CSVDataImporter;
import com.rahul.moneywallet.ui.activity.BackupListActivity;
import com.rahul.moneywallet.ui.activity.ExportActivity;
import com.rahul.moneywallet.ui.fragment.dialog.GenericProgressDialog;
import com.rahul.moneywallet.ui.view.theme.ThemedDialog;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by andre on 21/03/2018.
 */
public class DatabaseSettingFragment extends PreferenceFragmentCompat {
    public static final String EXCEPTION = "DatabaseSettingFragment::Results::Exception";
    private static final String TAG_PROGRESS_DIALOG = "DatabaseSettingFragment::tag::GenericProgressDialog";
    private Preference mBackupServicesPreference;
    private Preference mImportDataPreference;
    private Preference mExportDataPreference;
    private LocalBroadcastManager mLocalBroadcastManager;
    private GenericProgressDialog mProgressDialog;
    Executor executor;
    private final BroadcastReceiver mLocalBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case LocalAction.ACTION_IMPORT_SERVICE_STARTED:
                        if (mProgressDialog == null) {
                            mProgressDialog = GenericProgressDialog.newInstance(R.string.title_data_importing, R.string.message_data_import_running, true);
                        }
                        mProgressDialog.show(getParentFragmentManager(), TAG_PROGRESS_DIALOG);
                        break;
                    case LocalAction.ACTION_IMPORT_SERVICE_FINISHED:
                        if (mProgressDialog != null) {
                            mProgressDialog.dismissAllowingStateLoss();
                            mProgressDialog = null;
                        }
                        ThemedDialog.buildMaterialDialog(getContext())
                                .title(R.string.title_success)
                                .content(R.string.message_data_import_success)
                                .positiveText(android.R.string.ok)
                                .show();
                        break;
                    case LocalAction.ACTION_IMPORT_SERVICE_FAILED:
                        if (mProgressDialog != null) {
                            mProgressDialog.dismissAllowingStateLoss();
                            mProgressDialog = null;
                        }
                        Exception exception = (Exception) intent.getSerializableExtra(EXCEPTION);
                        ThemedDialog.buildMaterialDialog(getContext())
                                .title(R.string.title_failed)
                                .content(R.string.message_data_import_failed, exception.getMessage())
                                .positiveText(android.R.string.ok)
                                .show();
                        break;

                }
            }
        }
    };

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.settings_database);
        mBackupServicesPreference = findPreference("backup_services");
        mImportDataPreference = findPreference("import_data");
        mExportDataPreference = findPreference("export_data");

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LocalAction.ACTION_IMPORT_SERVICE_STARTED);
        intentFilter.addAction(LocalAction.ACTION_IMPORT_SERVICE_FINISHED);
        intentFilter.addAction(LocalAction.ACTION_IMPORT_SERVICE_FAILED);

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(requireContext());
        mLocalBroadcastManager.registerReceiver(mLocalBroadcastReceiver, intentFilter);
        executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mBackupServicesPreference.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), BackupListActivity.class);
            intent.putExtra(BackupListActivity.BACKUP_MODE, BackupListActivity.FULL);
            startActivity(intent);
            return false;
        });

        ActivityResultLauncher<String> importSelector = registerForActivityResult(new ActivityResultContracts.GetContent(), result -> {
            if (result == null)
                return;
            String path = result.getPath();
            Log.d("File selector", path);
            Runnable importer = () -> {
                Intent intent = new Intent(LocalAction.ACTION_IMPORT_SERVICE_STARTED);
                mLocalBroadcastManager.sendBroadcast(intent);
                try (InputStream inputStream = requireContext().getContentResolver().openInputStream(result);
                     BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(inputStream)));
                     CSVDataImporter csvDataImporter = new CSVDataImporter(getContext(), reader)) {

                    csvDataImporter.importData();
                    intent = new Intent(LocalAction.ACTION_IMPORT_SERVICE_FINISHED);
                    mLocalBroadcastManager.sendBroadcast(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    intent = new Intent(LocalAction.ACTION_IMPORT_SERVICE_FAILED);
                    intent.putExtra(EXCEPTION, e);
                    mLocalBroadcastManager.sendBroadcast(intent);
                }
            };
            executor.execute(importer);
        });
        mImportDataPreference.setOnPreferenceClickListener(preference -> {
            // getContext().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            importSelector.launch("*/*");
            return false;
        });
        mExportDataPreference.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), ExportActivity.class);
            startActivity(intent);
            return false;
        });
        FragmentManager fragmentManager = getParentFragmentManager();
        mProgressDialog = (GenericProgressDialog) fragmentManager.findFragmentByTag(TAG_PROGRESS_DIALOG);
    }

    @NonNull
    @Override
    public RecyclerView onCreateRecyclerView(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, Bundle savedInstanceState) {
        RecyclerView recyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState);
        recyclerView.setPadding(0, 0, 0, 0);
        return recyclerView;
    }

    @Override
    public void onDestroy() {
        if (mLocalBroadcastManager != null) {
            mLocalBroadcastManager.unregisterReceiver(mLocalBroadcastReceiver);
        }
        super.onDestroy();

    }
}
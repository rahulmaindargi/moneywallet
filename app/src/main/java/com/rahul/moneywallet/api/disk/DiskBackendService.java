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

package com.rahul.moneywallet.api.disk;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.rahul.moneywallet.R;
import com.rahul.moneywallet.api.AbstractBackendServiceDelegate;
import com.rahul.moneywallet.api.BackendException;
import com.rahul.moneywallet.api.BackendServiceFactory;
import com.rahul.moneywallet.ui.view.theme.ThemedDialog;

/**
 * Created by andrea on 21/11/18.
 */
public class DiskBackendService extends AbstractBackendServiceDelegate {

    private ActivityResultLauncher<String> launcher;

    public DiskBackendService(BackendServiceStatusListener listener) {
        super(listener);
    }

    @Override
    public String getId() {
        return BackendServiceFactory.SERVICE_ID_EXTERNAL_MEMORY;
    }

    @Override
    public int getName() {
        return R.string.service_backup_external_memory;
    }

    @Override
    public int getBackupCoverMessage() {
        return R.string.cover_message_backup_external_memory_title;
    }

    @Override
    public int getBackupCoverAction() {
        return R.string.cover_message_backup_external_memory_button;
    }

    @Override
    public boolean isServiceEnabled(Context context) {
        String permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        int result = ContextCompat.checkSelfPermission(context, permission);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void setup(final ComponentActivity activity) throws BackendException {

        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            ThemedDialog.buildMaterialDialog(activity)
                    .title(R.string.title_request_permission)
                    .content(R.string.message_permission_required_external_storage)
                    .positiveText(android.R.string.ok)
                    .negativeText(android.R.string.cancel)
                    .onPositive((dialog, which) -> launcher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)).show();
        } else {
            launcher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }

    @Override
    public void teardown(ComponentActivity activity) {
        // action not supported: cannot revoke storage permission
    }

    @Override
    public void registerForActivityResult(Fragment fragment, Activity activity) {
        launcher = fragment.registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    setBackendServiceEnabled(isGranted);
                    if (!isGranted) {
                        setBackendServiceEnabled(false);
                        ThemedDialog.buildMaterialDialog(activity)
                                .title(R.string.title_warning)
                                .content(R.string.message_permission_required_not_granted)
                                .show();
                    }
                }
        );
    }
}
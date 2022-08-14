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

package com.rahul.moneywallet.ui.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.pnikosis.materialishprogress.ProgressWheel;
import com.rahul.moneywallet.R;
import com.rahul.moneywallet.broadcast.LocalAction;
import com.rahul.moneywallet.service.UpgradeLegacyEditionIntentService;
import com.rahul.moneywallet.storage.preference.PreferenceManager;
import com.rahul.moneywallet.ui.activity.base.ThemedActivity;
import com.rahul.moneywallet.ui.view.theme.ThemedDialog;

/**
 * Created by andrea on 30/07/18.
 */
public class LauncherActivity extends ThemedActivity {

    private static final String SS_UPGRADE_ERROR = "LauncherActivity::SavedState::UpgradeLegacyEditionError";



    private String mUpgradeLegacyEditionError = null;

    private ProgressWheel mProgressWheel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (UpgradeLegacyEditionIntentService.isLegacyEditionDetected(this)) {
            setContentView(R.layout.activity_launcher_legacy_edition_upgrade);
            mProgressWheel = findViewById(R.id.progress_wheel);
            // prepare the broadcast receiver
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(LocalAction.ACTION_LEGACY_EDITION_UPGRADE_STARTED);
            intentFilter.addAction(LocalAction.ACTION_LEGACY_EDITION_UPGRADE_FINISHED);
            intentFilter.addAction(LocalAction.ACTION_LEGACY_EDITION_UPGRADE_FAILED);
            LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, intentFilter);
            // start the service
            if (savedInstanceState == null) {
                mProgressWheel.setVisibility(View.INVISIBLE);
                startService(new Intent(this, UpgradeLegacyEditionIntentService.class));
            } else {
                mUpgradeLegacyEditionError = savedInstanceState.getString(SS_UPGRADE_ERROR);
                showUpgradeLegacyEditionErrorMessage();
            }
        } else {
            if (!PreferenceManager.isFirstStartDone()) {
                setContentView(R.layout.activity_launcher_first_start);
                Button firstStartButton = findViewById(R.id.first_start_button);
                Button restoreBackupButton = findViewById(R.id.restore_backup_button);
                ActivityResultLauncher<Intent> requestFirstStart = registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result.getResultCode() == RESULT_OK) {
                                PreferenceManager.setIsFirstStartDone(true);
                                startMainActivity();
                            }
                        }
                );
                firstStartButton.setOnClickListener(v -> {
                    Intent intent = new Intent(LauncherActivity.this, TutorialActivity.class);
                    requestFirstStart.launch(intent);
                });
                restoreBackupButton.setOnClickListener(v -> {
                    Intent intent = new Intent(LauncherActivity.this, BackupListActivity.class);
                    intent.putExtra(BackupListActivity.BACKUP_MODE, BackupListActivity.RESTORE_ONLY);
                    requestFirstStart.launch(intent);
                });
            } else {
                startMainActivity();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SS_UPGRADE_ERROR, mUpgradeLegacyEditionError);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
    }

    private void startMainActivity() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void showUpgradeLegacyEditionErrorMessage() {
        if (mProgressWheel != null) {
            mProgressWheel.setVisibility(View.INVISIBLE);
        }
        ThemedDialog.buildMaterialDialog(LauncherActivity.this)
                .title(R.string.title_failed)
                .content(R.string.message_error_legacy_upgrade_failed, mUpgradeLegacyEditionError)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .onAny((dialog, which) -> startMainActivity())
                .show();
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getAction() != null) {
                switch (intent.getAction()) {
                    case LocalAction.ACTION_LEGACY_EDITION_UPGRADE_STARTED:
                        if (mProgressWheel != null) {
                            mProgressWheel.setVisibility(View.VISIBLE);
                        }
                        break;
                    case LocalAction.ACTION_LEGACY_EDITION_UPGRADE_FINISHED:
                        startMainActivity();
                        break;
                    case LocalAction.ACTION_LEGACY_EDITION_UPGRADE_FAILED:
                        mUpgradeLegacyEditionError = intent.getStringExtra(UpgradeLegacyEditionIntentService.ERROR_MESSAGE);
                        showUpgradeLegacyEditionErrorMessage();
                        break;
                }
            }
        }

    };
}
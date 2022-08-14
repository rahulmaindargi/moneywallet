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

package com.rahul.moneywallet.ui.fragment.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.afollestad.materialdialogs.MaterialDialog;
import com.rahul.moneywallet.R;
import com.rahul.moneywallet.api.BackendServiceFactory;
import com.rahul.moneywallet.broadcast.AutoBackupBroadcastReceiver;
import com.rahul.moneywallet.model.IFile;
import com.rahul.moneywallet.storage.preference.BackendManager;
import com.rahul.moneywallet.ui.activity.BackendExplorerActivity;
import com.rahul.moneywallet.ui.view.theme.ThemedDialog;

/**
 * Created by andrea on 26/11/18.
 */
public class AutoBackupSettingDialog extends DialogFragment {

    private static final String SS_BACKEND_ID = "AutoBackupSettingDialog::SavedState::BackendId";
    private static final String SS_FOLDER = "AutoBackupSettingDialog::SavedState::Folder";


    private static final int OFFSET_MIN_HOURS = 24;
    private static final int OFFSET_MAX_HOURS = 168;
    private static final int OFFSET_BETWEEN_HOURS = 4;

    private String mBackendId;

    private SwitchCompat mServiceEnabledSwitchCompat;
    private CheckBox mOnlyWiFiCheckBox;
    private CheckBox mOnlyDataChangedCheckBox;
    private TextView mOffsetTextView;
    private SeekBar mOffsetSeekBar;
    private TextView mFolderTextView;
    private EditText mPasswordEditText;

    private IFile mFolder;

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();
        if (activity == null) {
            return super.onCreateDialog(savedInstanceState);
        }
        if (savedInstanceState != null) {
            mBackendId = savedInstanceState.getString(SS_BACKEND_ID);
            mFolder = savedInstanceState.getParcelable(SS_FOLDER);
        }
        MaterialDialog dialog = ThemedDialog.buildMaterialDialog(activity)
                .title(R.string.dialog_auto_backup_title)
                .customView(R.layout.dialog_auto_backup_setting, true)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .onPositive((dialog1, which) -> onSaveSetting())
                .build();
        View view = dialog.getCustomView();
        if (view != null) {
            mServiceEnabledSwitchCompat = view.findViewById(R.id.auto_backup_enable_switch);
            mOnlyWiFiCheckBox = view.findViewById(R.id.auto_backup_wifi_check_box);
            mOnlyDataChangedCheckBox = view.findViewById(R.id.auto_backup_data_change_check_box);
            mOffsetTextView = view.findViewById(R.id.auto_backup_offset_text_view);
            mOffsetSeekBar = view.findViewById(R.id.auto_backup_offset_seek_bar);
            mFolderTextView = view.findViewById(R.id.auto_backup_folder_text_view);
            mPasswordEditText = view.findViewById(R.id.auto_backup_password_edit_text);
            // set listeners
            mOffsetSeekBar.setMax((OFFSET_MAX_HOURS - OFFSET_MIN_HOURS) / OFFSET_BETWEEN_HOURS);
            mOffsetSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    AutoBackupSettingDialog.this.onProgressChanged(progress);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    // not used
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    // not used
                }

            });
            ActivityResultLauncher<Intent> requestFolderPicker = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            mFolder = result.getData().getParcelableExtra(BackendExplorerActivity.RESULT_FILE);
                            onFolderChanged();
                        }
                    }
            );
            mFolderTextView.setOnClickListener(v -> {
                Activity activity1 = getActivity();
                if (activity1 != null) {
                    Intent intent = new Intent(activity1, BackendExplorerActivity.class);
                    intent.putExtra(BackendExplorerActivity.BACKEND_ID, mBackendId);
                    intent.putExtra(BackendExplorerActivity.MODE, BackendExplorerActivity.MODE_FOLDER_PICKER);
                    requestFolderPicker.launch(intent);
                }
            });
            if (savedInstanceState == null) {
                mServiceEnabledSwitchCompat.setChecked(BackendManager.isAutoBackupEnabled(mBackendId));
                mOnlyWiFiCheckBox.setChecked(BackendManager.isAutoBackupOnWiFiOnly(mBackendId));
                mOnlyDataChangedCheckBox.setChecked(BackendManager.isAutoBackupWhenDataIsChangedOnly(mBackendId));
                mOffsetSeekBar.setProgress((BackendManager.getAutoBackupHoursOffset(mBackendId) - OFFSET_MIN_HOURS) / OFFSET_BETWEEN_HOURS);
                mFolder = BackendServiceFactory.getFile(mBackendId, BackendManager.getAutoBackupFolder(mBackendId));
                mPasswordEditText.setText(BackendManager.getAutoBackupPassword(mBackendId));
            }
            onProgressChanged(mOffsetSeekBar.getProgress());
            onFolderChanged();
        }
        return dialog;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SS_BACKEND_ID, mBackendId);
        outState.putParcelable(SS_FOLDER, mFolder);
    }

    public void show(FragmentManager fragmentManager, String tag, String backendId) {
        mBackendId = backendId;
        show(fragmentManager, tag);
    }

    private void onProgressChanged(int progress) {
        int hours = OFFSET_MIN_HOURS + (progress * OFFSET_BETWEEN_HOURS);
        mOffsetTextView.setText(getString(R.string.hint_auto_backup_every_n_hours, hours));
    }

    private void onFolderChanged() {
        if (mFolder != null) {
            mFolderTextView.setText(mFolder.getName());
        } else {
            mFolderTextView.setText(R.string.hint_auto_backup_root_folder);
        }
    }

    private void onSaveSetting() {
        BackendManager.setAutoBackupEnabled(mBackendId, mServiceEnabledSwitchCompat.isChecked());
        BackendManager.setAutoBackupOnWiFiOnly(mBackendId, mOnlyWiFiCheckBox.isChecked());
        BackendManager.setAutoBackupWhenDataIsChangedOnly(mBackendId, mOnlyDataChangedCheckBox.isChecked());
        BackendManager.setAutoBackupHoursOffset(mBackendId, OFFSET_MIN_HOURS + (mOffsetSeekBar.getProgress() * OFFSET_BETWEEN_HOURS));
        BackendManager.setAutoBackupFolder(mBackendId, mFolder != null ? mFolder.encodeToString() : null);
        BackendManager.setAutoBackupPassword(mBackendId, mPasswordEditText.getText().toString());
        AutoBackupBroadcastReceiver.scheduleAutoBackupTask(getActivity());
    }

}
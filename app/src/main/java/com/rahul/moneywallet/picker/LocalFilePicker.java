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

package com.rahul.moneywallet.picker;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.rahul.moneywallet.api.BackendServiceFactory;
import com.rahul.moneywallet.model.LocalFile;
import com.rahul.moneywallet.ui.activity.BackendExplorerActivity;

/**
 * Created by andrea on 01/02/18.
 */
public class LocalFilePicker extends Fragment {

    public static final int MODE_FILE_PICKER = 0;
    public static final int MODE_FOLDER_PICKER = 1;

    private static final String SS_PICKER_MODE = "LocalFilePicker::SavedState::PickerMode";
    private static final String SS_CURRENT_FILE = "LocalFilePicker::SavedState::CurrentIcon";

    private static final String ARG_PICKER_MODE = "LocalFilePicker::Argument::PickerMode";

    private Controller mController;

    private int mPickerMode;
    private LocalFile mCurrentFile;
    private ActivityResultLauncher<Intent> requestFilePicker;
    private ActivityResultLauncher<String> permissionLauncher;

    public static LocalFilePicker createPicker(FragmentManager fragmentManager, String tag, int mode) {
        LocalFilePicker filePicker = (LocalFilePicker) fragmentManager.findFragmentByTag(tag);
        if (filePicker == null) {
            Bundle arguments = new Bundle();
            arguments.putInt(ARG_PICKER_MODE, mode);
            filePicker = new LocalFilePicker();
            filePicker.setArguments(arguments);
            fragmentManager.beginTransaction().add(filePicker, tag).commit();
        }
        return filePicker;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Controller) {
            mController = (Controller) context;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mPickerMode = savedInstanceState.getInt(SS_PICKER_MODE);
            mCurrentFile = savedInstanceState.getParcelable(SS_CURRENT_FILE);
        } else {
            Bundle arguments = getArguments();
            if (arguments != null) {
                mPickerMode = arguments.getInt(ARG_PICKER_MODE);
            } else {
                mPickerMode = MODE_FILE_PICKER;
            }
            mCurrentFile = null;
        }
        requestFilePicker = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        mCurrentFile = result.getData().getParcelableExtra(BackendExplorerActivity.RESULT_FILE);
                        fireCallbackSafely();
                    }
                }
        );
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
            if (result) {
                startPicker(getActivity());
            }
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        fireCallbackSafely();
    }

    private void fireCallbackSafely() {
        if (mController != null) {
            mController.onLocalFileChanged(getTag(), mPickerMode, mCurrentFile);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SS_PICKER_MODE, mPickerMode);
        outState.putParcelable(SS_CURRENT_FILE, mCurrentFile);
    }

    public boolean isSelected() {
        return mCurrentFile != null;
    }

    public LocalFile getCurrentFile() {
        return mCurrentFile;
    }

    public void showPicker() {
        Activity activity = getActivity();
        if (activity != null) {
            if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                startPicker(activity);
            } else {
                permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }
    }

    private void startPicker(Context context) {
        Intent intent = new Intent(context, BackendExplorerActivity.class);
        intent.putExtra(BackendExplorerActivity.BACKEND_ID, BackendServiceFactory.SERVICE_ID_EXTERNAL_MEMORY);
        switch (mPickerMode) {
            case MODE_FILE_PICKER:
                intent.putExtra(BackendExplorerActivity.MODE, BackendExplorerActivity.MODE_FILE_PICKER);
                break;
            case MODE_FOLDER_PICKER:
                intent.putExtra(BackendExplorerActivity.MODE, BackendExplorerActivity.MODE_FOLDER_PICKER);
                break;
        }
        requestFilePicker.launch(intent);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mController = null;
    }



    public interface Controller {

        void onLocalFileChanged(String tag, int mode, LocalFile localFile);
    }
}
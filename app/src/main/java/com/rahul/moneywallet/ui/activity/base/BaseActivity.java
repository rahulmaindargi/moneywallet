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

package com.rahul.moneywallet.ui.activity.base;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.rahul.moneywallet.model.LockMode;
import com.rahul.moneywallet.storage.preference.PreferenceManager;
import com.rahul.moneywallet.ui.activity.LockActivity;

/**
 * Created by andrea on 24/07/18.
 */
public abstract class BaseActivity extends ThemedActivity {


    private static final long MAX_LOCK_TIME_INTERVAL = 1000;

    boolean mActivityLocked = true;
    boolean mActivityLockEnabled = true;
    boolean mActivityResultOk = true;
    private ActivityResultLauncher<Intent> requestCodeLock;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestCodeLock = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK) {
                        moveTaskToBack(true);
                        mActivityResultOk = false;
                    } else {
                        mActivityResultOk = true;
                    }
                }
        );

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (PreferenceManager.getCurrentLockMode() != LockMode.NONE) {
            if ((System.currentTimeMillis() - PreferenceManager.getLastLockTime()) > MAX_LOCK_TIME_INTERVAL) {
                mActivityLocked = true;
                if (mActivityResultOk) {
                    Intent intent = new Intent(this, LockActivity.class);
                    requestCodeLock.launch(intent);
                } else {
                    mActivityResultOk = true;
                }
            } else {
                mActivityLocked = false;
            }
            mActivityLockEnabled = true;
        } else {
            mActivityLocked = false;
            mActivityLockEnabled = false;
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (mActivityLockEnabled && !mActivityLocked) {
            PreferenceManager.setLastLockTime(System.currentTimeMillis());
        }
    }
}
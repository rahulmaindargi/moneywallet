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

package com.rahul.moneywallet;

import android.app.Application;
import android.content.Context;

import androidx.multidex.MultiDex;

import com.rahul.moneywallet.broadcast.AutoBackupBroadcastReceiver;
import com.rahul.moneywallet.broadcast.DailyBroadcastReceiver;
import com.rahul.moneywallet.broadcast.RecurrenceBroadcastReceiver;
import com.rahul.moneywallet.storage.preference.BackendManager;
import com.rahul.moneywallet.storage.preference.PreferenceManager;
import com.rahul.moneywallet.ui.notification.NotificationContract;
import com.rahul.moneywallet.ui.view.theme.ThemeEngine;
import com.rahul.moneywallet.utils.CurrencyManager;

/**
 * Created by andrea on 17/01/18.
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        PreferenceManager.initialize(this);
        BackendManager.initialize(this);
        ThemeEngine.initialize(this);
        CurrencyManager.initialize(this);
        NotificationContract.initializeNotificationChannels(this);
        initializeScheduledTimers();
    }

    private void initializeScheduledTimers() {
        // The application may be killed by the OS when resources are needed or by the user for
        // every kind of reasons. When the application is killed all the scheduled operations are
        // canceled by the OS. This is the best place where all those things can be scheduled again.
        DailyBroadcastReceiver.scheduleDailyNotification(this);
        RecurrenceBroadcastReceiver.scheduleRecurrenceTask(this);
        AutoBackupBroadcastReceiver.scheduleAutoBackupTask(this);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
        //Reflection.unseal(base);
    }
}
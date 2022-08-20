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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.rahul.moneywallet.R;
import com.rahul.moneywallet.broadcast.LocalAction;
import com.rahul.moneywallet.model.LockMode;
import com.rahul.moneywallet.service.AbstractCurrencyRateDownloadIntentService;
import com.rahul.moneywallet.storage.preference.PreferenceManager;
import com.rahul.moneywallet.ui.activity.CurrencyListActivity;
import com.rahul.moneywallet.ui.activity.LockActivity;
import com.rahul.moneywallet.ui.preference.ThemedInputPreference;
import com.rahul.moneywallet.ui.preference.ThemedListPreference;
import com.rahul.moneywallet.utils.DateFormatter;

import java.util.Date;

/**
 * Created by andrea on 07/03/18.
 */
public class UtilitySettingFragment extends PreferenceFragmentCompat {

    private ThemedListPreference mDailyReminderPreference;
    private ThemedListPreference mSecurityModeListPreference;
    private Preference mSecurityModeChangeKeyPreference;
    private ThemedListPreference mExchangeRateServiceListPreference;
    private ThemedInputPreference mExchangeRateCustomApiKey;
    private Preference mExchangeRateUpdatePreference;
    private Preference mCurrencyManagementPreference;
    private ActivityResultLauncher<Intent> requestLockActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Activity activity = getActivity();
        if (activity != null) {
            LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(activity);
            broadcastManager.registerReceiver(mLocalBroadcastReceiver, new IntentFilter(LocalAction.ACTION_EXCHANGE_RATES_UPDATED));
        }
        requestLockActivity = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> setupCurrentLockMode()
        );
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Activity activity = getActivity();
        if (activity != null) {
            LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(activity);
            broadcastManager.unregisterReceiver(mLocalBroadcastReceiver);
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.settings_utility);
        mDailyReminderPreference = findPreference("daily_reminder");
        mSecurityModeListPreference = findPreference("security_mode");
        mSecurityModeChangeKeyPreference = findPreference("security_change_key");
        mExchangeRateServiceListPreference = findPreference("exchange_rate_source");
        mExchangeRateCustomApiKey = findPreference("exchange_rate_api_key");
        mExchangeRateUpdatePreference = findPreference("exchange_rate_update");
        mCurrencyManagementPreference = findPreference("currency_management");
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // setup preference logic
        mDailyReminderPreference.setEntries(new String[] {
                getString(R.string.setting_item_security_none),
                "00:00", "01:00", "02:00", "03:00", "04:00", "05:00",
                "06:00", "07:00", "08:00", "09:00", "10:00", "11:00",
                "12:00", "13:00", "14:00", "15:00", "16:00", "17:00",
                "18:00", "19:00", "20:00", "21:00", "22:00", "23:00"
        });
        mDailyReminderPreference.setEntryValues(new String[] {
                String.valueOf(PreferenceManager.DAILY_REMINDER_DISABLED),
                "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11",
                "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23"
        });
        if (isFingerprintAuthSupported(getActivity())) {
            mSecurityModeListPreference.setEntries(new String[] {
                    getString(R.string.setting_item_security_none),
                    getString(R.string.setting_item_security_pin),
                    getString(R.string.setting_item_security_sequence),
                    getString(R.string.setting_item_security_fingerprint)
            });
            mSecurityModeListPreference.setEntryValues(new String[] {
                    String.valueOf(PreferenceManager.LOCK_MODE_NONE),
                    String.valueOf(PreferenceManager.LOCK_MODE_PIN),
                    String.valueOf(PreferenceManager.LOCK_MODE_SEQUENCE),
                    String.valueOf(PreferenceManager.LOCK_MODE_FINGERPRINT)
            });
        } else {
            mSecurityModeListPreference.setEntries(new String[] {
                    getString(R.string.setting_item_security_none),
                    getString(R.string.setting_item_security_pin),
                    getString(R.string.setting_item_security_sequence)
            });
            mSecurityModeListPreference.setEntryValues(new String[] {
                    String.valueOf(PreferenceManager.LOCK_MODE_NONE),
                    String.valueOf(PreferenceManager.LOCK_MODE_PIN),
                    String.valueOf(PreferenceManager.LOCK_MODE_SEQUENCE)
            });
        }
        mExchangeRateServiceListPreference.setEntries(new String[] {
                getString(R.string.setting_item_utility_exchange_rates_service_oer)
        });
        mExchangeRateServiceListPreference.setEntryValues(new String[]{
                String.valueOf(PreferenceManager.SERVICE_OPEN_EXCHANGE_RATE)
        });
        // setup current (or default) values
        setupCurrentDailyReminder();
        setupCurrentLockMode();
        setupCurrentExchangeRateService();
        setupCurrentExchangeRateCustomApiKey();
        setupCurrentExchangeRateUpdate();
        // attach a listener to get notified when values changes
        mDailyReminderPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            int hour = Integer.parseInt((String) newValue);
            PreferenceManager.setCurrentDailyReminder(getActivity(), hour);
            setupCurrentDailyReminder();
            return false;
        });
        mSecurityModeListPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            String oldValue = ((ThemedListPreference) preference).getValue();
            String value = (String) newValue;
            if (TextUtils.equals(oldValue, value)) {
                // the value is not changed
                return false;
            }
            Intent intent = null;
            int integerValue = Integer.parseInt(value);
            switch (integerValue) {
                case PreferenceManager.LOCK_MODE_NONE:
                    intent = LockActivity.disableLock(getActivity());
                    break;
                case PreferenceManager.LOCK_MODE_PIN:
                case PreferenceManager.LOCK_MODE_SEQUENCE:
                case PreferenceManager.LOCK_MODE_FINGERPRINT:
                    if (Integer.parseInt(oldValue) == PreferenceManager.LOCK_MODE_NONE) {
                        intent = LockActivity.enableLock(getActivity(), LockMode.get(integerValue));
                    } else {
                        intent = LockActivity.changeMode(getActivity(), LockMode.get(integerValue));
                    }
                    break;
            }
            if (intent != null) {
                requestLockActivity.launch(intent);
            }
            return false;
        });
        mSecurityModeChangeKeyPreference.setOnPreferenceClickListener(preference -> {
            startActivity(LockActivity.changeKey(getActivity()));
            return false;
        });
        mExchangeRateServiceListPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            int index = Integer.parseInt((String) newValue);
            PreferenceManager.setCurrentExchangeRateService(index);
            setupCurrentExchangeRateService();
            setupCurrentExchangeRateCustomApiKey();
            return false;
        });
        mExchangeRateCustomApiKey.setInput(R.string.setting_item_utility_exchange_rates_custom_api_key_hint, true, InputType.TYPE_CLASS_TEXT);
        mExchangeRateCustomApiKey.setOnPreferenceChangeListener((preference, newValue) -> {
            int service = PreferenceManager.getCurrentExchangeRateService();
            PreferenceManager.setServiceApiKey(service, (String) newValue);
            setupCurrentExchangeRateCustomApiKey();
            return false;
        });
        mExchangeRateUpdatePreference.setOnPreferenceClickListener(preference -> {
            Activity activity = getActivity();
            if (activity != null) {
                Intent intent = AbstractCurrencyRateDownloadIntentService.buildIntent(activity);
                activity.startService(intent);
            }
            return false;
        });
        mCurrencyManagementPreference.setOnPreferenceClickListener(preference -> {
            Activity activity = getActivity();
            if (activity != null) {
                Intent intent = new Intent(activity, CurrencyListActivity.class);
                intent.putExtra(CurrencyListActivity.ACTIVITY_MODE, CurrencyListActivity.CURRENCY_MANAGER);
                startActivity(intent);
            }
            return false;
        });
    }

    @NonNull
    @Override
    public RecyclerView onCreateRecyclerView(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, Bundle savedInstanceState) {
        RecyclerView recyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState);
        recyclerView.setPadding(0, 0, 0, 0);
        return recyclerView;
    }

    private boolean isFingerprintAuthSupported(Context context) {

        return BiometricManager.BIOMETRIC_SUCCESS == BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK);
    }

    private void setupCurrentDailyReminder() {
        int hour = PreferenceManager.getCurrentDailyReminder();
        mDailyReminderPreference.setValue(String.valueOf(hour));
        if (hour == PreferenceManager.DAILY_REMINDER_DISABLED) {
            mDailyReminderPreference.setSummary(R.string.setting_item_daily_reminder_none);
        } else {
            String summary = getString(R.string.setting_summary_daily_reminder, hour);
            mDailyReminderPreference.setSummary(summary);
        }
    }

    private void setupCurrentLockMode() {
        LockMode lockMode = PreferenceManager.getCurrentLockMode();
        mSecurityModeListPreference.setValue(lockMode.getValueAsString());
        switch (lockMode) {
            case NONE:
                mSecurityModeListPreference.setSummary(R.string.setting_item_security_none);
                mSecurityModeChangeKeyPreference.setVisible(false);
                break;
            case PIN:
                mSecurityModeListPreference.setSummary(R.string.setting_item_security_pin);
                mSecurityModeChangeKeyPreference.setTitle(R.string.setting_title_security_change_pin);
                mSecurityModeChangeKeyPreference.setVisible(true);
                break;
            case SEQUENCE:
                mSecurityModeListPreference.setSummary(R.string.setting_item_security_sequence);
                mSecurityModeChangeKeyPreference.setTitle(R.string.setting_title_security_change_sequence);
                mSecurityModeChangeKeyPreference.setVisible(true);
                break;
            case FINGERPRINT:
                mSecurityModeListPreference.setSummary(R.string.setting_item_security_fingerprint);
                mSecurityModeChangeKeyPreference.setVisible(false);
                break;
        }
    }

    private void setupCurrentExchangeRateService() {
        int index = PreferenceManager.getCurrentExchangeRateService();
        mExchangeRateServiceListPreference.setValue(String.valueOf(index));
        if (index == PreferenceManager.SERVICE_OPEN_EXCHANGE_RATE) {
            mExchangeRateServiceListPreference.setSummary(R.string.setting_item_utility_exchange_rates_service_oer);
            mExchangeRateCustomApiKey.setContent(R.string.setting_item_utility_exchange_rates_service_oer_custom_api_key_message);
        }
        mExchangeRateCustomApiKey.setVisible(!PreferenceManager.hasCurrentExchangeRateServiceDefaultApiKey());
    }

    private void setupCurrentExchangeRateCustomApiKey() {
        if (!PreferenceManager.hasCurrentExchangeRateServiceDefaultApiKey()) {
            String apiKey = PreferenceManager.getCurrentExchangeRateServiceCustomApiKey();
            if (!TextUtils.isEmpty(apiKey)) {
                mExchangeRateCustomApiKey.setSummary(getString(R.string.setting_summary_exchange_rate_api_key, apiKey));
                mExchangeRateCustomApiKey.setCurrentValue(apiKey);
            } else {
                mExchangeRateCustomApiKey.setSummary(R.string.setting_summary_exchange_rate_api_key_missing);
                mExchangeRateCustomApiKey.setCurrentValue(null);
            }
        } else {
            mExchangeRateCustomApiKey.setSummary(null);
            mExchangeRateCustomApiKey.setCurrentValue(null);
        }
    }

    private void setupCurrentExchangeRateUpdate() {
        long timestamp = PreferenceManager.getLastExchangeRateUpdateTimestamp();
        String summary = DateFormatter.getDateFromToday(new Date(timestamp));
        String fullSummary = getString(R.string.setting_summary_exchange_rate_update, summary);
        mExchangeRateUpdatePreference.setSummary(fullSummary);
    }


    private final BroadcastReceiver mLocalBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (LocalAction.ACTION_EXCHANGE_RATES_UPDATED.equals(intent.getAction())) {
                setupCurrentExchangeRateUpdate();
            }
        }

    };
}
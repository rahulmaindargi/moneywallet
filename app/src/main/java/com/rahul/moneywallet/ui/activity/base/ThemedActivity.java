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

import android.app.ActivityManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowInsetsController;

import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.LayoutInflaterCompat;

import com.rahul.moneywallet.R;
import com.rahul.moneywallet.ui.view.theme.ITheme;
import com.rahul.moneywallet.ui.view.theme.ThemeEngine;
import com.rahul.moneywallet.ui.view.theme.ThemedLayoutInflater;
import com.rahul.moneywallet.utils.Utils;

/**
 * This activity is used as base activity for all the application activities.
 * It will automatically apply the current theme to all the views that are subscribed
 * to the ThemeEngine.
 * The first step is done during the inflation of the layout: here the theme properties are
 * automatically set to the view that is subscribed just after the creation.
 * The activity will than register itself as an observer for the current theme changes.
 * Whenever a property of the current theme changes, the observer will be notified.
 * Before the destruction the activity MUST un subscribe as observer to avoid memory leaks.
 */
public abstract class ThemedActivity extends AppCompatActivity implements ThemeEngine.ThemeObserver {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeEngine.registerObserver(this);
        applyLayoutInflaterWrapper();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        onThemeSetup(ThemeEngine.getTheme());
    }

    /**
     * This method is used to set the layout inflater factory even if it is already set.
     * Use reflection to access the private boolean field and set it as false.
     * The AppCompat library uses it's own factory to correct the xml layouts.
     * In this way both the factories will coexist without problems.
     */
    private void applyLayoutInflaterWrapper() {
        LayoutInflater inflater = getLayoutInflater();
        final LayoutInflater.Factory2 baseFactory = inflater.getFactory2();
        try {
            LayoutInflaterCompat.setFactory2(getLayoutInflater().cloneInContext(getApplicationContext()),
                    new ThemedLayoutInflater(baseFactory));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ThemeEngine.unregisterObserver(this);
    }

    @Override
    public void onThemeChanged(ITheme theme) {
        ThemeEngine.applyTheme(getWindow().peekDecorView(), true);
        onThemeSetup(theme);
    }

    /**
     * This method is called by the activity when the activity has been created and
     * dynamically when the theme engine detects a change of a value of the theme.
     * @param theme current theme to apply
     */
    @CallSuper
    protected void onThemeSetup(ITheme theme) {
        setupActivityBaseTheme(theme);
    }

    private void setupActivityBaseTheme(ITheme theme) {
        onThemeStatusBar(theme);
        onThemeStatusBarIcons(theme);
        onThemeTaskDescription(theme);
        onThemeWindowBackground(theme);
    }

    protected void onThemeStatusBar(ITheme theme) {
        getWindow().setStatusBarColor(theme.getColorPrimaryDark());
    }

    protected void onThemeStatusBarIcons(ITheme theme) {
        WindowInsetsController insetsController = getWindow().getInsetsController();
        //int systemUiVisibility = insetsController.getSystemBarsAppearance();
        int statusBarColor = theme.getColorPrimaryDark();
        boolean isStatusBarLight = Utils.isColorLight(statusBarColor);
        if (isStatusBarLight) {
            insetsController.setSystemBarsAppearance(WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
        } else {
            insetsController.setSystemBarsAppearance(~WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
        }
    }

    protected void onThemeTaskDescription(ITheme theme) {
        String name = getString(R.string.app_name);
        setTaskDescription(new ActivityManager.TaskDescription(name, R.mipmap.ic_launcher, theme.getColorPrimary()));
    }

    protected void onThemeWindowBackground(ITheme theme) {
        View view = getWindow().getDecorView();
        view.setBackgroundColor(theme.getColorWindowBackground());
    }
}
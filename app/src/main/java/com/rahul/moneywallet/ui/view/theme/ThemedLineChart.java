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

package com.rahul.moneywallet.ui.view.theme;

import android.content.Context;
import android.util.AttributeSet;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;

/**
 * Created by andrea on 21/08/18.
 */
public class ThemedLineChart extends LineChart implements ThemeEngine.ThemeConsumer {

    public ThemedLineChart(Context context) {
        super(context);
    }

    public ThemedLineChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ThemedLineChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onApplyTheme(ITheme theme) {
        int textColor = theme.getTextColorSecondary();
        int iconColor = theme.getIconColor();
        // style xAxis
        XAxis xAxis = getXAxis();
        xAxis.setAxisLineColor(iconColor);
        xAxis.setGridColor(iconColor);
        xAxis.setTextColor(textColor);
        // style yAxis left
        YAxis yLeftAxis = getAxisLeft();
        yLeftAxis.setZeroLineColor(iconColor);
        yLeftAxis.setAxisLineColor(iconColor);
        yLeftAxis.setGridColor(iconColor);
        yLeftAxis.setTextColor(textColor);
        // style yAxis right
        YAxis yRightAxis = getAxisRight();
        yRightAxis.setZeroLineColor(iconColor);
        yRightAxis.setAxisLineColor(iconColor);
        yRightAxis.setGridColor(iconColor);
        yRightAxis.setTextColor(textColor);
        // style legend
        Legend legend = getLegend();
        legend.setTextColor(textColor);
        // style description
        Description description = getDescription();
        description.setTextColor(textColor);
        // style the chart itself
        setNoDataTextColor(textColor);
    }
}
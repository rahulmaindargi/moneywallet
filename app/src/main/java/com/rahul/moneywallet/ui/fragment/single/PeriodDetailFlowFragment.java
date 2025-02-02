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

package com.rahul.moneywallet.ui.fragment.single;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.itsronald.widget.ViewPagerIndicator;
import com.rahul.moneywallet.R;
import com.rahul.moneywallet.background.PeriodDetailFlowLoader;
import com.rahul.moneywallet.model.Money;
import com.rahul.moneywallet.model.PeriodDetailFlowData;
import com.rahul.moneywallet.storage.preference.CurrentWalletController;
import com.rahul.moneywallet.storage.preference.PreferenceManager;
import com.rahul.moneywallet.ui.activity.TransactionListActivity;
import com.rahul.moneywallet.ui.adapter.pager.PieChartViewPagerAdapter;
import com.rahul.moneywallet.ui.adapter.recycler.PeriodDetailFlowAdapter;
import com.rahul.moneywallet.utils.MoneyFormatter;

import java.util.Date;
import java.util.Objects;

/**
 * Created by andrea on 01/05/18.
 */
public class PeriodDetailFlowFragment extends Fragment implements PeriodDetailFlowAdapter.Controller, LoaderManager.LoaderCallbacks<PeriodDetailFlowData>, CurrentWalletController {

    private static final int LOADER_FRAGMENT_DATA = 3648;

    private static final String ARG_START_DATE = "PeriodDetailFlowFragment::Arguments::StartDate";
    private static final String ARG_END_DATE = "PeriodDetailFlowFragment::Arguments::EndDate";
    private static final String ARG_INCOMES = "PeriodDetailFlowFragment::Arguments::Incomes";

    private static final String ARG_SELECTED_CATEGORY = "PeriodDetailFlowFragment::Arguments::SelectedCategory";


    public static PeriodDetailFlowFragment newInstance(Date start, Date end, boolean incomes) {
        Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_START_DATE, start);
        arguments.putSerializable(ARG_END_DATE, end);
        arguments.putBoolean(ARG_INCOMES, incomes);

        PeriodDetailFlowFragment fragment = new PeriodDetailFlowFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    private TextView mHeaderLeftTextView;
    private TextView mHeaderRightTextView;
    private RecyclerView mRecyclerView;

    private PieChartViewPagerAdapter mPieChartViewPagerAdapter;
    private ViewPagerIndicator mPieChartViewPagerIndicator;
    private PeriodDetailFlowAdapter mRecyclerViewAdapter;

    private Date mStartDate;
    private Date mEndDate;
    private boolean mIncomes;


    private final MoneyFormatter mMoneyFormatter = MoneyFormatter.getInstance();

    private BroadcastReceiver mBroadcastReceiver;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mBroadcastReceiver = PreferenceManager.registerCurrentWalletObserver(context, this);
    }

    @Override
    public void onDetach() {
        PreferenceManager.unregisterCurrentWalletObserver(getActivity(), mBroadcastReceiver);
        super.onDetach();
    }

    @Nullable
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_panel_period_detail, container, false);
        mHeaderLeftTextView = view.findViewById(R.id.left_text_view);
        mHeaderRightTextView = view.findViewById(R.id.right_text_view);
        ViewPager pieChartViewPager = view.findViewById(R.id.chart_view_pager);
        mPieChartViewPagerIndicator = view.findViewById(R.id.view_pager_indicator);
        mRecyclerView = view.findViewById(R.id.recycler_view);
        // unpack the arguments bundle
        Bundle arguments = getArguments();
        if (arguments != null) {
            mStartDate = (Date) arguments.getSerializable(ARG_START_DATE);
            mEndDate = (Date) arguments.getSerializable(ARG_END_DATE);
            mIncomes = arguments.getBoolean(ARG_INCOMES);
        } else {
            throw new IllegalStateException("The fragment must be initialized using the newInstance() method");
        }
        // create the adapter
        mPieChartViewPagerAdapter = new PieChartViewPagerAdapter();
        pieChartViewPager.setAdapter(mPieChartViewPagerAdapter);
        mRecyclerViewAdapter = new PeriodDetailFlowAdapter(this, mIncomes);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setNestedScrollingEnabled(false);
        mRecyclerView.setAdapter(mRecyclerViewAdapter);
        // return the view
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mHeaderLeftTextView.setText(mIncomes ? R.string.hint_incomes : R.string.hint_expenses);
        LoaderManager.getInstance(this).restartLoader(LOADER_FRAGMENT_DATA, null, this);
    }

    @Override
    public void onCategoryClick(long id, String name) {
        Loader<?> objectLoader = Objects.requireNonNull(LoaderManager.getInstance(this).getLoader(LOADER_FRAGMENT_DATA));
        long selectedCategory = ((PeriodDetailFlowLoader) objectLoader).getSelectedCategory();
        Log.d("PeriodDetailFlowFragment", "selectedCategory : " + selectedCategory);
        if (!mIncomes && selectedCategory == -1) {
            Bundle bundle = new Bundle();
            bundle.putLong(ARG_SELECTED_CATEGORY, id);
            mHeaderLeftTextView.setText(name);
            LoaderManager.getInstance(this).restartLoader(LOADER_FRAGMENT_DATA, bundle, this);
            return;
        }

        Intent intent = new Intent(getActivity(), TransactionListActivity.class);
        intent.putExtra(TransactionListActivity.CATEGORY_ID, id);
        intent.putExtra(TransactionListActivity.START_DATE, mStartDate);
        intent.putExtra(TransactionListActivity.END_DATE, mEndDate);
        startActivity(intent);
    }

    @NonNull
    @Override
    public Loader<PeriodDetailFlowData> onCreateLoader(int id, Bundle args) {
        long selectedCategory = -1;
        if (args != null && args.containsKey(ARG_SELECTED_CATEGORY)) {
            selectedCategory = args.getLong(ARG_SELECTED_CATEGORY);
        }
        return new PeriodDetailFlowLoader(getActivity(), mStartDate, mEndDate, mIncomes, selectedCategory);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<PeriodDetailFlowData> loader, PeriodDetailFlowData data) {
        if (data != null) {
            if (mIncomes) {
                mMoneyFormatter.applyTintedIncome(mHeaderRightTextView, data.getTotalMoney());
            } else {
                mMoneyFormatter.applyTintedExpense(mHeaderRightTextView, data.getTotalMoney());
            }
            mPieChartViewPagerAdapter.setData(data);
            mRecyclerViewAdapter.setData(data);
            mPieChartViewPagerIndicator.setVisibility(data.getChartCount() > 1 ? View.VISIBLE : View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);
        } else {
            mMoneyFormatter.applyNotTinted(mHeaderRightTextView, Money.empty());
            mPieChartViewPagerAdapter.setData(null);
            mRecyclerViewAdapter.setData(null);
            mPieChartViewPagerIndicator.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<PeriodDetailFlowData> loader) {
        // nothing to release
    }

    @Override
    public void onCurrentWalletChanged(long walletId) {
        LoaderManager.getInstance(this).restartLoader(LOADER_FRAGMENT_DATA, null, this);
    }
}
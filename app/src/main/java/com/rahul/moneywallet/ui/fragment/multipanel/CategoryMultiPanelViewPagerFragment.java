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

package com.rahul.moneywallet.ui.fragment.multipanel;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.PagerAdapter;
import android.view.MenuItem;

import com.rahul.moneywallet.R;
import com.rahul.moneywallet.broadcast.LocalAction;
import com.rahul.moneywallet.broadcast.Message;
import com.rahul.moneywallet.storage.database.Contract;
import com.rahul.moneywallet.ui.activity.CategorySortActivity;
import com.rahul.moneywallet.ui.activity.NewEditCategoryActivity;
import com.rahul.moneywallet.ui.activity.NewEditItemActivity;
import com.rahul.moneywallet.ui.adapter.pager.CategoryViewPagerAdapter;
import com.rahul.moneywallet.ui.fragment.base.MultiPanelViewPagerItemFragment;
import com.rahul.moneywallet.ui.fragment.base.SecondaryPanelFragment;
import com.rahul.moneywallet.ui.fragment.secondary.CategoryItemFragment;

/**
 * Created by andrea on 10/02/18.
 */
public class CategoryMultiPanelViewPagerFragment extends MultiPanelViewPagerItemFragment {

    private static final String SECONDARY_FRAGMENT_TAG = "CategoryMultiPanelViewPagerFragment::Tag::SecondaryPanelFragment";

    @NonNull
    @Override
    protected PagerAdapter onCreatePagerAdapter(FragmentManager fragmentManager) {
        return new CategoryViewPagerAdapter(fragmentManager, getActivity(), true, true);
    }

    @Override
    protected int getTitleRes() {
        return R.string.menu_category;
    }

    @MenuRes
    @Override
    protected int onInflateMenu() {
        return R.menu.menu_category_multipanel_fragment;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_sort_items:
                Intent intent = new Intent(getActivity(), CategorySortActivity.class);
                switch (getViewPagerPosition()) {
                    case 0:
                        intent.putExtra(CategorySortActivity.TYPE, Contract.CategoryType.INCOME);
                        break;
                    case 1:
                        intent.putExtra(CategorySortActivity.TYPE, Contract.CategoryType.EXPENSE);
                        break;
                    case 2:
                        intent.putExtra(CategorySortActivity.TYPE, Contract.CategoryType.SYSTEM);
                        break;
                }
                startActivity(intent);
                break;
        }
        return false;
    }

    @Override
    protected void onFloatingActionButtonClick() {
        Intent intent = new Intent(getActivity(), NewEditCategoryActivity.class);
        intent.putExtra(NewEditItemActivity.MODE, NewEditItemActivity.Mode.NEW_ITEM);
        switch (getViewPagerPosition()) {
            case 0:
            case 2:
                intent.putExtra(NewEditCategoryActivity.TYPE, Contract.CategoryType.INCOME);
                break;
            case 1:
                intent.putExtra(NewEditCategoryActivity.TYPE, Contract.CategoryType.EXPENSE);
                break;
        }
        startActivity(intent);
    }

    @Override
    protected SecondaryPanelFragment onCreateSecondaryPanel() {
        return new CategoryItemFragment();
    }

    @Override
    protected String getSecondaryFragmentTag() {
        return SECONDARY_FRAGMENT_TAG;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        IntentFilter filter = new IntentFilter(LocalAction.ACTION_ITEM_CLICK);
        LocalBroadcastManager.getInstance(context).registerReceiver(mItemClickReceiver, filter);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Activity activity = getActivity();
        if (activity != null) {
            LocalBroadcastManager.getInstance(activity).unregisterReceiver(mItemClickReceiver);
        }
    }

    private BroadcastReceiver mItemClickReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getIntExtra(Message.ITEM_TYPE, 0) == Message.TYPE_CATEGORY) {
                showItemId(intent.getLongExtra(Message.ITEM_ID, 0L));
                showSecondaryPanel();
            }
        }

    };
}
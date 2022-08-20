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


import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.DrawableRes;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.rahul.moneywallet.R;
import com.rahul.moneywallet.storage.database.Contract;
import com.rahul.moneywallet.storage.database.DataContentProvider;
import com.rahul.moneywallet.ui.activity.base.SinglePanelActivity;
import com.rahul.moneywallet.ui.adapter.recycler.TransactionCursorAdapter;
import com.rahul.moneywallet.ui.view.AdvancedRecyclerView;
import com.rahul.moneywallet.ui.view.theme.ThemedDialog;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by andrea on 04/04/18.
 */
public class SearchActivity extends SinglePanelActivity implements TransactionCursorAdapter.ActionListener, SwipeRefreshLayout.OnRefreshListener {

    private static final String SS_SEARCH_FLAGS = "SearchActivity::SavedState::SearchFlags";

    private static final int LOADER_ID = 4325;
    private static final String ARG_QUERY = "SearchActivity::Loader::QueryString";

    private EditText mSearchEditText;
    private AdvancedRecyclerView mAdvancedRecyclerView;

    private TransactionCursorAdapter mCursorAdapter;

    private boolean[] mSearchFlags;

    @Override
    protected void onCreatePanelView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        mAdvancedRecyclerView = new AdvancedRecyclerView(this);
        mCursorAdapter = new TransactionCursorAdapter(this);
        mAdvancedRecyclerView.setAdapter(mCursorAdapter);
        mAdvancedRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdvancedRecyclerView.setEmptyText(R.string.message_no_transaction_found);
        mAdvancedRecyclerView.setState(AdvancedRecyclerView.State.EMPTY);
        mAdvancedRecyclerView.setOnRefreshListener(this);
        if (savedInstanceState != null) {
            mSearchFlags = savedInstanceState.getBooleanArray(SS_SEARCH_FLAGS);
        } else {
            mSearchFlags = new boolean[] {
                    true,   // description
                    true,   // category
                    true,   // date
                    true,   // money
                    true,   // note
                    true,   // event
                    true    // place
            };
        }
        parent.addView(mAdvancedRecyclerView);
    }

    @Override
    protected void onToolbarReady(Toolbar toolbar) {
        View view = LayoutInflater.from(this).inflate(R.layout.layout_toolbar_search_view, toolbar, true);
        mSearchEditText = view.findViewById(R.id.search_edit_text);
        mSearchEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mAdvancedRecyclerView.setState(AdvancedRecyclerView.State.LOADING);
                loadTransactionAsync(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }

        });
        mAdvancedRecyclerView.setState(AdvancedRecyclerView.State.LOADING);
        loadTransactionAsync(mSearchEditText.getText().toString());
    }

    private void loadTransactionAsync(String query) {
        Bundle args = new Bundle();
        args.putString(ARG_QUERY, query);
        LoaderManager.getInstance(this).restartLoader(LOADER_ID, args, new LoaderManager.LoaderCallbacks<Cursor>() {

            @NonNull
            @Override
            public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
                String query = args.getString(ARG_QUERY);
                Uri uri = DataContentProvider.CONTENT_TRANSACTIONS;
                StringBuilder selection = new StringBuilder();
                if (mSearchFlags[0]) {
                    appendSelection(selection, Contract.Transaction.DESCRIPTION);
                }
                if (mSearchFlags[1]) {
                    appendSelection(selection, Contract.Transaction.CATEGORY_NAME);
                }
                if (mSearchFlags[2]) {
                    appendSelection(selection, Contract.Transaction.DATE);
                }
                if (mSearchFlags[3]) {
                    appendSelection(selection, Contract.Transaction.MONEY);
                }
                if (mSearchFlags[4]) {
                    appendSelection(selection, Contract.Transaction.NOTE);
                }
                if (mSearchFlags[5]) {
                    appendSelection(selection, Contract.Transaction.EVENT_NAME);
                }
                if (mSearchFlags[6]) {
                    appendSelection(selection, Contract.Transaction.PLACE_NAME);
                }
                String[] selectionArgs = getSelectionArguments(query);
                String sortOrder = Contract.Transaction.DATE + " DESC";
                return new CursorLoader(SearchActivity.this, uri, null, selection.toString(), selectionArgs, sortOrder);
            }

            @Override
            public void onLoadFinished(@NonNull Loader loader, Cursor cursor) {
                mCursorAdapter.changeCursor(cursor);
                if (cursor != null && cursor.getCount() > 0) {
                    mAdvancedRecyclerView.setState(AdvancedRecyclerView.State.READY);
                } else {
                    mAdvancedRecyclerView.setState(AdvancedRecyclerView.State.EMPTY);
                }
            }

            @Override
            public void onLoaderReset(@NonNull Loader loader) {
                mCursorAdapter.changeCursor(null);
            }
        });
    }

    @Override
    protected int getActivityTitleRes() {
        return 0;
    }

    @Override
    protected boolean isFloatingActionButtonEnabled() {
        return false;
    }

    @DrawableRes
    protected int getNavigationIcon() {
        return R.drawable.ic_arrow_back_black_24dp; // Use the collapse search view icon instead!
    }

    @MenuRes
    protected int onInflateMenu() {
        return R.menu.menu_search_activity;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.action_filter) {
            showFilterDialog();
        }
        return false;
    }

    private void showFilterDialog() {
        String[] items = new String[] {
                getString(R.string.hint_description),
                getString(R.string.hint_category),
                getString(R.string.hint_date),
                getString(R.string.hint_money),
                getString(R.string.hint_note),
                getString(R.string.hint_event),
                getString(R.string.hint_place)
        };
        ThemedDialog.buildMaterialDialog(this)
                .title(R.string.dialog_filter_search_title)
                .items(items)
                .itemsCallbackMultiChoice(getSelectedIndices(), (dialog, which, text) -> {
                    mSearchFlags = new boolean[]{
                            isChecked(0, which),
                            isChecked(1, which),
                            isChecked(2, which),
                            isChecked(3, which),
                            isChecked(4, which),
                            isChecked(5, which),
                            isChecked(6, which)
                    };
                    mAdvancedRecyclerView.setState(AdvancedRecyclerView.State.LOADING);
                    loadTransactionAsync(mSearchEditText.getText().toString());
                    return true;
                })
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .show();
    }

    private Integer[] getSelectedIndices() {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < mSearchFlags.length; i++) {
            if (mSearchFlags[i]) {
                indices.add(i);
            }
        }
        return indices.toArray(new Integer[0]);
    }

    private boolean isChecked(int index, Integer[] which) {
        for (Integer integer : which) {
            if (integer == index) {
                return true;
            }
        }
        return false;
    }


    private void appendSelection(StringBuilder builder, String column) {
        if (builder.length() != 0) {
            builder.append(" OR ");
        }
        builder.append(column);
        builder.append(" LIKE '%'||?||'%'");
    }

    private String[] getSelectionArguments(String query) {
        List<String> arguments = new ArrayList<>();
        for (boolean flag : mSearchFlags) {
            if (flag) {
                arguments.add(query);
            }
        }
        return arguments.toArray(new String[0]);
    }


    @Override
    public void onHeaderClick(Date startDate, Date endDate) {
        // this method will never be called by the adapter!
    }

    @Override
    public void onTransactionClick(long id) {
        Intent intent = new Intent(this, NewEditTransactionActivity.class);
        intent.putExtra(NewEditItemActivity.MODE, NewEditItemActivity.Mode.EDIT_ITEM);
        intent.putExtra(NewEditItemActivity.ID, id);
        startActivity(intent);
    }

    @Override
    public void onRefresh() {
        mAdvancedRecyclerView.setState(AdvancedRecyclerView.State.REFRESHING);
        loadTransactionAsync(mSearchEditText.getText().toString());
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBooleanArray(SS_SEARCH_FLAGS, mSearchFlags);
    }
}
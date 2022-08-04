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
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.rahul.moneywallet.R;
import com.rahul.moneywallet.storage.database.Contract;
import com.rahul.moneywallet.storage.database.DataContentProvider;
import com.rahul.moneywallet.ui.activity.NewEditItemActivity;
import com.rahul.moneywallet.ui.activity.NewEditPersonActivity;
import com.rahul.moneywallet.ui.adapter.recycler.AbstractCursorAdapter;
import com.rahul.moneywallet.ui.adapter.recycler.PersonCursorAdapter;
import com.rahul.moneywallet.ui.fragment.base.MultiPanelCursorListItemFragment;
import com.rahul.moneywallet.ui.fragment.base.SecondaryPanelFragment;
import com.rahul.moneywallet.ui.fragment.secondary.PersonItemFragment;
import com.rahul.moneywallet.ui.view.AdvancedRecyclerView;

/**
 * Created by andrea on 02/03/18.
 */
public class PersonMultiPanelFragment extends MultiPanelCursorListItemFragment implements PersonCursorAdapter.ActionListener {

    private static final String SECONDARY_FRAGMENT_TAG = "PersonMultiPanelFragment::Tag::SecondaryPanelFragment";

    @Override
    protected void onPrepareRecyclerView(AdvancedRecyclerView recyclerView) {
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setEmptyText(R.string.message_no_person_found);
    }

    @Override
    protected AbstractCursorAdapter onCreateAdapter() {
        return new PersonCursorAdapter(this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Activity activity = getActivity();
        if (activity != null) {
            Uri uri = DataContentProvider.CONTENT_PEOPLE;
            String[] projection = new String[] {
                    Contract.Person.ID,
                    Contract.Person.NAME,
                    Contract.Person.ICON
            };
            String sortOrder = Contract.Person.NAME + " ASC";
            return new CursorLoader(activity, uri, projection, null, null, sortOrder);
        }
        return null;
    }

    @Override
    protected SecondaryPanelFragment onCreateSecondaryPanel() {
        return new PersonItemFragment();
    }

    @Override
    protected String getSecondaryFragmentTag() {
        return SECONDARY_FRAGMENT_TAG;
    }

    @Override
    protected int getTitleRes() {
        return R.string.menu_people;
    }

    @Override
    public void onPersonClick(long id) {
        showItemId(id);
        showSecondaryPanel();
    }

    @Override
    protected void onFloatingActionButtonClick() {
        Intent intent = new Intent(getActivity(), NewEditPersonActivity.class);
        intent.putExtra(NewEditItemActivity.MODE, NewEditItemActivity.Mode.NEW_ITEM);
        startActivity(intent);
    }
}
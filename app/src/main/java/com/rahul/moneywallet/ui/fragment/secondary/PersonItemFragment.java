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
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.rahul.moneywallet.R;
import com.rahul.moneywallet.model.Icon;
import com.rahul.moneywallet.storage.database.Contract;
import com.rahul.moneywallet.storage.database.DataContentProvider;
import com.rahul.moneywallet.ui.activity.NewEditItemActivity;
import com.rahul.moneywallet.ui.activity.NewEditPersonActivity;
import com.rahul.moneywallet.ui.activity.TransactionListActivity;
import com.rahul.moneywallet.ui.fragment.base.SecondaryPanelFragment;
import com.rahul.moneywallet.ui.view.theme.ThemedDialog;
import com.rahul.moneywallet.utils.IconLoader;

/**
 * Created by andrea on 03/04/18.
 */
public class PersonItemFragment extends SecondaryPanelFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int PERSON_LOADER_ID = 53323;

    private View mProgressLayout;
    private View mMainLayout;

    private ImageView mAvatarImageView;
    private TextView mNameTextView;
    private TextView mNoteTextView;

    @Override
    protected void onCreateHeaderView(LayoutInflater inflater, @NonNull ViewGroup parent, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_header_show_icon_name_item, parent, true);
        mAvatarImageView = view.findViewById(R.id.avatar_image_view);
        mNameTextView = view.findViewById(R.id.name_text_view);
    }

    @Override
    protected void onCreateBodyView(LayoutInflater inflater, @NonNull ViewGroup parent, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_panel_show_person_item, parent, true);
        mProgressLayout = view.findViewById(R.id.secondary_panel_progress_wheel);
        mMainLayout = view.findViewById(R.id.secondary_panel_layout);
        mNoteTextView = view.findViewById(R.id.note_text_view);
    }

    @Override
    protected String getTitle() {
        return getString(R.string.title_fragment_item_person);
    }

    @Override
    protected int onInflateMenu() {
        return R.menu.menu_list_edit_delete_item;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_show_transaction_list:
                Intent intent = new Intent(getActivity(), TransactionListActivity.class);
                intent.putExtra(TransactionListActivity.PERSON_ID, getItemId());
                startActivity(intent);
                break;
            case R.id.action_edit_item:
                intent = new Intent(getActivity(), NewEditPersonActivity.class);
                intent.putExtra(NewEditItemActivity.MODE, NewEditItemActivity.Mode.EDIT_ITEM);
                intent.putExtra(NewEditItemActivity.ID, getItemId());
                startActivity(intent);
                break;
            case R.id.action_delete_item:
                showDeleteDialog(getActivity());
                break;
        }
        return false;
    }

    private void showDeleteDialog(Context context) {
        ThemedDialog.buildMaterialDialog(context)
                .title(R.string.title_warning)
                .content(R.string.message_delete_person)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .onPositive(new MaterialDialog.SingleButtonCallback() {

                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        Activity activity = getActivity();
                        if (activity != null) {
                            Uri uri = ContentUris.withAppendedId(DataContentProvider.CONTENT_PEOPLE, getItemId());
                            ContentResolver contentResolver = activity.getContentResolver();
                            contentResolver.delete(uri, null, null);
                            navigateBackSafely();
                            showItemId(0L);
                        }
                    }

                })
                .show();
    }

    @Override
    protected void onShowItemId(long itemId) {
        setLoadingScreen(true);
        getLoaderManager().restartLoader(PERSON_LOADER_ID, null, this);
    }

    private void setLoadingScreen(boolean loading) {
        if (loading) {
            mAvatarImageView.setImageDrawable(null);
            mNameTextView.setText(null);
            mProgressLayout.setVisibility(View.VISIBLE);
            mMainLayout.setVisibility(View.GONE);
        } else {
            mProgressLayout.setVisibility(View.GONE);
            mMainLayout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Activity activity = getActivity();
        if (activity != null) {
            Uri uri = ContentUris.withAppendedId(DataContentProvider.CONTENT_PEOPLE, getItemId());
            String[] projection = new String[] {
                    Contract.Person.NAME,
                    Contract.Person.ICON,
                    Contract.Person.NOTE
            };
            return new CursorLoader(getActivity(), uri, projection, null, null, null);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor != null && cursor.moveToFirst()) {
            Icon icon = IconLoader.parse(cursor.getString(cursor.getColumnIndexOrThrow(Contract.Person.ICON)));
            IconLoader.loadInto(icon, mAvatarImageView);
            mNameTextView.setText(cursor.getString(cursor.getColumnIndexOrThrow(Contract.Person.NAME)));
            String note = cursor.getString(cursor.getColumnIndexOrThrow(Contract.Person.NOTE));
            if (!TextUtils.isEmpty(note)) {
                mNoteTextView.setText(note);
                mNoteTextView.setVisibility(View.VISIBLE);
            } else {
                mNoteTextView.setVisibility(View.INVISIBLE);
            }
        } else {
            showItemId(0L);
        }
        setLoadingScreen(false);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // nothing to release
    }
}
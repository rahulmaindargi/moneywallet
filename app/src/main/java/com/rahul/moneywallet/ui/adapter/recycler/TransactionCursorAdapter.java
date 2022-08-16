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

package com.rahul.moneywallet.ui.adapter.recycler;

import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rahul.moneywallet.R;
import com.rahul.moneywallet.model.CurrencyUnit;
import com.rahul.moneywallet.model.Icon;
import com.rahul.moneywallet.model.Money;
import com.rahul.moneywallet.storage.database.Contract;
import com.rahul.moneywallet.storage.wrapper.TransactionHeaderCursor;
import com.rahul.moneywallet.utils.CurrencyManager;
import com.rahul.moneywallet.utils.DateFormatter;
import com.rahul.moneywallet.utils.DateUtils;
import com.rahul.moneywallet.utils.IconLoader;
import com.rahul.moneywallet.utils.MoneyFormatter;

import java.util.Date;

/**
 * Created by andrea on 03/03/18.
 */
public class TransactionCursorAdapter extends AbstractCursorAdapter<RecyclerView.ViewHolder> {

    private final ActionListener mActionListener;

    private int mIndexType;
    private int mIndexHeaderStartDate;
    private int mIndexHeaderEndDate;
    private int mIndexHeaderMoney;
    private int mIndexHeaderGroupType;
    private int mIndexHeaderExpense;
    private int mIndexCategoryName;
    private int mIndexCategoryIcon;
    private int mIndexTransactionId;
    private int mIndexTransactionDirection;
    private int mIndexTransactionDescription;
    private int mIndexTransactionDate;
    private int mIndexTransactionMoney;
    private int mIndexCurrency;

    private final MoneyFormatter mMoneyFormatter;

    public TransactionCursorAdapter(ActionListener actionListener) {
        super(null, Contract.Transaction.ID);
        mActionListener = actionListener;
        mMoneyFormatter = MoneyFormatter.getInstance();
    }

    @Override
    protected void onLoadColumnIndices(@NonNull Cursor cursor) {
        mIndexType = cursor.getColumnIndexOrThrow(TransactionHeaderCursor.COLUMN_ITEM_TYPE);
        mIndexHeaderStartDate = cursor.getColumnIndexOrThrow(TransactionHeaderCursor.COLUMN_HEADER_START_DATE);
        mIndexHeaderEndDate = cursor.getColumnIndexOrThrow(TransactionHeaderCursor.COLUMN_HEADER_END_DATE);
        mIndexHeaderMoney = cursor.getColumnIndexOrThrow(TransactionHeaderCursor.COLUMN_HEADER_MONEY);
        mIndexHeaderGroupType = cursor.getColumnIndexOrThrow(TransactionHeaderCursor.COLUMN_HEADER_GROUP_TYPE);
        mIndexHeaderExpense = cursor.getColumnIndexOrThrow(TransactionHeaderCursor.COLUMN_HEADER_EXPENSE);
        mIndexCategoryName = cursor.getColumnIndexOrThrow(Contract.Transaction.CATEGORY_NAME);
        mIndexCategoryIcon = cursor.getColumnIndexOrThrow(Contract.Transaction.CATEGORY_ICON);
        mIndexTransactionId = cursor.getColumnIndexOrThrow(Contract.Transaction.ID);
        mIndexTransactionDirection = cursor.getColumnIndexOrThrow(Contract.Transaction.DIRECTION);
        mIndexTransactionDescription = cursor.getColumnIndexOrThrow(Contract.Transaction.DESCRIPTION);
        mIndexTransactionDate = cursor.getColumnIndexOrThrow(Contract.Transaction.DATE);
        mIndexTransactionMoney = cursor.getColumnIndexOrThrow(Contract.Transaction.MONEY);
        mIndexCurrency = cursor.getColumnIndexOrThrow(Contract.Transaction.WALLET_CURRENCY);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, Cursor cursor) {
        if (viewHolder instanceof HeaderViewHolder) {
            onBindHeaderViewHolder((HeaderViewHolder) viewHolder, cursor);
        } else if (viewHolder instanceof TransactionViewHolder) {
            onBindItemViewHolder((TransactionViewHolder) viewHolder, cursor);
        }
    }

    private void onBindItemViewHolder(TransactionViewHolder holder, Cursor cursor) {
        Icon icon = IconLoader.parse(cursor.getString(mIndexCategoryIcon));
        IconLoader.loadInto(icon, holder.mAvatarImageView);
        holder.mPrimaryTextView.setText(cursor.getString(mIndexCategoryName));
        holder.mSecondaryTextView.setText(cursor.getString(mIndexTransactionDescription));
        CurrencyUnit currency = CurrencyManager.getCurrency(cursor.getString(mIndexCurrency));
        long money = cursor.getLong(mIndexTransactionMoney);
        if (cursor.getInt(mIndexTransactionDirection) == Contract.Direction.INCOME) {
            mMoneyFormatter.applyTintedIncome(holder.mMoneyTextView, currency, money);
        } else {
            mMoneyFormatter.applyTintedExpense(holder.mMoneyTextView, currency, money);
        }
        Date date = DateUtils.getDateFromSQLDateTimeString(cursor.getString(mIndexTransactionDate));
        DateFormatter.applyDate(holder.mDateTextView, date);
    }

    private void onBindHeaderViewHolder(HeaderViewHolder holder, Cursor cursor) {
        Date start = DateUtils.getDateFromSQLDateTimeString(cursor.getString(mIndexHeaderStartDate));
        Date end = DateUtils.getDateFromSQLDateTimeString(cursor.getString(mIndexHeaderEndDate));
        DateFormatter.applyDateRange(holder.mLeftTextView, start, end);
        Money money = Money.parse(cursor.getString(mIndexHeaderMoney));
        mMoneyFormatter.applyTinted(holder.mRightTextView, money);
        try {
            Money expense = Money.parse(cursor.getString(mIndexHeaderExpense));
            mMoneyFormatter.applyTinted(holder.mRightTextExpenseView, expense);
        } catch (Exception e) {

            // IF Exception just  log to fix but skip showing it. and show existing total as it is
            Log.e("HeaderMoney", "Cursor missing Header Expense", e);
            holder.mRightTextView.setText("");
            mMoneyFormatter.applyTinted(holder.mRightTextExpenseView, money);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TransactionHeaderCursor.TYPE_HEADER) {
            View itemView = inflater.inflate(R.layout.adapter_header_item, parent, false);
            return new HeaderViewHolder(itemView);
        } else if (viewType == TransactionHeaderCursor.TYPE_ITEM){
            View itemView = inflater.inflate(R.layout.adapter_transaction_item, parent, false);
            return new TransactionViewHolder(itemView);
        } else {
            throw new IllegalArgumentException("Invalid view type: " + viewType);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (mIndexType != -1) {
            return getSafeCursor(position).getInt(mIndexType);
        } else {
            return TransactionHeaderCursor.TYPE_ITEM;
        }
    }

    public class HeaderViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final TextView mLeftTextView;
        private final TextView mRightTextView;
        private final TextView mRightTextExpenseView;

        /*package-local*/ HeaderViewHolder(View itemView) {
            super(itemView);
            mLeftTextView = itemView.findViewById(R.id.left_text_view);
            mRightTextView = itemView.findViewById(R.id.right_text_view);
            mRightTextExpenseView = itemView.findViewById(R.id.right_text_view_expense);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (mActionListener != null) {
                Cursor cursor = getSafeCursor(getBindingAdapterPosition());
                if (cursor != null) {
                    Date start = DateUtils.getDateFromSQLDateTimeString(cursor.getString(mIndexHeaderStartDate));
                    Date end = DateUtils.getDateFromSQLDateTimeString(cursor.getString(mIndexHeaderEndDate));
                    mActionListener.onHeaderClick(start, end);
                }
            }
        }
    }

    /*package-local*/ class TransactionViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final ImageView mAvatarImageView;
        private final TextView mPrimaryTextView;
        private final TextView mMoneyTextView;
        private final TextView mSecondaryTextView;
        private final TextView mDateTextView;

        /*package-local*/ TransactionViewHolder(View itemView) {
            super(itemView);
            mAvatarImageView = itemView.findViewById(R.id.avatar_image_view);
            mPrimaryTextView = itemView.findViewById(R.id.primary_text_view);
            mMoneyTextView = itemView.findViewById(R.id.money_text_view);
            mSecondaryTextView = itemView.findViewById(R.id.secondary_text_view);
            mDateTextView = itemView.findViewById(R.id.date_text_view);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (mActionListener != null) {
                Cursor cursor = getSafeCursor(getBindingAdapterPosition());
                if (cursor != null) {
                    mActionListener.onTransactionClick(cursor.getLong(mIndexTransactionId));
                }
            }
        }
    }

    public interface ActionListener {

        void onHeaderClick(Date startDate, Date endDate);

        void onTransactionClick(long id);
    }
}
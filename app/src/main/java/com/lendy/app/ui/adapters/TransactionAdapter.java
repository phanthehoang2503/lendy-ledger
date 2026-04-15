package com.lendy.app.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.lendy.app.R;
import com.lendy.app.data.entities.TransactionRecord;
import com.lendy.app.utils.FormatUtils;

/******************************************************************************
 * TransactionAdapter - Sử dụng ListAdapter
 * CHỨC NĂNG: Hiển thị lịch sử giao dịch chi tiết.
 *****************************************************************************/
public class TransactionAdapter extends ListAdapter<TransactionRecord, TransactionAdapter.TransactionViewHolder> {

    public interface OnTransactionLongClickListener {
        void onTransactionLongClick(TransactionRecord record);
    }

    private OnTransactionLongClickListener longClickListener;

    public TransactionAdapter() {
        super(new DiffUtil.ItemCallback<TransactionRecord>() {
            @Override
            public boolean areItemsTheSame(@NonNull TransactionRecord oldItem, @NonNull TransactionRecord newItem) {
                return oldItem.id == newItem.id;
            }

            @Override
            public boolean areContentsTheSame(@NonNull TransactionRecord oldItem, @NonNull TransactionRecord newItem) {
                return oldItem.equals(newItem);
            }
        });
    }

    public void setOnTransactionLongClickListener(OnTransactionLongClickListener listener) {
        this.longClickListener = listener;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        TransactionRecord record = getItem(position);
        holder.bind(record);

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onTransactionLongClick(record);
                return true;
            }
            return false;
        });
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        private final View indicator;
        private final TextView textPersonName, textType, textAmount, textBalanceSnapshot, textNote, textDate;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            indicator = itemView.findViewById(R.id.indicator);
            textPersonName = itemView.findViewById(R.id.textPersonName);
            textType = itemView.findViewById(R.id.textType);
            textAmount = itemView.findViewById(R.id.textAmount);
            textBalanceSnapshot = itemView.findViewById(R.id.textBalanceSnapshot);
            textNote = itemView.findViewById(R.id.textNote);
            textDate = itemView.findViewById(R.id.textDate);
        }

        public void bind(TransactionRecord record) {
            textPersonName.setText(record.personNameSnapshot != null ? record.personNameSnapshot
                    : itemView.getContext().getString(R.string.anonymous_person));
            textAmount.setText(FormatUtils.formatCurrency(record.amount));

            String formattedBalance = FormatUtils
                    .formatCurrencyAbs(record.balanceSnapshot != null ? record.balanceSnapshot : 0);
            textBalanceSnapshot
                    .setText(itemView.getContext().getString(R.string.remaining_debt_format, formattedBalance));

            textNote.setText(record.note != null && !record.note.isEmpty() ? record.note
                    : itemView.getContext().getString(R.string.no_note));
            textDate.setText(FormatUtils.formatDateTime(record.timestamp));

            int colorRes = R.color.outline;
            String typeText = itemView.getContext().getString(R.string.transaction_type_default);

            if (record.type != null) {
                switch (record.type) {
                    case LEND:
                        colorRes = R.color.receivable;
                        typeText = itemView.getContext().getString(R.string.transaction_type_lend);
                        break;
                    case REPAY:
                        colorRes = R.color.payable;
                        typeText = itemView.getContext().getString(R.string.transaction_type_repay);
                        break;
                    case BORROW:
                        colorRes = R.color.payable;
                        typeText = itemView.getContext().getString(R.string.transaction_type_borrow);
                        break;
                    case PAY_BACK:
                        colorRes = R.color.receivable;
                        typeText = itemView.getContext().getString(R.string.transaction_type_pay_back);
                        break;
                }
            }

            int color = ContextCompat.getColor(itemView.getContext(), colorRes);
            indicator.setBackgroundColor(color);
            textType.setText(typeText);
            textAmount.setTextColor(color);
        }
    }
}

package com.lendy.app.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.lendy.app.R;
import com.lendy.app.data.entities.TransactionRecord;
import com.lendy.app.utils.FormatUtils;

import java.util.ArrayList;
import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    public interface OnTransactionLongClickListener {
        void onTransactionLongClick(TransactionRecord record);
    }

    private List<TransactionRecord> transactions = new ArrayList<>();
    private OnTransactionLongClickListener longClickListener;
    private boolean useClassicColors = false;

    public void setOnTransactionLongClickListener(OnTransactionLongClickListener listener) {
        this.longClickListener = listener;
    }

    public TransactionRecord getTransactionAt(int position) {
        return transactions.get(position);
    }

    public void setTransactions(List<TransactionRecord> transactions) {
        this.transactions = transactions;
        notifyDataSetChanged();
    }

    public void setUseClassicColors(boolean useClassicColors) {
        this.useClassicColors = useClassicColors;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        TransactionRecord record = transactions.get(position);
        holder.bind(record, useClassicColors);

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onTransactionLongClick(record);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        private final View indicator;
        private final TextView textPersonName;
        private final TextView textType;
        private final TextView textAmount;
        private final TextView textBalanceSnapshot;
        private final TextView textNote;
        private final TextView textDate;

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

        public void bind(TransactionRecord record, boolean useClassicColors) {
            // Hiển thị tên (snapshot)
            textPersonName.setText(record.personNameSnapshot != null ? record.personNameSnapshot : itemView.getContext().getString(R.string.anonymous_person));

            // Hiển thị số tiền
            textAmount.setText(FormatUtils.formatCurrency(record.amount));

            // Hiển thị số nợ sau giao dịch
            String formattedBalance = FormatUtils.formatCurrencyAbs(record.balanceSnapshot != null ? record.balanceSnapshot : 0);
            textBalanceSnapshot.setText(itemView.getContext().getString(R.string.remaining_debt_format, formattedBalance));

            // Hiển thị ghi chú và ngày tháng
            textNote.setText(record.note != null && !record.note.isEmpty() ? record.note : itemView.getContext().getString(R.string.no_note));
            textDate.setText(FormatUtils.formatDateTime(record.timestamp));

            // Định dạng màu sắc và tag dựa trên loại giao dịch
            int color;
            String typeText;

            if (record.type == null) {
                color = ContextCompat.getColor(itemView.getContext(), R.color.outline);
                typeText = itemView.getContext().getString(R.string.transaction_type_default);
            } else {
                switch (record.type) {
                    case LEND:
                        color = ContextCompat.getColor(itemView.getContext(), useClassicColors ? R.color.classic_receivable : R.color.receivable);
                        typeText = itemView.getContext().getString(R.string.transaction_type_lend);
                        break;
                    case REPAY:
                        color = ContextCompat.getColor(itemView.getContext(), useClassicColors ? R.color.classic_payable : R.color.payable);
                        typeText = itemView.getContext().getString(R.string.transaction_type_repay);
                        break;
                    case BORROW:
                        color = ContextCompat.getColor(itemView.getContext(), useClassicColors ? R.color.classic_payable : R.color.payable);
                        typeText = itemView.getContext().getString(R.string.transaction_type_borrow);
                        break;
                    case PAY_BACK:
                        color = ContextCompat.getColor(itemView.getContext(), useClassicColors ? R.color.classic_receivable : R.color.receivable);
                        typeText = itemView.getContext().getString(R.string.transaction_type_pay_back);
                        break;
                    default:
                        color = ContextCompat.getColor(itemView.getContext(), R.color.outline);
                        typeText = itemView.getContext().getString(R.string.transaction_type_default);
                }
            }

            indicator.setBackgroundColor(color);
            textType.setText(typeText);
            textAmount.setTextColor(color);
        }
    }
}

package com.lendy.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.lendy.app.data.entities.TransactionRecord;
import com.lendy.app.data.TransactionType;
import com.lendy.app.utils.FormatUtils;
import java.util.ArrayList;
import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private List<TransactionRecord> transactions = new ArrayList<>();
    private OnTransactionLongClickListener longClickListener;

    public interface OnTransactionLongClickListener {
        void onTransactionLongClick(TransactionRecord record);
    }

    public void setOnTransactionLongClickListener(OnTransactionLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setTransactions(List<TransactionRecord> transactions) {
        this.transactions = transactions;
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
        holder.bind(record, longClickListener);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        private final TextView textTransType, textTransDate, textTransAmount, textTransNote;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            textTransType = itemView.findViewById(R.id.textTransType);
            textTransDate = itemView.findViewById(R.id.textTransDate);
            textTransAmount = itemView.findViewById(R.id.textTransAmount);
            textTransNote = itemView.findViewById(R.id.textTransNote);
        }

        public void bind(TransactionRecord record, OnTransactionLongClickListener longClickListener) {
            String typeStr = "";
            int color;

            if (record.type == TransactionType.LEND) {
                typeStr = "Cho vay thêm";
                color = ContextCompat.getColor(itemView.getContext(), R.color.receivable);
            } else if (record.type == TransactionType.REPAY) {
                typeStr = "Được trả nợ";
                color = ContextCompat.getColor(itemView.getContext(), R.color.receivable);
            } else if (record.type == TransactionType.BORROW) {
                typeStr = "Đi vay thêm";
                color = ContextCompat.getColor(itemView.getContext(), R.color.payable);
            } else if (record.type == TransactionType.PAY_BACK) {
                typeStr = "Trả nợ họ";
                color = ContextCompat.getColor(itemView.getContext(), R.color.payable);
            } else {
                color = ContextCompat.getColor(itemView.getContext(), R.color.outline);
            }

            textTransType.setText(typeStr);
            textTransType.setTextColor(color);

            // CLEAN CODE: Dùng FormatUtils
            textTransDate.setText(FormatUtils.formatDateTime(record.timestamp));
            textTransAmount.setText(FormatUtils.formatCurrency(record.amount));
            textTransAmount.setTextColor(color);

            if (record.note != null && !record.note.isEmpty()) {
                textTransNote.setVisibility(View.VISIBLE);
                textTransNote.setText(record.note);
            } else {
                textTransNote.setVisibility(View.GONE);
            }

            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onTransactionLongClick(record);
                    return true;
                }
                return false;
            });
        }
    }
}

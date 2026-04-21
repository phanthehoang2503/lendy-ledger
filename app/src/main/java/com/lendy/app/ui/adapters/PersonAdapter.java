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

import com.google.android.material.card.MaterialCardView;
import com.lendy.app.R;
import com.lendy.app.data.entities.Person;
import com.lendy.app.utils.FormatUtils;

import java.util.Locale;

/******************************************************************************
 * PersonAdapter - Sử dụng ListAdapter
 * CHỨC NĂNG: Hiển thị danh sách khách hàng và số dư nợ.
 *****************************************************************************/
public class PersonAdapter extends ListAdapter<Person, PersonAdapter.PersonViewHolder> {

    private final OnPersonClickListener listener;
    private final OnPersonLongClickListener longListener;

    public interface OnPersonClickListener {
        void onPersonClick(Person person);
    }

    public interface OnPersonLongClickListener {
        void onPersonLongClick(Person person);
    }

    public PersonAdapter(OnPersonClickListener listener, OnPersonLongClickListener longListener) {
        super(new DiffUtil.ItemCallback<Person>() {
            @Override
            public boolean areItemsTheSame(@NonNull Person oldItem, @NonNull Person newItem) {
                return oldItem.id == newItem.id;
            }

            @Override
            public boolean areContentsTheSame(@NonNull Person oldItem, @NonNull Person newItem) {
                return oldItem.equals(newItem);
            }
        });
        this.listener = listener;
        this.longListener = longListener;
    }

    @NonNull
    @Override
    public PersonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_person, parent, false);
        return new PersonViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PersonViewHolder holder, int position) {
        holder.bind(getItem(position), listener, longListener);
    }

    public static class PersonViewHolder extends RecyclerView.ViewHolder {
        private final TextView textInitial, textName, textPhone, textBalance;
        private final MaterialCardView avatarContainer;

        private final int[] avatarColorResIds = {
                R.color.avatar_color_1, R.color.avatar_color_2, R.color.avatar_color_3,
                R.color.avatar_color_4, R.color.avatar_color_5, R.color.avatar_color_6,
                R.color.avatar_color_7, R.color.avatar_color_8, R.color.avatar_color_9,
                R.color.avatar_color_10
        };

        public PersonViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarContainer = itemView.findViewById(R.id.avatarContainer);
            textInitial = itemView.findViewById(R.id.textInitial);
            textName = itemView.findViewById(R.id.textName);
            textPhone = itemView.findViewById(R.id.textPhone);
            textBalance = itemView.findViewById(R.id.textBalance);
        }

        public void bind(final Person person, final OnPersonClickListener listener,
                final OnPersonLongClickListener longListener) {
            textName.setText(person.name);
            if (person.phoneNumber != null && !person.phoneNumber.isEmpty()) {
                textPhone.setText(person.phoneNumber);
            } else {
                textPhone.setText(itemView.getContext().getString(R.string.no_phone_yet));
            }

            if (person.name != null && !person.name.isEmpty()) {
                textInitial.setText(person.name.substring(0, 1).toUpperCase(Locale.ROOT));
                int colorIndex = (person.name.hashCode() & 0x7FFFFFFF) % avatarColorResIds.length;
                avatarContainer.setCardBackgroundColor(
                        ContextCompat.getColor(itemView.getContext(), avatarColorResIds[colorIndex]));
            }

            String formattedBalance = FormatUtils.formatCurrencyAbs(person.totalBalance);

            if (person.totalBalance > 0) {
                textBalance.setText(itemView.getContext().getString(R.string.receivable_format, formattedBalance));
                textBalance.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.receivable));
            } else if (person.totalBalance < 0) {
                textBalance.setText(itemView.getContext().getString(R.string.payable_format, formattedBalance));
                textBalance.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.payable));
            } else {
                textBalance.setText(itemView.getContext().getString(R.string.settled_balance));
                textBalance.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.outline));
            }

            itemView.setOnClickListener(v -> {
                if (listener != null)
                    listener.onPersonClick(person);
            });
            itemView.setOnLongClickListener(v -> {
                if (longListener != null) {
                    longListener.onPersonLongClick(person);
                    return true;
                }
                return false;
            });
        }
    }
}
package com.lendy.app.ui.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.lendy.app.R;
import com.lendy.app.data.entities.Person;
import com.lendy.app.utils.FormatUtils;
import java.util.ArrayList;
import java.util.List;

public class PersonAdapter extends RecyclerView.Adapter<PersonAdapter.PersonViewHolder> {

    private List<Person> people = new ArrayList<>();
    private List<Person> peopleFull = new ArrayList<>();
    private boolean useUnifiedColor = false;
    private boolean useClassicColors = false;
    private final OnPersonClickListener listener;
    private final OnPersonLongClickListener longListener;

    private final int[] avatarColors = {
            Color.parseColor("#EF5350"), Color.parseColor("#EC407A"),
            Color.parseColor("#AB47BC"), Color.parseColor("#7E57C2"),
            Color.parseColor("#5C6BC0"), Color.parseColor("#42A5F5"),
            Color.parseColor("#26A69A"), Color.parseColor("#66BB6A"),
            Color.parseColor("#FFA726"), Color.parseColor("#8D6E63")
    };

    public interface OnPersonClickListener {
        void onPersonClick(Person person);
    }

    public interface OnPersonLongClickListener {
        void onPersonLongClick(Person person);
    }

    public PersonAdapter(OnPersonClickListener listener, OnPersonLongClickListener longListener) {
        this.listener = listener;
        this.longListener = longListener;
    }

    public void setPeople(List<Person> people) {
        this.peopleFull = new ArrayList<>(people);
        this.people = new ArrayList<>(people);
        notifyDataSetChanged();
    }

    public void setUseUnifiedColor(boolean useUnifiedColor) {
        this.useUnifiedColor = useUnifiedColor;
        notifyDataSetChanged();
    }

    public void setUseClassicColors(boolean useClassicColors) {
        this.useClassicColors = useClassicColors;
        notifyDataSetChanged();
    }

    public void filter(String k) {
        people.clear();
        if (k.isEmpty()) {
            people.addAll(peopleFull);
        } else {
            String filterPattern = k.toLowerCase().trim();
            for (Person item : peopleFull) {
                if (item.name.toLowerCase().contains(filterPattern) ||
                        (item.phoneNumber != null && item.phoneNumber.contains(filterPattern))) {
                    people.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    public Person getPersonAt(int position) {
        if (position >= 0 && position < people.size()) {
            return people.get(position);
        }
        return null;
    }

    @NonNull
    @Override
    public PersonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_person, parent, false);
        return new PersonViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PersonViewHolder holder, int position) {
        Person person = people.get(position);
        holder.bind(person, listener, longListener, avatarColors, useUnifiedColor, useClassicColors);
    }

    @Override
    public int getItemCount() {
        return people.size();
    }

    public static class PersonViewHolder extends RecyclerView.ViewHolder {
        private final TextView textInitial, textName, textPhone, textBalance;
        private final MaterialCardView avatarContainer;

        public PersonViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarContainer = itemView.findViewById(R.id.avatarContainer);
            textInitial = itemView.findViewById(R.id.textInitial);
            textName = itemView.findViewById(R.id.textName);
            textPhone = itemView.findViewById(R.id.textPhone);
            textBalance = itemView.findViewById(R.id.textBalance);
        }

        public void bind(final Person person, final OnPersonClickListener listener,
                final OnPersonLongClickListener longListener, int[] colors, boolean useUnifiedColor, boolean useClassicColors) {
            textName.setText(person.name);
            if (person.phoneNumber != null && !person.phoneNumber.isEmpty()) {
                textPhone.setVisibility(View.VISIBLE);
                textPhone.setText(person.phoneNumber);
            } else {
                textPhone.setVisibility(View.VISIBLE);
                textPhone.setText("Chưa có SĐT");
            }

            if (person.name != null && !person.name.isEmpty()) {
                textInitial.setText(person.name.substring(0, 1).toUpperCase());
                int colorIndex = (person.name.hashCode() & 0x7FFFFFFF) % colors.length;
                avatarContainer.setCardBackgroundColor(colors[colorIndex]);
            }

            String formattedBalance = FormatUtils.formatCurrencyAbs(person.totalBalance);

            if (person.totalBalance > 0) {
                // Họ nợ mình
                textBalance.setText("Nợ bạn: " + formattedBalance);
                int colorRes = useClassicColors ? R.color.classic_receivable : R.color.receivable;
                textBalance.setTextColor(ContextCompat.getColor(itemView.getContext(), colorRes));
            } else if (person.totalBalance < 0) {
                // Mình nợ họ
                textBalance.setText("Bạn nợ: " + formattedBalance);
                int colorRes = useClassicColors ? R.color.classic_payable : R.color.payable;
                textBalance.setTextColor(ContextCompat.getColor(itemView.getContext(), colorRes));
            } else {
                // Bằng 0
                textBalance.setText("Đã hết nợ");
                textBalance.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.outline));
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPersonClick(person);
                }
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

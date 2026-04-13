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
        holder.bind(person, listener, longListener, avatarColors);
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
                         final OnPersonLongClickListener longListener, int[] colors) {
            textName.setText(person.name);
            textPhone.setText(person.phoneNumber != null && !person.phoneNumber.isEmpty() ? person.phoneNumber : "Chưa có SĐT");
            
            if (person.name != null && !person.name.isEmpty()) {
                textInitial.setText(person.name.substring(0, 1).toUpperCase());
                int colorIndex = (person.name.hashCode() & 0x7FFFFFFF) % colors.length;
                avatarContainer.setCardBackgroundColor(colors[colorIndex]);
            }

            // CLEAN CODE: Dùng FormatUtils
            String formattedBalance = FormatUtils.formatCurrencyAbs(person.totalBalance);

            if (person.totalBalance > 0) {
                textBalance.setText("+" + formattedBalance);
                textBalance.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.receivable));
            } else if (person.totalBalance < 0) {
                textBalance.setText("-" + formattedBalance);
                textBalance.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.payable));
            } else {
                textBalance.setText(formattedBalance);
                textBalance.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.outline));
            }

            itemView.setOnClickListener(v -> listener.onPersonClick(person));
            itemView.setOnLongClickListener(v -> {
                longListener.onPersonLongClick(person);
                return true;
            });
        }
    }
}

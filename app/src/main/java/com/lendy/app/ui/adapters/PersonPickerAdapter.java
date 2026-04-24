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

import com.lendy.app.databinding.ItemPersonPickerBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PersonPickerAdapter extends ListAdapter<Person, PersonPickerAdapter.ViewHolder> {

    public interface OnPersonSelectedListener {
        void onSelected(Person person);
    }

    private final OnPersonSelectedListener listener;
    private List<Person> fullList = new ArrayList<>();

    public PersonPickerAdapter(OnPersonSelectedListener listener) {
        super(new DiffUtil.ItemCallback<Person>() {
            @Override
            public boolean areItemsTheSame(@NonNull Person old, @NonNull Person next) {
                return old.id == next.id;
            }
            @Override
            public boolean areContentsTheSame(@NonNull Person old, @NonNull Person next) {
                return old.equals(next);
            }
        });
        this.listener = listener;
    }

    public void setFullList(List<Person> list) {
        this.fullList = list;
        submitList(list);
    }

    // Lọc danh sách theo text người dùng gõ
    public void filter(String query) {
        if (query.isEmpty()) {
            submitList(fullList);
        } else {
            List<Person> filtered = new ArrayList<>();
            String lower = query.toLowerCase(Locale.ROOT).trim();
            for (Person p : fullList) {
                if ((p.name != null && p.name.toLowerCase(Locale.ROOT).contains(lower)) ||
                        (p.phoneNumber != null && p.phoneNumber.contains(lower))) {
                    filtered.add(p);
                }
            }
            submitList(filtered);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPersonPickerBinding binding = ItemPersonPickerBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Person p = getItem(position);
        holder.binding.textName.setText(p.name);
        holder.binding.textPhone.setText(p.phoneNumber != null ? p.phoneNumber : "");

        // Setup Avatar
        if (p.name != null && !p.name.isEmpty()) {
            holder.binding.textInitial.setText(p.name.substring(0, 1).toUpperCase(Locale.ROOT));
            int[] colors = {
                    R.color.avatar_color_1,
                    R.color.avatar_color_2,
                    R.color.avatar_color_3,
                    R.color.avatar_color_4,
                    R.color.avatar_color_5,
                    R.color.avatar_color_6
            };
            int colorIdx = (p.name.hashCode() & 0x7FFFFFFF) % colors.length;
            holder.binding.avatarContainer.setCardBackgroundColor(
                    ContextCompat.getColor(holder.itemView.getContext(), colors[colorIdx]));
        }
        holder.itemView.setOnClickListener(v -> listener.onSelected(p));
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemPersonPickerBinding binding;
        ViewHolder(ItemPersonPickerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

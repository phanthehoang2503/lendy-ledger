package com.lendy.app.ui.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.lendy.app.ui.fragments.ContactsFragment;
import com.lendy.app.ui.fragments.HistoryFragment;
import com.lendy.app.ui.fragments.HomeFragment;
import com.lendy.app.ui.fragments.StatsFragment;

public class MainPagerAdapter extends FragmentStateAdapter {

    private static final int PAGE_COUNT = 4;

    public MainPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new HomeFragment();
            case 1: return new StatsFragment();
            case 2: return new HistoryFragment();
            case 3: return new ContactsFragment();
            default: throw new IllegalArgumentException("Invalid position: " + position);
        }
    }

    @Override
    public int getItemCount() {
        return PAGE_COUNT;
    }
}

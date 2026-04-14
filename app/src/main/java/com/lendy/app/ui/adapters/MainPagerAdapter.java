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
            default: return new HomeFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}

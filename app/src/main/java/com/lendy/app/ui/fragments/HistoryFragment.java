package com.lendy.app.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lendy.app.R;
import com.lendy.app.repository.LendyRepository;
import com.lendy.app.ui.activities.PersonDetailActivity;
import com.lendy.app.ui.adapters.TransactionAdapter;
import com.lendy.app.viewmodel.LendyViewModel;
import com.lendy.app.viewmodel.LendyViewModelFactory;

import java.util.ArrayList;

public class HistoryFragment extends Fragment {

    private LendyViewModel viewModel;
    private TransactionAdapter adapter;
    private View emptyView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        emptyView = view.findViewById(R.id.layoutEmptyHistory);
        setupRecyclerView(view);
        
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViewModel();
    }

    private void setupRecyclerView(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        adapter = new TransactionAdapter();
        adapter.setUseClassicColors(true);
        recyclerView.setAdapter(adapter);
    }

    private void setupViewModel() {
        if (getActivity() == null) return;

        viewModel = new ViewModelProvider(requireActivity()).get(LendyViewModel.class);
        viewModel.getAllTransactions().observe(getViewLifecycleOwner(), transactions -> {
            if (transactions == null || transactions.isEmpty()) {
                emptyView.setVisibility(View.VISIBLE);
            } else {
                emptyView.setVisibility(View.GONE);
            }
            adapter.setTransactions(transactions != null ? transactions : new ArrayList<>());
        });
    }
}

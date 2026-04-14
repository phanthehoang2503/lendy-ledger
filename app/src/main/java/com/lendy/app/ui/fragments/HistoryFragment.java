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
import com.lendy.app.ui.adapters.PersonAdapter;
import com.lendy.app.viewmodel.LendyViewModel;
import com.lendy.app.viewmodel.LendyViewModelFactory;

import java.util.ArrayList;

public class HistoryFragment extends Fragment {

    private LendyViewModel viewModel;
    private PersonAdapter adapter;
    private View emptyView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        emptyView = view.findViewById(R.id.layoutEmptyHistory);
        setupRecyclerView(view);
        setupViewModel();

        return view;
    }

    private void setupRecyclerView(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        adapter = new PersonAdapter(person -> {
            Intent intent = new Intent(getActivity(), PersonDetailActivity.class);
            intent.putExtra(PersonDetailActivity.EXTRA_PERSON_ID, person.id);
            intent.putExtra(PersonDetailActivity.EXTRA_PERSON_NAME, person.name);
            intent.putExtra(PersonDetailActivity.EXTRA_PERSON_PHONE, person.phoneNumber);
            startActivity(intent);
        }, person -> {
            // Long click: Placeholder
        });
        
        recyclerView.setAdapter(adapter);
    }

    private void setupViewModel() {
        if (getActivity() == null) return;
        
        LendyRepository repository = new LendyRepository(getActivity().getApplication());
        viewModel = new ViewModelProvider(requireActivity(), new LendyViewModelFactory(repository)).get(LendyViewModel.class);

        viewModel.getCompletedDebts().observe(getViewLifecycleOwner(), people -> {
            if (people == null || people.isEmpty()) {
                emptyView.setVisibility(View.VISIBLE);
            } else {
                emptyView.setVisibility(View.GONE);
            }
            adapter.setPeople(people != null ? people : new ArrayList<>());
        });
    }
}

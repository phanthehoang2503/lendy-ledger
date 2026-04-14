package com.lendy.app.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class HomeFragment extends Fragment {

    private LendyViewModel viewModel;
    private PersonAdapter adapter;
    private View emptyView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        emptyView = view.findViewById(R.id.layoutEmptyHome);

        // Connect Quick Add Bar
        View btnQuickAdd = view.findViewById(R.id.cardQuickAdd);
        if (btnQuickAdd != null) {
            btnQuickAdd.setOnClickListener(v -> {
                if (getActivity() instanceof com.lendy.app.ui.activities.MainActivity) {
                    ((com.lendy.app.ui.activities.MainActivity) getActivity()).showAddPersonDialog();
                }
            });
        }

        setupRecyclerView(view);
        setupViewModel();

        return view;
    }

    private void setupRecyclerView(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewHome);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new PersonAdapter(person -> {
            // Click: Mở chi tiết
            Intent intent = new Intent(getActivity(), PersonDetailActivity.class);
            intent.putExtra(PersonDetailActivity.EXTRA_PERSON_ID, person.id);
            intent.putExtra(PersonDetailActivity.EXTRA_PERSON_NAME, person.name);
            intent.putExtra(PersonDetailActivity.EXTRA_PERSON_PHONE, person.phoneNumber);
            startActivity(intent);
        }, person -> {
            // TODO: implement long-click actions such as edit/delete
        });

        recyclerView.setAdapter(adapter);
    }

    private void setupViewModel() {
        if (getActivity() == null)
            return;

        LendyRepository repository = new LendyRepository(getActivity().getApplication());
        viewModel = new ViewModelProvider(requireActivity(), new LendyViewModelFactory(repository))
                .get(LendyViewModel.class);

        viewModel.getActiveDebts().observe(getViewLifecycleOwner(), people -> {
            if (people == null || people.isEmpty()) {
                emptyView.setVisibility(View.VISIBLE);
            } else {
                emptyView.setVisibility(View.GONE);
            }
            adapter.setPeople(people != null ? people : new ArrayList<>());
        });
    }
}

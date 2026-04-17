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

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.lendy.app.R;
import com.lendy.app.repository.LendyRepository;
import com.lendy.app.ui.activities.PersonDetailActivity;
import com.lendy.app.data.entities.Person;
import com.lendy.app.ui.adapters.PersonAdapter;
import com.lendy.app.utils.PersonDialogHelper;
import com.lendy.app.viewmodel.LendyViewModel;
import com.lendy.app.viewmodel.LendyViewModelFactory;

public class HomeFragment extends Fragment {

    public interface AddDebtFlowHost {
        void showAddDebtFlow();
    }

    private LendyViewModel viewModel;
    private PersonAdapter adapter;
    private View emptyView;
    private AddDebtFlowHost dialogHost;

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
                if (dialogHost != null) {
                    dialogHost.showAddDebtFlow();
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
        }, this::showPersonOptionsDialog);

        recyclerView.setAdapter(adapter);
    }

    private void setupViewModel() {
        if (getActivity() == null)
            return;

        viewModel = new ViewModelProvider(
                requireActivity(),
                new LendyViewModelFactory(
                        LendyRepository.getInstance(requireActivity().getApplication())))
                .get(LendyViewModel.class);

        viewModel.getActiveDebts().observe(getViewLifecycleOwner(), people -> {
            if (people == null || people.isEmpty()) {
                emptyView.setVisibility(View.VISIBLE);
            } else {
                emptyView.setVisibility(View.GONE);
            }
            adapter.submitList(people);
        });
    }
    private void showPersonOptionsDialog(Person person) {
        String[] options = {"Chỉnh sửa thông tin", "Xóa người này"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(person.name)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        PersonDialogHelper.showEditPersonDialog(requireContext(), viewModel, person);
                    } else {
                        showDeleteConfirmation(person);
                    }
                })
                .show();
    }

    private void showDeleteConfirmation(Person person) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa '" + person.name + "'? Toàn bộ lịch sử giao dịch sẽ bị mất.")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    viewModel.removePerson(person);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onAttach(@NonNull android.content.Context context) {
        super.onAttach(context);
        if (context instanceof AddDebtFlowHost) {
            dialogHost = (AddDebtFlowHost) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        dialogHost = null;
    }
}

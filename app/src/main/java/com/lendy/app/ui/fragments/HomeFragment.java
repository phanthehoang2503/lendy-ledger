package com.lendy.app.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import com.lendy.app.R;
import com.lendy.app.repository.LendyRepository;
import com.lendy.app.ui.activities.PersonDetailActivity;
import com.lendy.app.data.entities.Person;
import com.lendy.app.ui.adapters.PersonAdapter;
import com.lendy.app.viewmodel.LendyViewModel;
import com.lendy.app.viewmodel.LendyViewModelFactory;

import java.util.ArrayList;

public class HomeFragment extends Fragment {

    public interface AddPersonDialogHost {
        void showAddPersonDialog();
    }

    private LendyViewModel viewModel;
    private PersonAdapter adapter;
    private View emptyView;
    private AddPersonDialogHost dialogHost;

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
                    dialogHost.showAddPersonDialog();
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
    private void showPersonOptionsDialog(Person person) {
        String[] options = {"Chỉnh sửa thông tin", "Xóa người này"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(person.name)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showEditPersonDialog(person);
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

    private void showEditPersonDialog(Person person) {
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_person, null);
        v.findViewById(R.id.layoutAmount).setVisibility(View.GONE);
        v.findViewById(R.id.toggleGroup).setVisibility(View.GONE);
        v.findViewById(R.id.layoutNote).setVisibility(View.GONE);
        v.findViewById(R.id.scrollChips).setVisibility(View.GONE);

        TextInputEditText editName = v.findViewById(R.id.editName);
        TextInputEditText editPhone = v.findViewById(R.id.editPhone);

        editName.setText(person.name);
        editPhone.setText(person.phoneNumber);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.edit_person)
                .setView(v)
                .setPositiveButton("Lưu", null)
                .setNegativeButton("Hủy", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            android.widget.Button button = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String name = editName.getText().toString().trim();
                String phone = editPhone.getText().toString().trim();

                if (name.isEmpty()) {
                    editName.setError("Vui lòng nhập tên người nợ");
                    return;
                }

                person.name = name;
                person.phoneNumber = phone;
                person.updatedAt = System.currentTimeMillis();

                viewModel.updatePerson(person);
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    @Override
    public void onAttach(@NonNull android.content.Context context) {
        super.onAttach(context);
        if (context instanceof AddPersonDialogHost) {
            dialogHost = (AddPersonDialogHost) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        dialogHost = null;
    }
}

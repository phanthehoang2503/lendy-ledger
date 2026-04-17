package com.lendy.app.ui.fragments;

import android.content.Intent;
import android.content.Context;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.lendy.app.R;
import com.lendy.app.data.entities.Person;
import com.lendy.app.repository.LendyRepository;
import com.lendy.app.ui.activities.PersonDetailActivity;
import com.lendy.app.ui.adapters.PersonAdapter;
import com.lendy.app.viewmodel.LendyViewModel;
import com.lendy.app.viewmodel.LendyViewModelFactory;

import java.util.ArrayList;
import java.util.Objects;

public class ContactsFragment extends Fragment {

    private LendyViewModel viewModel;
    private PersonAdapter adapter;
    private View emptyView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contacts, container, false);
        emptyView = view.findViewById(R.id.layoutEmptyContacts);

        // nút bên màn hình danh bạ
        FloatingActionButton fab = view.findViewById(R.id.fabAddContact);
        if (fab != null) {
            fab.setOnClickListener(v -> showAddContactDialog());
        }

        setupRecyclerView(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViewModel();
    }

    private void showAddContactDialog() {
        // 1. Inflate layout
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_person, null);

        // 2. Ẩn các trường không cần thiết
        v.findViewById(R.id.layoutAmount).setVisibility(View.GONE);
        v.findViewById(R.id.toggleGroup).setVisibility(View.GONE);
        v.findViewById(R.id.layoutNote).setVisibility(View.GONE);
        v.findViewById(R.id.scrollChips).setVisibility(View.GONE);

        TextInputEditText editName = v.findViewById(R.id.editName);
        TextInputEditText editPhone = v.findViewById(R.id.editPhone);

        // 3. Tạo Dialog
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.add_new_contact_title) // Dùng string mới tạo ở Phase 1
                .setView(v)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            android.widget.Button button = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String name = Objects.requireNonNull(editName.getText()).toString().trim();
                String phone = Objects.requireNonNull(editPhone.getText()).toString().trim();

                if (name.isEmpty()) {
                    editName.setError("Vui lòng nhập tên");
                    return;
                }

                button.setEnabled(false);
                Person person = new Person();
                person.name = name;
                person.phoneNumber = phone;
                person.updatedAt = System.currentTimeMillis();

                viewModel.addOrUpdatePersonTransactional(person, new LendyRepository.PersonUpsertCallback() {
                    @Override
                    public void onSuccess() {
                        dialog.dismiss();
                    }

                    @Override
                    public void onDuplicate() {
                        editName.setError("Người này đã có trong danh bạ");
                        button.setEnabled(true);
                    }

                    @Override
                    public void onError(Exception exception) {
                        if (!isAdded()) {
                            return;
                        }
                        button.setEnabled(true);
                        Context context = getContext();
                        if (context != null) {
                            android.widget.Toast
                                    .makeText(context, "Không thể lưu danh bạ", android.widget.Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }
                });
            });
        });
        dialog.show();
    }

    private void setupRecyclerView(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewContacts);
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

        viewModel.getAllPeople().observe(getViewLifecycleOwner(), people -> {
            if (people == null || people.isEmpty()) {
                emptyView.setVisibility(View.VISIBLE);
            } else {
                emptyView.setVisibility(View.GONE);
            }
            adapter.submitList(people);
        });
    }

    private void showPersonOptionsDialog(Person person) {
        String[] options = { "Chỉnh sửa thông tin", "Xóa người này" };
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
                String name = Objects.requireNonNull(editName.getText()).toString().trim();
                String phone = editPhone.getText().toString().trim();

                if (name.isEmpty()) {
                    editName.setError("Vui lòng nhập tên người nợ");
                    return;
                }

                button.setEnabled(false);
                person.name = name;
                person.phoneNumber = phone;
                person.updatedAt = System.currentTimeMillis();

                viewModel.addOrUpdatePersonTransactional(person,
                        new LendyRepository.PersonUpsertCallback() {
                            @Override
                            public void onSuccess() {
                                dialog.dismiss();
                            }

                            @Override
                            public void onDuplicate() {
                                editName.setError("Tên đã tồn tại");
                                button.setEnabled(true);
                            }

                            @Override
                            public void onError(Exception exception) {
                                if (!isAdded()) {
                                    return;
                                }
                                button.setEnabled(true);
                                Context context = getContext();
                                if (context != null) {
                                    android.widget.Toast.makeText(context, "Không thể lưu danh bạ",
                                            android.widget.Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            });
        });

        dialog.show();
    }
}

package com.lendy.app.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.lendy.app.R;
import com.lendy.app.data.entities.Person;
import com.lendy.app.databinding.DialogAddPersonBinding;
import com.lendy.app.databinding.FragmentContactsBinding;
import com.lendy.app.repository.LendyRepository;
import com.lendy.app.ui.activities.PersonDetailActivity;
import com.lendy.app.ui.adapters.PersonAdapter;
import com.lendy.app.viewmodel.LendyViewModel;
import com.lendy.app.viewmodel.LendyViewModelFactory;

import java.util.Objects;

/**
 * ContactsFragment - Màn hình quản lý danh bạ người nợ.
 * Chức năng:
 * - Hiển thị danh sách tất cả mọi người (cả người đang nợ và người đã trả hết).
 * - Thêm mới liên hệ (người nợ) mà không cần nhập số tiền ngay.
 * - Chỉnh sửa thông tin cá nhân (Tên, SĐT).
 * - Xóa liên hệ khỏi hệ thống.
 */
public class ContactsFragment extends Fragment {

    private LendyViewModel viewModel;
    private PersonAdapter adapter;
    private FragmentContactsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        // 1. Khởi tạo ViewBinding cho Fragment
        binding = FragmentContactsBinding.inflate(inflater, container, false);

        // 2. Nút thêm liên hệ mới
        if (binding.fabAddContact != null) {
            binding.fabAddContact.setOnClickListener(v -> showAddContactDialog());
        }

        // 3. Cài đặt danh sách
        setupRecyclerView();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViewModel();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * Hiển thị hộp thoại để thêm một liên hệ mới vào danh bạ.
     * Ở đây mình dùng chung layout 'dialog_add_person' nhưng ẩn đi các phần nhập tiền.
     */
    private void showAddContactDialog() {
        // Sử dụng ViewBinding cho Dialog để code sạch hơn
        DialogAddPersonBinding dialogBinding = DialogAddPersonBinding.inflate(getLayoutInflater());

        // Ẩn các trường không cần thiết (vì đây chỉ là thêm danh bạ, chưa phát sinh nợ)
        hideContactDialogOptionalFields(dialogBinding);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.add_new_contact_title)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String name = Objects.requireNonNull(dialogBinding.editName.getText()).toString().trim();
                String phone = Objects.requireNonNull(dialogBinding.editPhone.getText()).toString().trim();

                if (name.isEmpty()) {
                    dialogBinding.editName.setError(getString(R.string.error_enter_name));
                    return;
                }

                button.setEnabled(false);
                Person person = new Person();
                person.name = name;
                person.phoneNumber = phone;
                person.updatedAt = System.currentTimeMillis();

                // Lưu vào Database thông qua ViewModel
                viewModel.addOrUpdatePersonTransactional(person, new LendyRepository.PersonUpsertCallback() {
                    @Override
                    public void onSuccess() {
                        dialog.dismiss();
                    }

                    @Override
                    public void onDuplicate() {
                        dialogBinding.editName.setError(getString(R.string.error_contact_exists));
                        button.setEnabled(true);
                    }

                    @Override
                    public void onError(Exception exception) {
                        if (!isAdded()) return;
                        button.setEnabled(true);
                        Toast.makeText(requireContext(), getString(R.string.toast_contact_save_failed), Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });
        dialog.show();
    }

    /**
     * Cài đặt danh sách hiển thị danh bạ.
     */
    private void setupRecyclerView() {
        binding.recyclerViewContacts.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new PersonAdapter(person -> {
            // Nhấn vào người: Xem chi tiết giao dịch
            Intent intent = new Intent(getActivity(), PersonDetailActivity.class);
            intent.putExtra(PersonDetailActivity.EXTRA_PERSON_ID, person.id);
            intent.putExtra(PersonDetailActivity.EXTRA_PERSON_NAME, person.name);
            intent.putExtra(PersonDetailActivity.EXTRA_PERSON_PHONE, person.phoneNumber);
            startActivity(intent);
        }, this::showPersonOptionsDialog);

        binding.recyclerViewContacts.setAdapter(adapter);
    }

    /**
     * Lấy dữ liệu danh bạ từ ViewModel.
     */
    private void setupViewModel() {
        if (getActivity() == null) return;

        viewModel = new ViewModelProvider(
                requireActivity(),
                new LendyViewModelFactory(
                        LendyRepository.getInstance(requireActivity().getApplication())))
                .get(LendyViewModel.class);

        // Theo dõi toàn bộ danh sách người nợ (bao gồm cả người nợ = 0)
        viewModel.getAllPeople().observe(getViewLifecycleOwner(), people -> {
            if (people == null || people.isEmpty()) {
                binding.layoutEmptyContacts.setVisibility(View.VISIBLE);
            } else {
                binding.layoutEmptyContacts.setVisibility(View.GONE);
            }
            adapter.submitList(people);
        });
    }

    /**
     * Hiển thị menu tùy chọn (Sửa/Xóa) cho một liên hệ.
     */
    private void showPersonOptionsDialog(Person person) {
        String[] options = {
            getString(R.string.option_edit_person_info),
            getString(R.string.option_delete_person)
        };
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

    /**
     * Xác nhận xóa vĩnh viễn một người và lịch sử của họ.
     */
    private void showDeleteConfirmation(Person person) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.confirm_delete_contact_title)
                .setMessage(getString(R.string.confirm_delete_contact_message, person.name))
                .setPositiveButton(R.string.label_delete, (dialog, which) -> {
                    viewModel.removePerson(person);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /**
     * Hộp thoại chỉnh sửa thông tin cá nhân.
     */
    private void showEditPersonDialog(Person person) {
        DialogAddPersonBinding dialogBinding = DialogAddPersonBinding.inflate(getLayoutInflater());

        hideContactDialogOptionalFields(dialogBinding);

        dialogBinding.editName.setText(person.name);
        dialogBinding.editPhone.setText(person.phoneNumber);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.edit_person)
                .setView(dialogBinding.getRoot())
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String name = Objects.requireNonNull(dialogBinding.editName.getText()).toString().trim();
                String phone = Objects.requireNonNull(dialogBinding.editPhone.getText()).toString().trim();

                if (name.isEmpty()) {
                    dialogBinding.editName.setError(getString(R.string.error_enter_debtor_name));
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
                                dialogBinding.editName.setError(getString(R.string.error_name_exists));
                                button.setEnabled(true);
                            }

                            @Override
                            public void onError(Exception exception) {
                                if (!isAdded()) return;
                                button.setEnabled(true);
                                Toast.makeText(requireContext(), getString(R.string.toast_contact_save_failed), Toast.LENGTH_SHORT).show();
                            }
                        });
            });
        });

        dialog.show();
    }

    private void hideContactDialogOptionalFields(DialogAddPersonBinding dialogBinding) {
        dialogBinding.layoutAmount.setVisibility(View.GONE);
        dialogBinding.toggleGroup.setVisibility(View.GONE);
        dialogBinding.layoutNote.setVisibility(View.GONE);
        dialogBinding.scrollChips.setVisibility(View.GONE);
    }
}

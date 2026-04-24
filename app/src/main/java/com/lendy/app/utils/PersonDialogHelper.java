package com.lendy.app.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.lendy.app.R;
import com.lendy.app.data.entities.Person;
import com.lendy.app.databinding.DialogAddPersonBinding;
import com.lendy.app.repository.LendyRepository;
import com.lendy.app.viewmodel.LendyViewModel;

import java.util.Objects;

/**
 * PersonDialogHelper - Hỗ trợ hiển thị các hộp thoại liên quan đến người nợ.
 * Giúp tập trung logic UI Dialog vào một nơi để dễ bảo trì.
 */
public class PersonDialogHelper {

    /**
     * Hiển thị Dialog để Thêm mới hoặc Chỉnh sửa thông tin liên hệ.
     * 
     * @param context Context hiển thị
     * @param viewModel ViewModel để thực hiện lưu dữ liệu
     * @param person Người nợ cần sửa (truyền null nếu là thêm mới)
     */
    public static void showAddOrEditContactDialog(Context context, LendyViewModel viewModel, Person person) {
        boolean isEdit = (person != null);
        DialogAddPersonBinding binding = DialogAddPersonBinding.inflate(LayoutInflater.from(context));

        // 1. Cấu hình giao diện (Ẩn các trường nhập tiền vì đây chỉ là quản lý liên hệ)
        binding.layoutAmount.setVisibility(View.GONE);
        binding.toggleGroup.setVisibility(View.GONE);
        binding.layoutNote.setVisibility(View.GONE);
        binding.scrollChips.setVisibility(View.GONE);

        if (isEdit) {
            binding.editName.setText(person.name);
            binding.editPhone.setText(person.phoneNumber);
        }

        // 2. Khởi tạo Dialog
        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(isEdit ? R.string.edit_person : R.string.add_new_contact_title)
                .setView(binding.getRoot())
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(v -> {
                String name = Objects.requireNonNull(binding.editName.getText()).toString().trim();
                String phone = Objects.requireNonNull(binding.editPhone.getText()).toString().trim();

                if (name.isEmpty()) {
                    binding.editName.setError(context.getString(R.string.error_enter_name));
                    return;
                }

                button.setEnabled(false);
                
                // Chuẩn bị đối tượng dữ liệu
                Person targetPerson = isEdit ? person : new Person();
                targetPerson.name = name;
                targetPerson.phoneNumber = phone;
                targetPerson.updatedAt = System.currentTimeMillis();

                // 3. Thực hiện lưu Transactional (An toàn & Check trùng lặp)
                viewModel.addOrUpdatePersonTransactional(targetPerson, new LendyRepository.PersonUpsertCallback() {
                    @Override
                    public void onSuccess() {
                        dialog.dismiss();
                    }

                    @Override
                    public void onDuplicate() {
                        binding.editName.setError(context.getString(R.string.error_name_exists));
                        button.setEnabled(true);
                    }

                    @Override
                    public void onError(Exception exception) {
                        button.setEnabled(true);
                        Toast.makeText(context, context.getString(R.string.toast_contact_save_failed), Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });

        dialog.show();
    }

    /**
     * @deprecated Sử dụng {@link #showAddOrEditContactDialog(Context, LendyViewModel, Person)} để hỗ trợ tốt hơn.
     */
    @Deprecated
    public static void showEditPersonDialog(Context context, LendyViewModel viewModel, Person person) {
        showAddOrEditContactDialog(context, viewModel, person);
    }
}

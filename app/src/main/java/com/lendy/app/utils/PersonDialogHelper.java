package com.lendy.app.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.lendy.app.R;
import com.lendy.app.data.entities.Person;
import com.lendy.app.viewmodel.LendyViewModel;

public class PersonDialogHelper {
    public static void showEditPersonDialog(Context context, LendyViewModel viewModel, Person person) {
        View v = LayoutInflater.from(context).inflate(R.layout.dialog_add_person, null);
        // Ẩn các trường không cần thiết khi sửa thông tin cơ bản
        v.findViewById(R.id.layoutAmount).setVisibility(View.GONE);
        v.findViewById(R.id.toggleGroup).setVisibility(View.GONE);
        v.findViewById(R.id.layoutNote).setVisibility(View.GONE);
        v.findViewById(R.id.scrollChips).setVisibility(View.GONE);
        TextInputEditText editName = v.findViewById(R.id.editName);
        TextInputEditText editPhone = v.findViewById(R.id.editPhone);
        editName.setText(person.name);
        editPhone.setText(person.phoneNumber);
        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle("Chỉnh sửa thông tin")
                .setView(v)
                .setPositiveButton("Lưu", null)
                .setNegativeButton("Hủy", null)
                .create();

        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button -> {
            String name = editName.getText().toString().trim();
            if (name.isEmpty()) {
                editName.setError("Name required");
                return;
            }

            person.name = name;
            person.phoneNumber = editPhone.getText().toString().trim();
            person.updatedAt = System.currentTimeMillis();
            viewModel.updatePerson(person);
            dialog.dismiss();
        });
    }
}

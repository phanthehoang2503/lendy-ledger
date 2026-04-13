package com.lendy.app.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.lendy.app.R;
import com.lendy.app.data.TransactionType;
import com.lendy.app.data.entities.Person;
import com.lendy.app.ui.adapters.PersonAdapter;
import com.lendy.app.viewmodel.LendyViewModel;
import com.lendy.app.viewmodel.LendyViewModelFactory;
import com.lendy.app.repository.LendyRepository;

import java.text.NumberFormat;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private LendyViewModel viewModel;
    private PersonAdapter adapter;
    private TextView textTotalLending, textTotalBorrowing;
    private View layoutEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        setSupportActionBar(findViewById(R.id.toolbar));

        textTotalLending = findViewById(R.id.textTotalLending);
        textTotalBorrowing = findViewById(R.id.textTotalBorrowing);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupUI();
        setupViewModel();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_history) {
            startActivity(new Intent(this, CompletedDebtsActivity.class));
            return true;
        } else if (id == R.id.action_contacts) {
            startActivity(new Intent(this, ContactsActivity.class));
            return true;
        } else if (id == R.id.action_reset) {
            showResetConfirmation();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showResetConfirmation() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.confirm_reset_title)
                .setMessage(R.string.confirm_reset_message)
                .setPositiveButton("Xóa hết", (d, w) -> viewModel.clearAllData())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void setupUI() {
        RecyclerView recyclerView = findViewById(R.id.recyclerViewPeople);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new PersonAdapter(person -> {
            Intent intent = new Intent(this, PersonDetailActivity.class);
            intent.putExtra(PersonDetailActivity.EXTRA_PERSON_ID, person.id);
            intent.putExtra(PersonDetailActivity.EXTRA_PERSON_NAME, person.name);
            intent.putExtra(PersonDetailActivity.EXTRA_PERSON_PHONE, person.phoneNumber);
            startActivity(intent);
        }, person -> {
            showEditPersonDialog(person);
        });
        recyclerView.setAdapter(adapter);

        // Vuốt để xóa, hoặc gì đó giống gmail trong điện thoại ấy
        new ItemTouchHelper(
                new ItemTouchHelper.SimpleCallback(0,
                        androidx.recyclerview.widget.ItemTouchHelper.LEFT
                                | androidx.recyclerview.widget.ItemTouchHelper.RIGHT) {
                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                        int position = viewHolder.getAdapterPosition();
                        Person person = adapter.getPersonAt(position);

                        // Hiển thị Dialog xác nhận trước khi làm thật
                        new MaterialAlertDialogBuilder(MainActivity.this)
                                .setTitle(R.string.confirm_delete_person_title)
                                .setMessage(R.string.confirm_delete_person_message)
                                .setPositiveButton("Xóa", (d, w) -> viewModel.removePerson(person))
                                .setNegativeButton("Hủy", (d, w) -> {
                                    // Trả lại vị trí cũ nếu Hủy
                                    adapter.notifyItemChanged(position);
                                })
                                .setOnCancelListener(dialog -> adapter.notifyItemChanged(position))
                                .show();
                    }
                }).attachToRecyclerView(recyclerView);

        findViewById(R.id.fabAdd).setOnClickListener(v -> showAddPersonDialog());
    }

    private void showEditPersonDialog(Person person) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_add_person, null);
        v.findViewById(R.id.layoutAmount).setVisibility(View.GONE);
        v.findViewById(R.id.toggleGroup).setVisibility(View.GONE);
        v.findViewById(R.id.layoutNote).setVisibility(View.GONE);
        v.findViewById(R.id.scrollChips).setVisibility(View.GONE);

        TextInputEditText editName = v.findViewById(R.id.editName);
        TextInputEditText editPhone = v.findViewById(R.id.editPhone);

        editName.setText(person.name);
        editPhone.setText(person.phoneNumber);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Cập nhật thông tin")
                .setView(v)
                .setPositiveButton("Lưu", null) // Để null để tự xử lý click sau show()
                .setNegativeButton("Hủy", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            android.widget.Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
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

                viewModel.addPerson(person);
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void setupViewModel() {
        LendyRepository repository = new LendyRepository(getApplication());
        viewModel = new ViewModelProvider(this, new LendyViewModelFactory(repository)).get(LendyViewModel.class);

        viewModel.getActiveDebts().observe(this, people -> {
            adapter.setPeople(people);
            // HIỂN THỊ TRẠNG THÁI TRỐNG
            if (people == null || people.isEmpty()) {
                layoutEmptyState.setVisibility(View.VISIBLE);
            } else {
                layoutEmptyState.setVisibility(View.GONE);
            }
        });

        viewModel.getGlobalSummary().observe(this, summary -> {
            if (summary == null)
                return;
            NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            textTotalLending.setText("+" + nf.format(summary.totalLending != null ? summary.totalLending : 0));
            textTotalBorrowing
                    .setText("-" + nf.format(Math.abs(summary.totalBorrowing != null ? summary.totalBorrowing : 0)));
        });

        viewModel.getErrorObserver().observe(this, event -> {
            String errorMsg = event.getContentIfNotHandled();
            if (errorMsg != null) {
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Cửa sổ để thêm người
    private void showAddPersonDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_person, null);
        TextInputEditText editName = dialogView.findViewById(R.id.editName);
        TextInputEditText editPhone = dialogView.findViewById(R.id.editPhone);
        TextInputEditText editAmount = dialogView.findViewById(R.id.editAmount);
        TextInputEditText editNote = dialogView.findViewById(R.id.editNote);
        MaterialButtonToggleGroup toggleGroup = dialogView.findViewById(R.id.toggleGroup);

        // quét ô nhập tiền
        editAmount.addTextChangedListener(new android.text.TextWatcher() {
            private String current = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (!s.toString().equals(current)) {
                    editAmount.removeTextChangedListener(this);

                    String cleanString = s.toString().replaceAll("[^\\d]", "");
                    if (!cleanString.isEmpty()) {
                        long parsed;
                        try {
                            parsed = Long.parseLong(cleanString);
                        } catch (NumberFormatException e) {
                            // Nếu số quá lớn vượt mức Long, gán giá trị lớn nhất có thể của kiểu Long
                            parsed = Long.MAX_VALUE;
                        }

                        try {
                            String formatted = com.lendy.app.utils.FormatUtils.formatThousand(parsed);
                            current = formatted;
                            editAmount.setText(formatted);
                            editAmount.setSelection(formatted.length());
                        } catch (Exception e) {
                            current = "";
                            editAmount.setText("");
                        }
                    } else {
                        current = "";
                        editAmount.setText("");
                    }

                    editAmount.addTextChangedListener(this);
                }
            }
        });

        // xử lý mấy nút chip
        View.OnClickListener chipListener = v -> {
            long currentVal = com.lendy.app.utils.FormatUtils.parseFormattedNumber(editAmount.getText().toString());
            long addVal = 0;
            int id = v.getId();
            if (id == R.id.chip20k)
                addVal = 20000;
            else if (id == R.id.chip50k)
                addVal = 50000;
            else if (id == R.id.chip100k)
                addVal = 100000;
            else if (id == R.id.chip200k)
                addVal = 200000;
            else if (id == R.id.chip500k)
                addVal = 500000;

            String newVal = com.lendy.app.utils.FormatUtils.formatThousand(currentVal + addVal);
            editAmount.setText(newVal);
            editAmount.setSelection(newVal.length());
        };

        dialogView.findViewById(R.id.chip20k).setOnClickListener(chipListener);
        dialogView.findViewById(R.id.chip50k).setOnClickListener(chipListener);
        dialogView.findViewById(R.id.chip100k).setOnClickListener(chipListener);
        dialogView.findViewById(R.id.chip200k).setOnClickListener(chipListener);
        dialogView.findViewById(R.id.chip500k).setOnClickListener(chipListener);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Thêm người nợ mới")
                .setView(dialogView)
                .setPositiveButton("Thêm", null) // Để null để tự xử lý click sau show()
                .setNegativeButton("Hủy", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            android.widget.Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String name = editName.getText().toString().trim();
                String phone = editPhone.getText().toString().trim();
                String amountStr = editAmount.getText().toString().trim();
                String note = editNote.getText().toString().trim();

                if (name.isEmpty()) {
                    editName.setError("Vui lòng nhập tên người nợ");
                    return;
                }

                Person person = new Person();
                person.name = name;
                person.phoneNumber = phone;
                person.updatedAt = System.currentTimeMillis();

                long amount = com.lendy.app.utils.FormatUtils.parseFormattedNumber(amountStr);
                TransactionType type = (toggleGroup.getCheckedButtonId() == R.id.btnLending)
                        ? TransactionType.LEND
                        : TransactionType.BORROW;

                viewModel.addPersonWithInitialBalance(person, amount, type, note);
                dialog.dismiss();
            });
        });

        dialog.show();
    }
}

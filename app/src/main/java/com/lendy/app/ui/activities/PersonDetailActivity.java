package com.lendy.app.ui.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.lendy.app.R;
import com.lendy.app.data.TransactionType;
import com.lendy.app.data.entities.Person;
import com.lendy.app.data.entities.TransactionRecord;
import com.lendy.app.repository.LendyRepository;
import com.lendy.app.ui.adapters.TransactionAdapter;
import com.lendy.app.viewmodel.LendyViewModel;
import com.lendy.app.viewmodel.LendyViewModelFactory;

import java.text.NumberFormat;
import java.util.Locale;

public class PersonDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PERSON_ID = "extra_person_id";
    public static final String EXTRA_PERSON_NAME = "extra_person_name";
    public static final String EXTRA_PERSON_PHONE = "extra_person_phone";

    private LendyViewModel viewModel;
    private TransactionAdapter adapter;
    private long personId;
    private long currentBalance = 0;
    private TextView textName, textPhone, textBalance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_person_detail);

        personId = getIntent().getLongExtra(EXTRA_PERSON_ID, -1);
        if (personId == -1) {
            android.widget.Toast.makeText(this, "Không tìm thấy thông tin người nợ", android.widget.Toast.LENGTH_SHORT).show();
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        String name = getIntent().getStringExtra(EXTRA_PERSON_NAME);
        String phone = getIntent().getStringExtra(EXTRA_PERSON_PHONE);

        textName = findViewById(R.id.textDetailName);
        textPhone = findViewById(R.id.textDetailPhone);
        textBalance = findViewById(R.id.textDetailBalance);

        textName.setText(name);
        textPhone.setText(phone != null && !phone.isEmpty() ? phone : "Không có số điện thoại");

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());

        setupRecyclerView();
        setupViewModel();
        setupFab();
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerViewTimeline);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionAdapter();

        // Vuốt hoặc ấn giữ đều được nhưng vuốt tiện hơn mà =))
        adapter.setOnTransactionLongClickListener(this::showTransactionOptionsDialog);

        recyclerView.setAdapter(adapter);

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
                        TransactionRecord record = adapter.getTransactionAt(position);

                        if (direction == androidx.recyclerview.widget.ItemTouchHelper.LEFT) {
                            // Vuốt trái -> Xóa
                            new MaterialAlertDialogBuilder(PersonDetailActivity.this)
                                    .setTitle("Xóa giao dịch này?")
                                    .setMessage("Số dư của người này sẽ được tự động tính toán lại.")
                                    .setPositiveButton("Xóa", (d, w) -> viewModel.deleteTransaction(record))
                                    .setNegativeButton("Hủy", (d, w) -> {
                                        adapter.notifyItemChanged(position); // Hoàn tác ui
                                    })
                                    .setOnCancelListener(dialog -> adapter.notifyItemChanged(position))
                                    .show();
                        } else {
                            // Vuốt phải -> Sửa
                            showEditTransactionDialog(record);
                            adapter.notifyItemChanged(position); // Reset ui card back
                        }
                    }
                }).attachToRecyclerView(recyclerView);
    }

    private void setupViewModel() {
        LendyRepository repository = new LendyRepository(getApplication());
        LendyViewModelFactory factory = new LendyViewModelFactory(repository);
        viewModel = new ViewModelProvider(this, factory).get(LendyViewModel.class);

        viewModel.getActiveDebts().observe(this, people -> {
            for (Person p : people) {
                if (p.id == personId) {
                    currentBalance = p.totalBalance;
                    updateBalanceUI(p.totalBalance);
                    break;
                }
            }
        });

        viewModel.getTimeline(personId).observe(this, transactions -> {
            if (transactions != null) {
                adapter.setTransactions(transactions);
            }
        });
    }

    private void updateBalanceUI(long balance) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        String formatted = formatter.format(Math.abs(balance));

        if (balance > 0) {
            textBalance.setText("+" + formatted);
            textBalance.setTextColor(ContextCompat.getColor(this, R.color.receivable));
        } else if (balance < 0) {
            textBalance.setText("-" + formatted);
            textBalance.setTextColor(ContextCompat.getColor(this, R.color.payable));
        } else {
            textBalance.setText(formatted);
            textBalance.setTextColor(ContextCompat.getColor(this, R.color.outline));
        }
    }

    private void showTransactionOptionsDialog(TransactionRecord record) {
        String[] options = { "Sửa số tiền", "Xóa giao dịch" };
        new MaterialAlertDialogBuilder(this)
                .setTitle("Tùy chọn giao dịch")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showEditTransactionDialog(record);
                    } else {
                        showDeleteConfirmDialog(record);
                    }
                })
                .show();
    }

    private void showDeleteConfirmDialog(TransactionRecord record) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Xóa giao dịch này?")
                .setMessage("Số dư của người này sẽ được tự động tính toán lại.")
                .setPositiveButton("Xóa", (d, w) -> viewModel.deleteTransaction(record))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showEditTransactionDialog(TransactionRecord oldRecord) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_transaction, null);
        dialogView.findViewById(R.id.toggleGroup).setVisibility(View.GONE);

        TextInputEditText editAmount = dialogView.findViewById(R.id.editAmount);
        TextInputEditText editNote = dialogView.findViewById(R.id.editNote);

        editAmount.setText(com.lendy.app.utils.FormatUtils.formatThousand(oldRecord.amount));
        editNote.setText(oldRecord.note);

        // TEXT WATCHER (FIX CURSOR)
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
                        try {
                            String formatted = com.lendy.app.utils.FormatUtils
                                    .formatThousand(Long.parseLong(cleanString));
                            current = formatted;
                            editAmount.setText(formatted);
                            editAmount.setSelection(formatted.length());
                        } catch (NumberFormatException e) {
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

        // Chips logic
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

        new MaterialAlertDialogBuilder(this)
                .setTitle("Sửa số tiền giao dịch")
                .setView(dialogView)
                .setPositiveButton("Cập nhật", (dialog, which) -> {
                    long newAmount = com.lendy.app.utils.FormatUtils
                            .parseFormattedNumber(editAmount.getText().toString());
                    String newNote = editNote.getText().toString().trim();

                    TransactionRecord newRecord = new TransactionRecord();
                    newRecord.id = oldRecord.id;
                    newRecord.personId = oldRecord.personId;
                    newRecord.amount = newAmount;
                    newRecord.type = oldRecord.type;
                    newRecord.note = newNote;
                    newRecord.timestamp = oldRecord.timestamp;

                    viewModel.updateTransaction(oldRecord, newRecord);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void setupFab() {
        FloatingActionButton fab = findViewById(R.id.fabAddTransaction);
        fab.setOnClickListener(v -> showAddTransactionDialog());
    }

    private void showAddTransactionDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_transaction, null);
        TextInputEditText editAmount = dialogView.findViewById(R.id.editAmount);
        TextInputEditText editNote = dialogView.findViewById(R.id.editNote);
        MaterialButtonToggleGroup toggleGroup = dialogView.findViewById(R.id.toggleGroup);
        MaterialButton btnLeft = dialogView.findViewById(R.id.btnLending);
        MaterialButton btnRight = dialogView.findViewById(R.id.btnBorrowing);

        if (currentBalance >= 0) {
            btnLeft.setText("Cho vay thêm");
            btnRight.setText("Họ trả nợ");
        } else {
            btnLeft.setText("Đi vay thêm");
            btnRight.setText("Trả nợ họ");
        }

        // Về cơ bản là cái hàm này nhìn cái input
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
                        try {
                            String formatted = com.lendy.app.utils.FormatUtils
                                    .formatThousand(Long.parseLong(cleanString));
                            current = formatted;
                            editAmount.setText(formatted);
                            editAmount.setSelection(formatted.length());
                        } catch (NumberFormatException e) {
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

        // Chips logic
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
                .setTitle("Thêm giao dịch mới")
                .setView(dialogView)
                .setPositiveButton("Lưu", null)
                .setNegativeButton("Hủy", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            android.widget.Button button = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                long amount = com.lendy.app.utils.FormatUtils.parseFormattedNumber(editAmount.getText().toString());
                String note = editNote.getText().toString().trim();

                if (amount <= 0) {
                    editAmount.setError("Số tiền phải lớn hơn 0");
                    return;
                }

                TransactionType type;
                if (currentBalance >= 0) {
                    type = (toggleGroup.getCheckedButtonId() == R.id.btnLending)
                            ? TransactionType.LEND
                            : TransactionType.REPAY;
                } else {
                    type = (toggleGroup.getCheckedButtonId() == R.id.btnLending)
                            ? TransactionType.BORROW
                            : TransactionType.PAY_BACK;
                }

                TransactionRecord record = new TransactionRecord();
                record.personId = personId;
                record.amount = amount;
                record.type = type;
                record.note = (note.isEmpty())
                        ? (type == TransactionType.LEND || type == TransactionType.BORROW ? "Giao dịch bổ sung"
                                : "Trả nợ")
                        : note;
                record.timestamp = System.currentTimeMillis();

                viewModel.addTransaction(record);
                dialog.dismiss();
            });
        });

        dialog.show();
    }
}

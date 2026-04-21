package com.lendy.app.ui.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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
import com.lendy.app.data.entities.TransactionRecord;
import com.lendy.app.repository.LendyRepository;
import com.lendy.app.ui.adapters.TransactionAdapter;
import com.lendy.app.utils.FormatUtils;
import com.lendy.app.viewmodel.LendyViewModel;
import com.lendy.app.viewmodel.LendyViewModelFactory;

import java.util.List;

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
            android.widget.Toast.makeText(this, "Không tìm thấy thông tin người nợ", android.widget.Toast.LENGTH_SHORT)
                    .show();
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
        textPhone.setText(phone != null && !phone.isEmpty() ? phone : getString(R.string.no_phone_number));

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());

        setupRecyclerView();
        setupViewModel();
        setupFab();
        setupEdgeToEdgeInsets();
    }

    private void setupEdgeToEdgeInsets() {
        View appBar = findViewById(R.id.detail_app_bar);
        RecyclerView recyclerView = findViewById(R.id.recyclerViewTimeline);
        FloatingActionButton fab = findViewById(R.id.fabAddTransaction);

        final int initialAppBarTopPadding = appBar.getPaddingTop();
        final int initialRecyclerBottomPadding = recyclerView.getPaddingBottom();
        final int initialFabBottomMargin = ((ViewGroup.MarginLayoutParams) fab.getLayoutParams()).bottomMargin;

        ViewCompat.setOnApplyWindowInsetsListener(appBar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), initialAppBarTopPadding + systemBars.top, v.getPaddingRight(),
                    v.getPaddingBottom());
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(recyclerView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(),
                    initialRecyclerBottomPadding + systemBars.bottom);
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(fab, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            layoutParams.bottomMargin = initialFabBottomMargin + systemBars.bottom;
            v.setLayoutParams(layoutParams);
            return insets;
        });

        ViewCompat.requestApplyInsets(findViewById(android.R.id.content));
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
                        ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                        int position = viewHolder.getBindingAdapterPosition();
                        if (position == RecyclerView.NO_POSITION)
                            return;

                        List<TransactionRecord> current = adapter.getCurrentList();
                        if (position < 0 || position >= current.size()) {
                            return;
                        }
                        TransactionRecord record = current.get(position);

                        if (direction == ItemTouchHelper.LEFT) {
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
        LendyRepository repository = LendyRepository.getInstance(getApplication());
        LendyViewModelFactory factory = new LendyViewModelFactory(repository);
        viewModel = new ViewModelProvider(this, factory).get(LendyViewModel.class);

        viewModel.getPersonById(personId).observe(this, person -> {
            if (person != null) {
                currentBalance = person.totalBalance;
                updateBalanceUI(person.totalBalance);

                // Cập nhật lại tên và SĐT nếu có thay đổi
                String displayName = person.isDeleted ? person.name + " (Đã xóa)" : person.name;
                textName.setText(displayName);
                textPhone.setText(person.phoneNumber != null && !person.phoneNumber.isEmpty()
                        ? person.phoneNumber
                        : getString(R.string.no_phone_short));
            }
        });

        viewModel.getTimeline(personId).observe(this, transactions -> {
            if (transactions != null) {
                adapter.submitList(transactions);
            }
        });
    }

    private void updateBalanceUI(long balance) {
        String formatted = FormatUtils.formatCurrencyAbs(balance);

        if (balance > 0) {
            // Nợ thu (Họ nợ mình)
            textBalance.setText(formatted);
            textBalance.setContentDescription(getString(R.string.balance_receivable_talkback, formatted));
            textBalance.setTextColor(ContextCompat.getColor(this, R.color.black));
        } else if (balance < 0) {
            // Nợ trả (Mình nợ họ)
            textBalance.setText(formatted);
            textBalance.setContentDescription(getString(R.string.balance_payable_talkback, formatted));
            textBalance.setTextColor(ContextCompat.getColor(this, R.color.black));
        } else {
            textBalance.setText(getString(R.string.zero_balance));
            textBalance.setContentDescription(getString(R.string.zero_balance));
            textBalance.setTextColor(ContextCompat.getColor(this, R.color.black));
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

        editAmount.setText(FormatUtils.formatThousand(oldRecord.amount));
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
                            String formatted = FormatUtils.formatThousand(Long.parseLong(cleanString));
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
            long currentVal = FormatUtils.parseFormattedNumber(editAmount.getText().toString());
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

            String newVal = FormatUtils.formatThousand(currentVal + addVal);
            editAmount.setText(newVal);
            editAmount.setSelection(newVal.length());
        };

        dialogView.findViewById(R.id.chip20k).setOnClickListener(chipListener);
        dialogView.findViewById(R.id.chip50k).setOnClickListener(chipListener);
        dialogView.findViewById(R.id.chip100k).setOnClickListener(chipListener);
        dialogView.findViewById(R.id.chip200k).setOnClickListener(chipListener);
        dialogView.findViewById(R.id.chip500k).setOnClickListener(chipListener);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Sửa số tiền giao dịch")
                .setView(dialogView)
                .setPositiveButton("Cập nhật", null)
                .setNegativeButton("Hủy", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            android.widget.Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(v -> {
                long newAmount = FormatUtils.parseFormattedNumber(
                        editAmount.getText().toString());
                String newNote = editNote.getText().toString().trim();

                if (newAmount <= 0) {
                    editAmount.setError("Số tiền phải lớn hơn 0");
                    return;
                }

                TransactionRecord newRecord = new TransactionRecord();
                newRecord.id = oldRecord.id;
                newRecord.personId = oldRecord.personId;
                newRecord.amount = newAmount;
                newRecord.type = oldRecord.type;
                newRecord.note = newNote;
                newRecord.imageUri = oldRecord.imageUri;
                newRecord.timestamp = oldRecord.timestamp;

                viewModel.updateTransaction(oldRecord, newRecord);
                dialog.dismiss();
            });
        });

        dialog.show();
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

        if (currentBalance > 0) {
            btnLeft.setText(R.string.btn_lend_more);
            btnRight.setText(R.string.btn_they_repay);
        } else if (currentBalance < 0) {
            btnLeft.setText(R.string.btn_borrow_more);
            btnRight.setText(R.string.btn_we_repay);
        } else {
            // currentBalance == 0
            btnLeft.setText(R.string.btn_lend);
            btnRight.setText(R.string.btn_borrow);
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
                            String formatted = FormatUtils.formatThousand(
                                    Long.parseLong(cleanString));
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
            long currentVal = FormatUtils.parseFormattedNumber(editAmount.getText().toString());
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

            String newVal = FormatUtils.formatThousand(currentVal + addVal);
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
            android.widget.Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                long amount = FormatUtils.parseFormattedNumber(editAmount.getText().toString());
                String note = editNote.getText().toString().trim();

                if (amount <= 0) {
                    editAmount.setError("Số tiền phải lớn hơn 0");
                    return;
                }

                TransactionType type;
                if (currentBalance > 0) {
                    type = (toggleGroup.getCheckedButtonId() == R.id.btnLending)
                            ? TransactionType.LEND
                            : TransactionType.REPAY;
                } else if (currentBalance < 0) {
                    type = (toggleGroup.getCheckedButtonId() == R.id.btnLending)
                            ? TransactionType.BORROW
                            : TransactionType.PAY_BACK;
                } else {
                    // currentBalance == 0
                    type = (toggleGroup.getCheckedButtonId() == R.id.btnLending)
                            ? TransactionType.LEND
                            : TransactionType.BORROW;
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

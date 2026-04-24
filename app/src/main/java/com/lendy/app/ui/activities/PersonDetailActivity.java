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
import com.lendy.app.databinding.ActivityPersonDetailBinding;
import com.lendy.app.repository.LendyRepository;
import com.lendy.app.ui.adapters.TransactionAdapter;
import com.lendy.app.utils.FormatUtils;
import com.lendy.app.viewmodel.LendyViewModel;
import com.lendy.app.viewmodel.LendyViewModelFactory;

import java.util.List;

/**
 * PersonDetailActivity - Màn hình hiển thị chi tiết các giao dịch của một người nợ.
 * Chức năng:
 * - Hiển thị tên, số điện thoại và tổng dư nợ hiện tại.
 * - Danh sách lịch sử giao dịch (Timeline) theo thời gian.
 * - Cho phép thêm, sửa, xóa giao dịch trực tiếp.
 */
public class PersonDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PERSON_ID = "extra_person_id";
    public static final String EXTRA_PERSON_NAME = "extra_person_name";
    public static final String EXTRA_PERSON_PHONE = "extra_person_phone";

    private LendyViewModel viewModel;
    private TransactionAdapter adapter;
    private long personId;
    private long currentBalance = 0;
    private ActivityPersonDetailBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        
        // 1. Khởi tạo ViewBinding
        binding = ActivityPersonDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 2. Lấy dữ liệu ID người nợ được truyền từ màn hình trước sang
        personId = getIntent().getLongExtra(EXTRA_PERSON_ID, -1);
        if (personId == -1) {
            android.widget.Toast.makeText(this, getString(R.string.error_person_not_found), android.widget.Toast.LENGTH_SHORT)
                    .show();
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        String name = getIntent().getStringExtra(EXTRA_PERSON_NAME);
        String phone = getIntent().getStringExtra(EXTRA_PERSON_PHONE);

        // 3. Hiển thị thông tin cơ bản lên Header
        binding.textDetailName.setText(name);
        binding.textDetailPhone.setText(phone != null && !phone.isEmpty() ? phone : getString(R.string.no_phone_number));

        // 4. Nút quay lại
        binding.toolbar.setOnClickListener(v -> finish());

        // 5. Cài đặt các thành phần bổ trợ
        setupRecyclerView(); // Danh sách giao dịch
        setupViewModel();    // Kết nối Database
        setupFab();          // Nút thêm giao dịch nhanh
        setupEdgeToEdgeInsets(); // Xử lý khoảng cách thanh hệ thống
    }

    /**
     * Cấu hình xử lý insets cho chế độ Edge-to-Edge, 
     * đảm bảo giao diện không bị đè bởi thanh trạng thái và thanh điều hướng.
     */
    private void setupEdgeToEdgeInsets() {
        final int initialAppBarTopPadding = binding.detailAppBar.getPaddingTop();
        final int initialRecyclerBottomPadding = binding.recyclerViewTimeline.getPaddingBottom();
        final int initialFabBottomMargin = ((ViewGroup.MarginLayoutParams) binding.fabAddTransaction.getLayoutParams()).bottomMargin;

        ViewCompat.setOnApplyWindowInsetsListener(binding.detailAppBar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), initialAppBarTopPadding + systemBars.top, v.getPaddingRight(),
                    v.getPaddingBottom());
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(binding.recyclerViewTimeline, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(),
                    initialRecyclerBottomPadding + systemBars.bottom);
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(binding.fabAddTransaction, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            layoutParams.bottomMargin = initialFabBottomMargin + systemBars.bottom;
            v.setLayoutParams(layoutParams);
            return insets;
        });

        ViewCompat.requestApplyInsets(binding.getRoot());
    }

    private void setupRecyclerView() {
        binding.recyclerViewTimeline.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionAdapter();

        // Vuốt hoặc ấn giữ đều được nhưng vuốt tiện hơn mà =))
        adapter.setOnTransactionLongClickListener(this::showTransactionOptionsDialog);

        binding.recyclerViewTimeline.setAdapter(adapter);

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
                                    .setTitle(R.string.confirm_delete_transaction_title)
                                    .setMessage(R.string.confirm_delete_transaction_message)
                                    .setPositiveButton(R.string.label_delete, (d, w) -> viewModel.deleteTransaction(record))
                                    .setNegativeButton(R.string.cancel, (d, w) -> {
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
                }).attachToRecyclerView(binding.recyclerViewTimeline);
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
                String displayName = person.isDeleted ? person.name + " " + getString(R.string.suffix_deleted) : person.name;
                binding.textDetailName.setText(displayName);
                binding.textDetailPhone.setText(person.phoneNumber != null && !person.phoneNumber.isEmpty()
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

    /**
     * Cập nhật màu sắc và nội dung hiển thị của số dư tổng.
     * @param balance Số dư (Dương: Họ nợ mình, Âm: Mình nợ họ)
     */
    private void updateBalanceUI(long balance) {
        String formatted = FormatUtils.formatCurrencyAbs(balance);

        if (balance > 0) {
            // Nợ thu (Họ nợ mình) - Hiển thị màu đen mặc định hoặc xanh tùy thiết kế
            binding.textDetailBalance.setText(formatted);
            binding.textDetailBalance.setContentDescription(getString(R.string.balance_receivable_talkback, formatted));
            binding.textDetailBalance.setTextColor(ContextCompat.getColor(this, R.color.black));
        } else if (balance < 0) {
            // Nợ trả (Mình nợ họ)
            binding.textDetailBalance.setText(formatted);
            binding.textDetailBalance.setContentDescription(getString(R.string.balance_payable_talkback, formatted));
            binding.textDetailBalance.setTextColor(ContextCompat.getColor(this, R.color.black));
        } else {
            // Hết nợ
            binding.textDetailBalance.setText(getString(R.string.zero_balance));
            binding.textDetailBalance.setContentDescription(getString(R.string.zero_balance));
            binding.textDetailBalance.setTextColor(ContextCompat.getColor(this, R.color.black));
        }
    }

    private void showTransactionOptionsDialog(TransactionRecord record) {
        String[] options = { getString(R.string.option_edit_amount), getString(R.string.option_delete_transaction) };
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.transaction_options_title)
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
                .setTitle(R.string.confirm_delete_transaction_title)
                .setMessage(R.string.confirm_delete_transaction_message)
                .setPositiveButton(R.string.label_delete, (d, w) -> viewModel.deleteTransaction(record))
                .setNegativeButton(R.string.cancel, null)
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
                .setTitle(R.string.edit_transaction_title)
                .setView(dialogView)
                .setPositiveButton(R.string.label_update, null)
                .setNegativeButton(R.string.cancel, null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            android.widget.Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(v -> {
                long newAmount = FormatUtils.parseFormattedNumber(
                        editAmount.getText().toString());
                String newNote = editNote.getText().toString().trim();

                if (newAmount <= 0) {
                    editAmount.setError(getString(R.string.error_amount_must_be_positive));
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
        binding.fabAddTransaction.setOnClickListener(v -> showAddTransactionDialog());
    }

    /**
     * Hiển thị hộp thoại thêm giao dịch mới (Cho vay thêm / Trả bớt).
     */
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
                .setTitle(R.string.add_transaction_title)
                .setView(dialogView)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            android.widget.Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                long amount = FormatUtils.parseFormattedNumber(editAmount.getText().toString());
                String note = editNote.getText().toString().trim();

                if (amount <= 0) {
                    editAmount.setError(getString(R.string.error_amount_must_be_positive));
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
                        ? (type == TransactionType.LEND || type == TransactionType.BORROW ? getString(R.string.default_note_additional)
                                : getString(R.string.default_note_repayment))
                        : note;
                record.timestamp = System.currentTimeMillis();

                viewModel.addTransaction(record);
                dialog.dismiss();
            });
        });

        dialog.show();
    }
}

package com.lendy.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.lendy.app.data.TransactionType;
import com.lendy.app.data.entities.Person;
import com.lendy.app.data.entities.TransactionRecord;
import com.lendy.app.repository.LendyRepository;
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
        
        // LONG CLICK ĐỂ SỬA/XÓA GIAO DỊCH
        adapter.setOnTransactionLongClickListener(this::showTransactionOptionsDialog);
        
        recyclerView.setAdapter(adapter);
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
        String[] options = {"Sửa số tiền", "Xóa giao dịch"};
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
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_person, null);
        dialogView.findViewById(R.id.layoutName).setVisibility(View.GONE);
        dialogView.findViewById(R.id.layoutPhone).setVisibility(View.GONE);
        dialogView.findViewById(R.id.toggleGroup).setVisibility(View.GONE);

        TextInputEditText editAmount = dialogView.findViewById(R.id.editAmount);
        editAmount.setText(String.valueOf(oldRecord.amount));

        new MaterialAlertDialogBuilder(this)
                .setTitle("Sửa số tiền giao dịch")
                .setView(dialogView)
                .setPositiveButton("Cập nhật", (dialog, which) -> {
                    String amountStr = editAmount.getText().toString().trim();
                    if (!amountStr.isEmpty()) {
                        long newAmount = Long.parseLong(amountStr);
                        
                        // Tạo bản sao mới để cập nhật
                        TransactionRecord newRecord = new TransactionRecord();
                        newRecord.id = oldRecord.id;
                        newRecord.personId = oldRecord.personId;
                        newRecord.amount = newAmount;
                        newRecord.type = oldRecord.type;
                        newRecord.note = oldRecord.note;
                        newRecord.timestamp = oldRecord.timestamp;
                        
                        viewModel.updateTransaction(oldRecord, newRecord);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void setupFab() {
        FloatingActionButton fab = findViewById(R.id.fabAddTransaction);
        fab.setOnClickListener(v -> showAddTransactionDialog());
    }

    private void showAddTransactionDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_person, null);
        dialogView.findViewById(R.id.layoutName).setVisibility(View.GONE);
        dialogView.findViewById(R.id.layoutPhone).setVisibility(View.GONE);
        
        TextInputEditText editAmount = dialogView.findViewById(R.id.editAmount);
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

        new MaterialAlertDialogBuilder(this)
                .setTitle("Thêm giao dịch mới")
                .setView(dialogView)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String amountStr = editAmount.getText().toString().trim();
                    if (!amountStr.isEmpty()) {
                        long amount = Long.parseLong(amountStr);
                        TransactionType type;
                        
                        if (currentBalance >= 0) {
                            type = (toggleGroup.getCheckedButtonId() == R.id.btnLending) 
                                   ? TransactionType.LEND : TransactionType.REPAY;
                        } else {
                            type = (toggleGroup.getCheckedButtonId() == R.id.btnLending) 
                                   ? TransactionType.BORROW : TransactionType.PAY_BACK;
                        }

                        TransactionRecord record = new TransactionRecord();
                        record.personId = personId;
                        record.amount = amount;
                        record.type = type;
                        record.timestamp = System.currentTimeMillis();
                        record.note = "Giao dịch bổ sung";
                        
                        viewModel.addTransaction(record);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}

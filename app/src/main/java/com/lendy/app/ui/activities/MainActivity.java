package com.lendy.app.ui.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.lendy.app.R;
import com.lendy.app.data.TransactionType;
import com.lendy.app.data.entities.Person;
import com.lendy.app.data.entities.TransactionRecord;
import com.lendy.app.repository.LendyRepository;
import com.lendy.app.ui.adapters.MainPagerAdapter;
import com.lendy.app.ui.adapters.PersonPickerAdapter;
import com.lendy.app.ui.fragments.HomeFragment;
import com.lendy.app.utils.CurrencyTextWatcher;
import com.lendy.app.viewmodel.LendyViewModel;
import com.lendy.app.viewmodel.LendyViewModelFactory;

import java.util.List;

public class MainActivity extends AppCompatActivity implements HomeFragment.AddDebtFlowHost {

    private LendyViewModel viewModel;
    private ViewPager2 viewPager;
    private BottomNavigationView bottomNav;
    private TextView textTotalLending, textTotalBorrowing, textAppTitle;
    private View welcomeContainer, summaryCard;
    private Observer<List<Person>> addDebtFlowObserver;
    private AlertDialog pendingAddTransactionDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        // 1. Ánh xạ View
        viewPager = findViewById(R.id.viewPager);
        bottomNav = findViewById(R.id.bottomNav);
        textTotalLending = findViewById(R.id.textTotalLending);
        textTotalBorrowing = findViewById(R.id.textTotalBorrowing);
        welcomeContainer = findViewById(R.id.header_welcome_container);
        summaryCard = findViewById(R.id.summary_card);
        textAppTitle = findViewById(R.id.textAppTitle);

        // 2. Toolbar
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // 3. Setup
        setupNavigation();
        setupViewModel();

        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(viewPager, (v, insets) -> {
            int navHeight = bottomNav.getHeight();
            if (navHeight > 0) {
                v.setPadding(0, 0, 0, navHeight);
            } else {
                bottomNav.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                    @Override
                    public void onLayoutChange(View view, int left, int top, int right, int bottom, int oldLeft,
                            int oldTop, int oldRight, int oldBottom) {
                        bottomNav.removeOnLayoutChangeListener(this);
                        v.setPadding(0, 0, 0, bottomNav.getHeight());
                    }
                });
            }
            return insets;
        });
    }

    @Override
    public void showAddDebtFlow() {
        if (addDebtFlowObserver != null) {
            viewModel.getAllPeople().removeObserver(addDebtFlowObserver);
        }

        addDebtFlowObserver = people -> {
            if (people == null || people.isEmpty()) {
                // Danh bạ trống -> Nhảy thẳng bước thêm người mới
                showAddNewPersonDialog();
            } else {
                // Có người -> Hiện dialog chọn người
                showSelectPersonDialog(people);
            }
            // Sau khi check xong thì remove đúng observer one-shot
            if (addDebtFlowObserver != null) {
                viewModel.getAllPeople().removeObserver(addDebtFlowObserver);
                addDebtFlowObserver = null;
            }
        };

        viewModel.getAllPeople().observe(this, addDebtFlowObserver);
    }

    private void showSelectPersonDialog(List<Person> people) {
        View v = getLayoutInflater().inflate(R.layout.dialog_select_person, null);
        RecyclerView rv = v.findViewById(R.id.recyclerViewPicker);
        TextInputEditText editSearch = v.findViewById(R.id.editSearch);
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(v)
                .create();

        // Setup Adapter
        PersonPickerAdapter adapter = new PersonPickerAdapter(person -> {
            // KHI CHỌN 1 NGƯỜI:
            // 1. Đóng dialog hiện tại
            // 2. Mở dialog nhập tiền cho người đó
            dialog.dismiss();
            showAddTransactionForPerson(person);
        });

        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);
        adapter.setFullList(people);

        // Xử lý Search Bar
        editSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                adapter.filter(s.toString());
            }
        });

        v.findViewById(R.id.btnAddNewPerson).setOnClickListener(view -> {
            dialog.dismiss();
            showAddNewPersonDialog();
        });
        dialog.show();
    }

    private void setupNavigation() {
        MainPagerAdapter adapter = new MainPagerAdapter(this);
        viewPager.setAdapter(adapter);

        // Vuốt -> Chọn Menu
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                int menuId;
                switch (position) {
                    case 0:
                        menuId = R.id.nav_home;
                        break;
                    case 1:
                        menuId = R.id.nav_stats;
                        break;
                    case 2:
                        menuId = R.id.nav_history;
                        break;
                    case 3:
                        menuId = R.id.nav_contacts;
                        break;
                    default:
                        menuId = R.id.nav_home;
                }
                bottomNav.setSelectedItemId(menuId);
                updateHeaderVisibility(position);
            }
        });

        // Bấm Menu -> Chuyển Trang
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home)
                viewPager.setCurrentItem(0);
            else if (id == R.id.nav_stats)
                viewPager.setCurrentItem(1);
            else if (id == R.id.nav_history)
                viewPager.setCurrentItem(2);
            else if (id == R.id.nav_contacts)
                viewPager.setCurrentItem(3);
            return true;
        });

        viewPager.setUserInputEnabled(false);
    }

    private void setupViewModel() {
        LendyRepository repository = LendyRepository.getInstance(getApplication());
        viewModel = new ViewModelProvider(this, new LendyViewModelFactory(repository)).get(LendyViewModel.class);

        viewModel.getGlobalSummary().observe(this, summary -> {
            if (summary == null)
                return;

            long lendingVal = summary.totalLending != null ? summary.totalLending : 0;
            long borrowingVal = Math.abs(summary.totalBorrowing != null ? summary.totalBorrowing : 0);

            // Format số
            textTotalLending.setText(com.lendy.app.utils.FormatUtils.formatCurrency(lendingVal));
            textTotalBorrowing.setText(com.lendy.app.utils.FormatUtils.formatCurrency(borrowingVal));

            textTotalLending.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.receivable));
            textTotalBorrowing.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.payable));
        });

        viewModel.getErrorObserver().observe(this, event -> {
            String errorMsg = event.getContentIfNotHandled();
            if (errorMsg != null) {
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                if (pendingAddTransactionDialog != null && pendingAddTransactionDialog.isShowing()) {
                    android.widget.Button saveButton = pendingAddTransactionDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    if (saveButton != null) {
                        saveButton.setEnabled(true);
                    }
                }
            }
        });

        viewModel.getTransactionAddedObserver().observe(this, event -> {
            Boolean added = event.getContentIfNotHandled();
            if (Boolean.TRUE.equals(added)) {
                Toast.makeText(this, "Đã thêm giao dịch", Toast.LENGTH_SHORT).show();
                if (pendingAddTransactionDialog != null && pendingAddTransactionDialog.isShowing()) {
                    pendingAddTransactionDialog.dismiss();
                }
                pendingAddTransactionDialog = null;
            }
        });
    }

    private void updateHeaderVisibility(int position) {
        if (position == 0) {
            textAppTitle.setText(getString(R.string.tab_home));
            welcomeContainer.setVisibility(View.VISIBLE);
            summaryCard.setVisibility(View.VISIBLE);
        } else {
            welcomeContainer.setVisibility(View.GONE);
            summaryCard.setVisibility(View.GONE);

            // Cập nhật tiêu đề động theo từng Tab
            switch (position) {
                case 1:
                    textAppTitle.setText(getString(R.string.tab_stats));
                    break;
                case 2:
                    textAppTitle.setText(getString(R.string.tab_history));
                    break;
                case 3:
                    textAppTitle.setText(getString(R.string.tab_contacts));
                    break;
            }
        }
    }

    private void showAddNewPersonDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_person, null);
        TextInputEditText editName = dialogView.findViewById(R.id.editName);
        TextInputEditText editPhone = dialogView.findViewById(R.id.editPhone);
        TextInputEditText editAmount = dialogView.findViewById(R.id.editAmount);
        TextInputEditText editNote = dialogView.findViewById(R.id.editNote);
        MaterialButtonToggleGroup toggleGroup = dialogView.findViewById(R.id.toggleGroup);

        // Setup Quick Add Chips
        setupQuickAddChips(dialogView, editAmount);

        // Format tiền
        editAmount.addTextChangedListener(new CurrencyTextWatcher(editAmount));

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Thêm người nợ mới")
                .setView(dialogView)
                .setPositiveButton("Thêm", null)
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

                button.setEnabled(false);
                viewModel.checkActivePersonExists(name, phone, exists -> {
                    if (exists) {
                        editName.setError("Người này đã tồn tại");
                        button.setEnabled(true);
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
        });

        dialog.show();
    }

    private void setupQuickAddChips(View dialogView, TextInputEditText editAmount) {
        int[] chipIds = { R.id.chip20k, R.id.chip50k, R.id.chip100k, R.id.chip200k, R.id.chip500k };
        long[] amounts = { 20000, 50000, 100000, 200000, 500000 };

        for (int i = 0; i < chipIds.length; i++) {
            final long amount = amounts[i];
            View chip = dialogView.findViewById(chipIds[i]);
            if (chip != null) {
                chip.setOnClickListener(v -> {
                    String currentStr = editAmount.getText().toString().replaceAll("[^\\d]", "");
                    Long currentAmount = parseAmountSafely(currentStr);
                    if (currentAmount == null) currentAmount = 0L;
                    long finalAmount = currentAmount + amount;

                    editAmount.setText(com.lendy.app.utils.FormatUtils.formatThousand(finalAmount));
                    editAmount.setSelection(editAmount.getText().length());
                });
            }
        }
    }

    private void showAddTransactionForPerson(Person person) {
        View v = getLayoutInflater().inflate(R.layout.dialog_transaction, null);
        TextInputEditText editAmount = v.findViewById(R.id.editAmount);
        TextInputEditText editNote = v.findViewById(R.id.editNote);
        MaterialButtonToggleGroup toggleGroup = v.findViewById(R.id.toggleGroup);

        // 2. Format tiền & setup chips (mượn lại hàm setup đã có)
        editAmount.addTextChangedListener(new CurrencyTextWatcher(editAmount));
        setupQuickAddChips(v, editAmount);

        // 3. Logic Toggle Button
        if (person.totalBalance == 0) {
            toggleGroup.check(R.id.btnLending); // Mặc định cho vay
        } else if (person.totalBalance > 0) {
            toggleGroup.check(R.id.btnLending); // Đang nợ mình -> mặc định cho vay thêm
        } else {
            toggleGroup.check(R.id.btnBorrowing); // Mình đang nợ họ -> mặc định vay thêm
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Ghi khoản nợ mới")
                .setView(v)
                .setPositiveButton("Lưu", null)
                .setNegativeButton("Hủy", null)
                .create();

        dialog.setOnDismissListener(d -> {
            if (pendingAddTransactionDialog == dialog) {
                pendingAddTransactionDialog = null;
            }
        });

        dialog.setOnShowListener(dialogInterface -> {
            android.widget.Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(buttonView -> {
                long amount = com.lendy.app.utils.FormatUtils.parseFormattedNumber(editAmount.getText().toString());
                if (amount <= 0) {
                    editAmount.setError("Vui lòng nhập số tiền hợp lệ");
                    return;
                }

                // Xác định loại giao dịch dựa trên toggle và số dư hiện tại
                com.lendy.app.data.TransactionType type;
                int checkedId = toggleGroup.getCheckedButtonId();

                if (checkedId == R.id.btnLending) {
                    type = (person.totalBalance >= 0) ? com.lendy.app.data.TransactionType.LEND : com.lendy.app.data.TransactionType.PAY_BACK;
                } else {
                    type = (person.totalBalance <= 0) ? com.lendy.app.data.TransactionType.BORROW : com.lendy.app.data.TransactionType.REPAY;
                }

                TransactionRecord record = new TransactionRecord();
                record.personId = person.id;
                record.amount = amount;
                record.type = type;
                record.note = editNote.getText().toString().trim();
                record.timestamp = System.currentTimeMillis();

                positiveButton.setEnabled(false);
                pendingAddTransactionDialog = dialog;
                viewModel.addTransaction(record);
            });
        });

        dialog.show();
    }


    private Long parseAmountSafely(String digits) {
        if (digits == null || digits.isEmpty()) return null;
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

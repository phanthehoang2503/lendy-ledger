package com.lendy.app.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.lendy.app.R;
import com.lendy.app.data.TransactionType;
import com.lendy.app.data.entities.Person;
import com.lendy.app.ui.adapters.MainPagerAdapter;
import com.lendy.app.viewmodel.LendyViewModel;
import com.lendy.app.viewmodel.LendyViewModelFactory;
import com.lendy.app.repository.LendyRepository;

import java.text.NumberFormat;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private LendyViewModel viewModel;
    private ViewPager2 viewPager;
    private BottomNavigationView bottomNav;
    private TextView textTotalLending, textTotalBorrowing;
    private View welcomeContainer, summaryCard;

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
            // Sử dụng post() để chắc chắn bottomNav đã được đo (measured)
            bottomNav.post(() -> {
                v.setPadding(0, 0, 0, bottomNav.getHeight());
            });
            return insets;
        });
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
        LendyRepository repository = new LendyRepository(getApplication());
        viewModel = new ViewModelProvider(this, new LendyViewModelFactory(repository)).get(LendyViewModel.class);

        // Summary vẫn nằm ở MainActivity vì nó cố định ở Top
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

    private void updateHeaderVisibility(int position) {
        // Animation khi ẩn/hiện
        TransitionManager.beginDelayedTransition((ViewGroup) findViewById(R.id.main));
        if (position == 0) {
            welcomeContainer.setVisibility(View.VISIBLE);
            summaryCard.setVisibility(View.VISIBLE);
        } else {
            welcomeContainer.setVisibility(View.GONE);
            summaryCard.setVisibility(View.GONE);
        }
    }

    public void showAddPersonDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_person, null);
        TextInputEditText editName = dialogView.findViewById(R.id.editName);
        TextInputEditText editPhone = dialogView.findViewById(R.id.editPhone);
        TextInputEditText editAmount = dialogView.findViewById(R.id.editAmount);
        TextInputEditText editNote = dialogView.findViewById(R.id.editNote);
        MaterialButtonToggleGroup toggleGroup = dialogView.findViewById(R.id.toggleGroup);

        // Setup Quick Add Chips
        setupQuickAddChips(dialogView, editAmount);

        // Format tiền
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
                        long parsed = Long.parseLong(cleanString);
                        String formatted = com.lendy.app.utils.FormatUtils.formatThousand(parsed);
                        current = formatted;
                        editAmount.setText(formatted);
                        editAmount.setSelection(formatted.length());
                    }
                    editAmount.addTextChangedListener(this);
                }
            }
        });

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

    private void setupQuickAddChips(View dialogView, TextInputEditText editAmount) {
        int[] chipIds = { R.id.chip20k, R.id.chip50k, R.id.chip100k, R.id.chip200k, R.id.chip500k };
        long[] amounts = { 20000, 50000, 100000, 200000, 500000 };

        for (int i = 0; i < chipIds.length; i++) {
            final long amount = amounts[i];
            View chip = dialogView.findViewById(chipIds[i]);
            if (chip != null) {
                chip.setOnClickListener(v -> {
                    String currentStr = editAmount.getText().toString().replaceAll("[^\\d]", "");
                    long currentAmount = currentStr.isEmpty() ? 0 : Long.parseLong(currentStr);
                    long finalAmount = currentAmount + amount;

                    editAmount.setText(com.lendy.app.utils.FormatUtils.formatThousand(finalAmount));
                    editAmount.setSelection(editAmount.getText().length());
                });
            }
        }
    }
}

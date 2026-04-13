package com.lendy.app;

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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.lendy.app.data.TransactionType;
import com.lendy.app.data.entities.Person;
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
            new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.confirm_delete_person_title)
                .setMessage(R.string.confirm_delete_person_message)
                .setPositiveButton("Xóa", (d, w) -> viewModel.removePerson(person))
                .setNegativeButton("Hủy", null)
                .show();
        });
        recyclerView.setAdapter(adapter);

        findViewById(R.id.fabAdd).setOnClickListener(v -> showAddPersonDialog());
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
            if (summary == null) return;
            NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            textTotalLending.setText("+" + nf.format(summary.totalLending != null ? summary.totalLending : 0));
            textTotalBorrowing.setText("-" + nf.format(Math.abs(summary.totalBorrowing != null ? summary.totalBorrowing : 0)));
        });

        viewModel.getErrorObserver().observe(this, event -> {
            String errorMsg = event.getContentIfNotHandled();
            if (errorMsg != null) {
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddPersonDialog() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_add_person, null);
        TextInputEditText editName = v.findViewById(R.id.editName);
        TextInputEditText editPhone = v.findViewById(R.id.editPhone);
        TextInputEditText editAmount = v.findViewById(R.id.editAmount);
        MaterialButtonToggleGroup toggle = v.findViewById(R.id.toggleGroup);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Thêm người nợ mới")
                .setView(v)
                .setPositiveButton("Thêm", (d, w) -> {
                    String name = editName.getText().toString().trim();
                    if (name.isEmpty()) return;
                    long amount = 0;
                    try { amount = Long.parseLong(editAmount.getText().toString()); } catch (Exception e) {}
                    TransactionType type = (toggle.getCheckedButtonId() == R.id.btnBorrowing) 
                                           ? TransactionType.BORROW : TransactionType.LEND;
                    Person p = new Person();
                    p.name = name;
                    p.phoneNumber = editPhone.getText().toString().trim();
                    p.updatedAt = System.currentTimeMillis();
                    viewModel.addPersonWithInitialBalance(p, amount, type);
                })
                .setNegativeButton("Hủy", null).show();
    }
}

package com.lendy.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.lendy.app.data.entities.Person;
import com.lendy.app.repository.LendyRepository;
import com.lendy.app.viewmodel.LendyViewModel;
import com.lendy.app.viewmodel.LendyViewModelFactory;

public class ContactsActivity extends AppCompatActivity {

    private LendyViewModel viewModel;
    private PersonAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_contacts);

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());

        setupRecyclerView();
        setupViewModel();
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerViewContacts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PersonAdapter(person -> {
            // Click: Mở chi tiết giao dịch
            Intent intent = new Intent(this, PersonDetailActivity.class);
            intent.putExtra(PersonDetailActivity.EXTRA_PERSON_ID, person.id);
            intent.putExtra(PersonDetailActivity.EXTRA_PERSON_NAME, person.name);
            intent.putExtra(PersonDetailActivity.EXTRA_PERSON_PHONE, person.phoneNumber);
            startActivity(intent);
        }, this::showEditPersonDialog);
        recyclerView.setAdapter(adapter);
    }

    private void setupViewModel() {
        LendyRepository repository = new LendyRepository(getApplication());
        viewModel = new ViewModelProvider(this, new LendyViewModelFactory(repository)).get(LendyViewModel.class);
        viewModel.getAllPeople().observe(this, people -> adapter.setPeople(people));
    }

    private void showEditPersonDialog(Person person) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_add_person, null);
        v.findViewById(R.id.layoutAmount).setVisibility(View.GONE);
        v.findViewById(R.id.toggleGroup).setVisibility(View.GONE);

        TextInputEditText editName = v.findViewById(R.id.editName);
        TextInputEditText editPhone = v.findViewById(R.id.editPhone);

        editName.setText(person.name);
        editPhone.setText(person.phoneNumber);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.edit_person)
                .setView(v)
                .setPositiveButton("Lưu", (d, w) -> {
                    String name = editName.getText().toString().trim();
                    if (name.isEmpty()) return;
                    person.name = name;
                    person.phoneNumber = editPhone.getText().toString().trim();
                    viewModel.addPerson(person); // upsert
                })
                .setNegativeButton("Hủy", null).show();
    }
}

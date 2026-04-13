package com.lendy.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lendy.app.repository.LendyRepository;
import com.lendy.app.viewmodel.LendyViewModel;
import com.lendy.app.viewmodel.LendyViewModelFactory;

public class CompletedDebtsActivity extends AppCompatActivity {

    private LendyViewModel viewModel;
    private PersonAdapter adapter;
    private TextView textNoData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_completed_debts);

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());
        textNoData = findViewById(R.id.textNoData);

        setupRecyclerView();
        setupViewModel();
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerViewCompleted);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PersonAdapter(person -> {
            Intent intent = new Intent(this, PersonDetailActivity.class);
            intent.putExtra(PersonDetailActivity.EXTRA_PERSON_ID, person.id);
            intent.putExtra(PersonDetailActivity.EXTRA_PERSON_NAME, person.name);
            intent.putExtra(PersonDetailActivity.EXTRA_PERSON_PHONE, person.phoneNumber);
            startActivity(intent);
        }, person -> {
            // Có thể thêm chức năng xóa ở đây nếu muốn
        });
        recyclerView.setAdapter(adapter);
    }

    private void setupViewModel() {
        LendyRepository repository = new LendyRepository(getApplication());
        viewModel = new ViewModelProvider(this, new LendyViewModelFactory(repository)).get(LendyViewModel.class);
        
        viewModel.getCompletedDebts().observe(this, people -> {
            if (people == null || people.isEmpty()) {
                textNoData.setVisibility(View.VISIBLE);
                adapter.setPeople(people);
            } else {
                textNoData.setVisibility(View.GONE);
                adapter.setPeople(people);
            }
        });
    }
}

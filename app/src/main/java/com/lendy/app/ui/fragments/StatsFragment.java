package com.lendy.app.ui.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.lendy.app.R;
import com.lendy.app.data.entities.Person;
import com.lendy.app.data.model.SummaryDTO;
import com.lendy.app.repository.LendyRepository;
import com.lendy.app.ui.adapters.PersonAdapter;
import com.lendy.app.viewmodel.LendyViewModel;
import com.lendy.app.viewmodel.LendyViewModelFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StatsFragment extends Fragment {

    private LendyViewModel viewModel;
    private PieChart pieChart;
    private PersonAdapter topDebtorsAdapter;
    private RecyclerView recyclerViewTopDebtors;
    private View textTopDebtorsTitle, cardTopDebtors;
    private MaterialButtonToggleGroup toggleGroup;
    private boolean showReceivables = true;
    private List<Person> currentPeople = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stats, container, false);

        pieChart = view.findViewById(R.id.pieChart);
        textTopDebtorsTitle = view.findViewById(R.id.textTopDebtorsTitle);
        cardTopDebtors = view.findViewById(R.id.cardTopDebtors);

        setupRecyclerView(view);
        setupViewModel();

        return view;
    }

    private void setupRecyclerView(View view) {
        recyclerViewTopDebtors = view.findViewById(R.id.recyclerViewTopDebtors);
        toggleGroup = view.findViewById(R.id.toggleGroupStats);
        recyclerViewTopDebtors.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        // Adapter mini cho danh sách Top 3
        topDebtorsAdapter = new PersonAdapter(person -> {}, person -> {});
        topDebtorsAdapter.setUseUnifiedColor(true);
        topDebtorsAdapter.setUseClassicColors(true);
        recyclerViewTopDebtors.setAdapter(topDebtorsAdapter);

        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                showReceivables = (checkedId == R.id.btnReceivables);
                refreshTopList();
            }
        });
    }

    private void setupViewModel() {
        if (getActivity() == null)
            return;

        LendyRepository repository = new LendyRepository(getActivity().getApplication());
        viewModel = new ViewModelProvider(requireActivity(), new LendyViewModelFactory(repository))
                .get(LendyViewModel.class);

        // 1. Cập nhật biểu đồ tròn
        viewModel.getGlobalSummary().observe(getViewLifecycleOwner(), summary -> {
            if (summary != null) {
                updateChart(summary);
            }
        });

        // 2. Cập nhật danh sách Top 3
        viewModel.getActiveDebts().observe(getViewLifecycleOwner(), people -> {
            this.currentPeople = people != null ? people : new ArrayList<>();
            refreshTopList();
        });
    }

    private void refreshTopList() {
        boolean hasData = currentPeople != null && !currentPeople.isEmpty();
        if (hasData) {
            List<Person> filteredList = new ArrayList<>();
            for (Person p : currentPeople) {
                if (showReceivables && p.totalBalance > 0) {
                    filteredList.add(p);
                } else if (!showReceivables && p.totalBalance < 0) {
                    filteredList.add(p);
                }
            }

            boolean hasFilteredData = !filteredList.isEmpty();
            textTopDebtorsTitle.setVisibility(hasFilteredData ? View.VISIBLE : View.GONE);
            cardTopDebtors.setVisibility(hasFilteredData ? View.VISIBLE : View.GONE);
            toggleGroup.setVisibility(View.VISIBLE);

            if (hasFilteredData) {
                // Sắp xếp
                if (showReceivables) {
                    // Họ nợ mình:  số lớn nhất lên đầu
                    Collections.sort(filteredList, (p1, p2) -> Double.compare(p2.totalBalance, p1.totalBalance));
                } else {
                    // Mình nợ họ: số âm bé nhất (nợ nhiều nhất) lên đầu
                    Collections.sort(filteredList, (p1, p2) -> Double.compare(p1.totalBalance, p2.totalBalance));
                }

                // Lấy tối đa 3 người
                int limit = Math.min(filteredList.size(), 3);
                topDebtorsAdapter.setPeople(filteredList.subList(0, limit));
            }
        } else {
            textTopDebtorsTitle.setVisibility(View.GONE);
            cardTopDebtors.setVisibility(View.GONE);
            toggleGroup.setVisibility(View.GONE);
            topDebtorsAdapter.setPeople(new ArrayList<>());
        }
    }

    private void updateChart(SummaryDTO summary) {
        float lending = summary.totalLending != null ? summary.totalLending.floatValue() : 0f;
        float borrowing = summary.totalBorrowing != null ? summary.totalBorrowing.floatValue() : 0f;

        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(lending, getString(R.string.label_lend)));
        entries.add(new PieEntry(borrowing, getString(R.string.label_borrow)));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(new int[] {
                ContextCompat.getColor(requireContext(), R.color.classic_receivable),
                ContextCompat.getColor(requireContext(), R.color.classic_payable)
        });
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(14f);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.getDescription().setEnabled(false);
        pieChart.setCenterText(getString(R.string.total_debt_center));
        pieChart.setDrawEntryLabels(false);
        pieChart.animateY(1000);
        pieChart.invalidate();
    }
}

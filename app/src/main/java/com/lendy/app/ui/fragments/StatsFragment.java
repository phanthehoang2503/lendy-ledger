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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stats, container, false);

        pieChart = view.findViewById(R.id.pieChart);
        setupRecyclerView(view);
        setupViewModel();

        return view;
    }

    private void setupRecyclerView(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewTopDebtors);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // Adapter mini cho danh sách Top 3 (Không cần click hay long click phức tạp)
        topDebtorsAdapter = new PersonAdapter(person -> {}, person -> {});
        recyclerView.setAdapter(topDebtorsAdapter);
    }

    private void setupViewModel() {
        if (getActivity() == null) return;

        LendyRepository repository = new LendyRepository(getActivity().getApplication());
        viewModel = new ViewModelProvider(requireActivity(), new LendyViewModelFactory(repository)).get(LendyViewModel.class);

        // 1. Cập nhật biểu đồ tròn
        viewModel.getGlobalSummary().observe(getViewLifecycleOwner(), summary -> {
            if (summary != null) {
                updateChart(summary);
            }
        });

        // 2. Cập nhật danh sách Top 3 (Lấy từ Active Debts và sắp xếp)
        viewModel.getActiveDebts().observe(getViewLifecycleOwner(), people -> {
            if (people != null && !people.isEmpty()) {
                List<Person> sortedList = new ArrayList<>(people);
                // Sắp xếp giảm dần theo nợ (Person.balance)
                Collections.sort(sortedList, (p1, p2) -> Double.compare(Math.abs(p2.totalBalance), Math.abs(p1.totalBalance)));
                
                // Lấy tối đa 3 người
                int limit = Math.min(sortedList.size(), 3);
                topDebtorsAdapter.setPeople(sortedList.subList(0, limit));
            } else {
                topDebtorsAdapter.setPeople(new ArrayList<>());
            }
        });
    }

    private void updateChart(SummaryDTO summary) {
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(summary.totalLending, "Họ nợ mình"));
        entries.add(new PieEntry(summary.totalBorrowing, "Mình nợ họ"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(new int[]{
                ContextCompat.getColor(requireContext(), R.color.receivable),
                ContextCompat.getColor(requireContext(), R.color.payable)
        });
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(14f);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.getDescription().setEnabled(false);
        pieChart.setCenterText("Tổng nợ");
        pieChart.setDrawEntryLabels(false); // Ẩn label đè lên biểu đồ cho sạch
        pieChart.animateY(1000);
        pieChart.invalidate();
    }
}

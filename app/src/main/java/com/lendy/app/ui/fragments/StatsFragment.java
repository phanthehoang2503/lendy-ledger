package com.lendy.app.ui.fragments;

import android.content.Intent;

import com.lendy.app.databinding.FragmentStatsBinding;
import com.lendy.app.ui.activities.PersonDetailActivity;

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

/**
 * StatsFragment - Màn hình thống kê dữ liệu.
 * Chức năng:
 * - Hiển thị biểu đồ tròn (PieChart) so sánh tỷ lệ Cho vay vs Đi vay.
 * - Hiển thị danh sách "Top 3" những người nợ nhiều nhất hoặc mình nợ nhiều nhất.
 */
public class StatsFragment extends Fragment {

    private LendyViewModel viewModel;
    private PersonAdapter topDebtorsAdapter;
    private FragmentStatsBinding binding;
    private boolean showReceivables = true; // Mặc định hiển thị danh sách "Họ nợ mình"
    private List<Person> currentPeople = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        // 1. Khởi tạo ViewBinding
        binding = FragmentStatsBinding.inflate(inflater, container, false);

        // 2. Cài đặt các thành phần UI
        setupRecyclerView();
        setupViewModel();

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * Thiết lập danh sách Top những người nợ.
     */
    private void setupRecyclerView() {
        binding.recyclerViewTopDebtors.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        topDebtorsAdapter = new PersonAdapter(person -> {
            // Khi nhấn vào một người trong danh sách Top: Mở chi tiết nợ
            Intent intent = new Intent(getActivity(), PersonDetailActivity.class);
            intent.putExtra(PersonDetailActivity.EXTRA_PERSON_ID, person.id);
            intent.putExtra(PersonDetailActivity.EXTRA_PERSON_NAME, person.name);
            intent.putExtra(PersonDetailActivity.EXTRA_PERSON_PHONE, person.phoneNumber);
            startActivity(intent);
        }, null);
        binding.recyclerViewTopDebtors.setAdapter(topDebtorsAdapter);

        // Xử lý khi người dùng bấm chuyển giữa tab "Họ nợ mình" và "Mình nợ họ"
        binding.toggleGroupStats.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                showReceivables = (checkedId == R.id.btnReceivables);
                refreshTopList(); // Làm mới danh sách hiển thị
            }
        });
    }

    /**
     * Kết nối dữ liệu từ ViewModel.
     */
    private void setupViewModel() {
        if (getActivity() == null)
            return;

        viewModel = new ViewModelProvider(
                requireActivity(),
                new LendyViewModelFactory(
                        LendyRepository.getInstance(requireActivity().getApplication())))
                .get(LendyViewModel.class);

        // 1. Cập nhật biểu đồ tròn khi số liệu tổng thay đổi
        viewModel.getGlobalSummary().observe(getViewLifecycleOwner(), summary -> {
            if (summary != null) {
                updateChart(summary);
            }
        });

        // 2. Cập nhật danh sách Top 3 khi danh bạ có thay đổi
        viewModel.getActiveDebts().observe(getViewLifecycleOwner(), people -> {
            this.currentPeople = people != null ? people : new ArrayList<>();
            refreshTopList();
        });
    }

    /**
     * Logic lọc và sắp xếp để lấy ra Top 3 người nợ nhiều nhất.
     */
    private void refreshTopList() {
        boolean hasData = currentPeople != null && !currentPeople.isEmpty();
        if (hasData) {
            List<Person> filteredList = new ArrayList<>();
            // Lọc danh sách dựa trên Tab đang chọn
            for (Person p : currentPeople) {
                if (showReceivables && p.totalBalance > 0) {
                    filteredList.add(p);
                } else if (!showReceivables && p.totalBalance < 0) {
                    filteredList.add(p);
                }
            }

            boolean hasFilteredData = !filteredList.isEmpty();
            binding.textTopDebtorsTitle.setVisibility(hasFilteredData ? View.VISIBLE : View.GONE);
            binding.recyclerViewTopDebtors.setVisibility(hasFilteredData ? View.VISIBLE : View.GONE);
            binding.toggleGroupStats.setVisibility(View.VISIBLE);

            if (hasFilteredData) {
                // Thực hiện sắp xếp (Sorting)
                if (showReceivables) {
                    // Họ nợ mình: Những người nợ số tiền lớn nhất lên đầu
                    Collections.sort(filteredList, (p1, p2) -> Double.compare(p2.totalBalance, p1.totalBalance));
                } else {
                    // Mình nợ họ: Những khoản nợ mình âm nhiều nhất (phải trả nhiều nhất) lên đầu
                    Collections.sort(filteredList, (p1, p2) -> Double.compare(p1.totalBalance, p2.totalBalance));
                }

                // Chỉ lấy tối đa 3 người để hiển thị cho gọn
                int limit = Math.min(filteredList.size(), 3);
                topDebtorsAdapter.submitList(filteredList.subList(0, limit));
            }
        } else {
            // Trường hợp không có dữ liệu
            binding.textTopDebtorsTitle.setVisibility(View.GONE);
            binding.recyclerViewTopDebtors.setVisibility(View.GONE);
            binding.toggleGroupStats.setVisibility(View.GONE);
            topDebtorsAdapter.submitList(new ArrayList<>());
        }
    }

    /**
     * Vẽ biểu đồ tròn so sánh tỷ lệ nợ.
     */
    private void updateChart(SummaryDTO summary) {
        float lending = summary.totalLending != null ? summary.totalLending.floatValue() : 0f;
        float borrowing = summary.totalBorrowing != null ? summary.totalBorrowing.floatValue() : 0f;

        if (lending == 0 && borrowing == 0) {
            // Không có nợ nần gì thì ẩn biểu đồ
            binding.pieChart.setVisibility(View.GONE);
            binding.textChartEmpty.setVisibility(View.VISIBLE);
            return;
        } else {
            binding.pieChart.setVisibility(View.VISIBLE);
            binding.textChartEmpty.setVisibility(View.GONE);
        }

        List<PieEntry> entries = new ArrayList<>();
        if (lending > 0) entries.add(new PieEntry(lending, getString(R.string.label_lend)));
        if (borrowing > 0) entries.add(new PieEntry(borrowing, getString(R.string.label_borrow)));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(new int[] {
                ContextCompat.getColor(requireContext(), R.color.classic_receivable),
                ContextCompat.getColor(requireContext(), R.color.classic_payable)
        });
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(14f);

        PieData data = new PieData(dataSet);
        binding.pieChart.setData(data);
        binding.pieChart.getDescription().setEnabled(false);
        binding.pieChart.setCenterText(getString(R.string.total_debt_center));
        binding.pieChart.setDrawEntryLabels(false);
        binding.pieChart.animateY(1000); // Hiệu ứng vẽ biểu đồ xoay tròn
        binding.pieChart.invalidate();
    }
}

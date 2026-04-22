package com.lendy.app.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lendy.app.R;
import com.lendy.app.databinding.FragmentHistoryBinding;
import com.lendy.app.repository.LendyRepository;
import com.lendy.app.ui.activities.PersonDetailActivity;
import com.lendy.app.ui.adapters.TransactionAdapter;
import com.lendy.app.viewmodel.LendyViewModel;
import com.lendy.app.viewmodel.LendyViewModelFactory;

import java.util.ArrayList;

/**
 * HistoryFragment - Màn hình hiển thị toàn bộ lịch sử giao dịch của ứng dụng.
 * Chức năng:
 * - Liệt kê tất cả các giao dịch (Cho vay, Thu nợ, Vay thêm, Trả nợ) của mọi người nợ.
 * - Hiển thị thông báo trống nếu chưa có bất kỳ giao dịch nào được ghi lại.
 */
public class HistoryFragment extends Fragment {

    private LendyViewModel viewModel;
    private TransactionAdapter adapter;
    private FragmentHistoryBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 1. Khởi tạo ViewBinding
        binding = FragmentHistoryBinding.inflate(inflater, container, false);

        // 2. Cài đặt danh sách giao dịch
        setupRecyclerView();
        
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // 3. Kết nối với ViewModel để lấy dữ liệu
        setupViewModel();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Giải phóng bộ nhớ
        binding = null;
    }

    /**
     * Cài đặt danh sách lịch sử giao dịch.
     */
    private void setupRecyclerView() {
        binding.recyclerViewHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        adapter = new TransactionAdapter();
        binding.recyclerViewHistory.setAdapter(adapter);
    }

    /**
     * Kết nối dữ liệu lịch sử từ Database thông qua ViewModel.
     */
    private void setupViewModel() {
        if (getActivity() == null) return;

        viewModel = new ViewModelProvider(
                requireActivity(),
                new LendyViewModelFactory(
                        LendyRepository.getInstance(requireActivity().getApplication())))
                .get(LendyViewModel.class);

        // Lắng nghe danh sách tất cả giao dịch trong hệ thống
        viewModel.getAllTransactions().observe(getViewLifecycleOwner(), transactions -> {
            if (transactions == null || transactions.isEmpty()) {
                // Hiển thị layout trống nếu không có dữ liệu
                binding.layoutEmptyHistory.setVisibility(View.VISIBLE);
            } else {
                binding.layoutEmptyHistory.setVisibility(View.GONE);
            }
            // Đưa dữ liệu vào Adapter để hiển thị lên màn hình
            adapter.submitList(transactions);
        });
    }
}

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

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.lendy.app.R;
import com.lendy.app.databinding.FragmentHomeBinding;
import com.lendy.app.repository.LendyRepository;
import com.lendy.app.ui.activities.PersonDetailActivity;
import com.lendy.app.data.entities.Person;
import com.lendy.app.ui.adapters.PersonAdapter;
import com.lendy.app.utils.PersonDialogHelper;
import com.lendy.app.viewmodel.LendyViewModel;
import com.lendy.app.viewmodel.LendyViewModelFactory;

/**
 * HomeFragment - Trang chủ hiển thị danh sách những người đang có nợ (Active Debts).
 * Chức năng:
 * - Hiển thị tóm tắt các khoản nợ đang hoạt động.
 * - Cho phép bấm nhanh để thêm nợ mới (gọi qua MainActivity).
 * - Xem chi tiết hoặc quản lý thông tin từng người nợ.
 */
public class HomeFragment extends Fragment {

    /**
     * Interface dùng để giao tiếp với Activity chủ (MainActivity)
     * nhằm thực hiện luồng thêm nợ mới.
     */
    public interface AddDebtFlowHost {
        void showAddDebtFlow();
    }

    private LendyViewModel viewModel;
    private PersonAdapter adapter;
    private FragmentHomeBinding binding;
    private AddDebtFlowHost dialogHost;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        // 1. Khởi tạo ViewBinding cho Fragment
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        // 2. Kết nối nút "Thêm nợ nhanh" với Activity chủ
        if (binding.cardQuickAdd != null) {
            binding.cardQuickAdd.setOnClickListener(v -> {
                if (dialogHost != null) {
                    dialogHost.showAddDebtFlow();
                }
            });
        }

        // 3. Cài đặt danh sách (RecyclerView) và Dữ liệu (ViewModel)
        setupRecyclerView();
        setupViewModel();

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Giải phóng binding để tránh rò rỉ bộ nhớ (Memory Leak)
        binding = null;
    }

    /**
     * Thiết lập danh sách hiển thị người nợ.
     */
    private void setupRecyclerView() {
        binding.recyclerViewHome.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new PersonAdapter(person -> {
            // Khi nhấn vào một người: Mở màn hình chi tiết nợ của người đó
            Intent intent = new Intent(getActivity(), PersonDetailActivity.class);
            intent.putExtra(PersonDetailActivity.EXTRA_PERSON_ID, person.id);
            intent.putExtra(PersonDetailActivity.EXTRA_PERSON_NAME, person.name);
            intent.putExtra(PersonDetailActivity.EXTRA_PERSON_PHONE, person.phoneNumber);
            startActivity(intent);
        }, this::showPersonOptionsDialog);

        binding.recyclerViewHome.setAdapter(adapter);
    }

    /**
     * Kết nối và lắng nghe thay đổi dữ liệu từ Database.
     */
    private void setupViewModel() {
        if (getActivity() == null)
            return;

        viewModel = new ViewModelProvider(
                requireActivity(),
                new LendyViewModelFactory(
                        LendyRepository.getInstance(requireActivity().getApplication())))
                .get(LendyViewModel.class);

        // Theo dõi danh sách những người đang có nợ
        viewModel.getActiveDebts().observe(getViewLifecycleOwner(), people -> {
            if (people == null || people.isEmpty()) {
                // Nếu không có ai nợ nần gì thì hiện thông báo trống
                binding.layoutEmptyHome.setVisibility(View.VISIBLE);
            } else {
                binding.layoutEmptyHome.setVisibility(View.GONE);
            }
            adapter.submitList(people);
        });
    }

    /**
     * Hiển thị menu tùy chọn khi nhấn giữ hoặc bấm vào icon menu của một người nợ.
     */
    private void showPersonOptionsDialog(Person person) {
        String[] options = {"Chỉnh sửa thông tin", "Xóa người này"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(person.name)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        PersonDialogHelper.showEditPersonDialog(requireContext(), viewModel, person);
                    } else {
                        showDeleteConfirmation(person);
                    }
                })
                .show();
    }

    /**
     * Hộp thoại xác nhận trước khi xóa một người nợ khỏi hệ thống.
     */
    private void showDeleteConfirmation(Person person) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa '" + person.name + "'? Toàn bộ lịch sử giao dịch sẽ bị mất.")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    viewModel.removePerson(person);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onAttach(@NonNull android.content.Context context) {
        super.onAttach(context);
        if (context instanceof AddDebtFlowHost) {
            dialogHost = (AddDebtFlowHost) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        dialogHost = null;
    }
}

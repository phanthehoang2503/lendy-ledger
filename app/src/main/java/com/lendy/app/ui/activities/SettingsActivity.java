package com.lendy.app.ui.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.lendy.app.R;
import com.lendy.app.databinding.ActivitySettingsBinding;
import com.lendy.app.repository.LendyRepository;

/**
 * SettingsActivity - Màn hình cài đặt của ứng dụng.
 * Hiện tại hỗ trợ tính năng quan trọng nhất là Xóa toàn bộ dữ liệu (Reset app).
 */
public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";
    private ActivitySettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        
        // 1. Khởi tạo ViewBinding
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 2. Thiết lập thanh công cụ (Toolbar)
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.settings_title);
        }

        // 3. Xử lý khoảng cách cho Edge-to-Edge
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 4. Xử lý sự kiện bấm vào nút "Xóa toàn bộ dữ liệu"
        binding.cardClearData.setOnClickListener(v -> {
            // Hiển thị hộp thoại xác nhận trước khi xóa (vì đây là thao tác nguy hiểm)
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.confirm_reset_title)
                    .setMessage(R.string.confirm_reset_message)
                    .setPositiveButton(R.string.action_reset, (dialog, which) -> {
                        // Gọi Repository để thực hiện xóa sạch Database
                        LendyRepository.getInstance(getApplication()).clearAllData(new LendyRepository.ClearDataCallback() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(SettingsActivity.this, getString(R.string.data_cleared_toast), Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(Exception exception) {
                                Log.e(TAG, "Lỗi khi xóa sạch dữ liệu", exception);
                                Toast.makeText(SettingsActivity.this, getString(R.string.data_clear_failed_toast), Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });
    }

    /**
     * Xử lý nút quay lại (Back) trên thanh công cụ.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

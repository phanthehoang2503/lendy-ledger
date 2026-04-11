
package com.lendy.app.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.lendy.app.data.LendyDatabase;
import com.lendy.app.data.dao.TransactionDao;
import com.lendy.app.data.dao.PersonDao;
import com.lendy.app.data.entities.Person;
import com.lendy.app.data.entities.TransactionRecord;
import com.lendy.app.data.model.SummaryDTO;
import androidx.lifecycle.MutableLiveData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/******************************************************************************
 * ../repository/LendyRepository.java - LendyRepository
 * CHỨC NĂNG: Lớp trung gian điều phối dữ liệu giữa Giao diện (UI) và Cơ sở dữ
 * liệu.
 *****************************************************************************/
public class LendyRepository {
    private final TransactionDao transactionDao;
    private final PersonDao personDao;

    private final MutableLiveData<String> errorNotifier = new MutableLiveData<>();

    // Bộ máy chạy ngầm: Giúp thực hiện lưu/xóa dữ liệu mà không làm đứng màn hình
    // điện thoại
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public LendyRepository(Application application) {
        LendyDatabase db = LendyDatabase.getInstance(application);
        transactionDao = db.transactionDao();
        personDao = db.personDao();
    }

    /**
     * TẠO GIAO DỊCH MỚI:
     * Đẩy việc lưu vào bộ máy chạy ngầm (executor).
     */
    public void createTransaction(TransactionRecord record) {
        executor.execute(() -> {
            try {
                transactionDao.addTransaction(record);
            } catch (Exception e) {
                errorNotifier.postValue("Lỗi thêm giao dịch: " + e.getMessage());
            }
        });
    }

    /**
     * CẬP NHẬT GIAO DỊCH:
     * Dùng khi người dùng sửa số tiền hoặc nội dung Record cũ.
     */
    public void updateTransaction(TransactionRecord oldRecord, TransactionRecord newRecord) {
        executor.execute(() -> {
            try {
                transactionDao.updateTransaction(oldRecord, newRecord);
            } catch (Exception e) {
                errorNotifier.postValue("Lỗi cập nhật: " + e.getMessage());
            }
        });
    }

    /**
     * XÓA GIAO DỊCH:
     * Xóa sạch một dòng lịch sử và tự nảy lại tiền nợ.
     */
    public void deleteTransaction(TransactionRecord record) {
        executor.execute(() -> {
            try {
                transactionDao.removeTransaction(record);
            } catch (Exception e) {
                errorNotifier.postValue("Lỗi xóa giao dịch: " + e.getMessage());
            }
        });
    }

    // --- CÁC THAO TÁC VỚI NGƯỜI NỢ (PERSON) ---

    // Thêm hoặc cập nhật một người nợ
    public void upsertPerson(Person person) {
        executor.execute(() -> {
            try {
                personDao.insert(person);
            } catch (Exception e) {
                errorNotifier.postValue("Lỗi lưu thông tin người nợ: " + e.getMessage());
            }
        });
    }

    // Xóa một người khỏi danh sách (Xóa luôn cả lịch sử liên quan)
    public void deletePerson(Person person) {
        executor.execute(() -> {
            try {
                personDao.delete(person);
            } catch (Exception e) {
                errorNotifier.postValue("Lỗi xóa người nợ: " + e.getMessage());
            }
        });
    }

    // --- CÁC HÀM TRUY VẤN DỮ LIỆU (GETTERS) ---
    // Các hàm này trả về LiveData - Một dạng dữ liệu "sống".
    // Khi dữ liệu dưới máy thay đổi, màn hình điện thoại sẽ tự động vẽ lại mà không
    // cần load lại trang.

    // Trả về danh sách những người đang nợ
    public LiveData<List<Person>> getActiveDebts() {
        return personDao.getPeopleWithActiveBalance();
    }

    // Trả về danh sách những người đã trả hết nợ
    public LiveData<List<Person>> getCompletedDebts() {
        return personDao.getPeopleCompleted();
    }

    // Lấy link tới toàn bộ danh sách người nợ
    public LiveData<List<Person>> getAllPeople() {
        return personDao.getAllPeople();
    }

    // Lấy thông tin 1 người cụ thể theo ID
    public LiveData<Person> getPersonById(long id) {
        return personDao.getPersonById(id);
    }

    // Lấy toàn bộ dòng thời gian giao dịch của một người
    public LiveData<List<TransactionRecord>> getTimeline(long personId) {
        return transactionDao.getTimeline(personId);
    }

    // Lấy con số tổng kết cho màn hình chính (Tổng cho vay / Tổng đi vay)
    public LiveData<SummaryDTO> getGlobalSummary() {
        return personDao.getGlobalSummary();
    }

    public LiveData<String> getErrorNotifier() {
        return errorNotifier;
    }
}

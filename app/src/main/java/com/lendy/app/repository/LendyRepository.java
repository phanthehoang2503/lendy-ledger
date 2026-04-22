package com.lendy.app.repository;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.lendy.app.data.LendyDatabase;
import com.lendy.app.data.TransactionType;
import com.lendy.app.data.dao.PersonDao;
import com.lendy.app.data.dao.TransactionDao;
import com.lendy.app.data.entities.Person;
import com.lendy.app.data.entities.TransactionRecord;
import com.lendy.app.data.model.SummaryDTO;
import com.lendy.app.utils.Event;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/******************************************************************************
 * ../repository/LendyRepository.java - LendyRepository
 * Điều phối dữ liệu giữa Giao diện (UI) và Cơ sở dữ liệu.
 * Đóng gói các thao tác nền, đảm bảo tính nguyên tử qua bộ máy chạy ngầm.
 *****************************************************************************/
/**
 * LendyRepository - Lớp quản lý dữ liệu trung tâm của ứng dụng.
 * Nhiệm vụ:
 * - Điều phối dữ liệu giữa Database (Room) và ViewModel.
 * - Đảm bảo các thao tác nặng (ghi file, truy vấn db) luôn chạy ngầm (Background Thread) để app mượt mà.
 * - Xử lý tính nguyên tử (Transaction): Đảm bảo khi lưu một người nợ mới thì khoản nợ của họ cũng phải được lưu thành công, nếu lỗi 1 cái thì hủy cả 2.
 */
public class LendyRepository {

    /** Callback thông báo kết quả kiểm tra sự tồn tại của người nợ */
    public interface PersonExistsCallback {
        void onResult(boolean exists);
        void onError(Exception exception);
    }

    /** Callback thông báo kết quả khi thêm/cập nhật thông tin người nợ */
    public interface PersonUpsertCallback {
        void onSuccess();
        void onDuplicate(); // Trùng tên/SĐT
        void onError(Exception exception);
    }

    /** Callback đặc biệt khi thêm người mới kèm số dư nợ ban đầu */
    public interface PersonWithBalanceCallback {
        void onSuccess();
        void onDuplicate();
        void onError(Exception exception);
    }

    /** Callback thông báo kết quả khi thực hiện reset toàn bộ ứng dụng */
    public interface ClearDataCallback {
        void onSuccess();
        void onError(Exception exception);
    }

    private static final String TAG = "LendyRepository";
    private final LendyDatabase db;
    private final TransactionDao transactionDao;
    private final PersonDao personDao;
    
    // LiveData dùng để thông báo lỗi/thành công về cho UI thông qua ViewModel
    private final MutableLiveData<Event<String>> errorNotifier = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> transactionAddedNotifier = new MutableLiveData<>();
    
    // Executor giúp chạy các lệnh Database ở luồng riêng (không gây lag màn hình)
    private final ExecutorService executor;
    private static LendyRepository instance;

    /** Khởi tạo Repository theo mô hình Singleton */
    private LendyRepository(Application application) {
        this(LendyDatabase.getInstance(application), Executors.newSingleThreadExecutor());
    }

    public static synchronized LendyRepository getInstance(Application application) {
        if (instance == null) {
            instance = new LendyRepository(application);
        }
        return instance;
    }

    @VisibleForTesting
    LendyRepository(LendyDatabase db, ExecutorService executor) {
        this.db = db;
        this.transactionDao = db.transactionDao();
        this.personDao = db.personDao();
        this.executor = executor;
    }

    /**
     * Thêm một người nợ mới cùng với khoản nợ ban đầu của họ.
     * Sử dụng Transaction để đảm bảo nếu tạo giao dịch lỗi thì người đó cũng không được tạo.
     */
    public void addPersonWithBalance(Person person, long amount, TransactionType type, String note) {
        addPersonWithBalance(person, amount, type, note, null);
    }

    public void addPersonWithBalance(Person person, long amount, TransactionType type, String note,
            PersonWithBalanceCallback callback) {
        executor.execute(() -> {
            try {
                db.runInTransaction(() -> {
                    person.totalBalance = 0;
                    long personId = personDao.insert(person);
                    if (personId == -1L) {
                        throw new DuplicatePersonException();
                    }

                    // Nếu có nhập số tiền ban đầu -> Tạo ngay một bản ghi giao dịch
                    if (amount != 0) {
                        TransactionRecord firstRecord = new TransactionRecord();
                        firstRecord.personId = personId;
                        firstRecord.amount = Math.abs(amount);
                        firstRecord.type = type;
                        firstRecord.timestamp = System.currentTimeMillis();
                        firstRecord.note = (note != null && !note.trim().isEmpty()) ? note : "Khởi tạo số nợ";
                        transactionDao.addTransactionInExistingTransaction(firstRecord);
                    }
                });
                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(callback::onSuccess);
                }
            } catch (DuplicatePersonException duplicate) {
                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(callback::onDuplicate);
                }
            } catch (Exception e) {
                postError("Lỗi khởi tạo người nợ.", e);
                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError(e));
                }
            }
        });
    }

    /**
     * Reset toàn bộ dữ liệu ứng dụng về trạng thái ban đầu.
     */
    public void clearAllData() {
        clearAllData(null);
    }

    public void clearAllData(ClearDataCallback callback) {
        executor.execute(() -> {
            try {
                db.runInTransaction(() -> {
                    transactionDao.deleteAllTransactions();
                    personDao.deleteAll();
                });
                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(callback::onSuccess);
                }
            } catch (Exception e) {
                postError("Lỗi khi xóa dữ liệu.", e);
                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError(e));
                }
            }
        });
    }

    /**
     * Tạo một giao dịch mới đơn lẻ.
     */
    public void createTransaction(TransactionRecord record) {
        createTransactions(java.util.Collections.singletonList(record));
    }

    /**
     * Tạo danh sách nhiều giao dịch cùng lúc trong một chu kỳ Transaction.
     */
    public void createTransactions(List<TransactionRecord> records) {
        executor.execute(() -> {
            try {
                db.runInTransaction(() -> {
                    for (TransactionRecord record : records) {
                        transactionDao.addTransactionInExistingTransaction(record);
                    }
                });
                transactionAddedNotifier.postValue(new Event<>(true));
            } catch (Exception e) {
                postError("Lỗi lưu giao dịch.", e);
            }
        });
    }

    /**
     * Chỉnh sửa một giao dịch đã tồn tại.
     */
    public void updateTransaction(TransactionRecord oldRecord, TransactionRecord newRecord) {
        executor.execute(() -> {
            try {
                transactionDao.updateTransaction(oldRecord, newRecord);
            } catch (Exception e) {
                postError("Lỗi cập nhật giao dịch.", e);
            }
        });
    }

    /**
     * Xóa một giao dịch.
     */
    public void deleteTransaction(TransactionRecord record) {
        executor.execute(() -> {
            try {
                transactionDao.removeTransaction(record);
            } catch (Exception e) {
                postError("Lỗi xóa giao dịch.", e);
            }
        });
    }

    /**
     * Lưu thông tin người nợ (Thêm mới hoặc ghi đè).
     */
    public void upsertPerson(Person person) {
        executor.execute(() -> {
            try {
                personDao.insert(person);
            } catch (Exception e) {
                postError("Lỗi lưu người nợ.", e);
            }
        });
    }

    /**
     * Thêm hoặc Cập nhật thông tin người nợ kèm theo thông báo kết quả.
     */
    public void addOrUpdatePersonTransactional(Person person, PersonUpsertCallback callback) {
        executor.execute(() -> {
            try {
                int result = personDao.addOrUpdatePerson(person);
                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (result == PersonDao.RESULT_DUPLICATE) {
                            callback.onDuplicate();
                        } else {
                            callback.onSuccess();
                        }
                    });
                }
            } catch (Exception e) {
                postError("Lỗi lưu người nợ.", e);
                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError(e));
                }
            }
        });
    }

    /**
     * Kiểm tra xem một người đã tồn tại trong Database chưa (Dựa trên Tên + SĐT).
     */
    public void checkActivePersonExists(String name, String phone, PersonExistsCallback callback) {
        executePersonExistenceCheck(() -> personDao.findActivePerson(name, phone) != null, callback);
    }

    /**
     * Kiểm tra trùng lặp nhưng loại trừ một ID cụ thể (dùng khi cập nhật thông tin).
     */
    public void checkActivePersonExistsExceptId(String name, String phone, long excludeId,
            PersonExistsCallback callback) {
        executePersonExistenceCheck(() -> personDao.findActivePersonExceptId(name, phone, excludeId) != null, callback);
    }

    /** Luồng kiểm tra sự tồn tại của người nợ chạy ngầm */
    private void executePersonExistenceCheck(Callable<Boolean> checker, PersonExistsCallback callback) {
        executor.execute(() -> {
            try {
                boolean result = checker.call();
                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onResult(result));
                }
            } catch (Exception e) {
                postError("Lỗi kiểm tra trùng người nợ.", e);
                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError(e));
                }
            }
        });
    }

    /**
     * Xóa một người khỏi danh sách hiển thị (Sử dụng Soft Delete để bảo toàn dữ liệu).
     */
    public void deletePerson(Person person) {
        executor.execute(() -> {
            try {
                personDao.softDelete(person.id);
            } catch (Exception e) {
                postError("Lỗi xóa người nợ.", e);
            }
        });
    }

    /** Ghi log lỗi và đẩy thông báo lỗi về màn hình UI */
    private void postError(String userMessage, Exception e) {
        Log.e(TAG, userMessage, e);
        errorNotifier.postValue(new Event<>(userMessage));
    }

    // --- Các hàm lấy dữ liệu dưới dạng LiveData ---

    public LiveData<List<TransactionRecord>> getAllTransactions() {
        return transactionDao.getAllTransactions();
    }

    public LiveData<List<Person>> getActiveDebts() {
        return personDao.getPeopleWithActiveBalance();
    }

    public LiveData<List<Person>> getCompletedDebts() {
        return personDao.getPeopleCompleted();
    }

    public LiveData<List<Person>> getAllPeople() {
        return personDao.getAllPeople();
    }

    public LiveData<Person> getPersonById(long id) {
        return personDao.getPersonById(id);
    }

    public LiveData<List<TransactionRecord>> getTimeline(long personId) {
        return transactionDao.getTimeline(personId);
    }

    public LiveData<SummaryDTO> getGlobalSummary() {
        return personDao.getGlobalSummary();
    }

    public LiveData<Event<String>> getErrorNotifier() {
        return errorNotifier;
    }

    public LiveData<Event<Boolean>> getTransactionAddedNotifier() {
        return transactionAddedNotifier;
    }

    private static class DuplicatePersonException extends RuntimeException {
    }
}

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
public class LendyRepository {
    public interface PersonExistsCallback {
        void onResult(boolean exists);

        void onError(Exception exception);
    }

    public interface PersonUpsertCallback {
        void onSuccess();

        void onDuplicate();

        void onError(Exception exception);
    }

    public interface PersonWithBalanceCallback {
        void onSuccess();

        void onDuplicate();

        void onError(Exception exception);
    }

    private static final String TAG = "LendyRepository";
    private final LendyDatabase db;
    private final TransactionDao transactionDao;
    private final PersonDao personDao;
    private final MutableLiveData<Event<String>> errorNotifier = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> transactionAddedNotifier = new MutableLiveData<>();
    private final ExecutorService executor;
    private static LendyRepository instance;

    // Default Constructor for App usage
    private LendyRepository(Application application) {
        this(LendyDatabase.getInstance(application), Executors.newSingleThreadExecutor());
    }

    public static synchronized LendyRepository getInstance(Application application) {
        if (instance == null) {
            instance = new LendyRepository(application);
        }
        return instance;
    }

    // Constructor for Testing (DI - DI là gì thì lên mạng search ngen)
    @VisibleForTesting // hàm này cho test thôi khỏi cần trình bày
    LendyRepository(LendyDatabase db, ExecutorService executor) {
        this.db = db;
        this.transactionDao = db.transactionDao();
        this.personDao = db.personDao();
        this.executor = executor;
    }

    // Add person with debt
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

    public void clearAllData() {
        executor.execute(() -> {
            try {
                personDao.deleteAll();
            } catch (Exception e) {
                postError("Lỗi khi xóa dữ liệu.", e);
            }
        });
    }

    public void createTransaction(TransactionRecord record) {
        createTransactions(java.util.Collections.singletonList(record));
    }

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

    public void updateTransaction(TransactionRecord oldRecord, TransactionRecord newRecord) {
        executor.execute(() -> {
            try {
                transactionDao.updateTransaction(oldRecord, newRecord);
            } catch (Exception e) {
                postError("Lỗi cập nhật giao dịch.", e);
            }
        });
    }

    public void deleteTransaction(TransactionRecord record) {
        executor.execute(() -> {
            try {
                transactionDao.removeTransaction(record);
            } catch (Exception e) {
                postError("Lỗi xóa giao dịch.", e);
            }
        });
    }

    public void upsertPerson(Person person) {
        executor.execute(() -> {
            try {
                personDao.insert(person);
            } catch (Exception e) {
                postError("Lỗi lưu người nợ.", e);
            }
        });
    }

    public void addOrUpdatePersonTransactional(Person person, PersonUpsertCallback callback) {
        executor.execute(() -> {
            try {
                int result = personDao.addOrUpdatePerson(person);
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (result == PersonDao.RESULT_DUPLICATE) {
                        callback.onDuplicate();
                    } else {
                        callback.onSuccess();
                    }
                });
            } catch (Exception e) {
                postError("Lỗi lưu người nợ.", e);
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e));
            }
        });
    }

    public void checkActivePersonExists(String name, String phone, PersonExistsCallback callback) {
        executePersonExistenceCheck(() -> personDao.findActivePerson(name, phone) != null, callback);
    }

    public void checkActivePersonExistsExceptId(String name, String phone, long excludeId,
            PersonExistsCallback callback) {
        executePersonExistenceCheck(() -> personDao.findActivePersonExceptId(name, phone, excludeId) != null, callback);
    }

    private void executePersonExistenceCheck(Callable<Boolean> checker, PersonExistsCallback callback) {
        executor.execute(() -> {
            try {
                boolean result = checker.call();
                new Handler(Looper.getMainLooper()).post(() -> callback.onResult(result));
            } catch (Exception e) {
                postError("Lỗi kiểm tra trùng người nợ.", e);
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e));
            }
        });
    }

    public void deletePerson(Person person) {
        executor.execute(() -> {
            try {
                personDao.softDelete(person.id);
            } catch (Exception e) {
                postError("Lỗi xóa người nợ.", e);
            }
        });
    }

    private void postError(String userMessage, Exception e) {
        Log.e(TAG, userMessage, e);
        errorNotifier.postValue(new Event<>(userMessage));
    }

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

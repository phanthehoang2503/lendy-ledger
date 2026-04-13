
package com.lendy.app.repository;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;
import com.lendy.app.data.LendyDatabase;
import com.lendy.app.data.dao.TransactionDao;
import com.lendy.app.data.dao.PersonDao;
import com.lendy.app.utils.Event;
import com.lendy.app.data.entities.Person;
import com.lendy.app.data.entities.TransactionRecord;
import com.lendy.app.data.model.SummaryDTO;
import com.lendy.app.data.TransactionType;
import androidx.lifecycle.MutableLiveData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LendyRepository {
    private static final String TAG = "LendyRepository";
    private final TransactionDao transactionDao;
    private final PersonDao personDao;
    private final MutableLiveData<Event<String>> errorNotifier = new MutableLiveData<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public LendyRepository(Application application) {
        LendyDatabase db = LendyDatabase.getInstance(application);
        transactionDao = db.transactionDao();
        personDao = db.personDao();
    }

    public void addPersonWithBalance(Person person, long amount, TransactionType type) {
        executor.execute(() -> {
            try {
                person.totalBalance = 0;
                long personId = personDao.insert(person);
                
                if (amount != 0) {
                    TransactionRecord firstRecord = new TransactionRecord();
                    firstRecord.personId = personId;
                    firstRecord.amount = Math.abs(amount);
                    firstRecord.type = type;
                    firstRecord.timestamp = System.currentTimeMillis();
                    firstRecord.note = "Khởi tạo số nợ";
                    transactionDao.addTransaction(firstRecord);
                }
            } catch (Exception e) {
                postError("Lỗi khởi tạo người nợ.", e);
            }
        });
    }

    // HÀM XÓA TẤT CẢ DỮ LIỆU
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
        executor.execute(() -> {
            try {
                transactionDao.addTransaction(record);
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
                postError("Lỗi lưu người nợ.", e);            }
        });
    }

    public void deletePerson(Person person) {
        executor.execute(() -> {
            try {
                personDao.delete(person);
            } catch (Exception e) {
                postError("Lỗi xóa người nợ.", e);            }
        });
    }

    private void postError(String userMessage, Exception e) {
        Log.e(TAG, userMessage, e);
        errorNotifier.postValue(new Event<>(userMessage));
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
}

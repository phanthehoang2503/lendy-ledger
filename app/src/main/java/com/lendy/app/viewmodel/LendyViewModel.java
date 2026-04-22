package com.lendy.app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.lendy.app.data.TransactionType;
import com.lendy.app.utils.Event;
import com.lendy.app.data.entities.Person;
import com.lendy.app.data.entities.TransactionRecord;
import com.lendy.app.data.model.SummaryDTO;
import com.lendy.app.repository.LendyRepository;

import java.util.List;
import java.util.Objects;

import lombok.Getter;

/**
 * LendyViewModel - Bộ não điều phối dữ liệu giữa Giao diện (UI) và Kho lưu trữ (Repository).
 * Chức năng:
 * - Cung cấp các dòng dữ liệu LiveData để UI tự động cập nhật (Reactive UI).
 * - Chuyển tiếp các yêu cầu thêm/sửa/xóa từ UI tới Repository.
 * - Quản lý trạng thái thông báo lỗi hoặc thông báo thành công cho UI.
 */
public class LendyViewModel extends ViewModel {
    private final LendyRepository repository;
    
    @Getter
    private final LiveData<List<Person>> activeDebts; // Danh sách những người đang có nợ
    @Getter
    private final LiveData<List<Person>> completedDebts; // Danh sách những người đã tất toán
    @Getter
    private final LiveData<SummaryDTO> globalSummary; // Tóm tắt tổng nợ cho vay và đi vay
    @Getter
    private final LiveData<Event<String>> errorObserver; // Quan sát các thông báo lỗi phát sinh
    @Getter
    private final LiveData<Event<Boolean>> transactionAddedObserver; // Thông báo khi một giao dịch đã được thêm thành công

    public LendyViewModel(LendyRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.activeDebts = repository.getActiveDebts();
        this.completedDebts = repository.getCompletedDebts();
        this.globalSummary = repository.getGlobalSummary();
        this.errorObserver = repository.getErrorNotifier();
        this.transactionAddedObserver = repository.getTransactionAddedNotifier();
    }

    /**
     * Lấy toàn bộ danh sách người nợ (bao gồm cả người đã xóa nếu dùng Soft Delete).
     */
    public LiveData<List<Person>> getAllPeople() {
        return repository.getAllPeople();
    }

    /**
     * Lấy lịch sử giao dịch (Timeline) của một cá nhân cụ thể.
     */
    public LiveData<List<TransactionRecord>> getTimeline(long personId) {
        return repository.getTimeline(personId);
    }

    /**
     * Lấy toàn bộ lịch sử giao dịch của tất cả mọi người trong hệ thống.
     */
    public LiveData<List<TransactionRecord>> getAllTransactions() {
        return repository.getAllTransactions();
    }

    /**
     * Tìm kiếm thông tin một người nợ theo ID.
     */
    public LiveData<Person> getPersonById(long id) {
        return repository.getPersonById(id);
    }

    /**
     * Thêm một bản ghi giao dịch mới.
     */
    public void addTransaction(TransactionRecord record) {
        repository.createTransaction(record);
    }

    /**
     * Thêm danh sách nhiều giao dịch cùng lúc.
     */
    public void addTransactions(List<TransactionRecord> records) {
        repository.createTransactions(records);
    }

    /**
     * Cập nhật thông tin một giao dịch cũ thành giao dịch mới.
     */
    public void updateTransaction(TransactionRecord oldRecord, TransactionRecord newRecord) {
        repository.updateTransaction(oldRecord, newRecord);
    }

    /**
     * Xóa một bản ghi giao dịch.
     */
    public void deleteTransaction(TransactionRecord record) {
        repository.deleteTransaction(record);
    }

    /**
     * Thêm một người mới vào danh bạ kèm theo khoản nợ khởi tạo đầu tiên.
     */
    public void addPersonWithInitialBalance(Person person, long amount, TransactionType type, String note) {
        repository.addPersonWithBalance(person, amount, type, note);
    }

    /**
     * Thêm một người mới kèm số dư khởi tạo và lắng nghe kết quả qua callback.
     */
    public void addPersonWithInitialBalance(Person person, long amount, TransactionType type, String note,
            LendyRepository.PersonWithBalanceCallback callback) {
        repository.addPersonWithBalance(person, amount, type, note, callback);
    }

    /**
     * Thêm mới hoặc Cập nhật thông tin một người nợ.
     */
    public void addPerson(Person person) {
        repository.upsertPerson(person);
    }

    /**
     * Cập nhật thông tin cá nhân.
     */
    public void updatePerson(Person person) {
        repository.upsertPerson(person);
    }

    /**
     * Thêm hoặc Cập nhật thông tin người nợ trong một Transaction (Đảm bảo an toàn dữ liệu).
     */
    public void addOrUpdatePersonTransactional(Person person, LendyRepository.PersonUpsertCallback callback) {
        repository.addOrUpdatePersonTransactional(person, callback);
    }

    /**
     * Kiểm tra xem một người (theo tên hoặc SĐT) đã tồn tại trong danh sách nợ hiện tại chưa.
     */
    public void checkActivePersonExists(String name, String phone, LendyRepository.PersonExistsCallback callback) {
        repository.checkActivePersonExists(name, phone, callback);
    }

    /**
     * Kiểm tra sự tồn tại của một người nhưng loại trừ một ID cụ thể (Dùng khi cập nhật thông tin).
     */
    public void checkActivePersonExistsExceptId(String name, String phone, long excludeId,
            LendyRepository.PersonExistsCallback callback) {
        repository.checkActivePersonExistsExceptId(name, phone, excludeId, callback);
    }

    /**
     * Xóa một người khỏi hệ thống (Đánh dấu xóa hoặc xóa vĩnh viễn tùy cấu hình).
     */
    public void removePerson(Person person) {
        repository.deletePerson(person);
    }

    /**
     * Xóa sạch toàn bộ dữ liệu trong Database (Dùng cho tính năng Reset App).
     */
    public void clearAllData() {
        repository.clearAllData();
    }
}

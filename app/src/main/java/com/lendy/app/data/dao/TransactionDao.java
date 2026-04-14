
package com.lendy.app.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import com.lendy.app.data.entities.Person;
import com.lendy.app.data.entities.TransactionRecord;
import java.util.List;

/******************************************************************************
 * ../data/dao/TransactionDao.java - TransactionDao
 * CHỨC NĂNG: Xử lý các phép tính toán cộng/trừ tiền và lưu lịch sử giao dịch.
 *****************************************************************************/
@Dao
public abstract class TransactionDao {
    // Lưu một giao dịch mới vào máy
    @Insert
    public abstract long insertRecord(TransactionRecord record);

    // Cập nhật thông tin một giao dịch đã có
    @Update
    public abstract int updateRecord(TransactionRecord record);

    @Update
    public abstract void updateRecords(List<TransactionRecord> records);

    // Xóa một giao dịch khỏi máy
    @Delete
    public abstract int deleteRecord(TransactionRecord record);

    // Lấy thông tin một giao dịch cụ thể
    @Query("SELECT * FROM transactions WHERE id = :id")
    public abstract TransactionRecord getRecordByIdSync(long id);

    // Tìm một người trong danh bạ bằng ID
    @Query("SELECT * FROM people WHERE id = :personId")
    public abstract Person getPersonSync(long personId);

    @Update
    public abstract void updatePerson(Person person);

    // Lấy toàn bộ lịch sử giao dịch (Toàn app), cái mới nhất hiện lên đầu
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC")
    public abstract LiveData<List<TransactionRecord>> getAllTransactions();

    // Lấy toàn bộ lịch sử giao dịch của một người, cái mới nhất hiện lên đầu
    @Query("SELECT * FROM transactions WHERE personId = :personId ORDER BY timestamp DESC, id DESC")
    public abstract LiveData<List<TransactionRecord>> getTimeline(long personId);

    // Lấy toàn bộ lịch sử giao dịch của một người theo thứ tự thời gian (phục vụ tính toán lại)
    @Query("SELECT * FROM transactions WHERE personId = :personId ORDER BY timestamp ASC, id ASC")
    public abstract List<TransactionRecord> getTimelineSync(long personId);

    /**
     * Khi gọi hàm này, máy sẽ vừa lưu lịch sử, vừa tự cộng/trừ tiền vào ví người
     * đó.
     * 
     * @Transaction đảm bảo nếu một trong hai việc bị lỗi thì máy sẽ không làm gì
     *              cả.
     */
    @Transaction
    public void addTransaction(TransactionRecord record) {
        Person person = getPersonSync(record.personId);
        if (person == null) {
            throw new IllegalStateException("Không tìm thấy người có mã ID " + record.personId);
        }
        if (person.isDeleted) {
            throw new IllegalStateException("Không thể thêm giao dịch cho người đã bị xóa.");
        }

        long delta = calculateDelta(record);
        
        // Cập nhật số dư trong Object person trước
        person.totalBalance += delta;
        person.updatedAt = System.currentTimeMillis();
        
        // Lưu Snapshot vào giao dịch để sau này tra cứu "Số dư lúc đó"
        record.balanceSnapshot = person.totalBalance;
        record.personNameSnapshot = person.name;

        insertRecord(record);
        updatePerson(person);
    }

    /**
     * Khi xóa một dòng lịch sử, máy sẽ tự động "hoàn nguyên" lại số tiền cũ.
     */
    @Transaction
    public void removeTransaction(TransactionRecord record) {
        // Lấy dữ liệu thật từ DB để đảm bảo không bị sai lệch số dư nếu object truyền
        // vào bị cũ (stale)
        TransactionRecord persistedRecord = getRecordByIdSync(record.id);
        if (persistedRecord == null) {
            throw new IllegalStateException("Không tìm thấy giao dịch mã " + record.id + " để xóa.");
        }

        int deleted = deleteRecord(persistedRecord);
        if (deleted != 1) {
            throw new IllegalStateException(
                    "Không thể xóa giao dịch mã " + record.id + ". Dữ liệu có thể đã bị thay đổi.");
        }

        adjustBalance(persistedRecord.personId, -calculateDelta(persistedRecord));
        
        // Cập nhật lại snapshot sau khi xóa
        recomputeSnapshotsForPerson(persistedRecord.personId);
    }

    /**
     * Đây là hàm phức tạp nhất. Nó sẽ:
     * 1. Trừ đi số tiền cũ đã lỡ cộng/trừ trước đó.
     * 2. Cộng lại số tiền mới vừa sửa.
     * 3. Lưu lại nội dung mới.
     */
    @Transaction
    public void updateTransaction(TransactionRecord oldRecord, TransactionRecord newRecord) {
        if (oldRecord.id != newRecord.id) {
            throw new IllegalArgumentException("Không thể cập nhật: ID giao dịch cũ và mới không khớp.");
        }

        // Lấy dữ liệu cũ từ DB để tính toán chênh lệch
        TransactionRecord persistedOldRecord = getRecordByIdSync(oldRecord.id);
        if (persistedOldRecord == null) {
            throw new IllegalStateException("Không tìm thấy giao dịch cũ mã " + oldRecord.id);
        }

        // 1. Hoàn nguyên tiền cũ
        adjustBalance(persistedOldRecord.personId, -calculateDelta(persistedOldRecord));

        // 2. Áp dụng tiền mới
        adjustBalance(newRecord.personId, calculateDelta(newRecord));

        // 3. Lưu dữ liệu mới
        int updated = updateRecord(newRecord);
        if (updated != 1) {
            throw new IllegalStateException("Không thể cập nhật giao dịch. Bản ghi có thể đã bị xóa.");
        }
        
        recomputeSnapshotsForPerson(newRecord.personId);
        if (oldRecord.personId != newRecord.personId) {
            recomputeSnapshotsForPerson(oldRecord.personId);
        }
    }

    /**
     * Tìm người đó trong máy, lấy số dư cũ cộng thêm phần chênh lệch (delta).
     */
    private void adjustBalance(long personId, long delta) {
        Person person = getPersonSync(personId);
        if (person == null) {
            // Nếu không tìm thấy người này, máy sẽ báo lỗi để tránh làm sai nợ
            throw new IllegalStateException(
                    "Không tìm thấy người có mã ID " + personId + ". Dữ liệu có thể đã bị lỗi!");
        }
        if (person.isDeleted) {
            throw new IllegalStateException("Không thể thay đổi số dư của người đã bị xóa.");
        }
        person.totalBalance += delta;
        person.updatedAt = System.currentTimeMillis(); // Ghi nhận thời gian vừa cập nhật
        updatePerson(person);
    }

    /**
     * Tính toán lại toàn bộ snapshot cho một người để đảm bảo tính nhất quán khi có sửa/xóa.
     */
    @Transaction
    public void recomputeSnapshotsForPerson(long personId) {
        Person person = getPersonSync(personId);
        if (person == null) return;

        List<TransactionRecord> timeline = getTimelineSync(personId);
        java.util.List<TransactionRecord> recordsToUpdate = new java.util.ArrayList<>();
        long currentBalance = 0;

        for (TransactionRecord record : timeline) {
            currentBalance += calculateDelta(record);
            
            // Chỉ cập nhật nếu snapshot bị sai khác để tối ưu hóa việc ghi đĩa
            if (record.balanceSnapshot == null || record.balanceSnapshot != currentBalance || 
                !person.name.equals(record.personNameSnapshot)) {
                
                record.balanceSnapshot = currentBalance;
                record.personNameSnapshot = person.name;
                recordsToUpdate.add(record);
            }
        }

        // Cập nhật hàng loạt (Batch update) để tăng tốc độ
        if (!recordsToUpdate.isEmpty()) {
            updateRecords(recordsToUpdate);
        }

        // Cập nhật lại số dư cuối cùng vào bảng people phòng trường hợp lệch
        if (person.totalBalance != currentBalance) {
            person.totalBalance = currentBalance;
            updatePerson(person);
        }
    }

    private long calculateDelta(TransactionRecord record) {
        if (record.amount < 0) {
            // Không chấp nhận số tiền âm để tránh làm sai lệch logic sổ sách
            throw new IllegalArgumentException("Số tiền không được âm cho giao dịch mã " + record.id);
        }

        switch (record.type) {
            case LEND:
                return record.amount; // Cho vay thêm -> Nợ tăng (+)
            case REPAY:
                return -record.amount; // Bạn trả bớt -> Nợ giảm (-)
            case BORROW:
                return -record.amount; // Tôi vay -> Số dư nợ âm (-)
            case PAY_BACK:
                return record.amount; // Tôi trả -> Số dư âm tiến về 0 (+)
            default:
                throw new IllegalArgumentException(
                        "Loại giao dịch lạ (" + record.type + ") cho giao dịch mã " + record.id);
        }
    }
}

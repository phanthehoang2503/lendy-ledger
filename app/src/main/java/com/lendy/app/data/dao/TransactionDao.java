
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

    // Xóa một giao dịch khỏi máy
    @Delete
    public abstract int deleteRecord(TransactionRecord record);

    // Tìm một người trong danh bạ bằng ID
    @Query("SELECT * FROM people WHERE id = :personId")
    public abstract Person getPersonSync(long personId);

    // Cập nhật số dư mới cho một người
    @Update
    public abstract void updatePerson(Person person);

    // Lấy toàn bộ lịch sử giao dịch của một người, cái mới nhất hiện lên đầu
    @Query("SELECT * FROM transactions WHERE personId = :personId ORDER BY timestamp DESC")
    public abstract LiveData<List<TransactionRecord>> getTimeline(long personId);

    /**
     * Khi gọi hàm này, máy sẽ vừa lưu lịch sử, vừa tự cộng/trừ tiền vào ví người
     * đó.
     * 
     * @Transaction đảm bảo nếu một trong hai việc bị lỗi thì máy sẽ không làm gì
     *              cả.
     */
    @Transaction
    public void addTransaction(TransactionRecord record) {
        insertRecord(record);
        adjustBalance(record.personId, calculateDelta(record));
    }

    /**
     * Khi xóa một dòng lịch sử, máy sẽ tự động "hoàn nguyên" lại số tiền cũ.
     */
    @Transaction
    public void removeTransaction(TransactionRecord record) {
        int deleted = deleteRecord(record);
        // Nếu không xóa được (có thể do ID không tồn tại hoặc dữ liệu bị thay đổi),
        // ném lỗi ngay để không nảy tiền sai.
        if (deleted != 1) {
            throw new IllegalStateException(
                    "Không thể xóa giao dịch mã " + record.id + ". Dữ liệu có thể đã bị thay đổi.");
        }
        adjustBalance(record.personId, -calculateDelta(record));
    }

    /**
     * Đây là hàm phức tạp nhất. Nó sẽ:
     * 1. Trừ đi số tiền cũ đã lỡ cộng/trừ trước đó.
     * 2. Cộng lại số tiền mới vừa sửa.
     * 3. Lưu lại nội dung mới.
     */
    @Transaction
    public void updateTransaction(TransactionRecord oldRecord, TransactionRecord newRecord) {
        // Hoàn nguyên tiền cũ
        adjustBalance(oldRecord.personId, -calculateDelta(oldRecord));
        // Áp dụng tiền mới
        adjustBalance(newRecord.personId, calculateDelta(newRecord));

        int updated = updateRecord(newRecord);
        if (updated != 1) {
            throw new IllegalStateException(
                    "Không thể cập nhật giao dịch. Dữ liệu có thể đã bị thay đổi hoặc không tồn tại.");
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
        person.totalBalance += delta;
        person.updatedAt = System.currentTimeMillis(); // Ghi nhận thời gian vừa cập nhật
        updatePerson(person);
    }

    private long calculateDelta(TransactionRecord record) {
        switch (record.type) {
            case LEND:
                return record.amount; // Cho vay thêm -> Nợ tăng (+)
            case REPAY:
                return -record.amount; // Họ trả bớt -> Nợ giảm (-)
            case BORROW:
                return -record.amount; // Mình đi vay -> Số dư nợ âm (-)
            case PAY_BACK:
                return record.amount; // Mình trả nợ -> Số dư âm tiến về 0 (+)
            default:
                // Nếu gặp một loại giao dịch lạ chưa được định nghĩa, báo lỗi ngay để tránh sai
                // sổ sách.
                throw new IllegalArgumentException(
                        "Loại giao dịch lạ (" + record.type + ") cho giao dịch mã " + record.id);
        }
    }
}

package com.lendy.app.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.lendy.app.data.entities.Person;
import com.lendy.app.data.model.SummaryDTO;

import java.util.List;

/******************************************************************************
 * ../data/dao/PersonDao.java - PersonDao
 * CHỨC NĂNG: Quản lý danh sách người nợ và tính toán tổng số tiền toàn bộ app.
 *****************************************************************************/
@Dao
public interface PersonDao {
    // Thêm một người mới, nếu trùng ID thì ghi đè lên cái cũ
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Person person);

    // Cập nhật thông tin của một người
    @Update
    void update(Person person);

    // Xóa một người khỏi danh sách
    @Delete
    void delete(Person person);

    // Lấy toàn bộ danh sách người nợ, sắp xếp theo tên A-Z
    @Query("SELECT * FROM people ORDER BY name ASC")
    LiveData<List<Person>> getAllPeople();

    // Lấy những người đang có nợ để hiện ở màn hình chính
    @Query("SELECT * FROM people WHERE totalBalance != 0 ORDER BY updatedAt DESC")
    LiveData<List<Person>> getPeopleWithActiveBalance();

    // Lấy những người đã trả hết nợ
    @Query("SELECT * FROM people WHERE totalBalance == 0 ORDER BY updatedAt DESC")
    LiveData<List<Person>> getPeopleCompleted();

    // Tìm thông tin một người theo ID
    @Query("SELECT * FROM people WHERE id = :id")
    LiveData<Person> getPersonById(long id);

    /**
     * TRUY VẤN TỔNG KẾT:
     * Máy sẽ duyệt qua toàn bộ danh sách để tính:
     * 1. Tổng tiền người ta nợ mình (LENDING): Những người có số dư > 0.
     * 2. Tổng tiền mình nợ người ta (BORROWING): Những người có số dư < 0 (Lấy giá
     * trị tuyệt đối).
     */
    @Query("SELECT " +
            "SUM(CASE WHEN totalBalance > 0 THEN totalBalance ELSE 0 END) as totalLending, " +
            "SUM(CASE WHEN totalBalance < 0 THEN ABS(totalBalance) ELSE 0 END) as totalBorrowing " +
            "FROM people")
    LiveData<SummaryDTO> getGlobalSummary();
}

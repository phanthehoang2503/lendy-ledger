
package com.lendy.app.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.lendy.app.data.dao.TransactionDao;
import com.lendy.app.data.dao.PersonDao;
import com.lendy.app.data.entities.Person;
import com.lendy.app.data.entities.TransactionRecord;

/******************************************************************************
 * ../data/LendyDatabase.java - LendyDatabase
 * CHỨC NĂNG: Khởi tạo và quản lý toàn bộ Cơ sở dữ liệu Room của ứng dụng.
 *****************************************************************************/
@Database(entities = { Person.class, TransactionRecord.class }, version = 1)
public abstract class LendyDatabase extends RoomDatabase {
    private static LendyDatabase instance;

    public abstract TransactionDao transactionDao();

    public abstract PersonDao personDao();

    /**
     * HÀM LẤY KẾT NỐI:
     * Đảm bảo app chỉ mở duy nhất 1 "cửa" vào Database để tránh xung đột dữ liệu.
     */
    public static synchronized LendyDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    LendyDatabase.class, "lendy_db")
                    .build();
        }
        return instance;
    }
}

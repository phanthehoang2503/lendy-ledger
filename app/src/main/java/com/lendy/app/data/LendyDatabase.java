
package com.lendy.app.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.lendy.app.data.dao.TransactionDao;
import com.lendy.app.data.dao.PersonDao;
import com.lendy.app.data.entities.Person;
import com.lendy.app.data.entities.TransactionRecord;

/******************************************************************************
 * ../data/LendyDatabase.java - LendyDatabase
 * CHỨC NĂNG: Khởi tạo và quản lý toàn bộ Cơ sở dữ liệu Room của ứng dụng.
 *****************************************************************************/
@Database(entities = { Person.class, TransactionRecord.class }, version = 2)
public abstract class LendyDatabase extends RoomDatabase {
    private static LendyDatabase instance;

    public abstract TransactionDao transactionDao();

    public abstract PersonDao personDao();

    /**
     * DI CƯ DỮ LIỆU: Từ v1 sang v2
     * Thêm cột isDeleted cho Person và balanceSnapshot, personNameSnapshot cho TransactionRecord.
     */
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Thêm cột cho bảng people
            database.execSQL("ALTER TABLE people ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0");
            
            // Thêm cột cho bảng transactions
            database.execSQL("ALTER TABLE transactions ADD COLUMN balanceSnapshot INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE transactions ADD COLUMN personNameSnapshot TEXT");
        }
    };

    /**
     * HÀM LẤY KẾT NỐI:
     * Đảm bảo app chỉ mở duy nhất 1 "cửa" vào Database để tránh xung đột dữ liệu.
     */
    public static synchronized LendyDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    LendyDatabase.class, "lendy_db")
                    .addMigrations(MIGRATION_1_2)
                    .build();
        }
        return instance;
    }
}

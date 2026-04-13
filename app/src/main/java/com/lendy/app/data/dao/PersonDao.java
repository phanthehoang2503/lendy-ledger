package com.lendy.app.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Upsert;
import com.lendy.app.data.entities.Person;
import com.lendy.app.data.model.SummaryDTO;

import java.util.List;

@Dao
public interface PersonDao {
    @Upsert
    long insert(Person person);

    @Update
    void update(Person person);

    @Delete
    void delete(Person person);

    // LỆNH XÓA SẠCH DỮ LIỆU
    @Query("DELETE FROM people")
    void deleteAll();

    @Query("SELECT * FROM people ORDER BY name ASC")
    LiveData<List<Person>> getAllPeople();

    @Query("SELECT * FROM people WHERE totalBalance != 0 ORDER BY updatedAt DESC")
    LiveData<List<Person>> getPeopleWithActiveBalance();

    @Query("SELECT * FROM people WHERE totalBalance == 0 ORDER BY updatedAt DESC")
    LiveData<List<Person>> getPeopleCompleted();

    @Query("SELECT * FROM people WHERE id = :id")
    LiveData<Person> getPersonById(long id);

    @Query("SELECT " +
            "COALESCE(SUM(CASE WHEN totalBalance > 0 THEN totalBalance ELSE 0 END), 0) as totalLending, " +
            "COALESCE(SUM(CASE WHEN totalBalance < 0 THEN ABS(totalBalance) ELSE 0 END), 0) as totalBorrowing " +
            "FROM people")
    LiveData<SummaryDTO> getGlobalSummary();
}

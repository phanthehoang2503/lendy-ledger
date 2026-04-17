package com.lendy.app.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import androidx.room.Upsert;
import com.lendy.app.data.entities.Person;
import com.lendy.app.data.model.SummaryDTO;

import java.util.List;

@Dao
public interface PersonDao {
    int RESULT_SUCCESS = 1;
    int RESULT_DUPLICATE = 0;

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Person person);

    @Update
    void update(Person person);

    @Query("UPDATE people SET isDeleted = 1 WHERE id = :id")
    void softDelete(long id);

    @Query("DELETE FROM people")
    void deleteAll();

    @Query("SELECT * FROM people WHERE isDeleted = 0 ORDER BY name ASC")
    LiveData<List<Person>> getAllPeople();

    @Query("SELECT * FROM people WHERE isDeleted = 0 AND totalBalance != 0 ORDER BY updatedAt DESC")
    LiveData<List<Person>> getPeopleWithActiveBalance();

    @Query("SELECT * FROM people WHERE isDeleted = 0 AND totalBalance == 0 ORDER BY updatedAt DESC")
    LiveData<List<Person>> getPeopleCompleted();

    @Query("SELECT * FROM people WHERE id = :id AND isDeleted = 0")
    LiveData<Person> getPersonById(long id);

    @Query("SELECT * FROM people WHERE id = :id")
    LiveData<Person> getPersonByIdIncludingDeleted(long id);

    @Query("SELECT * FROM people WHERE name = :name AND phoneNumber = :phone AND isDeleted = 0 LIMIT 1")
    Person findActivePerson(String name, String phone);

    @Query("SELECT * FROM people WHERE name = :name AND phoneNumber = :phone AND isDeleted = 0 AND id != :excludeId LIMIT 1")
    Person findActivePersonExceptId(String name, String phone, long excludeId);

    @Query("SELECT * FROM people WHERE name = :name AND phoneNumber = :phone LIMIT 1")
    Person findPersonByNameAndPhone(String name, String phone);

    @Query("SELECT * FROM people WHERE name = :name AND phoneNumber = :phone AND id != :excludeId LIMIT 1")
    Person findPersonByNameAndPhoneExceptId(String name, String phone, long excludeId);

    @Transaction
    default int addOrUpdatePerson(Person person) {
        if (person.id > 0) {
            Person duplicate = findPersonByNameAndPhoneExceptId(person.name, person.phoneNumber, person.id);
            if (duplicate != null) {
                return RESULT_DUPLICATE;
            }
            update(person);
            return RESULT_SUCCESS;
        }

        Person existing = findPersonByNameAndPhone(person.name, person.phoneNumber);
        if (existing != null) {
            if (existing.isDeleted) {
                existing.isDeleted = false;
                existing.updatedAt = person.updatedAt;
                existing.phoneNumber = person.phoneNumber;
                existing.name = person.name;
                update(existing);
                return RESULT_SUCCESS;
            }
            return RESULT_DUPLICATE;
        }

        long rowId = insert(person);
        return rowId == -1L ? RESULT_DUPLICATE : RESULT_SUCCESS;
    }

    @Query("SELECT " +
            "COALESCE(SUM(CASE WHEN totalBalance > 0 THEN totalBalance ELSE 0 END), 0) as totalLending, " +
            "COALESCE(SUM(CASE WHEN totalBalance < 0 THEN ABS(totalBalance) ELSE 0 END), 0) as totalBorrowing " +
            "FROM people WHERE isDeleted = 0")
    LiveData<SummaryDTO> getGlobalSummary();
}

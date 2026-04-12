package com.lendy.app;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.lendy.app.data.LendyDatabase;
import com.lendy.app.data.TransactionType;
import com.lendy.app.data.dao.PersonDao;
import com.lendy.app.data.dao.TransactionDao;
import com.lendy.app.data.entities.Person;
import com.lendy.app.data.entities.TransactionRecord;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class TransactionDaoTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private LendyDatabase db;
    private PersonDao personDao;
    private TransactionDao transactionDao;

    @Before
    public void createDb() {
        db = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                LendyDatabase.class)
                .allowMainThreadQueries()
                .build();
        personDao = db.personDao();
        transactionDao = db.transactionDao();
    }

    @After
    public void closeDb() {
        db.close();
    }

    @Test
    public void insertTransactionUpdatesPersonBalance() throws Exception {
        // Prepare Person
        Person p1 = new Person(1L, "Debt Guy", "012", 0L, System.currentTimeMillis());
        personDao.insert(p1);

        // Add Transaction (Lend 500k)
        TransactionRecord record = new TransactionRecord(
                0L, 1L, 500000L, TransactionType.LEND, "Mua xe", null, System.currentTimeMillis()
        );
        transactionDao.addTransaction(record);

        // Check if Person's balance was updated directly from trigger/logic if there is any
        // Since logic might be in repository, wait, looking at repo, it just calls dao.addTransaction
        // Let's see timeline
        List<TransactionRecord> timeline = LiveDataTestUtil.getOrAwaitValue(transactionDao.getTimeline(1L));
        assertEquals(1, timeline.size());
        assertEquals(500000L, timeline.get(0).getAmount());
    }
}

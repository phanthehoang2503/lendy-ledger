package com.lendy.app.repository;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.lendy.app.LiveDataTestUtil;
import com.lendy.app.data.LendyDatabase;
import com.lendy.app.data.TransactionType;
import com.lendy.app.data.entities.Person;
import com.lendy.app.data.entities.TransactionRecord;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class LendyRepositoryTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private LendyDatabase db;
    private LendyRepository repository;

    @Before
    public void createDb() {
        // Khởi tạo In-Memory DB để test mà không ảnh hưởng DB thật
        db = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                LendyDatabase.class)
                .allowMainThreadQueries()
                .build();

        repository = new LendyRepository(db, Executors.newSingleThreadExecutor());
    }

    @After
    public void closeDb() {
        db.close();
    }

    @Test
    public void testAddPersonWithBalanceAtomicity() throws Exception {
        Person newPerson = new Person();
        newPerson.name = "John Doe";
        repository.addPersonWithBalance(newPerson, 500000, TransactionType.LEND, "");

        // Kiểm tra xem Person đã được lưu chưa
        List<Person> activeDebts = LiveDataTestUtil.getOrAwaitValue(repository.getActiveDebts());
        assertEquals(1, activeDebts.size());
        assertEquals("John Doe", activeDebts.get(0).name);
        assertEquals(500000, activeDebts.get(0).totalBalance);

        // Kiểm tra xem Transactionđã được tạo đồng thời chưa
        long savedPersonId = activeDebts.get(0).id;
        List<TransactionRecord> timeline = LiveDataTestUtil.getOrAwaitValue(repository.getTimeline(savedPersonId));
        assertEquals(1, timeline.size());
        assertEquals(TransactionType.LEND, timeline.get(0).type);
        assertEquals(500000, timeline.get(0).amount);
    }

    @Test
    public void testClearAllData() throws Exception {
        Person p1 = new Person();
        p1.name = "Debt 1";
        repository.addPersonWithBalance(p1, 100000, TransactionType.BORROW, "");

        // Đảm bảo dữ liệu đã vào trước khi xóa
        LiveDataTestUtil.getOrAwaitValue(repository.getAllPeople());

        // Gọi lệnh xóa sạch
        repository.clearAllData();

        // Kiểm tra kết quả sau khi xóa
        List<Person> remainingPeople = LiveDataTestUtil.getOrAwaitValue(repository.getAllPeople());
        assertTrue(remainingPeople.isEmpty());
    }
}

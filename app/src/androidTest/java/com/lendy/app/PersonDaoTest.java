package com.lendy.app;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.lendy.app.data.LendyDatabase;
import com.lendy.app.data.dao.PersonDao;
import com.lendy.app.data.entities.Person;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class PersonDaoTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private LendyDatabase db;
    private PersonDao personDao;

    @Before
    public void createDb() {
        db = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                LendyDatabase.class)
                .allowMainThreadQueries()
                .build();
        personDao = db.personDao();
    }

    @After
    public void closeDb() {
        db.close();
    }

    @Test
    public void insertAndGetPerson() throws Exception {
        Person person = new Person(1L, "Nguyen Van A", "0123456789", 500000L, System.currentTimeMillis());
        personDao.insert(person);

        Person loaded = LiveDataTestUtil.getOrAwaitValue(personDao.getPersonById(1L));
        assertEquals(loaded.getName(), person.getName());
        assertEquals(loaded.getTotalBalance(), 500000L);
    }

    @Test
    public void testActiveAndCompletedDebts() throws Exception {
        Person p1 = new Person(1L, "Active 1", "01", 1000L, System.currentTimeMillis());
        Person p2 = new Person(2L, "Completed 1", "02", 0L, System.currentTimeMillis());
        Person p3 = new Person(3L, "Active 2", "03", -500L, System.currentTimeMillis());

        personDao.insert(p1);
        personDao.insert(p2);
        personDao.insert(p3);

        List<Person> activeDebts = LiveDataTestUtil.getOrAwaitValue(personDao.getPeopleWithActiveBalance());
        List<Person> completedDebts = LiveDataTestUtil.getOrAwaitValue(personDao.getPeopleCompleted());

        assertEquals(2, activeDebts.size());
        assertEquals(1, completedDebts.size());
        assertEquals("Completed 1", completedDebts.get(0).getName());
    }

    @Test
    public void testDeletePerson() throws Exception {
        Person p1 = new Person(1L, "To Be Deleted", "01", 1000L, System.currentTimeMillis());
        personDao.insert(p1);
        personDao.delete(p1);

        Person loaded = LiveDataTestUtil.getOrAwaitValue(personDao.getPersonById(1L));
        assertTrue(loaded == null);
    }
}

package com.lendy.app;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;

import com.lendy.app.data.TransactionType;
import com.lendy.app.data.entities.Person;
import com.lendy.app.data.entities.TransactionRecord;
import com.lendy.app.repository.LendyRepository;
import com.lendy.app.viewmodel.LendyViewModel;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LendyViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private LendyRepository repository;

    private LendyViewModel viewModel;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        
        // Mock default LiveData returns
        when(repository.getActiveDebts()).thenReturn(new MutableLiveData<>());
        when(repository.getCompletedDebts()).thenReturn(new MutableLiveData<>());
        when(repository.getGlobalSummary()).thenReturn(new MutableLiveData<>());
        when(repository.getErrorNotifier()).thenReturn(new MutableLiveData<>());

        viewModel = new LendyViewModel(repository);
    }

    @Test
    public void testAddTransaction() {
        TransactionRecord record = new TransactionRecord(
                0L, 1L, 1000L, TransactionType.LEND, "Test", null, System.currentTimeMillis()
        );
        viewModel.addTransaction(record);
        verify(repository).createTransaction(record);
    }

    @Test
    public void testAddPerson() {
        Person person = new Person(1L, "Debt Guy", "012", 0L, System.currentTimeMillis());
        viewModel.addPerson(person);
        verify(repository).upsertPerson(person);
    }

    @Test
    public void testGetTimeline() {
        MutableLiveData<List<TransactionRecord>> liveData = new MutableLiveData<>();
        liveData.setValue(Collections.emptyList());
        when(repository.getTimeline(1L)).thenReturn(liveData);

        viewModel.getTimeline(1L);
        verify(repository).getTimeline(1L);
    }
}

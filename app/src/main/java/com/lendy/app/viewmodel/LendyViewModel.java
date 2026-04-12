package com.lendy.app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.lendy.app.utils.Event;
import com.lendy.app.data.entities.Person;
import com.lendy.app.data.entities.TransactionRecord;
import com.lendy.app.data.model.SummaryDTO;
import com.lendy.app.repository.LendyRepository;

import java.util.List;

import lombok.Getter;

public class LendyViewModel extends ViewModel {
    private final LendyRepository repository;
    @Getter
    private final LiveData<List<Person>> activeDebts;
    @Getter
    private final LiveData<List<Person>> completedDebts;
    @Getter
    private final LiveData<SummaryDTO> globalSummary;
    @Getter
    private final LiveData<Event<String>> errorObserver;

    public LendyViewModel(LendyRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.activeDebts = repository.getActiveDebts();
        this.completedDebts = repository.getCompletedDebts();
        this.globalSummary = repository.getGlobalSummary();
        this.errorObserver = repository.getErrorNotifier();
    }

    public LiveData<List<TransactionRecord>> getTimeline(long personId) {
        return repository.getTimeline(personId);
    }

    public void addTransaction(TransactionRecord record) {
        repository.createTransaction(record);
    }

    public void updateTransaction(TransactionRecord oldRecord, TransactionRecord newRecord) {
        repository.updateTransaction(oldRecord, newRecord);
    }

    public void deleteTransaction(TransactionRecord record) {
        repository.deleteTransaction(record);
    }

    public void addPerson(Person person) {
        repository.upsertPerson(person);
    }

    public void removePerson(Person person) {
        repository.deletePerson(person);
    }
}

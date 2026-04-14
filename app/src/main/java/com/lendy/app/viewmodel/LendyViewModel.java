package com.lendy.app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.lendy.app.data.TransactionType;
import com.lendy.app.utils.Event;
import com.lendy.app.data.entities.Person;
import com.lendy.app.data.entities.TransactionRecord;
import com.lendy.app.data.model.SummaryDTO;
import com.lendy.app.repository.LendyRepository;

import java.util.List;
import java.util.Objects;

import lombok.Getter;

/******************************************************************************
 * ../viewmodel/LendyViewModel.java - LendyViewModel
 * Bộ phận điều khiển, kết nối Giao diện và Dữ liệu.
 * Đảm bảo các luồng thông báo lỗi, dữ liệu được cập nhật theo chu kỳ sống UI.
 *****************************************************************************/
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

    public LiveData<List<Person>> getAllPeople() {
        return repository.getAllPeople();
    }

    public LiveData<List<TransactionRecord>> getTimeline(long personId) {
        return repository.getTimeline(personId);
    }

    public LiveData<List<TransactionRecord>> getAllTransactions() {
        return repository.getAllTransactions();
    }

    public LiveData<Person> getPersonById(long id) {
        return repository.getPersonById(id);
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

    public void addPersonWithInitialBalance(Person person, long amount, TransactionType type, String note) {
        repository.addPersonWithBalance(person, amount, type, note);
    }

    public void addPerson(Person person) {
        repository.upsertPerson(person);
    }

    public void updatePerson(Person person) {
        repository.upsertPerson(person);
    }

    public void removePerson(Person person) {
        repository.deletePerson(person);
    }

    public void clearAllData() {
        repository.clearAllData();
    }
}

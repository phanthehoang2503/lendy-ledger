/******************************************************************************
 * ../viewmodel/LendyViewModelFactory.java - LendyViewModelFactory
 * CHỨC NĂNG: Giúp khởi tạo ViewModel kèm theo tham số Repository.
 *****************************************************************************/
package com.lendy.app.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.lendy.app.repository.LendyRepository;

public class LendyViewModelFactory implements ViewModelProvider.Factory {
    private final LendyRepository repository;

    public LendyViewModelFactory(LendyRepository repository) {
        this.repository = repository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(LendyViewModel.class)) {
            return (T) new LendyViewModel(repository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}

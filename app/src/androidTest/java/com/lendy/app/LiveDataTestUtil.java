package com.lendy.app;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class LiveDataTestUtil {
    public static <T> T getOrAwaitValue(final LiveData<T> liveData) throws InterruptedException, TimeoutException {
        final Object[] data = new Object[1];
        final CountDownLatch latch = new CountDownLatch(1);
        Observer<T> observer = new Observer<T>() {
            @Override
            public void onChanged(@Nullable T o) {
                data[0] = o;
                latch.countDown();
                liveData.removeObserver(this);
            }
        };
        liveData.observeForever(observer);

        try {
            if (!latch.await(2, TimeUnit.SECONDS)) {
                throw new TimeoutException("LiveData value was never set.");
            }
        } finally {
            liveData.removeObserver(observer);
        }

        @SuppressWarnings("unchecked")
        T result = (T) data[0];
        return result;
    }
}

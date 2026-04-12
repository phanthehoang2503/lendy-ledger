package com.lendy.app.utils;

import java.util.concurrent.atomic.AtomicBoolean;

/******************************************************************************
 * ../utils/Event.java - Event Wrapper
 * các thông báo (Toast, Snackbar) chỉ hiển thị đúng 1 lần.
 *****************************************************************************/
public class Event<T> {
    private final T content;
    private final AtomicBoolean hasBeenHandled = new AtomicBoolean(false);

    public Event(T content) {
        this.content = content;
    }

    /**
     * Lấy nội dung nếu chưa từng được xử lý.
     */
    public T getContentIfNotHandled() {
        if (hasBeenHandled.compareAndSet(false, true)) {
            return content;
        } else {
            return null;
        }
    }

    /**
     * Chỉ xem nội dung mà không đánh dấu là đã xử lý.
     */
    public T peekContent() {
        return content;
    }

    public boolean hasBeenHandled() {
        return hasBeenHandled.get();
    }
}


package com.lendy.app.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/******************************************************************************
 * ../utils/DateUtils.java - DateUtils
 * định dạng thời gian theo TimeZone và Locale của hệ thống.
 *****************************************************************************/
public class DateUtils {
    /**
     * Chuyển đổi timestamp thành chuỗi đẹp: HH:mm - dd/MM/yyyy
     */
    public static String formatDateTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault());

        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(new Date(timestamp));
    }
}

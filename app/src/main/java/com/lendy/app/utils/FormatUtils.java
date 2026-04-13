package com.lendy.app.utils;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FormatUtils {
    private static final Locale VIETNAM = new Locale("vi", "VN");
    private static final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(VIETNAM);
    private static final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public static String formatCurrency(long amount) {
        return currencyFormatter.format(amount);
    }

    public static String formatCurrencyAbs(long amount) {
        return currencyFormatter.format(Math.abs(amount));
    }

    public static String formatDateTime(long timestamp) {
        return dateTimeFormatter.format(new Date(timestamp));
    }
}

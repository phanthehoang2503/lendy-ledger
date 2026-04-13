package com.lendy.app.utils;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FormatUtils {
    private static final Locale VIETNAM = Locale.forLanguageTag("vi-VN");
    private static final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(VIETNAM);

    public static String formatCurrency(long amount) {
        return formatThousand(amount) + " ₫";
    }

    public static String formatCurrencyAbs(long amount) {
        return formatThousand(Math.abs(amount)) + " ₫";
    }

    public static String formatDateTime(long timestamp) {
        SimpleDateFormat dateTimeFormatter = new SimpleDateFormat(
                "dd/MM/yyyy HH:mm",
                Locale.getDefault());
        return dateTimeFormatter.format(new Date(timestamp));
    }

    /**
     * Định dạng số tiền, dùng cho hiển thị trong EditText khi đang nhập tiền
     */
    public static String formatThousand(long amount) {
        NumberFormat nf = NumberFormat.getInstance(VIETNAM);
        return nf.format(amount);
    }

    /**
     * Chuyển đổi chuỗi định dạng về số Long nguyên thủy.
     */
    public static long parseFormattedNumber(String input) {
        if (input == null || input.isEmpty())
            return 0;
        String cleanString = input.replaceAll("[^\\d]", "");
        if (cleanString.isEmpty()) return 0;
        try{
            return Long.parseLong(cleanString);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}

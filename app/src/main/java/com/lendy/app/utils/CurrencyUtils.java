package com.lendy.app.utils;

import java.text.NumberFormat;
import java.util.Locale;

/******************************************************************************
 * ../utils/CurrencyUtils.java - CurrencyUtils
 * Định dạng tiền: 1000000 -> 1.000.000 ₫
 *****************************************************************************/
public class CurrencyUtils {
    public static String formatVND(long amount) {
        Locale vietnam = Locale.forLanguageTag("vi-VN");
        NumberFormat format = NumberFormat.getCurrencyInstance(vietnam);
        return format.format(amount);
    }
}

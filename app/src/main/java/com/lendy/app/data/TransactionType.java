/******************************************************************************
 * app/src/main/java/com/lendy/app/data/TransactionType.java - TransactionType
 * CHỨC NĂNG: Định nghĩa 4 loại hành động cơ bản: Cho vay, Đi vay, Được trả, Trả nợ.
 *****************************************************************************/
package com.lendy.app.data;

public enum TransactionType {
    LEND,      // Mình cho người ta vay thêm tiền
    BORROW,    // Mình đi vay tiền của người ta
    REPAY,     // Người ta trả bớt nợ cho mình
    PAY_BACK   // Mình mang tiền đi trả nợ cho người ta
}

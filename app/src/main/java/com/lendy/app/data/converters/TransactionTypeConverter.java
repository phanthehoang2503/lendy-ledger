
package com.lendy.app.data.converters;

import androidx.room.TypeConverter;
import com.lendy.app.data.TransactionType;

/******************************************************************************
 * ../data/converters/TransactionTypeConverter.java - TransactionTypeConverter
 * CHỨC NĂNG: Giúp máy chuyển đổi giữa kiểu "Chữ" (Vay/Trả) và kiểu "Dữ liệu
 * máy".
 *****************************************************************************/
public class TransactionTypeConverter {
    // Chuyển từ kiểu dữ liệu máy sang chữ để lưu vào Database
    @TypeConverter
    public static String fromType(TransactionType type) {
        return type == null ? null : type.name();
    }

    // Đọc từ Database lên và chuyển lại thành kiểu dữ liệu máy để xử lý
    @TypeConverter
    public static TransactionType toType(String name) {
        if (name == null)
            return null;
        try {
            return TransactionType.valueOf(name);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}

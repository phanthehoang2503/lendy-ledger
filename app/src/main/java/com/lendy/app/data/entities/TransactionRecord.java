package com.lendy.app.data.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import com.lendy.app.data.TransactionType;
import com.lendy.app.data.converters.TransactionTypeConverter;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/******************************************************************************
 * ../data/entities/TransactionRecord.java - TransactionRecord
 * CHỨC NĂNG: Khai báo các cột dữ liệu cho bảng "Lịch sử giao dịch".
 *****************************************************************************/
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(tableName = "transactions", foreignKeys = @ForeignKey(entity = Person.class, parentColumns = "id", childColumns = "personId", onDelete = ForeignKey.CASCADE)) // Nếu
																																																																																			// xóa
																																																																																			// người
																																																																																			// nợ,
																																																																																			// lịch
																																																																																			// sử
																																																																																			// giao
																																																																																			// dịch
																																																																																			// cũng
																																																																																			// biến
																																																																																			// mất
																																																																																			// theo
@TypeConverters({ TransactionTypeConverter.class })
public class TransactionRecord {
	@PrimaryKey(autoGenerate = true)
	public long id; // Mã giao dịch tự động tăng
	public long personId; // Mã người nợ liên kết (ID của bảng Person)
	public long amount; // Số tiền giao dịch
	public TransactionType type; // Loại giao dịch (Vay/Trả...)
	public String note; // Ghi chú đính kèm
	public String imageUri; // Ảnh đính kèm (Lưu đường dẫn ảnh)
	public long timestamp; // Thời gian thực hiện
}

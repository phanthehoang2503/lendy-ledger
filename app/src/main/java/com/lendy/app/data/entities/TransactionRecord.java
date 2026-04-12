package com.lendy.app.data.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import androidx.annotation.NonNull;
import com.lendy.app.data.TransactionType;
import com.lendy.app.data.converters.TransactionTypeConverter;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/******************************************************************************
 * ../data/entities/TransactionRecord.java - TransactionRecord
 * Lịch sử giao dịch
 *****************************************************************************/
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(tableName = "transactions", foreignKeys = @ForeignKey(entity = Person.class, parentColumns = "id", childColumns = "personId", onDelete = ForeignKey.CASCADE))
@TypeConverters({ TransactionTypeConverter.class })
public class TransactionRecord { // <-- lo cái này trở xuống kệ đống anote trên đi.
	@PrimaryKey(autoGenerate = true)
	public long id;
	public long personId;
	public long amount;
	@NonNull
	public TransactionType type;
	public String note;
	public String imageUri;
	public long timestamp;
}

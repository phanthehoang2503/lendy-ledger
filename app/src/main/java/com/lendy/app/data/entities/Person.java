
package com.lendy.app.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/******************************************************************************
 * ../data/entities/Person.java - Person
 * CHỨC NĂNG: Khai báo các cột dữ liệu cho bảng "Người nợ".
 *****************************************************************************/
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(tableName = "people")
public class Person {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String name;
    public String phoneNumber;
    public long totalBalance;
    public long updatedAt;
    public boolean isDeleted;
}


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
    public long id; // Mã số định danh tự động tăng
    public String name; // Tên người nợ
    public String phoneNumber; // Số điện thoại
    public long totalBalance; // Số dư nợ (Dương: họ nợ mình, Âm: mình nợ họ)
    public long updatedAt; // Thời điểm cập nhật cuối cùng
}

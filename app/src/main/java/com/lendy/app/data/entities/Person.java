package com.lendy.app.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(tableName = "people")
public class Person {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String name;
    public String phoneNumber;
    public long totalBalance; // > 0: họ nợ mình, < 0: mình nợ họ
    public long updatedAt;
}

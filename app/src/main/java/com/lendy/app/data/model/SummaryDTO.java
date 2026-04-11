/******************************************************************************
 * app/src/main/java/com/lendy/app/data/model/SummaryDTO.java - SummaryDTO
 * CHỨC NĂNG: Một cái "giỏ" nhỏ để chứa 2 con số tổng kết: Tổng cho vay & Tổng đi vay.
 *****************************************************************************/
package com.lendy.app.data.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SummaryDTO {
    public long totalLending;    // Tổng số tiền người ta đang nợ mình
    public long totalBorrowing;  // Tổng số tiền mình đang đi nợ người ta
}

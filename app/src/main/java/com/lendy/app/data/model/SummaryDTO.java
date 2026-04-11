package com.lendy.app.data.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/******************************************************************************
 * ../data/model/SummaryDTO.java - SummaryDTO
 * CHỨC NĂNG: Một cái "giỏ" nhỏ để chứa 2 con số tổng kết: Tổng cho vay & Tổng
 * đi vay.
 *****************************************************************************/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SummaryDTO {
    public Long totalLending; // bạn nợ tôi
    public Long totalBorrowing; // tôi nợ bạn
}

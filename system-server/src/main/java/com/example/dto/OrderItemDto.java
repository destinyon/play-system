package com.example.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDto {
    private Integer id;
    private Integer dishId;
    private String dishName;
    private String dishImage;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal total;
}

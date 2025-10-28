package com.example.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItem {

    private Integer id;

    private Integer orderId;

    private Integer dishId;

    private String dishName;

    private String dishImage;

    private BigDecimal unitPrice;

    private Integer quantity;
}

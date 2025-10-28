package com.example.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderInfo {

    private Integer id;

    private String orderNo;

    private Integer userId;

    private Integer deliverymanId;

    private Integer restaurateurId;

    private Integer restaurantId;

    private String status;

    private String remark;

    private String deliveryAddress;

    private BigDecimal totalAmount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

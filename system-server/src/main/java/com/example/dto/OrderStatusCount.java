package com.example.dto;

import lombok.Data;

@Data
public class OrderStatusCount {
    private String status;
    private Long total;
}

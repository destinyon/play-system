package com.example.entity;

import lombok.Data;

@Data
public class Deliveryman {
    private Integer id;

    // 收入（分或元，根据项目约定）
    private Double income;

    private User user;
}

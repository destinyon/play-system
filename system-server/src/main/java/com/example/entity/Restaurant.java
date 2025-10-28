package com.example.entity;

import lombok.Data;

@Data
public class Restaurant {
    private Integer id;

    private String restaurantName;

    private String restaurantAddress;

    private String restaurantImageUrl;

    private String description;

    private Double lng;

    private Double lat;
    private Integer restaurateurId;
}

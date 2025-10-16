package com.example.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "restaurant")
public class Restaurant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "restaurant_name", nullable = false)
    private String restaurantName;

    @Column(name = "restaurant_address", nullable = false)
    private String restaurantAddress;
    
    private String restaurantImageUrl;

    @Column(name = "lng")
    private Double lng;

    @Column(name = "lat")
    private Double lat;

    @OneToOne
    @JoinColumn(name = "restaurateur_id", nullable = false, unique = true)
    private Restaurateur restaurateur;
}

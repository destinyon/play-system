package com.example.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.example.entity.Restaurant;
import java.util.List;

@Mapper
public interface RestaurantMapper {
    int insert(Restaurant restaurant);
    int deleteByRestaurateurId(Integer restaurateurId);
    List<Restaurant> listAll();
    Restaurant getByRestaurateurId(Integer restaurateurId);
    Restaurant getById(Integer id);
    int updateById(Restaurant restaurant);
}

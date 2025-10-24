package com.example.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.example.entity.Restaurateur;

@Mapper
public interface RestaurateurMapper {
    int insert(Restaurateur restaurateur);
    Restaurateur getByUserId(Integer userId);
    Restaurateur getById(Integer id);
    int deleteByUserId(Integer userId);

    int increaseIncome(@Param("id") Integer id, @Param("amount") Double amount);

    Integer getUserIdByRestaurateurId(@Param("id") Integer id);
}

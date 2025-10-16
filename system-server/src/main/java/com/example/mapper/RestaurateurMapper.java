package com.example.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.example.entity.Restaurateur;

@Mapper
public interface RestaurateurMapper {
    int insert(Restaurateur restaurateur);
    Restaurateur getByUserId(Integer userId);
    int deleteByUserId(Integer userId);
}

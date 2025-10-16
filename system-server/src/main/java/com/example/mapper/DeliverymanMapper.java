package com.example.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.example.entity.Deliveryman;

@Mapper
public interface DeliverymanMapper {
    int insert(Deliveryman deliveryman);
    int deleteByUserId(Integer userId);
}

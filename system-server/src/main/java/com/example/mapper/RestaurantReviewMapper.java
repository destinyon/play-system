package com.example.mapper;

import com.example.entity.RestaurantReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RestaurantReviewMapper {
    int insert(RestaurantReview review);

    List<RestaurantReview> listByRestaurant(@Param("restaurantId") Integer restaurantId,
                                            @Param("offset") int offset,
                                            @Param("limit") int limit);

    int countByRestaurant(@Param("restaurantId") Integer restaurantId);

    RestaurantReview findById(@Param("id") Integer id);

    void incrementLikes(@Param("reviewId") Integer reviewId, @Param("delta") int delta);

    Double averageRatingByRestaurant(@Param("restaurantId") Integer restaurantId);

    Integer sumLikesByRestaurant(@Param("restaurantId") Integer restaurantId);
}

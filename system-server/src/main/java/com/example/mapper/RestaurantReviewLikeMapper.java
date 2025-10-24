package com.example.mapper;

import com.example.entity.RestaurantReviewLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RestaurantReviewLikeMapper {
    RestaurantReviewLike findByReviewAndUser(@Param("reviewId") Integer reviewId,
                                             @Param("userId") Integer userId);

    int insert(RestaurantReviewLike like);

    int deleteById(@Param("id") Integer id);

    int deleteByReviewAndUser(@Param("reviewId") Integer reviewId,
                              @Param("userId") Integer userId);

    int countByReview(@Param("reviewId") Integer reviewId);

    java.util.List<Integer> findLikedReviewIds(@Param("userId") Integer userId,
                                               @Param("reviewIds") java.util.List<Integer> reviewIds);
}

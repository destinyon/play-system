package com.example.service;

import com.example.common.DataRequest;
import com.example.common.Result;
import com.example.entity.Restaurant;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface RestaurantService {
    Result<Map<String, String>> uploadPhoto(DataRequest request, MultipartFile file);

    Result<Restaurant> createOrUpdate(DataRequest request);

    Result<List<Restaurant>> listAll(DataRequest request);

    Result<Map<String, Object>> getProfile(DataRequest request);

    Result<Restaurant> saveProfile(DataRequest request);

    Result<Map<String, Object>> listReviews(DataRequest request);

    Result<Map<String, Object>> toggleReviewLike(DataRequest request);

    Result<Map<String, Object>> createReview(DataRequest request);
}

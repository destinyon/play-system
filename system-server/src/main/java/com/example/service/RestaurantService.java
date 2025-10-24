package com.example.service;

import com.example.common.DataRequest;
import com.example.common.Result;
import org.springframework.web.multipart.MultipartFile;

public interface RestaurantService {
    Result uploadPhoto(MultipartFile file);

    Result createOrUpdate(DataRequest request);

    Result listAll();

    Result getProfile(DataRequest request);

    Result saveProfile(DataRequest request);

    Result listReviews(DataRequest request);

    Result toggleReviewLike(DataRequest request);

    Result createReview(DataRequest request);
}

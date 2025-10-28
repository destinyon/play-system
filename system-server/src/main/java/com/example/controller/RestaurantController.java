package com.example.controller;

import com.example.common.DataRequest;
import com.example.common.Result;
import com.example.entity.Restaurant;
import com.example.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/restaurant")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;

    // Simple demo storage: upload handling moved to RestaurantService. Controller delegates to service.

    @PostMapping(value = "/uploadPhoto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, String>> uploadPhoto(@RequestPart("request") DataRequest request,
                              @RequestPart("file") MultipartFile file) {
        return restaurantService.uploadPhoto(request, file);
    }

    @PostMapping("/createOrUpdate")
    public Result<Restaurant> createOrUpdate(@RequestBody DataRequest request) {
        return restaurantService.createOrUpdate(request);
    }

    @PostMapping("/list")
    public Result<List<Restaurant>> list(@RequestBody(required = false) DataRequest request) {
        return restaurantService.listAll(request == null ? new DataRequest() : request);
    }

    @PostMapping("/profile/get")
    public Result<Map<String, Object>> getProfile(@RequestBody DataRequest request) {
        return restaurantService.getProfile(request);
    }

    @PostMapping("/profile/save")
    public Result<Restaurant> saveProfile(@RequestBody DataRequest request) {
        return restaurantService.saveProfile(request);
    }

    @PostMapping("/reviews/page")
    public Result<Map<String, Object>> listReviews(@RequestBody DataRequest request) {
        return restaurantService.listReviews(request);
    }

    @PostMapping("/reviews/like")
    public Result<Map<String, Object>> toggleLike(@RequestBody DataRequest request) {
        return restaurantService.toggleReviewLike(request);
    }

    @PostMapping("/reviews/create")
    public Result<Map<String, Object>> createReview(@RequestBody DataRequest request) {
        return restaurantService.createReview(request);
    }
}

package com.example.controller;

import com.example.common.DataRequest;
import com.example.common.Result;
import com.example.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/restaurant")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;

    // Simple demo storage: upload handling moved to RestaurantService. Controller delegates to service.

    @PostMapping(value = "/uploadPhoto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result uploadPhoto(@RequestPart("request") DataRequest request,
                              @RequestPart("file") MultipartFile file) {
        return restaurantService.uploadPhoto(request, file);
    }

    @PostMapping("/createOrUpdate")
    public Result createOrUpdate(@RequestBody DataRequest request) {
        return restaurantService.createOrUpdate(request);
    }

    @GetMapping("/list")
    public Result list() {
        return restaurantService.listAll();
    }

    @PostMapping("/profile/get")
    public Result getProfile(@RequestBody DataRequest request) {
        return restaurantService.getProfile(request);
    }

    @PostMapping("/profile/save")
    public Result saveProfile(@RequestBody DataRequest request) {
        return restaurantService.saveProfile(request);
    }

    @PostMapping("/reviews/page")
    public Result listReviews(@RequestBody DataRequest request) {
        return restaurantService.listReviews(request);
    }

    @PostMapping("/reviews/like")
    public Result toggleLike(@RequestBody DataRequest request) {
        return restaurantService.toggleReviewLike(request);
    }

    @PostMapping("/reviews/create")
    public Result createReview(@RequestBody DataRequest request) {
        return restaurantService.createReview(request);
    }
}

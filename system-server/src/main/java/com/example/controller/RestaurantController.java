package com.example.controller;

import com.example.common.DataRequest;
import com.example.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.example.service.RestaurantService;

@RestController
@RequestMapping("/api/restaurant")
public class RestaurantController {

    @Autowired
    private RestaurantService restaurantService;

    // Simple demo storage: upload handling moved to RestaurantService. Controller delegates to service.

    @PostMapping(value = "/uploadPhoto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result uploadPhoto(@RequestParam("file") MultipartFile file) {
        return restaurantService.uploadPhoto(file);
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

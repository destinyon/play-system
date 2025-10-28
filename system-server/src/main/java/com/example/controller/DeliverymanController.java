package com.example.controller;

import com.example.common.DataRequest;
import com.example.common.Result;
import com.example.service.DeliverymanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/deliveryman")
@RequiredArgsConstructor
public class DeliverymanController {

    private final DeliverymanService deliverymanService;

    @PostMapping("/stats")
    public Result getStats(@RequestBody DataRequest request) {
        return deliverymanService.getStats(request);
    }
}

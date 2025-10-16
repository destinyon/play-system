package com.example.service;

import com.example.common.Result;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface RestaurantService {
    Result uploadPhoto(MultipartFile file) throws IOException;
}

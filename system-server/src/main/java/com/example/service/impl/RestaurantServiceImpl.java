package com.example.service.impl;

import com.example.common.Result;
import com.example.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.example.util.FileUtil;

@Service
@RequiredArgsConstructor
public class RestaurantServiceImpl implements RestaurantService {

    @Override
    public Result uploadPhoto(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return Result.error("文件为空");
        String filename = FileUtil.saveFile(file, "restaurants");
        Map<String, Object> data = new HashMap<>();
        data.put("url", "/uploads/restaurants/" + filename);
        data.put("filename", filename);
        return Result.success(data);
    }
}

package com.example.service;

import com.example.common.DataRequest;
import com.example.common.Result;
import org.springframework.web.multipart.MultipartFile;

public interface MenuItemService {
    Result list(DataRequest request);

    Result detail(DataRequest request);

    Result listCategories(DataRequest request);

    Result create(DataRequest request);

    Result update(DataRequest request);

    Result delete(DataRequest request);

    Result uploadImage(DataRequest request, MultipartFile file);
}

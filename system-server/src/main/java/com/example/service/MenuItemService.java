package com.example.service;

import com.example.common.DataRequest;
import com.example.common.Result;
import com.example.dto.MenuItemDto;
import com.example.dto.PageResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface MenuItemService {
    Result<PageResult<MenuItemDto>> list(DataRequest request);

    Result<MenuItemDto> detail(DataRequest request);

    Result<List<String>> listCategories(DataRequest request);

    Result<Integer> create(DataRequest request);

    Result<Void> update(DataRequest request);

    Result<Void> delete(DataRequest request);

    Result<Map<String, Object>> uploadImage(DataRequest request, MultipartFile file);
}

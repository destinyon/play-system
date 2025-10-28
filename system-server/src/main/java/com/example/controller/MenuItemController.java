package com.example.controller;

import com.example.common.DataRequest;
import com.example.common.Result;
import com.example.dto.MenuItemDto;
import com.example.dto.PageResult;
import com.example.service.MenuItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/restaurateur/menu")
@RequiredArgsConstructor
public class MenuItemController {

    private final MenuItemService menuItemService;

    @PostMapping("/list")
    public Result<PageResult<MenuItemDto>> list(@RequestBody DataRequest request) {
        return menuItemService.list(request);
    }

    @PostMapping("/detail")
    public Result<MenuItemDto> detail(@RequestBody DataRequest request) {
        return menuItemService.detail(request);
    }

    @PostMapping("/categories")
    public Result<List<String>> categories(@RequestBody DataRequest request) {
        return menuItemService.listCategories(request);
    }

    @PostMapping("/create")
    public Result<Integer> create(@RequestBody DataRequest request) {
        return menuItemService.create(request);
    }

    @PostMapping("/update")
    public Result<Void> update(@RequestBody DataRequest request) {
        return menuItemService.update(request);
    }

    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody DataRequest request) {
        return menuItemService.delete(request);
    }

    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Object>> uploadImage(@RequestPart("request") DataRequest request,
                              @RequestPart("file") MultipartFile file) {
        return menuItemService.uploadImage(request, file);
    }

    @GetMapping("/default-image")
    public ResponseEntity<Resource> getDefaultImage() {
        try {
            File defaultImageFile = new File("uploads/dishes/default.png");
            if (defaultImageFile.exists()) {
                Resource resource = new FileSystemResource(defaultImageFile);
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_PNG)
                        .header(HttpHeaders.CACHE_CONTROL, "max-age=31536000")
                        .body(resource);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}

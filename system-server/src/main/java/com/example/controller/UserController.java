package com.example.controller;

import com.example.common.DataRequest;
import com.example.common.Result;
import com.example.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/getById")
    public Result<Map<String, Object>> getById(@RequestBody DataRequest dataRequest) {
        return userService.getById(dataRequest);
    }

    @PostMapping("/getByUsername")
    public Result<Map<String, Object>> getByUsername(@RequestBody DataRequest dataRequest) {
        return userService.getByUsername(dataRequest);
    }

    @PostMapping("/getAllUsers")
    public Result<java.util.List<Map<String, Object>>> getAllUsers(@RequestBody(required = false) DataRequest dataRequest) {
        return userService.getAllUsers(dataRequest == null ? new DataRequest() : dataRequest);
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody DataRequest dataRequest) {
        return userService.loginService(dataRequest);
    }

    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody DataRequest dataRequest) {
        return userService.registerService(dataRequest);
    }

    @PostMapping("/update")
    public Result<Map<String, Object>> update(@RequestBody DataRequest dataRequest) {
        return userService.updateUser(dataRequest);
    }

    @PostMapping("/changePassword")
    public Result<Void> changePassword(@RequestBody DataRequest dataRequest) {
        return userService.changePassword(dataRequest);
    }

    @PostMapping(value = "/uploadAvatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Object>> uploadAvatar(@RequestPart("request") DataRequest request,
                               @RequestPart("file") MultipartFile file) {
        return userService.uploadAvatar(request, file);
    }

    @GetMapping("/avatar/{filename:.+}")
    public ResponseEntity<byte[]> getAvatar(@PathVariable String filename) {
        Result<Map<String, Object>> result = userService.getAvatar(filename);
        if (result == null || result.getStatus() == null || result.getStatus() != 200 || result.getData() == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> data = result.getData();
        byte[] bytes = (byte[]) data.get("bytes");
        String contentType = data.get("contentType") instanceof String
                ? (String) data.get("contentType")
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        if (bytes == null || bytes.length == 0) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(bytes);
    }

    @PostMapping("/stats")
    public Result<Map<String, Object>> getUserStats(@RequestBody DataRequest dataRequest) {
        return userService.getUserStats(dataRequest);
    }
}

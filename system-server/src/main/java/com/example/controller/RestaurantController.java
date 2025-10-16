package com.example.controller;

import com.example.common.DataRequest;
import com.example.common.Result;
import com.example.entity.Restaurant;
import com.example.mapper.RestaurantMapper;
import com.example.mapper.RestaurateurMapper;
import com.example.mapper.UserMapper;
import com.example.entity.Restaurateur;
import com.example.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/restaurant")
public class RestaurantController {

    @Autowired
    private RestaurantMapper restaurantMapper;
    @Autowired
    private RestaurateurMapper restaurateurMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private com.example.service.RestaurantService restaurantService;

    // Simple demo storage: upload handling moved to RestaurantService. Controller delegates to service.

    @PostMapping(value = "/uploadPhoto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result uploadPhoto(@RequestParam("file") MultipartFile file) throws IOException {
        return restaurantService.uploadPhoto(file);
    }

    @PostMapping("/createOrUpdate")
    public Result createOrUpdate(@RequestBody DataRequest req) {
        Object obj = req.getData();
        System.out.println(obj);
        if (!(obj instanceof Map)) return Result.error("缺少数据");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) obj;
        // 校验必填项：name/address/lng/lat
        String name = (String) data.get("name");
        String address = (String) data.get("address");
        if (name == null || name.trim().isEmpty()) return Result.error("餐馆名称不能为空");
        if (address == null || address.trim().isEmpty()) return Result.error("详细地址不能为空");
        Restaurant r = new Restaurant();
        r.setRestaurantName(name.trim());
        r.setRestaurantAddress(address.trim());
        r.setRestaurantImageUrl((String) data.get("photoUrl"));
        Object lng = data.get("lng");
        Object lat = data.get("lat");
        if (lng == null || lat == null) return Result.error("经纬度不能为空");
        r.setLng(Double.valueOf(String.valueOf(lng)));
        r.setLat(Double.valueOf(String.valueOf(lat)));
        // bind to restaurateur id
        Object restObj = data.get("restaurateur");
        if (restObj instanceof Map) {
            @SuppressWarnings("unchecked") Map<String, Object> restaurateur = (Map<String, Object>) restObj;
            Integer rid = null;
            if (restaurateur.get("id") != null) {
                rid = Integer.valueOf(String.valueOf(restaurateur.get("id")));
            } else if (restaurateur.get("username") != null) {
                String username = String.valueOf(restaurateur.get("username"));
                User u = userMapper.getByUsername(username);
                if (u != null) {
                    Restaurateur rr0 = restaurateurMapper.getByUserId(u.getId());
                    if (rr0 == null) {
                        // auto-create restaurateur if not exists
                        rr0 = new Restaurateur();
                        rr0.setUser(u);
                        restaurateurMapper.insert(rr0);
                    }
                    if (rr0 != null) rid = rr0.getId();
                }
            }
            if (rid == null) return Result.error("无法解析商家信息");
            com.example.entity.Restaurateur rr = new com.example.entity.Restaurateur();
            rr.setId(rid);
            r.setRestaurateur(rr);
        } else {
            return Result.error("缺少商家信息");
        }
        // 若该商家已有记录则更新，否则插入（避免唯一键冲突）
        Restaurant existed = restaurantMapper.getByRestaurateurId(r.getRestaurateur().getId());
        if (existed != null) {
            r.setId(existed.getId());
            restaurantMapper.updateById(r);
        } else {
            restaurantMapper.insert(r);
        }
        return Result.success(r);
    }

    @GetMapping("/list")
    public Result list() {
        List<Restaurant> list = restaurantMapper.listAll();
        return Result.success(list);
    }
}

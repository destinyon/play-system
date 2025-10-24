package com.example.controller;

import com.example.common.DataRequest;
import com.example.common.Result;
import com.example.dto.MenuItemDto;
import com.example.dto.MenuItemForm;
import com.example.dto.PageResult;
import com.example.service.MenuItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/restaurateur/menu")
@RequiredArgsConstructor
public class MenuItemController {

    private final MenuItemService menuItemService;

    @PostMapping("/list")
    public Result page(@RequestBody DataRequest request) {
        Map<String, Object> data = request.getData();
        Integer restaurateurId = getIntValue(data, "restaurateurId");
        String keyword = (String) data.get("keyword");
        String category = (String) data.get("category");
        String status = (String) data.get("status");
        Integer page = request.getPage() != null ? request.getPage() : 1;
        Integer size = request.getSize() != null ? request.getSize() : 8;
        
        PageResult<MenuItemDto> result = menuItemService.page(restaurateurId, keyword, category, status, page, size);
        return Result.success(result);
    }

    @PostMapping("/detail")
    public Result detail(@RequestBody DataRequest request) {
        Map<String, Object> data = request.getData();
        Integer id = getIntValue(data, "id");
        Integer restaurateurId = getIntValue(data, "restaurateurId");
        
        MenuItemDto dto = menuItemService.getById(id, restaurateurId);
        if (dto == null) {
            return Result.error("未找到菜品");
        }
        return Result.success(dto);
    }

    @PostMapping("/categories")
    public Result categories(@RequestBody DataRequest request) {
        Map<String, Object> data = request.getData();
        Integer restaurateurId = getIntValue(data, "restaurateurId");
        
        List<String> categories = menuItemService.listCategories(restaurateurId);
        return Result.success(categories);
    }

    @PostMapping("/create")
    public Result create(@RequestBody DataRequest request) {
        try {
            Map<String, Object> data = request.getData();
            Integer restaurateurId = getIntValue(data, "restaurateurId");
            
            // Parse form from data map
            MenuItemForm form = parseMenuItemForm(data);
            
            menuItemService.create(restaurateurId, form);
            return Result.success("创建成功");
        } catch (IllegalArgumentException ex) {
            return Result.error(ex.getMessage());
        }
    }

    @PostMapping("/update")
    public Result update(@RequestBody DataRequest request) {
        try {
            Map<String, Object> data = request.getData();
            Integer id = getIntValue(data, "id");
            Integer restaurateurId = getIntValue(data, "restaurateurId");
            
            MenuItemForm form = parseMenuItemForm(data);
            form.setId(id);
            
            menuItemService.update(restaurateurId, form);
            return Result.success("更新成功");
        } catch (IllegalArgumentException ex) {
            return Result.error(ex.getMessage());
        }
    }

    @PostMapping("/delete")
    public Result remove(@RequestBody DataRequest request) {
        Map<String, Object> data = request.getData();
        Integer id = getIntValue(data, "id");
        Integer restaurateurId = getIntValue(data, "restaurateurId");
        
        menuItemService.softDelete(id, restaurateurId);
        return Result.success("删除成功");
    }
    
    private Integer getIntValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(value.toString());
    }
    
    private MenuItemForm parseMenuItemForm(Map<String, Object> data) {
        MenuItemForm form = new MenuItemForm();
        if (data.get("id") != null) {
            form.setId(getIntValue(data, "id"));
        }
        form.setName((String) data.get("name"));
        form.setCategory((String) data.get("category"));
        form.setDescription((String) data.get("description"));
        form.setImageUrl((String) data.get("imageUrl"));
        if (data.get("price") != null) {
            form.setPrice(new java.math.BigDecimal(data.get("price").toString()));
        }
        if (data.get("status") != null) {
            form.setStatus((String) data.get("status"));
        }
        return form;
    }
}

package com.example.service;

import com.example.dto.MenuItemDto;
import com.example.dto.MenuItemForm;
import com.example.dto.PageResult;

import java.util.List;

public interface MenuItemService {

    PageResult<MenuItemDto> page(Integer restaurateurId, String keyword, String category, String status, int page, int size);

    MenuItemDto getById(Integer id, Integer restaurateurId);

    void create(Integer restaurateurId, MenuItemForm form);

    void update(Integer restaurateurId, MenuItemForm form);

    void softDelete(Integer id, Integer restaurateurId);

    List<String> listCategories(Integer restaurateurId);
}

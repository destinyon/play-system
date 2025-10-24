package com.example.service.serviceImpl;

import com.example.dto.MenuItemDto;
import com.example.dto.MenuItemForm;
import com.example.dto.PageResult;
import com.example.entity.MenuItem;
import com.example.mapper.MenuItemMapper;
import com.example.service.MenuItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuItemServiceImpl implements MenuItemService {

    private final MenuItemMapper menuItemMapper;

    @Override
    public PageResult<MenuItemDto> page(Integer restaurateurId, String keyword, String category, String status, int page, int size) {
        if (restaurateurId == null) {
            return new PageResult<>(0, Collections.emptyList());
        }
        int pageNumber = Math.max(page, 1);
        int pageSize = Math.max(size, 1);
        int offset = (pageNumber - 1) * pageSize;
        List<MenuItem> items = menuItemMapper.pageByRestaurateur(restaurateurId, normalize(keyword), normalize(category), normalize(status), offset, pageSize);
        long total = menuItemMapper.countByRestaurateur(restaurateurId, normalize(keyword), normalize(category), normalize(status));
        List<MenuItemDto> records = items.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return new PageResult<>(total, records);
    }

    @Override
    public MenuItemDto getById(Integer id, Integer restaurateurId) {
        if (id == null) {
            return null;
        }
        MenuItem item = menuItemMapper.findById(id);
        if (item == null || !Objects.equals(item.getRestaurateurId(), restaurateurId) || Boolean.TRUE.equals(item.getDeleted())) {
            return null;
        }
        return toDto(item);
    }

    @Override
    public void create(Integer restaurateurId, MenuItemForm form) {
        if (restaurateurId == null || form == null) {
            throw new IllegalArgumentException("restaurateurId and form are required");
        }
        LocalDateTime now = LocalDateTime.now();
        MenuItem entity = new MenuItem();
        entity.setRestaurateurId(restaurateurId);
        entity.setName(form.getName());
        entity.setCategory(form.getCategory());
        entity.setPrice(form.getPrice());
        entity.setImageUrl(form.getImageUrl());
        entity.setDescription(form.getDescription());
        entity.setStatus(StringUtils.hasText(form.getStatus()) ? form.getStatus() : "ON_SHELF");
        entity.setDeleted(Boolean.FALSE);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        menuItemMapper.insert(entity);
    }

    @Override
    public void update(Integer restaurateurId, MenuItemForm form) {
        if (restaurateurId == null || form == null || form.getId() == null) {
            throw new IllegalArgumentException("restaurateurId and id are required");
        }
        MenuItem existing = menuItemMapper.findById(form.getId());
        if (existing == null || !Objects.equals(existing.getRestaurateurId(), restaurateurId) || Boolean.TRUE.equals(existing.getDeleted())) {
            throw new IllegalArgumentException("菜单不存在或已删除");
        }
        existing.setName(form.getName());
        existing.setCategory(form.getCategory());
        existing.setPrice(form.getPrice());
        existing.setImageUrl(form.getImageUrl());
        existing.setDescription(form.getDescription());
        existing.setStatus(StringUtils.hasText(form.getStatus()) ? form.getStatus() : existing.getStatus());
        existing.setUpdatedAt(LocalDateTime.now());
        menuItemMapper.update(existing);
    }

    @Override
    public void softDelete(Integer id, Integer restaurateurId) {
        if (id == null || restaurateurId == null) {
            return;
        }
        menuItemMapper.softDelete(id, restaurateurId);
    }

    @Override
    public List<String> listCategories(Integer restaurateurId) {
        if (restaurateurId == null) {
            return Collections.emptyList();
        }
        return menuItemMapper.listCategories(restaurateurId);
    }

    private MenuItemDto toDto(MenuItem item) {
        if (item == null) {
            return null;
        }
        return MenuItemDto.builder()
                .id(item.getId())
                .name(item.getName())
                .category(item.getCategory())
                .price(item.getPrice())
                .imageUrl(item.getImageUrl())
                .description(item.getDescription())
                .status(item.getStatus())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    private String normalize(String text) {
        return StringUtils.hasText(text) ? text : null;
    }
}

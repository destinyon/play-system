package com.example.service.serviceImpl;

import com.example.common.DataRequest;
import com.example.common.Result;
import com.example.dto.MenuItemDto;
import com.example.dto.PageResult;
import com.example.entity.MenuItem;
import com.example.mapper.MenuItemMapper;
import com.example.security.RequestDataHelper;
import com.example.service.MenuItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuItemServiceImpl implements MenuItemService {

    private final MenuItemMapper menuItemMapper;

    private static final String UPLOAD_DIR = "uploads/dishes";
    private static final Path UPLOAD_ROOT = Paths.get(System.getProperty("user.dir"), "uploads", "dishes");
    private static final String DEFAULT_IMAGE = "/uploads/dishes/default.jpg";
    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Pattern PRICE_PATTERN = Pattern.compile("^(?!0+(?:\\.0+)?$)(?:10000(?:\\.0{1,2})?|(?:\\d{1,4})(?:\\.\\d{1,2})?)$");
    private static final BigDecimal MAX_PRICE = new BigDecimal("10000");

    @Override
    public Result<PageResult<MenuItemDto>> list(DataRequest request) {
        Map<String, Object> data = safeData(request);
        Integer restaurateurId = toInteger(data.get("restaurateurId"));
        if (restaurateurId == null) {
            return Result.error("restaurateurId is required");
        }

        int page = request != null && request.getPage() != null ? Math.max(request.getPage(), 1) : 1;
        int size = request != null && request.getSize() != null ? Math.max(request.getSize(), 1) : 12;
        int offset = (page - 1) * size;

        String keyword = trimToNull(data.get("keyword"));
        String category = trimToNull(data.get("category"));
        String status = trimToNull(data.get("status"));
        String sortByRaw = trimToNull(data.get("sortBy"));
        String sortOrderRaw = trimToNull(data.get("sortOrder"));
        String sortBy = StringUtils.hasText(sortByRaw) ? sortByRaw : "created_at";
        String sortOrder = "ASC".equalsIgnoreCase(sortOrderRaw) ? "ASC" : "DESC";

        List<MenuItem> items = menuItemMapper.pageByRestaurateur(restaurateurId, keyword, category, status, sortBy, sortOrder, offset, size);
        long total = menuItemMapper.countByRestaurateur(restaurateurId, keyword, category, status);
        List<MenuItemDto> records = items.stream().map(this::toDto).collect(Collectors.toList());
        return Result.success(new PageResult<>(total, records));
    }

    @Override
    public Result<MenuItemDto> detail(DataRequest request) {
        Map<String, Object> data = safeData(request);
        Integer id = toInteger(data.get("id"));
        Integer restaurateurId = toInteger(data.get("restaurateurId"));
        if (id == null || restaurateurId == null) {
            return Result.error("missing id or restaurateurId");
        }

        MenuItem item = menuItemMapper.findById(id);
        if (item == null || Boolean.TRUE.equals(item.getDeleted()) || !Objects.equals(item.getRestaurateurId(), restaurateurId)) {
            return Result.error("menu item not found");
        }
        return Result.success(toDto(item));
    }

    @Override
    public Result<List<String>> listCategories(DataRequest request) {
        Map<String, Object> data = safeData(request);
        Integer restaurateurId = toInteger(data.get("restaurateurId"));
        if (restaurateurId == null) {
            return Result.error("restaurateurId is required");
        }
        List<String> categories = menuItemMapper.listCategories(restaurateurId);
        return Result.success(CollectionUtils.isEmpty(categories) ? Collections.emptyList() : categories);
    }

    @Override
    public Result<Integer> create(DataRequest request) {
        Map<String, Object> data = safeData(request);
        Integer restaurateurId = toInteger(data.get("restaurateurId"));
        if (restaurateurId == null) {
            return Result.error("restaurateurId is required");
        }

        MenuItem item;
        try {
            item = buildMenuItemFromRequest(data, false);
        } catch (IllegalArgumentException ex) {
            return Result.error(ex.getMessage());
        }
        item.setImageUrl(normalizeImageUrl(item.getImageUrl()));

        MenuItem existing = menuItemMapper.findByRestaurateurAndName(restaurateurId, item.getName());
        if (existing != null && !Boolean.TRUE.equals(existing.getDeleted())) {
            return Result.error("menu item already exists");
        }

        item.setRestaurateurId(restaurateurId);
        item.setDeleted(Boolean.FALSE);
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(item.getCreatedAt());
        menuItemMapper.insert(item);
        return Result.success("created", item.getId());
    }

    @Override
    public Result<Void> update(DataRequest request) {
        Map<String, Object> data = safeData(request);
        Integer id = toInteger(data.get("id"));
        Integer restaurateurId = toInteger(data.get("restaurateurId"));
        if (id == null || restaurateurId == null) {
            return Result.error("missing id or restaurateurId");
        }

        MenuItem existing = menuItemMapper.findById(id);
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted()) || !Objects.equals(existing.getRestaurateurId(), restaurateurId)) {
            return Result.error("menu item not found");
        }

        MenuItem payload;
        try {
            payload = buildMenuItemFromRequest(data, true);
        } catch (IllegalArgumentException ex) {
            return Result.error(ex.getMessage());
        }
        payload.setImageUrl(normalizeImageUrl(payload.getImageUrl()));

        String oldImage = existing.getImageUrl();
        boolean imageChanged = StringUtils.hasText(payload.getImageUrl()) && !Objects.equals(payload.getImageUrl(), oldImage);

        existing.setName(payload.getName());
        existing.setCategory(payload.getCategory());
        existing.setPrice(payload.getPrice());
        existing.setDescription(payload.getDescription());
        existing.setStatus(StringUtils.hasText(payload.getStatus()) ? payload.getStatus() : existing.getStatus());
        existing.setImageUrl(StringUtils.hasText(payload.getImageUrl()) ? payload.getImageUrl() : existing.getImageUrl());
        existing.setUpdatedAt(LocalDateTime.now());

        menuItemMapper.update(existing);

        if (imageChanged && StringUtils.hasText(oldImage) && !DEFAULT_IMAGE.equals(oldImage)) {
            removeImageFile(oldImage);
        }
        return Result.success("updated");
    }

    @Override
    public Result<Void> delete(DataRequest request) {
        Map<String, Object> data = safeData(request);
        Integer id = toInteger(data.get("id"));
        Integer restaurateurId = toInteger(data.get("restaurateurId"));
        if (id == null || restaurateurId == null) {
            return Result.error("missing id or restaurateurId");
        }

        MenuItem target = menuItemMapper.findById(id);
        if (target == null || Boolean.TRUE.equals(target.getDeleted()) || !Objects.equals(target.getRestaurateurId(), restaurateurId)) {
            return Result.error("menu item not found");
        }

        target.setDeleted(Boolean.TRUE);
        target.setUpdatedAt(LocalDateTime.now());
        menuItemMapper.update(target);
        removeImageFile(target.getImageUrl());
        return Result.success("deleted");
    }

    @Override
    public Result<Map<String, Object>> uploadImage(DataRequest request, MultipartFile file) {
        Map<String, Object> data = safeData(request);
        Integer itemId = toInteger(data.get("id"));
        Integer restaurateurId = toInteger(data.get("restaurateurId"));
        String dishName = toString(data.get("name"));

        if (itemId == null || restaurateurId == null) {
            return Result.error("missing id or restaurateurId");
        }

        MenuItem item = menuItemMapper.findById(itemId);
        if (item == null || Boolean.TRUE.equals(item.getDeleted()) || !Objects.equals(item.getRestaurateurId(), restaurateurId)) {
            return Result.error("menu item not found");
        }

        try {
            String imageUrl = storeDishImage(file, itemId, dishName);
            item.setImageUrl(imageUrl);
            item.setUpdatedAt(LocalDateTime.now());
            menuItemMapper.update(item);
            return Result.success("upload success", buildImageResponse(imageUrl));
        } catch (IllegalArgumentException | IOException ex) {
            return Result.error(ex.getMessage());
        }
    }

    private MenuItem buildMenuItemFromRequest(Map<String, Object> data, boolean update) {
        String name = trimToNull(data.get("name"));
        String category = trimToNull(data.get("category"));
        String status = trimToNull(data.get("status"));
        String priceRaw = trimToNull(data.get("price"));
        String imageUrl = trimToNull(data.get("imageUrl"));
        String description = trimToNull(data.get("description"));

        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("name is required");
        }
        if (name.length() > 100) {
            throw new IllegalArgumentException("name length cannot exceed 100 characters");
        }
        if (!StringUtils.hasText(status)) {
            status = "ON_SHELF";
        }

        if (!StringUtils.hasText(priceRaw)) {
            throw new IllegalArgumentException("price is required");
        }
        if (!PRICE_PATTERN.matcher(priceRaw).matches()) {
            throw new IllegalArgumentException("price must be between 0.01 and 10000, keep two decimals at most");
        }

        BigDecimal price = new BigDecimal(priceRaw).setScale(2, RoundingMode.HALF_UP);
        if (price.compareTo(BigDecimal.ZERO) <= 0 || price.compareTo(MAX_PRICE) > 0) {
            throw new IllegalArgumentException("price must be greater than 0 and less than or equal to 10000");
        }

        MenuItem item = new MenuItem();
        item.setName(name);
        item.setCategory(category);
        item.setPrice(price);
        item.setDescription(description);
        item.setStatus(status);
        if (StringUtils.hasText(imageUrl)) {
            item.setImageUrl(imageUrl);
        } else if (!update) {
            item.setImageUrl(DEFAULT_IMAGE);
        }
        return item;
    }

    private String storeDishImage(MultipartFile file, Integer itemId, String dishName) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file cannot be empty");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        }
        if (StringUtils.hasText(extension) && !ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("unsupported image format");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("file exceeds 5MB limit");
        }

        Files.createDirectories(UPLOAD_ROOT);

        String sanitizedName = sanitizeDishName(dishName);
        String newFilename = buildFileName(sanitizedName, itemId, extension);
        Path filePath = UPLOAD_ROOT.resolve(newFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        String relativePath = "/" + UPLOAD_DIR + "/" + newFilename;
        return normalizeImageUrl(relativePath);
    }

    private String buildFileName(String sanitizedName, Integer itemId, String extension) {
        String ext = StringUtils.hasText(extension) ? extension : "png";
        return sanitizedName + "-" + itemId + "." + ext;
    }

    private Map<String, Object> buildImageResponse(String imageUrl) {
        Map<String, Object> payload = new HashMap<>();
        String normalized = normalizeImageUrl(imageUrl);
        if (StringUtils.hasText(normalized)) {
            payload.put("url", normalized);
            payload.put("path", normalized.startsWith("/") ? normalized.substring(1) : normalized);
        } else {
            payload.put("url", DEFAULT_IMAGE);
        }
        return payload;
    }

    private String normalizeImageUrl(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String candidate = raw.trim().replace("\\", "/");
        if (candidate.startsWith("http://") || candidate.startsWith("https://") || candidate.startsWith("blob:")) {
            return candidate;
        }
        if (!candidate.startsWith("/")) {
            candidate = "/" + candidate;
        }
        return candidate;
    }

    private Path resolveStoragePath(String imageUrl) {
        String normalized = normalizeImageUrl(imageUrl);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        String relative = normalized.startsWith("/") ? normalized.substring(1) : normalized;
        return Paths.get(System.getProperty("user.dir")).resolve(relative);
    }

    private String sanitizeDishName(String dishName) {
        String value = StringUtils.hasText(dishName) ? dishName : "dish";
        String sanitized = value.replaceAll("[^A-Za-z0-9]", "");
        if (!StringUtils.hasText(sanitized)) {
            sanitized = "dish";
        }
        return sanitized.length() > 20 ? sanitized.substring(0, 20) : sanitized;
    }

    private void removeImageFile(String imageUrl) {
        if (!StringUtils.hasText(imageUrl) || DEFAULT_IMAGE.equals(imageUrl)) {
            return;
        }
        try {
            Path filePath = resolveStoragePath(imageUrl);
            if (filePath != null && Files.exists(filePath)) {
                Files.delete(filePath);
            }
        } catch (Exception ignored) {
        }
    }

    private MenuItemDto toDto(MenuItem item) {
        return MenuItemDto.builder()
            .id(item.getId())
            .name(item.getName())
            .category(item.getCategory())
            .price(item.getPrice())
            .imageUrl(normalizeImageUrl(item.getImageUrl()))
            .description(item.getDescription())
            .status(item.getStatus())
            .createdAt(item.getCreatedAt())
            .updatedAt(item.getUpdatedAt())
            .build();
    }

    private Map<String, Object> safeData(DataRequest request) {
        return RequestDataHelper.resolve(request);
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer integer) {
            return integer;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String trimToNull(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private String toString(Object value) {
        return value == null ? null : value.toString();
    }
}

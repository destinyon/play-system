package com.example.service.serviceImpl;

import com.example.common.DataRequest;
import com.example.common.Result;
import com.example.dto.MenuItemDto;
import com.example.dto.PageResult;
import com.example.entity.MenuItem;
import com.example.mapper.MenuItemMapper;
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
	public Result list(DataRequest request) {
		Map<String, Object> data = safeData(request);
		Integer restaurateurId = toInteger(data.get("restaurateurId"));
		if (restaurateurId == null) {
			return Result.error("restaurateurId 不能为空");
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
	public Result detail(DataRequest request) {
		Map<String, Object> data = safeData(request);
		Integer id = toInteger(data.get("id"));
		Integer restaurateurId = toInteger(data.get("restaurateurId"));
		if (id == null || restaurateurId == null) {
			return Result.error("参数不完整");
		}

		MenuItem item = menuItemMapper.findById(id);
		if (item == null || Boolean.TRUE.equals(item.getDeleted()) || !Objects.equals(item.getRestaurateurId(), restaurateurId)) {
			return Result.error("未找到菜品");
		}
		return Result.success(toDto(item));
	}

	@Override
	public Result listCategories(DataRequest request) {
		Map<String, Object> data = safeData(request);
		Integer restaurateurId = toInteger(data.get("restaurateurId"));
		if (restaurateurId == null) {
			return Result.error("restaurateurId 不能为空");
		}
		List<String> categories = menuItemMapper.listCategories(restaurateurId);
		return Result.success(CollectionUtils.isEmpty(categories) ? Collections.emptyList() : categories);
	}

	@Override
	public Result create(DataRequest request) {
		Map<String, Object> data = safeData(request);
		Integer restaurateurId = toInteger(data.get("restaurateurId"));
		if (restaurateurId == null) {
			return Result.error("restaurateurId 不能为空");
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
			return Result.error("该菜品已存在");
		}

		item.setRestaurateurId(restaurateurId);
		item.setDeleted(Boolean.FALSE);
		item.setCreatedAt(LocalDateTime.now());
		item.setUpdatedAt(item.getCreatedAt());
		menuItemMapper.insert(item);
		return Result.success("创建成功", item.getId());
	}

	@Override
	public Result update(DataRequest request) {
		Map<String, Object> data = safeData(request);
		Integer id = toInteger(data.get("id"));
		Integer restaurateurId = toInteger(data.get("restaurateurId"));
		if (id == null || restaurateurId == null) {
			return Result.error("参数不完整");
		}

		MenuItem existing = menuItemMapper.findById(id);
		if (existing == null || Boolean.TRUE.equals(existing.getDeleted()) || !Objects.equals(existing.getRestaurateurId(), restaurateurId)) {
			return Result.error("菜单不存在或已删除");
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

		return Result.success("更新成功");
	}

	@Override
	public Result delete(DataRequest request) {
		Map<String, Object> data = safeData(request);
		Integer id = toInteger(data.get("id"));
		Integer restaurateurId = toInteger(data.get("restaurateurId"));
		if (id == null || restaurateurId == null) {
			return Result.error("参数不完整");
		}

		MenuItem existing = menuItemMapper.findById(id);
		if (existing == null || Boolean.TRUE.equals(existing.getDeleted()) || !Objects.equals(existing.getRestaurateurId(), restaurateurId)) {
			return Result.error("菜单不存在或已删除");
		}

		if (StringUtils.hasText(existing.getImageUrl()) && !DEFAULT_IMAGE.equals(existing.getImageUrl())) {
			removeImageFile(existing.getImageUrl());
		}
		menuItemMapper.softDelete(id, restaurateurId);
		return Result.success("删除成功");
	}

	@Override
	public Result uploadImage(DataRequest request, MultipartFile file) {
		Map<String, Object> data = safeData(request);
		Integer restaurateurId = toInteger(data.get("restaurateurId"));
		Integer itemId = toInteger(data.get("itemId"));
		String dishName = trimToNull(data.get("dishName"));

		if (restaurateurId == null || itemId == null) {
			return Result.error("餐厅或菜品信息缺失");
		}
		if (file == null || file.isEmpty()) {
			return Result.error("文件不能为空");
		}

		MenuItem target = menuItemMapper.findById(itemId);
		if (target == null || Boolean.TRUE.equals(target.getDeleted()) || !Objects.equals(target.getRestaurateurId(), restaurateurId)) {
			return Result.error("未找到对应的菜品记录");
		}

		String previousImage = target.getImageUrl();
		String effectiveName = StringUtils.hasText(dishName) ? dishName : target.getName();

		String relativePath;
		try {
			relativePath = storeImageFile(file, effectiveName, itemId);
		} catch (IllegalArgumentException ex) {
			return Result.error(ex.getMessage());
		} catch (IOException ex) {
			return Result.error("文件保存失败: " + ex.getMessage());
		}

		menuItemMapper.updateImage(itemId, restaurateurId, relativePath, LocalDateTime.now());

		if (StringUtils.hasText(previousImage) && !Objects.equals(previousImage, relativePath) && !DEFAULT_IMAGE.equals(previousImage)) {
			removeImageFile(previousImage);
		}

		return Result.success(buildImageResponse(relativePath));
	}

	private MenuItem buildMenuItemFromRequest(Map<String, Object> data, boolean isUpdate) {
		String name = trimToNull(data.get("name"));
		if (!StringUtils.hasText(name)) {
			throw new IllegalArgumentException("菜品名称不能为空");
		}
		if (name.length() > 50) {
			throw new IllegalArgumentException("菜品名称不能超过50个字符");
		}

		BigDecimal price = parsePrice(data.get("price"));
		if (price == null) {
			throw new IllegalArgumentException("价格必须填写");
		}

		MenuItem item = new MenuItem();
		item.setId(toInteger(data.get("id")));
		item.setName(name);
		String category = trimToNull(data.get("category"));
		item.setCategory(category);
		item.setPrice(price);
		item.setDescription(toString(data.get("description")));
		String imageUrl = trimToNull(data.get("imageUrl"));
		item.setImageUrl(normalizeImageUrl(imageUrl));
		String status = trimToNull(data.get("status"));
		item.setStatus(StringUtils.hasText(status) ? status : (isUpdate ? null : "ON_SHELF"));
		return item;
	}

	private BigDecimal parsePrice(Object rawPrice) {
		if (rawPrice == null) {
			return null;
		}
		String priceStr = rawPrice.toString().trim();
		if (!PRICE_PATTERN.matcher(priceStr).matches()) {
			throw new IllegalArgumentException("价格必须为正数，且不超过10000，最多两位小数");
		}
		BigDecimal price = new BigDecimal(priceStr).setScale(2, RoundingMode.HALF_UP);
		if (price.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("价格必须大于0");
		}
		if (price.compareTo(MAX_PRICE) > 0) {
			throw new IllegalArgumentException("价格不能超过10000");
		}
		return price;
	}

	private String storeImageFile(MultipartFile file, String dishName, Integer itemId) throws IOException {
		String originalFilename = file.getOriginalFilename();
		if (!StringUtils.hasText(originalFilename)) {
			throw new IllegalArgumentException("文件名无效");
		}

		String filename = Objects.requireNonNull(originalFilename);
		String extension = "";
		int dotIndex = filename.lastIndexOf('.');
		if (dotIndex > 0) {
			extension = filename.substring(dotIndex + 1).toLowerCase();
		}
		if (!ALLOWED_EXTENSIONS.contains(extension)) {
			throw new IllegalArgumentException("不支持的文件格式，仅支持: " + String.join(", ", ALLOWED_EXTENSIONS));
		}
		if (file.getSize() > 5 * 1024 * 1024) {
			throw new IllegalArgumentException("文件大小不能超过5MB");
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
		String sanitized = value.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", "");
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
			// ignore io errors when cleaning up legacy files
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
		if (request == null || request.getData() == null) {
			return Collections.emptyMap();
		}
		return request.getData();
	}

	private Integer toInteger(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Integer) {
			return (Integer) value;
		}
		if (value instanceof Number) {
			return ((Number) value).intValue();
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

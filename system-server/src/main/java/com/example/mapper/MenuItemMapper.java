package com.example.mapper;

import com.example.entity.MenuItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MenuItemMapper {

    int insert(MenuItem item);

    int update(MenuItem item);

    MenuItem findById(@Param("id") Integer id);

    List<MenuItem> pageByRestaurateur(@Param("restaurateurId") Integer restaurateurId,
                                      @Param("keyword") String keyword,
                                      @Param("category") String category,
                                      @Param("status") String status,
                                      @Param("sortBy") String sortBy,
                                      @Param("sortOrder") String sortOrder,
                                      @Param("offset") Integer offset,
                                      @Param("limit") Integer limit);

    Long countByRestaurateur(@Param("restaurateurId") Integer restaurateurId,
                             @Param("keyword") String keyword,
                             @Param("category") String category,
                             @Param("status") String status);

    MenuItem findByRestaurateurAndName(@Param("restaurateurId") Integer restaurateurId,
                                       @Param("name") String name);

    List<String> listCategories(@Param("restaurateurId") Integer restaurateurId);

    int softDelete(@Param("id") Integer id,
                   @Param("restaurateurId") Integer restaurateurId);

    int updateImage(@Param("id") Integer id,
                    @Param("restaurateurId") Integer restaurateurId,
                    @Param("imageUrl") String imageUrl,
                    @Param("updatedAt") LocalDateTime updatedAt);
}

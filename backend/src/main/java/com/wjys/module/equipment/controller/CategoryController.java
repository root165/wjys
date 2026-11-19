package com.wjys.module.equipment.controller;

import com.wjys.common.annotation.OperLog;
import com.wjys.common.model.ApiResponse;
import com.wjys.module.equipment.dto.SaveCategoryRequest;
import com.wjys.module.equipment.service.CategoryService;
import com.wjys.module.equipment.vo.CategoryVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/equipment/categories")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('equipment:view')")
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping
    public ApiResponse<List<CategoryVO>> list() { return ApiResponse.success(categoryService.list()); }

    @PostMapping
    @PreAuthorize("hasAuthority('equipment:write') or hasRole('system_admin')")
    @OperLog("创建设备分类")
    public ApiResponse<CategoryVO> create(@Valid @RequestBody SaveCategoryRequest request) { return ApiResponse.success("设备分类创建成功", categoryService.create(request)); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('equipment:write') or hasRole('system_admin')")
    @OperLog("编辑设备分类")
    public ApiResponse<CategoryVO> update(@PathVariable Long id, @Valid @RequestBody SaveCategoryRequest request) { return ApiResponse.success("设备分类已更新", categoryService.update(id, request)); }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('equipment:write') or hasRole('system_admin')")
    @OperLog("删除设备分类")
    public ApiResponse<Void> delete(@PathVariable Long id) { categoryService.delete(id); return ApiResponse.success("设备分类已删除", null); }
}

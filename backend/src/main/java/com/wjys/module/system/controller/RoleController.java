package com.wjys.module.system.controller;

import com.wjys.common.annotation.OperLog;
import com.wjys.common.model.ApiResponse;
import com.wjys.module.system.dto.CreateRoleRequest;
import com.wjys.module.system.dto.UpdateRoleMenusRequest;
import com.wjys.module.system.service.RoleService;
import com.wjys.module.system.vo.RoleVO;
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
@RequestMapping("/api/system/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('system_admin')")
public class RoleController {
    private final RoleService roleService;

    @GetMapping
    public ApiResponse<List<RoleVO>> list() { return ApiResponse.success(roleService.list()); }

    @PostMapping
    @OperLog("创建角色")
    public ApiResponse<RoleVO> create(@Valid @RequestBody CreateRoleRequest request) { return ApiResponse.success("角色创建成功", roleService.create(request)); }

    @PutMapping("/{id}")
    @OperLog("编辑角色")
    public ApiResponse<RoleVO> update(@PathVariable Long id, @Valid @RequestBody CreateRoleRequest request) { return ApiResponse.success("角色已更新", roleService.update(id, request)); }

    @DeleteMapping("/{id}")
    @OperLog("删除角色")
    public ApiResponse<Void> delete(@PathVariable Long id) { roleService.delete(id); return ApiResponse.success("角色已删除", null); }

    @PutMapping("/{id}/menus")
    @OperLog("配置角色菜单")
    public ApiResponse<RoleVO> updateMenus(@PathVariable Long id, @Valid @RequestBody UpdateRoleMenusRequest request) { return ApiResponse.success(roleService.updateMenus(id, request)); }
}

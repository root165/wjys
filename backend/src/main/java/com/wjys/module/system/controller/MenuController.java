package com.wjys.module.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wjys.common.model.ApiResponse;
import com.wjys.module.system.entity.SysMenu;
import com.wjys.module.system.mapper.SysMenuMapper;
import com.wjys.module.system.vo.SystemMenuVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system/menus")
@RequiredArgsConstructor
@PreAuthorize("hasRole('system_admin')")
public class MenuController {
    private final SysMenuMapper menuMapper;

    @GetMapping
    public ApiResponse<List<SystemMenuVO>> list() {
        return ApiResponse.success(menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getStatus, 1)
                .orderByAsc(SysMenu::getSortNo)
                .orderByAsc(SysMenu::getId))
                .stream()
                .map(menu -> new SystemMenuVO(menu.getId(), menu.getParentId(), menu.getMenuName(), menu.getRoutePath(), menu.getComponent(), menu.getPermissionCode(), menu.getMenuType(), menu.getSortNo()))
                .toList());
    }
}

package com.wjys.module.system.controller;

import com.wjys.common.model.ApiResponse;
import com.wjys.common.model.PageResult;
import com.wjys.common.annotation.OperLog;
import com.wjys.module.system.dto.ChangeUserStatusRequest;
import com.wjys.module.system.dto.CreateUserRequest;
import com.wjys.module.system.dto.UpdateUserRolesRequest;
import com.wjys.module.system.service.UserService;
import com.wjys.module.system.vo.UserListItemVO;
import com.wjys.security.WjysPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/users")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('system:user:list')")
public class UserController {
    private final UserService userService;

    @GetMapping
    public ApiResponse<PageResult<UserListItemVO>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(userService.page(pageNum, pageSize, keyword));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:user:create') or hasRole('system_admin')")
    @OperLog("创建用户")
    public ApiResponse<UserListItemVO> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.success("用户创建成功", userService.create(request));
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('system_admin')")
    @OperLog("配置用户角色")
    public ApiResponse<Void> updateRoles(@PathVariable Long id, @Valid @RequestBody UpdateUserRolesRequest request) {
        userService.updateRoles(id, request);
        return ApiResponse.success(null);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('system:user:status') or hasRole('system_admin')")
    @OperLog("变更用户状态")
    public ApiResponse<Void> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChangeUserStatusRequest request,
            @AuthenticationPrincipal WjysPrincipal principal
    ) {
        userService.changeStatus(id, request, principal.id());
        return ApiResponse.success(null);
    }
}

package com.wjys.module.system.controller;

import com.wjys.common.model.ApiResponse;
import com.wjys.module.system.dto.LoginRequest;
import com.wjys.module.system.dto.RefreshTokenRequest;
import com.wjys.module.system.dto.ChangePasswordRequest;
import com.wjys.module.system.dto.UpdateProfileRequest;
import com.wjys.module.system.entity.SysUser;
import com.wjys.module.system.mapper.SysUserMapper;
import com.wjys.module.system.entity.SysMenu;
import com.wjys.module.system.mapper.SysMenuMapper;
import com.wjys.module.system.service.AuthService;
import com.wjys.module.system.vo.AuthInfoVO;
import com.wjys.module.system.vo.MenuVO;
import com.wjys.module.system.vo.TokenPairVO;
import com.wjys.module.system.vo.UserInfoVO;
import com.wjys.security.JwtTokenService;
import com.wjys.security.WjysPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final JwtTokenService tokenService;
    private final SysMenuMapper menuMapper;
    private final SysUserMapper userMapper;

    @PostMapping("/login")
    public ApiResponse<TokenPairVO> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenPairVO> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader("Authorization") String authorization) {
        authService.logout(authorization.substring("Bearer ".length()));
        return ApiResponse.success(null);
    }

    @GetMapping("/info")
    public ApiResponse<AuthInfoVO> info(@AuthenticationPrincipal WjysPrincipal principal) {
        SysUser current = userMapper.selectById(principal.id());
        UserInfoVO user = toUserInfo(current, principal);
        List<MenuVO> menus = menuMapper.selectByUserId(principal.id()).stream()
                .map(this::toMenu)
                .toList();
        return ApiResponse.success(new AuthInfoVO(user, menus));
    }

    @PutMapping("/profile")
    public ApiResponse<UserInfoVO> updateProfile(@AuthenticationPrincipal WjysPrincipal principal,
                                                  @Valid @RequestBody UpdateProfileRequest request) {
        SysUser current = authService.updateProfile(principal.id(), request);
        return ApiResponse.success("个人资料已保存", toUserInfo(current, principal));
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal WjysPrincipal principal,
                                             @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(principal.id(), request);
        return ApiResponse.success("密码已修改，请重新登录", null);
    }

    @GetMapping("/avatar")
    public ResponseEntity<byte[]> avatar(@AuthenticationPrincipal WjysPrincipal principal) {
        SysUser current = userMapper.selectById(principal.id());
        if (current == null || current.getAvatarFileId() == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(authService.avatarContentType(current.getAvatarFileId())))
                .cacheControl(CacheControl.noCache())
                .body(authService.avatarBytes(current.getAvatarFileId()));
    }

    private UserInfoVO toUserInfo(SysUser user, WjysPrincipal principal) {
        return new UserInfoVO(user.getId(), user.getUsername(), user.getNickname(), user.getPhone(),
                user.getEmail(), user.getAvatarFileId(), principal.roles(), principal.permissions());
    }

    private MenuVO toMenu(SysMenu menu) {
        return new MenuVO(menu.getMenuName(), menu.getRoutePath(), menu.getComponent(), menu.getPermissionCode());
    }
}

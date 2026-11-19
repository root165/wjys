package com.wjys.module.system.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wjys.common.exception.BusinessException;
import com.wjys.module.system.dto.LoginRequest;
import com.wjys.module.system.dto.ChangePasswordRequest;
import com.wjys.module.system.dto.UpdateProfileRequest;
import com.wjys.module.system.entity.SysUser;
import com.wjys.module.system.mapper.SysUserMapper;
import com.wjys.module.system.vo.TokenPairVO;
import com.wjys.security.JwtTokenService;
import com.wjys.security.TokenBlacklistService;
import com.wjys.security.WjysPrincipal;
import com.wjys.security.WjysUserDetailsService;
import com.wjys.support.file.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final WjysUserDetailsService userDetailsService;
    private final JwtTokenService tokenService;
    private final TokenBlacklistService blacklistService;
    private final FileService fileService;

    @Transactional
    public TokenPairVO login(LoginRequest request) {
        SysUser user = userMapper.selectByUsername(request.username());
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "账号已停用");
        }
        userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, user.getId())
                .set(SysUser::getLastLoginTime, LocalDateTime.now()));
        return issuePair(userDetailsService.loadPrincipalById(user.getId()));
    }

    public TokenPairVO refresh(String refreshToken) {
        JwtTokenService.TokenClaims claims = parseRefresh(refreshToken);
        WjysPrincipal principal = userDetailsService.loadUserByUsername(claims.username()) instanceof WjysPrincipal loaded
                ? loaded : null;
        if (principal == null || !principal.isEnabled() || !principal.tokenVersion().equals(claims.tokenVersion())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "刷新令牌已失效");
        }
        blacklistService.revoke(claims.jti(), claims.expiresAt());
        return issuePair(principal);
    }

    @Transactional
    public SysUser updateProfile(Long userId, UpdateProfileRequest request) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(HttpStatus.NOT_FOUND, "用户不存在");
        user.setNickname(request.nickname().trim());
        user.setPhone(blankToNull(request.phone()));
        user.setEmail(blankToNull(request.email()));
        if (request.avatarFileId() != null) {
            fileService.bindAll(java.util.List.of(request.avatarFileId()), "USER_AVATAR", userId);
            user.setAvatarFileId(request.avatarFileId());
        }
        userMapper.updateById(user);
        return user;
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        SysUser user = userMapper.selectById(userId);
        if (user == null || !passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "当前密码不正确");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userMapper.updateById(user);
        userMapper.incrementTokenVersion(userId);
    }

    public byte[] avatarBytes(Long fileId) { return fileService.read(fileId); }

    public String avatarContentType(Long fileId) { return fileService.contentType(fileId); }

    public void logout(String accessToken) {
        JwtTokenService.TokenClaims claims = tokenService.parse(accessToken);
        blacklistService.revoke(claims.jti(), claims.expiresAt());
    }

    private JwtTokenService.TokenClaims parseRefresh(String refreshToken) {
        try {
            JwtTokenService.TokenClaims claims = tokenService.parse(refreshToken);
            if (!"refresh".equals(claims.type())) {
                throw new IllegalArgumentException("不是刷新令牌");
            }
            if (blacklistService.isRevoked(claims.jti())) {
                throw new IllegalArgumentException("刷新令牌已使用");
            }
            return claims;
        } catch (RuntimeException exception) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "刷新令牌无效或已过期");
        }
    }

    private TokenPairVO issuePair(WjysPrincipal principal) {
        JwtTokenService.IssuedToken access = tokenService.issueAccess(principal);
        JwtTokenService.IssuedToken refresh = tokenService.issueRefresh(principal);
        return new TokenPairVO(
                access.value(),
                refresh.value(),
                tokenService.getAccessTtl().toSeconds(),
                tokenService.getRefreshTtl().toSeconds()
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

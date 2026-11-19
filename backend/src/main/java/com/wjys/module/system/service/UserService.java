package com.wjys.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wjys.common.exception.BusinessException;
import com.wjys.common.model.PageResult;
import com.wjys.module.system.dto.ChangeUserStatusRequest;
import com.wjys.module.system.dto.CreateUserRequest;
import com.wjys.module.system.entity.SysUser;
import com.wjys.module.system.mapper.SysUserMapper;
import com.wjys.module.system.mapper.SysRoleMapper;
import com.wjys.module.system.mapper.SysUserRoleMapper;
import com.wjys.module.system.dto.UpdateUserRolesRequest;
import com.wjys.module.system.vo.UserListItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    public PageResult<UserListItemVO> page(long pageNum, long pageSize, String keyword) {
        // Do not create an empty nested condition for an unfiltered list. MyBatis-Plus
        // renders that shape as `where ()`, which breaks the count SQL optimizer and
        // makes the otherwise valid first-page request return HTTP 500.
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .orderByDesc(SysUser::getCreatedTime);
        if (!normalizedKeyword.isEmpty()) {
            wrapper.and(item -> item.like(SysUser::getUsername, normalizedKeyword)
                    .or()
                    .like(SysUser::getNickname, normalizedKeyword));
        }
        Page<SysUser> page = userMapper.selectPage(
                new Page<>(pageNum, Math.min(Math.max(pageSize, 1), 100)), wrapper);
        List<UserListItemVO> list = page.getRecords().stream()
                .map(user -> toVO(user))
                .toList();
        return new PageResult<>(page.getTotal(), pageNum, pageSize, list);
    }

    @Transactional
    public UserListItemVO create(CreateUserRequest request) {
        if (userMapper.selectByUsername(request.username()) != null) {
            throw new BusinessException(HttpStatus.CONFLICT, "用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname());
        user.setDeptId(request.deptId());
        user.setStatus(1);
        user.setTokenVersion(0);
        userMapper.insert(user);
        replaceRoles(user.getId(), request.roleIds());
        return toVO(user);
    }

    @Transactional
    public void updateRoles(Long userId, UpdateUserRolesRequest request) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(HttpStatus.NOT_FOUND, "用户不存在");
        replaceRoles(userId, request.roleIds());
        userMapper.incrementTokenVersion(userId);
    }

    @Transactional
    public void changeStatus(Long userId, ChangeUserStatusRequest request, Long operatorId) {
        if (userId.equals(operatorId) && request.status() != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "不能停用当前登录账号");
        }
        if (!request.status().equals(0) && !request.status().equals(1)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "状态只能是 0 或 1");
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "用户不存在");
        }
        user.setStatus(request.status());
        userMapper.updateById(user);
        if (request.status() == 0) {
            userMapper.incrementTokenVersion(userId);
        }
    }

    private void replaceRoles(Long userId, List<Long> roleIds) {
        List<Long> distinctRoleIds = roleIds == null ? List.of() : roleIds.stream().distinct().toList();
        if (!distinctRoleIds.isEmpty()) {
            var roles = roleMapper.selectBatchIds(distinctRoleIds);
            if (roles.size() != distinctRoleIds.size() || roles.stream().anyMatch(role -> !Integer.valueOf(1).equals(role.getStatus()))) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "存在无效角色");
            }
        }
        userRoleMapper.deleteByUserId(userId);
        if (!distinctRoleIds.isEmpty()) userRoleMapper.insertLinks(userId, distinctRoleIds);
    }

    private UserListItemVO toVO(SysUser user) {
        return new UserListItemVO(user.getId(), user.getUsername(), user.getNickname(), user.getStatus(), user.getDeptId(), userMapper.selectRoleIds(user.getId()));
    }
}

package com.wjys.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wjys.common.exception.BusinessException;
import com.wjys.module.system.dto.CreateRoleRequest;
import com.wjys.module.system.dto.UpdateRoleMenusRequest;
import com.wjys.module.system.entity.SysMenu;
import com.wjys.module.system.entity.SysRole;
import com.wjys.module.system.mapper.SysMenuMapper;
import com.wjys.module.system.mapper.SysRoleMapper;
import com.wjys.module.system.mapper.SysRoleMenuMapper;
import com.wjys.module.system.vo.RoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;

    public List<RoleVO> list() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId)).stream()
                .map(this::toVO).toList();
    }

    @Transactional
    public RoleVO create(CreateRoleRequest request) {
        validateRoleCode(request.roleCode(), null);
        SysRole role = new SysRole();
        role.setRoleCode(request.roleCode());
        role.setRoleName(request.roleName());
        role.setDataScope(request.dataScope());
        role.setStatus(request.status() == null ? 1 : request.status());
        roleMapper.insert(role);
        return toVO(role);
    }

    @Transactional
    public RoleVO update(Long id, CreateRoleRequest request) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) throw new BusinessException(HttpStatus.NOT_FOUND, "角色不存在");
        if ("system_admin".equals(role.getRoleCode()) && !"system_admin".equals(request.roleCode())) {
            throw new BusinessException(HttpStatus.CONFLICT, "系统管理员角色编码不能修改");
        }
        validateRoleCode(request.roleCode(), id);
        role.setRoleCode(request.roleCode());
        role.setRoleName(request.roleName());
        role.setDataScope(request.dataScope());
        if (!"system_admin".equals(role.getRoleCode()) || request.status() == null || request.status() == 1) {
            role.setStatus(request.status() == null ? role.getStatus() : request.status());
        }
        roleMapper.updateById(role);
        return toVO(role);
    }

    @Transactional
    public void delete(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) throw new BusinessException(HttpStatus.NOT_FOUND, "角色不存在");
        if ("system_admin".equals(role.getRoleCode())) throw new BusinessException(HttpStatus.CONFLICT, "系统管理员角色不能删除");
        if (roleMapper.countUsers(id) > 0) throw new BusinessException(HttpStatus.CONFLICT, "角色已分配给用户，不能删除");
        if (roleMapper.countMenus(id) > 0) throw new BusinessException(HttpStatus.CONFLICT, "角色仍配置了菜单权限，不能删除");
        roleMapper.deleteById(id);
    }

    @Transactional
    public RoleVO updateMenus(Long roleId, UpdateRoleMenusRequest request) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) throw new BusinessException(HttpStatus.NOT_FOUND, "角色不存在");
        List<SysMenu> menus = request.menuIds().isEmpty() ? List.of() : menuMapper.selectBatchIds(request.menuIds());
        if (menus.size() != request.menuIds().stream().distinct().count() || menus.stream().anyMatch(menu -> !Integer.valueOf(1).equals(menu.getStatus()))) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "存在无效菜单");
        }
        roleMenuMapper.deleteByRoleId(roleId);
        if (!request.menuIds().isEmpty()) roleMenuMapper.insertLinks(roleId, request.menuIds());
        return toVO(role);
    }

    private RoleVO toVO(SysRole role) {
        return new RoleVO(role.getId(), role.getRoleCode(), role.getRoleName(), role.getDataScope(), role.getStatus(), roleMapper.selectMenuIds(role.getId()));
    }

    private void validateRoleCode(String roleCode, Long selfId) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, roleCode);
        if (selfId != null) wrapper.ne(SysRole::getId, selfId);
        if (roleMapper.selectOne(wrapper) != null) throw new BusinessException(HttpStatus.CONFLICT, "角色编码已存在");
    }
}

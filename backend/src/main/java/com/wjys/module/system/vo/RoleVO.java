package com.wjys.module.system.vo;

import java.util.List;

public record RoleVO(Long id, String roleCode, String roleName, Integer dataScope, Integer status, List<Long> menuIds) {
}

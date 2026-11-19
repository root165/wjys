package com.wjys.module.system.vo;

import java.util.List;

public record UserListItemVO(Long id, String username, String nickname, Integer status, Long deptId, List<Long> roleIds) {
}

package com.wjys.module.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateUserRequest(
        @NotBlank(message = "用户名不能为空") @Size(max = 64) String username,
        @NotBlank(message = "初始密码不能为空") @Size(min = 8, max = 64) String password,
        @NotBlank(message = "姓名不能为空") @Size(max = 64) String nickname,
        Long deptId,
        List<Long> roleIds
) {
}

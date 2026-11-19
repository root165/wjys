package com.wjys.module.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateRoleRequest(@NotBlank String roleCode, @NotBlank String roleName, @Min(1) @Max(5) Integer dataScope, @Min(0) @Max(1) Integer status) {
}

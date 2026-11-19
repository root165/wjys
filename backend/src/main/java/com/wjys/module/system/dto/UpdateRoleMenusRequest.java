package com.wjys.module.system.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateRoleMenusRequest(@NotNull List<Long> menuIds) {
}

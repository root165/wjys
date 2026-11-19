package com.wjys.module.system.dto;

import jakarta.validation.constraints.NotNull;

public record ChangeUserStatusRequest(@NotNull(message = "状态不能为空") Integer status) {
}

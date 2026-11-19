package com.wjys.module.repair.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record DispatchRequest(@NotNull Long assigneeId, String remark, LocalDateTime expectFinishTime) {
}

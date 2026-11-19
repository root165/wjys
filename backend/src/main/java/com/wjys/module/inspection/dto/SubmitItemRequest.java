package com.wjys.module.inspection.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record SubmitItemRequest(@NotNull Long taskItemId, @NotNull Integer checkResult, String checkValue, String remark, LocalDateTime checkTime, List<Long> fileIds) { }

package com.wjys.module.repair.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

public record FinishRequest(@NotBlank String finishReason, @NotBlank String repairPlan, @NotBlank String finishResult,
                            @PositiveOrZero Integer downtimeMinutes, List<Long> fileIds) {
}

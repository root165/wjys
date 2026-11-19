package com.wjys.module.maintenance.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record SaveMaintenancePlanRequest(
        @NotBlank @Size(max = 64) String planCode,
        @NotBlank @Size(max = 160) String planName,
        @NotNull Long equipId,
        @NotNull @Min(0) @Max(3) Integer maintainType,
        @NotNull @Min(0) @Max(3) Integer cycleType,
        @NotNull @Min(1) Integer cycleValue,
        @Size(max = 500) String content,
        Long assigneeId,
        @NotNull LocalDate nextDate
) { }

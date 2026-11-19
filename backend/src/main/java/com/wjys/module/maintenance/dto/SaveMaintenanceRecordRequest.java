package com.wjys.module.maintenance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SaveMaintenanceRecordRequest(
        Long planId,
        @NotNull Long equipId,
        @NotNull LocalDate maintainDate,
        @NotBlank @Size(min = 10, max = 1000) String maintainContent,
        @NotNull @Min(0) @Max(1) Integer maintainResult,
        @DecimalMin("0.00") BigDecimal cost,
        @Min(0) Integer downtimeMinutes,
        @NotEmpty List<@NotNull Long> fileIds,
        @Size(max = 500) String remark
) { }

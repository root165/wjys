package com.wjys.module.inspection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record CreatePlanRequest(@NotBlank String planCode, @NotBlank String planName, @NotBlank String cycleType, String cycleValue, @NotNull LocalDate startDate, LocalDate endDate, LocalTime executeTime, String assignmentMode, Long assignmentDeptId, Long responsibleUserId, @NotEmpty List<Long> pointIds) { }

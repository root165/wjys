package com.wjys.module.inspection.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record GenerateTaskRequest(@NotNull LocalDate planTime) { }

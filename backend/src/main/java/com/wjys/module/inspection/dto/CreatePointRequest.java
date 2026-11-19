package com.wjys.module.inspection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePointRequest(@NotNull Long equipId, @NotBlank String pointCode, @NotBlank String pointName, String location) { }

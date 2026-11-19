package com.wjys.module.inspection.dto;

import jakarta.validation.constraints.NotNull;

public record ChangePlanStatusRequest(@NotNull Integer status) { }

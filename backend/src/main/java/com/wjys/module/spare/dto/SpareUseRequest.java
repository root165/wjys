package com.wjys.module.spare.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SpareUseRequest(@NotNull Long spareId, @NotNull @Positive Integer quantity, String remark) {
}

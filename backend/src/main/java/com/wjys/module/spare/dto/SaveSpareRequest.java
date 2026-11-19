package com.wjys.module.spare.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record SaveSpareRequest(@NotBlank String spareCode, @NotBlank String spareName, String specification,
                               @NotBlank String unit, @NotNull @PositiveOrZero Integer stockQty,
                               @NotNull @PositiveOrZero Integer warningQty,
                               @NotNull @DecimalMin("0.00") BigDecimal unitPrice, @NotNull Integer status) {
}

package com.wjys.module.inspection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateItemRequest(@NotNull Long pointId, @NotBlank String itemCode, @NotBlank String itemName, String checkMethod, @NotBlank String valueType, BigDecimal minValue, BigDecimal maxValue, String unit, String optionsJson, Integer requiredPhoto) { }

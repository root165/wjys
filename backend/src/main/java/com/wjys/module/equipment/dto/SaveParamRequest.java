package com.wjys.module.equipment.dto;

import jakarta.validation.constraints.NotBlank;

public record SaveParamRequest(@NotBlank String paramName, String paramValue, String unit, Integer sortNo) {
}

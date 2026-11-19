package com.wjys.module.system.dto;

import jakarta.validation.constraints.NotBlank;

public record SaveDictRequest(@NotBlank String dictType, @NotBlank String dictLabel, @NotBlank String dictValue, Integer sortNo) {
}

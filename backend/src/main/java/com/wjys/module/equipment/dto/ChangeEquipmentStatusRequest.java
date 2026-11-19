package com.wjys.module.equipment.dto;

import jakarta.validation.constraints.NotNull;

public record ChangeEquipmentStatusRequest(@NotNull Integer status, String sourceType, Long sourceId, String reason) { }

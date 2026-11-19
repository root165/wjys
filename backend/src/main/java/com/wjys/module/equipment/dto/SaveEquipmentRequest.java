package com.wjys.module.equipment.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record SaveEquipmentRequest(Long categoryId, @NotBlank String equipCode, @NotBlank String equipName, String model, String manufacturer, String location, Long responsibleUserId, LocalDate purchaseDate, LocalDate warrantyExpire, String description) { }

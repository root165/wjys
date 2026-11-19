package com.wjys.module.equipment.vo;

public record EquipmentListVO(Long id, Long categoryId, String equipCode, String equipName, String model, String location, Integer status, String statusLabel) { }

package com.wjys.module.equipment.dto;

public record EquipmentQuery(String keyword, Long categoryId, Integer status, long pageNum, long pageSize) { }

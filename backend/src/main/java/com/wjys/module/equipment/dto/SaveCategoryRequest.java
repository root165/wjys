package com.wjys.module.equipment.dto;

import jakarta.validation.constraints.NotBlank;

public record SaveCategoryRequest(Long parentId, @NotBlank String categoryName, Integer sortNo) { }

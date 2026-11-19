package com.wjys.module.system.dto;

import jakarta.validation.constraints.NotBlank;

public record SaveDeptRequest(Long parentId, @NotBlank String deptName, Integer sortNo) {
}
